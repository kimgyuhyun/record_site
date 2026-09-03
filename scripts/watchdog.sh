#!/usr/bin/env bash
# 컨테이너 상태 + 침해 지표 주기 점검 워치독 (PLATFORM 9절 [절대]).
# cron 5분 주기로 돈다 — 등록은 scripts/deploy.sh 가 배포 성공 후 자동으로 한다.
#
# 왜 필요한가:
#   이 서버는 무인이다. 배포 시점에는 deploy.sh 의 IOC 게이트가 이미지를 검사하지만,
#   2026-06 OTT 침해는 둘 다 "실행 중 컨테이너에 런타임 주입"이었다 — 이미지는 깨끗했다.
#   즉 배포 시점 검사만으로는 이 유형을 영원히 못 잡는다. 여기가 그 빈틈을 메운다.
#
# 탐지 항목(오탐을 줄이려고 둘 다 만족하거나 이름이 정확히 일치할 때만 잡는다):
#   1) 컨테이너가 running 이 아님 (배포 실패·OOM·크래시)
#   2) 쓰기 가능 경로(/tmp, /dev/shm, /var/tmp)의 "1MB 초과 + 실행 비트" 파일
#      → 마이너 바이너리는 수 MB 다(2026-06 javae 는 7,022,816 bytes). 정상 앱은 이 경로에
#        대용량 실행물을 두지 않는다. backend 의 /tmp 는 JVM hsperfdata·톰캣 임시뿐이고
#        이 앱에는 파일 업로드 기능이 없어 대용량 임시파일이 생기지 않는다.
#   3) 알려진 마이너/드로퍼 파일명 (크기·권한 무관)
#   4) 스캔 자체가 실패함 — 아래 "조용히 눈머는 것" 참고
#
# 탐지 시: 네트워크 전부 분리 → 정지 → 알림. 증거 보존을 위해 rm 은 하지 않는다.
#   (restart policy 가 unless-stopped 라 명시적 stop 후에는 자동 재시작하지 않는다.)
#
# ⚠️ 조용히 눈머는 것을 막는 두 가지 (실제로 이 스크립트가 처음에 그랬다):
#   - `-size +1M` 은 busybox find 가 거부한다("invalid number '1M'"). alpine 기반
#     컨테이너(nginx, redis, frontend)가 전부 busybox 라 표현식이 통째로 실패했다.
#     그래서 바이트 단위(`+1048576c`)로 적는다 — GNU/busybox 양쪽이 받는 유일한 표기다.
#   - find 의 stderr 를 버리면 위 실패가 "탐지 0건"과 구분되지 않는다. 그래서 stderr 를
#     받아 두고 find 가 0 이 아닌 코드로 끝나면 그 자체를 사건으로 보고한다.
#
# set -e 를 쓰지 않는다: 컨테이너 하나에서 exec 가 실패해도 나머지 점검을 계속해야 한다.
set -uo pipefail

PROJECT_DIR="${PROJECT_DIR:-$HOME/record_site}"
# shellcheck source=scripts/notify.sh
. "$PROJECT_DIR/scripts/notify.sh"

COMPOSE_PROJECT=record_site
STATE_FILE="$PROJECT_DIR/logs/watchdog.state"

# 컨테이너 안에서 돌 스캔 스크립트. 없는 경로를 find 에 넘기면 그것만으로 실패 코드가
# 나오므로(read_only 컨테이너엔 /var/tmp 가 없다) 존재하는 경로만 골라서 넘긴다.
SCAN_SH='
paths=""
for p in /tmp /dev/shm /var/tmp; do
  [ -d "$p" ] && paths="$paths $p"
done
[ -z "$paths" ] && exit 0
find $paths -xdev -type f \( \( -size +1048576c -perm -0100 \) -o -name "xmrig*" -o -name javae -o -name minerd -o -name cpuminer -o -name kdevtmpfsi -o -name kinsing -o -name "grepb32*" \)
'

mkdir -p "$(dirname "$STATE_FILE")"
touch "$STATE_FILE"
findings_now=$(mktemp)
scan_err=$(mktemp)
trap 'rm -f "$findings_now" "$scan_err"' EXIT

