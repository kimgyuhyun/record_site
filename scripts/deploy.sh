#!/usr/bin/env bash
# 서버에서 실행되는 배포 스크립트.
# cd.yml 이 SSH 로 접속해 `git checkout -f $TAG` 로 배포 대상 커밋을 체크아웃한 뒤 이 스크립트를 호출한다.
# (배포하는 이미지와 compose/nginx 설정이 같은 커밋으로 일치하도록 서버를 그 sha 에 고정한다.)
#
# 필요한 환경변수:
#   TAG        배포할 이미지 태그(= commit sha)
#   GHCR_USER  GHCR 로그인 사용자명(인증은 토큰이 하므로 값 자체는 크게 중요치 않음)
#   GHCR_TOKEN 워크플로 GITHUB_TOKEN(단기) — 서버에 장기 PAT 를 남기지 않기 위해 매 배포마다 주입/폐기
set -euo pipefail

REGISTRY=ghcr.io
OWNER=kimgyuhyun
BACKEND_REPO="$REGISTRY/$OWNER/record_site-backend"
FRONTEND_REPO="$REGISTRY/$OWNER/record_site-frontend"
PROJECT_DIR="$HOME/record_site"
BACKUP_DIR="$PROJECT_DIR/backups"
NET=record_site_default            # compose 기본 네트워크(프로젝트명이 record_site 라서)
# 현재 운영 스택(base+prod+certbot) + GHCR 이미지 override + 보안 오버레이(netlock/hardening) + 관측성(monitoring).
# 새 오버레이를 추가하면 반드시 이 배열에도 넣어야 배포에 반영된다.
# monitoring 은 맨 뒤에 둔다 — 보안 오버레이 값을 덮지 않고, 자신의 추가분(backend env·새 서비스)만 얹는다.
# (monitoring 이 배열에 있어야 --remove-orphans 가 관측 컨테이너를 orphan 으로 지우지 않는다.)
COMPOSE=(docker compose
  -f docker-compose.yml
  -f docker-compose.prod.yml
  -f docker-compose.ghcr.yml
  -f docker-compose.certbot.yml
  -f docker-compose.netlock.yml
  -f docker-compose.hardening.yml
  -f docker-compose.monitoring.yml)

: "${TAG:?TAG required}"
: "${GHCR_USER:?GHCR_USER required}"
: "${GHCR_TOKEN:?GHCR_TOKEN required}"

cd "$PROJECT_DIR"
log(){ echo "[deploy $(date -u +%H:%M:%S)] $*"; }
fail(){ echo "[deploy ERROR] $*" >&2; exit 1; }

# ── 1) GHCR 로그인(단기 토큰) ──
log "docker login $REGISTRY"
echo "$GHCR_TOKEN" | docker login "$REGISTRY" -u "$GHCR_USER" --password-stdin

# ── 2) 이미지 pull (commit-sha 태그) ──
tag_backend="$BACKEND_REPO:$TAG"
tag_frontend="$FRONTEND_REPO:$TAG"
log "pull $tag_backend";  docker pull "$tag_backend"
log "pull $tag_frontend"; docker pull "$tag_frontend"

# ── 3) digest 고정 (태그 덮어쓰기 공격 무력화 — 스캔한 그 이미지 그대로 배포) ──
backend_digest=$(docker inspect --format '{{index .RepoDigests 0}}' "$tag_backend")
frontend_digest=$(docker inspect --format '{{index .RepoDigests 0}}' "$tag_frontend")
[ -n "$backend_digest" ]  || fail "cannot resolve backend digest"
[ -n "$frontend_digest" ] || fail "cannot resolve frontend digest"
log "backend  → $backend_digest"
log "frontend → $frontend_digest"

