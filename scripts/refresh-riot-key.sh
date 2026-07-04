#!/usr/bin/env bash
# RIOT dev 키(24시간마다 만료) 교체 헬퍼 — 서버에서 실행한다.
# dev 키는 https://developer.riotgames.com 에서 로그인 후 "Development API Key" 를 매일 새로 발급받아야 한다.
# (근본 해결은 만료가 느린 Personal/Production 키 승인. 그 전까진 이 스크립트로 매일 교체.)
#
# 사용:
#   bash scripts/refresh-riot-key.sh 'RGAPI-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx'
#   (인자 없이 실행하면 화면에 안 보이게 입력받는다)
#
# 동작: .env 의 RIOT_API_KEY 를 교체 → 지금 떠 있는 이미지(digest) 그대로 backend 만 재생성(새 .env 반영) → Riot 호출 검증.
# ※ compose 파일 목록은 scripts/deploy.sh 와 반드시 동일하게 유지할 것(하드닝 override 추가 시 여기도 추가).
set -euo pipefail
cd "$HOME/record_site"

COMPOSE=(docker compose
  -f docker-compose.yml
  -f docker-compose.prod.yml
  -f docker-compose.ghcr.yml
  -f docker-compose.certbot.yml)

NEWKEY="${1:-}"
if [ -z "$NEWKEY" ]; then
  read -rs -p "new RIOT_API_KEY: " NEWKEY; echo
fi
case "$NEWKEY" in
  RGAPI-*) : ;;
  *) echo "키 형식이 RGAPI-... 가 아님. 중단."; exit 1 ;;
esac

# .env 의 RIOT_API_KEY 교체(없으면 추가). 다른 값(DB_PASSWORD 등)은 건드리지 않는다.
if grep -qE '^RIOT_API_KEY=' .env; then
  sed -i "s|^RIOT_API_KEY=.*|RIOT_API_KEY=$NEWKEY|" .env
else
  printf 'RIOT_API_KEY=%s\n' "$NEWKEY" >> .env
fi
echo "[refresh] .env updated"

# 지금 떠 있는 이미지(digest)를 그대로 재사용해 backend 만 재생성 → 새 .env 를 다시 읽는다.
BACKEND_IMAGE="$(docker inspect --format '{{.Config.Image}}' lol-backend)"
FRONTEND_IMAGE="$(docker inspect --format '{{.Config.Image}}' lol-frontend)"
export BACKEND_IMAGE FRONTEND_IMAGE
echo "[refresh] recreating backend ($BACKEND_IMAGE)"
"${COMPOSE[@]}" up -d --no-build --force-recreate backend

# 검증: 새 키로 Riot 호출이 200 나는지
echo "[refresh] verifying new key against Riot..."
sleep 3
code=$(curl -s -o /dev/null -w '%{http_code}' -H "X-Riot-Token: $NEWKEY" \
  "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/Hide%20on%20bush/KR1")
if [ "$code" = "200" ]; then
  echo "[refresh] OK — Riot account-v1 HTTP 200. 전적검색 정상화됨."
else
  echo "[refresh] WARNING — Riot HTTP $code (200 이 아님). 키가 잘못됐거나 아직 활성화 전일 수 있음."
fi