# 컨테이너 목록은 compose 라벨로 얻는다 — 하드코딩하면 서비스가 늘 때마다
# 여기도 고쳐야 하고, 빠뜨려도 아무 경고가 없다(이 저장소가 이미 겪은 유형의 함정).
mapfile -t containers < <(docker ps -a \
  --filter "label=com.docker.compose.project=$COMPOSE_PROJECT" \
  --format '{{.Names}}' 2>/dev/null | sort)

if [ "${#containers[@]}" -eq 0 ]; then
  echo "sig=no-containers" >> "$findings_now"
fi

isolate() {
  local c="$1" nets n
  nets=$(docker inspect -f '{{range $k,$v := .NetworkSettings.Networks}}{{$k}} {{end}}' "$c" 2>/dev/null || true)
  for n in $nets; do
    docker network disconnect -f "$n" "$c" >/dev/null 2>&1 || true
  done
  docker stop "$c" >/dev/null 2>&1 || true
}

for c in "${containers[@]}"; do
  state=$(docker inspect -f '{{.State.Status}}' "$c" 2>/dev/null || echo unknown)

  if [ "$state" != "running" ]; then
    echo "sig=down:$c" >> "$findings_now"
    continue          # 떠 있지 않으면 파일 검사도 못 한다
  fi

  : > "$scan_err"
  scan=$(docker exec "$c" sh -c "$SCAN_SH" 2>"$scan_err")
  rc=$?

  if [ "$rc" -ne 0 ]; then
    # 스캔이 실패한 컨테이너는 "깨끗하다"가 아니라 "모른다"이다. 구분해서 보고한다.
    echo "sig=scanfail:$c" >> "$findings_now"
    continue
  fi

  hits=$(printf '%s\n' "$scan" | grep -v '^$' | head -5)
  if [ -n "$hits" ]; then
    echo "sig=ioc:$c" >> "$findings_now"
    # 격리를 알림보다 먼저 한다 — 알림이 실패해도 채굴·유출은 멈춰야 한다.
    isolate "$c"
    notify "침해 의심 — $c 격리함" \
"쓰기 가능 경로에서 마이너 지표가 발견되어 네트워크 분리 후 정지했습니다.
증거 보존을 위해 컨테이너는 삭제하지 않았습니다.

발견:
$hits

조치: docker logs $c / docker diff $c 로 확인하세요."
  fi
done

# ── 알림 중복 억제 ──
# 5분마다 같은 문구가 오면 사람이 경보를 무시하게 된다(PLATFORM 9절: 무시하는 경보는
# 없는 것보다 나쁘다). 상태가 "바뀐" 것만 보낸다.
sort -u "$findings_now" -o "$findings_now"

while read -r sig; do
  [ -n "$sig" ] || continue
  grep -qxF "$sig" "$STATE_FILE" && continue          # 이미 알린 문제
  case "$sig" in
    sig=down:*)
      notify "컨테이너 정지 — ${sig#sig=down:}" "running 상태가 아닙니다. docker ps -a 로 확인하세요." ;;
    sig=scanfail:*)
      notify "침해 스캔 실패 — ${sig#sig=scanfail:}" \
"이 컨테이너는 검사되지 않았습니다(깨끗한 것이 아니라 확인 불가). find 가 실패했습니다.
워치독이 이 컨테이너에 대해 눈이 먼 상태이므로 스캔 표현식을 점검하세요." ;;
    sig=no-containers)
      notify "스택 전체 부재" "compose 프로젝트 $COMPOSE_PROJECT 의 컨테이너가 하나도 없습니다." ;;
    sig=ioc:*) : ;;                                    # 위에서 이미 상세 알림을 보냈다
  esac
done < "$findings_now"

while read -r sig; do
  [ -n "$sig" ] || continue
  grep -qxF "$sig" "$findings_now" && continue        # 아직 진행 중
  notify "해소됨 — ${sig#sig=}" "이전에 보고된 문제가 더 이상 관측되지 않습니다."
done < "$STATE_FILE"

cp "$findings_now" "$STATE_FILE"
