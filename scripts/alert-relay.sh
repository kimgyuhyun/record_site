#!/usr/bin/env bash
# Prometheus 가 firing 으로 올린 경보를 사람에게 중계한다 (PLATFORM 9절).
# cron 5분 주기 — 등록은 scripts/deploy.sh 가 한다.
#
# 왜 필요한가:
#   monitoring/prometheus/alerts.yml 에 규칙이 있는데 Alertmanager 가 없다.
#   그래서 규칙이 firing 되어도 Prometheus /alerts 화면과 Grafana 목록에만 뜨고
#   아무도 부르지 않는다 — 무인 서버에서는 사실상 경보가 없는 것과 같다.
#   컨테이너를 하나 더 띄우는 대신(이 서버는 2 vCPU 다) watchdog.sh 가 이미 쓰는
#   발송 경로를 재사용한다.
#
# Alertmanager 를 안 쓰는 대가(알고 쓰는 것):
#   - 그룹핑·억제(inhibition)·무음(silence)이 없다. 현재 규칙이 4개라 필요 없다.
#   - 중복 억제는 아래 state 파일로 대신한다(같은 경보를 5분마다 재발송하지 않는다).
#   규칙이 늘거나 무음이 필요해지면 그때 Alertmanager 를 도입한다.
#
# ⚠️ 기준값 주의: alerts.yml 의 임계값(5xx 5%, p99 1s, 힙 90%)은 기준선 관측 없이 정해진
#    값이다(PLATFORM 9절 [상황]은 2주 이상 관측한 분위값 위에서 정하라고 한다).
#    지금까지는 아무 데도 안 갔으니 오탐이 드러나지 않았을 뿐이고, 이 스크립트를 켜는
#    순간부터는 오탐이 그대로 사람에게 간다. 2주 치 데이터가 쌓이면 임계값을 실측 기준으로
#    다시 잡을 것 — 그 전까지 오탐이 반복되면 임계값을 고치지, 이 릴레이를 끄지 않는다.
set -uo pipefail

PROJECT_DIR="${PROJECT_DIR:-$HOME/record_site}"
# shellcheck source=scripts/notify.sh
. "$PROJECT_DIR/scripts/notify.sh"

PROM_URL="${PROM_URL:-http://127.0.0.1:9090}"
STATE_FILE="$PROJECT_DIR/logs/alert-relay.state"

mkdir -p "$(dirname "$STATE_FILE")"
touch "$STATE_FILE"

# python3 는 우분투 클라우드 이미지에 cloud-init 의존으로 항상 들어 있다. 그래도 없을 때
# 조용히 죽으면 "경보가 없는 게 아니라 릴레이가 죽은 것"을 아무도 모른다 — 한 번은 알린다.
# 조건이 영구적이라 5분마다 재발송하지 않도록 마커로 한 번만 보낸다.
if ! command -v python3 >/dev/null 2>&1; then
  marker="$PROJECT_DIR/logs/.alert-relay-nopython"
  if [ ! -f "$marker" ]; then
    mkdir -p "$(dirname "$marker")" && touch "$marker"
    notify "경보 릴레이 중단" "python3 가 없어 Prometheus 경보를 파싱할 수 없습니다. 이 서버의 경보는 지금 아무 데도 가지 않습니다."
  fi
  exit 1
fi
rm -f "$PROJECT_DIR/logs/.alert-relay-nopython"

raw=$(curl -sS -m 10 "$PROM_URL/api/v1/alerts" 2>/dev/null) || {
  # Prometheus 자체가 죽은 경우다. watchdog.sh 가 컨테이너 정지로 따로 잡으므로
  # 여기서 또 알리지 않는다(같은 사건에 두 번 부르지 않는다).
  echo "[alert-relay] Prometheus 조회 실패 — watchdog 이 컨테이너 상태로 잡는다" >&2
  exit 0
}

# firing 만 골라 "이름|심각도|요약" 한 줄씩. jq 의존을 만들지 않으려고 python3 를 쓴다.
firing=$(printf '%s' "$raw" | python3 -c '
import json, sys
try:
    alerts = json.load(sys.stdin)["data"]["alerts"]
except Exception:
    sys.exit(0)
for a in alerts:
    if a.get("state") != "firing":
        continue
    lb = a.get("labels", {})
    an = a.get("annotations", {})
    print("|".join([
        lb.get("alertname", "unknown"),
        lb.get("severity", "-"),
        (an.get("summary") or an.get("description") or "").replace("\n", " ").strip(),
    ]))
' | sort -u)

now_file=$(mktemp); trap 'rm -f "$now_file"' EXIT
printf '%s\n' "$firing" | grep -v '^$' > "$now_file" || true

# 새로 firing 된 것만 발송
while IFS='|' read -r name sev summary; do
  [ -n "$name" ] || continue
  grep -qxF "$name|$sev|$summary" "$STATE_FILE" && continue
  notify "경보 발생 [$sev] $name" "$summary"
done < "$now_file"

# 사라진 것은 해소로 통지 — 사람이 "아직 진행 중인가?"를 확인하러 들어가지 않아도 되게.
while IFS='|' read -r name sev summary; do
  [ -n "$name" ] || continue
  grep -qxF "$name|$sev|$summary" "$now_file" && continue
  notify "경보 해소 [$sev] $name" "더 이상 firing 상태가 아닙니다."
done < "$STATE_FILE"

cp "$now_file" "$STATE_FILE"