# ── 4) IOC 스캔 게이트 (배포 전) — solo-project 감염 지표 검사 ──
#   파일명: xmrig/javae/minerd/cpuminer/kdevtmpfsi/kinsing/grepb32
#   문자열: C2 IP(221.156.167.200) / supportxmr / grepb32
ioc_scan(){
  local img="$1" name="$2" tmp cname names strings
  tmp=$(mktemp); cname="iocscan_${name}_$$"
  docker create --name "$cname" "$img" >/dev/null
  docker export "$cname" > "$tmp"
  docker rm -f "$cname" >/dev/null
  names=$(tar -tf "$tmp" 2>/dev/null | grep -Ei '(^|/)(xmrig|javae|minerd|cpuminer|kdevtmpfsi|kinsing|grepb32)[^/]*$' || true)
  strings=$(tar -xOf "$tmp" 2>/dev/null | grep -aEo '221\.156\.167\.200|supportxmr|grepb32' | sort -u | head || true)
  rm -f "$tmp"
  if [ -n "$names" ] || [ -n "$strings" ]; then
    echo "[IOC] match in $name image:"
    [ -n "$names" ]   && echo "  files:   $names"
    [ -n "$strings" ] && echo "  strings: $strings"
    return 1
  fi
  log "IOC scan clean: $name"
}
ioc_scan "$tag_backend"  backend  || fail "IOC gate blocked backend image"
ioc_scan "$tag_frontend" frontend || fail "IOC gate blocked frontend image"

# ── 5) DB 백업 (배포 전) — .env 의 DB_PASSWORD 사용(비번은 MYSQL_PWD 로 넘겨 argv 노출 회피) ──
mkdir -p "$BACKUP_DIR"
db_password=$(grep -E '^DB_PASSWORD=' .env | head -1 | cut -d= -f2-)
[ -n "$db_password" ] || fail "DB_PASSWORD not found in .env"
ts=$(date -u +%Y%m%d-%H%M%S)
log "mysqldump → backups/loldb-$ts.sql.gz"
docker exec -e MYSQL_PWD="$db_password" lol-mysql \
  mysqldump -uroot --single-transaction --skip-lock-tables loldb | gzip > "$BACKUP_DIR/loldb-$ts.sql.gz"
[ -s "$BACKUP_DIR/loldb-$ts.sql.gz" ] || fail "backup file is empty"
ls -1t "$BACKUP_DIR"/loldb-*.sql.gz 2>/dev/null | tail -n +11 | xargs -r rm -f   # 최근 10개만 보관

# ── 6) 롤백용 현재 이미지 기록 ──
prev_backend=$(docker inspect --format '{{.Image}}' lol-backend 2>/dev/null || true)
prev_frontend=$(docker inspect --format '{{.Image}}' lol-frontend 2>/dev/null || true)

# ── 7) 배포 (digest 고정, 서버 재빌드 금지) ──
export BACKEND_IMAGE="$backend_digest" FRONTEND_IMAGE="$frontend_digest"

