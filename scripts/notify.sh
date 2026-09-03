#!/usr/bin/env bash
# 알림 발송 공통 함수 — watchdog.sh 와 alert-relay.sh 가 source 해서 쓴다.
# 두 스크립트가 같은 채널·같은 형식으로 보내야 해서 한 파일에 둔다.
#
# 채널은 Discord 웹훅 하나다. ALERT_WEBHOOK_URL 은 .env 에서 읽는다(커밋되지 않는 값).
# 값이 없으면 발송만 건너뛰고 로컬 로그에는 남긴다 — 웹훅이 없다고 워치독 자체가
# 죽으면 "감시가 조용히 사라진" 상태가 되기 때문이다.
#
# ⚠️ 로컬 로그는 편의용이지 증거가 아니다(PLATFORM 9절: 침해된 호스트의 로그는 증거로
#    쓸 수 없다). 호스트 밖으로 나가는 경로는 웹훅뿐이므로 웹훅 설정을 생략하지 말 것.

PROJECT_DIR="${PROJECT_DIR:-$HOME/record_site}"
ALERT_LOG="${ALERT_LOG:-$PROJECT_DIR/logs/alert.log}"

# .env 에서 웹훅 URL 을 읽는다(deploy.sh 가 DB_PASSWORD 를 읽는 방식과 동일).
if [ -z "${ALERT_WEBHOOK_URL:-}" ] && [ -f "$PROJECT_DIR/.env" ]; then
  ALERT_WEBHOOK_URL=$(grep -E '^ALERT_WEBHOOK_URL=' "$PROJECT_DIR/.env" | head -1 | cut -d= -f2-)
fi

# 문자열을 JSON 문자열 값 안에 넣을 수 있게 이스케이프한다.
# jq 의존을 만들지 않으려고 sed/awk 로 처리한다 — 넣는 내용이 파일 경로·컨테이너명·
# 경보 문구로 한정돼 있어 아래 네 가지면 전부 커버된다.
#   1) 캐리지리턴·탭 제거    (JSON 은 제어문자를 날것으로 못 담는다)
#   2) 역슬래시를 먼저 이중화 (뒤에 하면 3번이 넣은 역슬래시까지 다시 먹는다 — 순서 중요)
#   3) 큰따옴표 이스케이프
#   4) 개행을 두 글자 \n 으로 (awk 의 "\\n" 은 개행이 아니라 역슬래시+n 이다)
_json_escape() {
  printf '%s' "$1" \
    | tr -d '\r' \
    | tr '\t' ' ' \
    | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g' \
    | awk 'BEGIN{ORS=""} {print (NR>1 ? "\\n" : "") $0}'
}

# notify <제목> <본문>
notify() {
  local subject="$1" body="$2" stamp line payload msg
  stamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  line="[$stamp] $subject :: $body"

  mkdir -p "$(dirname "$ALERT_LOG")"
  printf '%s\n' "$line" >> "$ALERT_LOG"

  if [ -z "${ALERT_WEBHOOK_URL:-}" ]; then
    echo "[notify] ALERT_WEBHOOK_URL 미설정 — 로컬 로그에만 기록했다: $subject" >&2
    return 0
  fi

  msg="**[record_site] $subject**
$body"
  # Discord content 상한(2000자)에 대한 안전망이다. 실제 문구는 수백 자라 걸릴 일이
  # 없고, 걸리더라도 전문은 로컬 로그에 남는다.
  msg="${msg:0:1500}"

  payload="{\"content\":\"$(_json_escape "$msg")\"}"
  if ! curl -sS -m 10 -X POST -H 'Content-Type: application/json' \
       -d "$payload" "$ALERT_WEBHOOK_URL" >/dev/null 2>&1; then
    echo "[notify] 웹훅 발송 실패(로컬 로그에는 남았다): $subject" >&2
    return 1
  fi
}
