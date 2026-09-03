#!/usr/bin/env bash
# 컨테이너 하드닝·노출 불변식 검사 (PLATFORM 2절, 3절).
# scripts/deploy.sh 가 배포 성공 직후 호출한다.
#
# 왜 필요한가:
#   deploy.sh 의 기존 검사는 전부 "네트워크"에 관한 것이었다. no-new-privileges 나
#   capability, 리소스 상한, 읽기전용 루트FS 가 배포 후에도 유지되는지는 아무도 보지
#   않았다. 이 값들은 compose 파일 하나를 빠뜨리면(예: refresh-riot-key.sh 가
#   hardening 오버레이 없이 backend 를 재생성) 조용히 사라지고, 컨테이너는 멀쩡히 뜬다.
#
# 설정 파일이 아니라 "돌고 있는 컨테이너"를 본다. 병합 결과가 맞아도 실제로 그 값으로
# 떠 있는지는 별개이기 때문이다(오버레이 누락은 정확히 그 차이로 나타난다).
#
# 종료 코드: 컨테이너 불변식이 깨지면 1. 호스트 자세(SSH)는 경고만 하고 실패시키지 않는다
#   — 서버에서 손으로 고쳐야 하는 항목이라 배포를 막는 것이 답이 아니다.
set -uo pipefail

COMPOSE_PROJECT=record_site
failed=0
warned=0

fail(){ echo "  [FAIL] $*" >&2; failed=1; }
warn(){ echo "  [WARN] $*" >&2; warned=1; }
ok(){   echo "  [ok]   $*"; }

# 루트FS 읽기전용을 요구하는 컨테이너와, 이유가 있어 면제된 컨테이너.
# 목록에 없는 컨테이너가 나타나면 실패시킨다 — 새 서비스를 추가하고 기대치를 정하지
# 않으면 "검사받지 않는 컨테이너"가 조용히 생기기 때문이다.
RO_REQUIRED="lol-backend lol-frontend lol-alloy lol-egress-proxy"
# 면제 사유(hardening/monitoring 오버레이 주석과 같은 내용):
#   lol-mysql      데이터 디렉터리 주변에 소켓·pid·tmp 를 쓴다
#   lol-redis      데이터 디렉터리에 쓴다
#   lol-nginx      certbot 갱신 반영을 위해 6시간마다 reload 하며 /var/run 에 쓴다
#   lol-certbot    갱신한 인증서를 볼륨에 쓴다
#   lol-prometheus / lol-grafana / lol-loki  각자 데이터 볼륨에 쓴다
RO_EXEMPT="lol-mysql lol-redis lol-nginx lol-certbot lol-prometheus lol-grafana lol-loki"

has_word(){ case " $2 " in *" $1 "*) return 0;; *) return 1;; esac; }

echo "[hardening] 컨테이너 불변식 검사"

mapfile -t containers < <(docker ps \
  --filter "label=com.docker.compose.project=$COMPOSE_PROJECT" \
  --format '{{.Names}}' 2>/dev/null | sort)

if [ "${#containers[@]}" -eq 0 ]; then
  echo "  [FAIL] 검사할 컨테이너가 없다 (compose 프로젝트 $COMPOSE_PROJECT)" >&2
  exit 1
fi