# nginx 설정 내용 해시 — 값이 바뀌면 compose 가 nginx 를 재생성한다.
# (단일 파일 bind mount 는 inode 고정이라, 파일만 바뀌면 컨테이너가 옛 내용을 계속 본다.
#  자세한 근거는 docker-compose.prod.yml 의 NGINX_CONF_SHA 주석 참고.)
NGINX_CONF_SHA=$(cat nginx/*.conf | sha256sum | cut -c1-16)
export NGINX_CONF_SHA
log "nginx conf sha → $NGINX_CONF_SHA"
log "compose up -d --no-build"
"${COMPOSE[@]}" up -d --no-build --remove-orphans

# ── 7-b) 엣지 nginx 업스트림 재해석 ──
#   nginx 는 upstream 호스트명(backend/frontend)을 "기동 시 1회만" IP 로 해석해 캐싱한다.
#   그런데 배포마다 backend/frontend 는 새 이미지로 재생성되며 IP 가 바뀔 수 있고,
#   nginx 는 conf 가 그대로면(NGINX_CONF_SHA 동일) 재생성되지 않아 옛 IP 를 계속 붙든다
#   → 엣지가 죽은 IP 로 프록시해 502(Bad Gateway). health 체크는 backend:8080 직결이라 이걸 놓친다.
#   그래서 up 직후 nginx 를 재기동해 현재 IP 로 다시 해석시킨다(이 시점엔 upstream 이 이미 떠 있어 해석 성공).
log "restart edge nginx to re-resolve upstream IPs"
docker restart lol-nginx >/dev/null

# ── 8) 배포 후 검증: 백엔드 health UP (내부망 원샷 컨테이너) ──
#   JRE 이미지엔 curl 이 없어, 이미 받아둔 nginx:alpine 의 busybox wget 을 재사용한다.
health_ok=false
for _ in $(seq 1 30); do
  out=$(docker run --rm --network "$NET" nginx:alpine \
        wget -qO- --timeout=3 http://backend:8080/actuator/health 2>/dev/null || true)
  case "$out" in *'"status":"UP"'*) health_ok=true; break;; esac
  sleep 3
done

if [ "$health_ok" != true ]; then
  log "HEALTH CHECK FAILED → rollback to previous images"
  if [ -n "$prev_backend" ] && [ -n "$prev_frontend" ]; then
    export BACKEND_IMAGE="$prev_backend" FRONTEND_IMAGE="$prev_frontend"
    "${COMPOSE[@]}" up -d --no-build || true
  fi
  fail "backend health check failed (rolled back to previous images)"
fi

# ── 9) 보안 불변식 검증: 웹 계층(frontend)의 격리 ──
#   컨테이너 하나가 뚫려도 인터넷으로 나가거나 DB 로 옆걸음 못 하는 상태가 배포의 전제 조건이다.
#   네트워크 오버레이가 누락/회귀되면(예: COMPOSE 배열에서 netlock 빠짐) 여기서 배포를 실패시킨다.
#   ※ frontend 는 nginx:alpine — busybox 의 wget/nc 를 그대로 쓴다.
netlock_ok=true

if docker exec lol-frontend timeout 5 wget -q -O /dev/null http://1.1.1.1 2>/dev/null; then
  echo "[netlock] FAIL: frontend reached the internet (1.1.1.1) — egress lock is not in effect" >&2
  netlock_ok=false
else
  log "netlock OK: frontend → internet BLOCKED"
fi

if docker exec lol-frontend timeout 5 nc -z mysql 3306 2>/dev/null; then
  echo "[netlock] FAIL: frontend reached mysql:3306 — data network is not isolated" >&2
  netlock_ok=false
else
  log "netlock OK: frontend → mysql:3306 BLOCKED"
fi

[ "$netlock_ok" = true ] || fail "security invariant check failed (network isolation)"

# ── 10) 엣지 nginx 가 "이 커밋의" 설정으로 돌고 있는지 ──
#   위 inode 함정 때문에 설정이 반영되지 않아도 컨테이너는 멀쩡히 떠 있다(옛 설정으로).
#   보안 헤더·레이트리밋·Host 드롭이 조용히 빠진 채 배포 성공으로 보이는 상황을 막는다.
host_conf_sha=$(sha256sum nginx/default.https.conf | cut -c1-64)
live_conf_sha=$(docker exec lol-nginx sha256sum /etc/nginx/conf.d/default.conf 2>/dev/null | cut -c1-64)
if [ "$host_conf_sha" != "$live_conf_sha" ]; then
  echo "[nginx] FAIL: edge nginx is serving a stale config" >&2
  echo "        repo=$host_conf_sha" >&2
  echo "        live=$live_conf_sha" >&2
  fail "edge nginx config is stale (container did not pick up this commit's conf)"
fi
log "nginx conf OK: edge is running this commit's config"

log "deploy OK ($TAG) — backend health UP"
docker image prune -f >/dev/null 2>&1 || true
docker logout "$REGISTRY" >/dev/null 2>&1 || true