for c in "${containers[@]}"; do
  info=$(docker inspect "$c" 2>/dev/null) || { fail "$c: inspect 실패"; continue; }

  # no-new-privileges — setuid 바이너리를 통한 권한 상승 차단
  if ! printf '%s' "$info" | grep -q 'no-new-privileges:true'; then
    fail "$c: no-new-privileges 가 없다"
  fi

  # capability 전부 제거 후 필요한 것만 되살리기. CapDrop 에 ALL 이 있는지만 본다
  # (CapAdd 는 서비스마다 달라 여기서 판정하지 않는다 — 오버레이 주석에 근거가 있다).
  capdrop=$(docker inspect -f '{{json .HostConfig.CapDrop}}' "$c" 2>/dev/null)
  case "$capdrop" in
    *ALL*) ;;
    *) fail "$c: cap_drop ALL 이 없다 (현재: $capdrop)" ;;
  esac

  # 리소스 상한 — 없으면 침해나 폭주 하나가 호스트를 멈춘다
  mem=$(docker inspect -f '{{.HostConfig.Memory}}' "$c" 2>/dev/null)
  [ "${mem:-0}" -gt 0 ] 2>/dev/null || fail "$c: 메모리 상한이 없다"
  cpu=$(docker inspect -f '{{.HostConfig.NanoCpus}}' "$c" 2>/dev/null)
  [ "${cpu:-0}" -gt 0 ] 2>/dev/null || fail "$c: CPU 상한이 없다"

  # 루트 파일시스템 읽기전용
  ro=$(docker inspect -f '{{.HostConfig.ReadonlyRootfs}}' "$c" 2>/dev/null)
  if has_word "$c" "$RO_REQUIRED"; then
    [ "$ro" = "true" ] || fail "$c: read_only 루트FS 가 꺼져 있다"
  elif has_word "$c" "$RO_EXEMPT"; then
    :   # 사유는 위 목록 주석에 있다
  else
    fail "$c: read_only 기대치가 정해지지 않은 컨테이너다 — check-hardening.sh 의 목록에 추가하라"
  fi

  # tmpfs 로 여는 경로에는 noexec·nosuid·nodev 가 붙어야 한다.
  # 쓰기 가능한데 실행까지 되면 읽기전용 루트FS 의 의미가 없다 — 2026-06 침해가
  # 밀려난 곳이 정확히 /tmp 였다.
  tmpfs=$(docker inspect -f '{{json .HostConfig.Tmpfs}}' "$c" 2>/dev/null)
  if [ "$tmpfs" != "null" ] && [ -n "$tmpfs" ]; then
    for flag in noexec nosuid nodev; do
      printf '%s' "$tmpfs" | grep -q "$flag" || fail "$c: tmpfs 에 $flag 가 없다 ($tmpfs)"
    done
  fi
done
[ "$failed" -eq 0 ] && ok "컨테이너 ${#containers[@]}개 — nnp/cap/limits/read_only/tmpfs 모두 통과"

# ── 호스트에 열린 포트 ──
# 인터넷에 여는 포트는 443 과 리다이렉트용 80 뿐이다(PLATFORM 3절).
# 나머지는 127.0.0.1 에만 묶여 있어야 한다. dev 오버레이를 서버에서 잘못 올리면
# 여기서 걸린다(MySQL 3307 / Redis 6379 가 0.0.0.0 으로 열린다).
echo "[hardening] 호스트 포트 노출 검사"
bad_ports=$(docker ps --filter "label=com.docker.compose.project=$COMPOSE_PROJECT" \
              --format '{{.Names}} {{.Ports}}' 2>/dev/null \
            | grep -oE '0\.0\.0\.0:[0-9]+|\[::\]:[0-9]+' | grep -oE '[0-9]+$' \
            | sort -u | grep -vE '^(80|443)$' || true)
if [ -n "$bad_ports" ]; then
  fail "80/443 외의 포트가 모든 인터페이스에 열려 있다: $(echo "$bad_ports" | tr '\n' ' ')"
else
  ok "공개 포트는 80/443 뿐"
fi

# ── 호스트 SSH 자세 (경고만) ──
# CD 가 인터넷에서 SSH 로 붙으므로 22 번이 열려 있다 = 상시 브루트포스 대상이다.
# 서버에서 손으로 고쳐야 하는 항목이라 배포를 실패시키지는 않는다.
echo "[hardening] SSH 자세 검사(경고만)"
sshd_files=$(ls /etc/ssh/sshd_config /etc/ssh/sshd_config.d/*.conf 2>/dev/null || true)
if [ -z "$sshd_files" ]; then
  warn "sshd 설정을 읽을 수 없다 — 확인 불가(통과가 아니다)"
else
  # 마지막에 유효한 값이 아니라 "no 로 명시한 줄이 있는가"만 본다. 정확한 유효 정책은
  # root 권한의 `sshd -T` 로만 알 수 있어, 여기서는 명백한 누락만 잡는다.
  # shellcheck disable=SC2086
  if ! grep -rhiE '^[[:space:]]*PasswordAuthentication[[:space:]]+no' $sshd_files >/dev/null 2>&1; then
    warn "PasswordAuthentication no 가 명시돼 있지 않다 — 비밀번호 로그인이 열려 있을 수 있다"
  fi
  # shellcheck disable=SC2086
  if grep -rhiE '^[[:space:]]*PermitRootLogin[[:space:]]+(yes|without-password)' $sshd_files >/dev/null 2>&1; then
    warn "PermitRootLogin 이 허용으로 설정돼 있다"
  fi
  [ "$warned" -eq 0 ] && ok "SSH 자세 이상 없음(명시 기준)"
fi

if [ "$failed" -ne 0 ]; then
  echo "[hardening] 실패 — 위 항목을 고치기 전에는 이 배포를 신뢰할 수 없다" >&2
  exit 1
fi
echo "[hardening] 통과"
exit 0
