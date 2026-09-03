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

# 관측 스택 설정 해시 — nginx 와 같은 inode 함정을 prometheus/loki/alloy 가 그대로 갖는다.
# 서비스별로 나누지 않고 4개를 하나로 묶는다: 하나만 바뀌어도 세 컨테이너가 같이 재생성되지만
# 관측 스택이라 서비스 영향이 없고 로직이 단순해진다.
# 파일 순서는 고정한다 — 와일드카드로 순서가 흔들리면 내용이 같아도 해시가 달라져 매 배포마다 재생성된다.
MONITORING_CONF_SHA=$(cat monitoring/prometheus/prometheus.yml \
                          monitoring/prometheus/alerts.yml \
                          monitoring/loki/loki-config.yml \
                          monitoring/alloy/config.alloy | sha256sum | cut -c1-16)
export MONITORING_CONF_SHA
log "monitoring conf sha → $MONITORING_CONF_SHA"

# 아웃바운드 허용목록 해시 — squid.conf 도 단일 파일 마운트라 같은 inode 함정을 갖는다.
# 이게 없으면 허용목록을 좁혀도 옛 설정이 계속 돈다(가장 위험한 방향의 무반영이다).
EGRESS_CONF_SHA=$(sha256sum proxy/squid.conf | cut -c1-16)
export EGRESS_CONF_SHA
log "egress conf sha → $EGRESS_CONF_SHA"
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

# ── 9-b) 아웃바운드 허용목록 검증 ──
#   backend 는 proxy 망(internal)에만 있고 아웃바운드는 egress-proxy(squid) 경유만 가능해야 한다.
#   이 셋을 다 봐야 하는 이유:
#     ① 직접 나갈 수 있으면 허용목록이 무의미하다(compose 가 networks 를 합집합으로 병합해서
#        prod.yml 에 egress 가 남으면 조용히 이렇게 된다 — 실제로 겪은 함정이다).
#     ② 허용 목적지가 막히면 전적검색이 통째로 죽는데 health 체크는 DB·Redis 만 봐서 못 잡는다.
#     ③ 차단 목적지도 봐야 한다. ②만 보면 "전부 허용" 로 잘못 설정된 squid 도 통과한다.
#   busybox nc 로 squid 에 직접 CONNECT 를 던진다 — DNS 를 squid 가 하므로 internal 망에서도
#   동작한다(wget/curl 은 호스트명을 스스로 해석하려다 실패해 거짓 결과를 낸다).
PROXY_NET=record_site_proxy
connect_probe(){   # <호스트> → squid 응답 첫 줄
  docker run --rm --network "$PROXY_NET" nginx:alpine sh -c \
    "printf 'CONNECT $1:443 HTTP/1.1\r\nHost: $1:443\r\n\r\n' | timeout 10 nc egress-proxy 3128 2>/dev/null | head -1" 2>/dev/null
}

if docker run --rm --network "$PROXY_NET" nginx:alpine timeout 5 wget -q -O /dev/null http://1.1.1.1 2>/dev/null; then
  echo "[egress] FAIL: backend 계층이 프록시 없이 인터넷에 직접 도달했다 — 허용목록이 무력화된 상태" >&2
  netlock_ok=false
else
  log "egress OK: proxy 망 → 인터넷 직접 BLOCKED"
fi

allowed_resp=$(connect_probe ddragon.leagueoflegends.com)
case "$allowed_resp" in
  *"200 Connection established"*) log "egress OK: 허용 목적지(ddragon) → 프록시 경유 통과" ;;
  *) echo "[egress] FAIL: 허용 목적지가 프록시를 통과하지 못했다 (응답: ${allowed_resp:-없음})" >&2
     echo "        전적검색이 죽은 상태로 배포될 수 있다 — squid 설정과 backend 의 proxy 망 부착을 확인하라" >&2
     netlock_ok=false ;;
esac

denied_resp=$(connect_probe pool.supportxmr.com)
case "$denied_resp" in
  *"403"*) log "egress OK: 차단 목적지(채굴풀) → 403 DENIED" ;;
  *) echo "[egress] FAIL: 차단돼야 할 목적지가 막히지 않았다 (응답: ${denied_resp:-없음})" >&2
     echo "        허용목록이 실제로 적용되지 않았다는 뜻이다" >&2
     netlock_ok=false ;;
esac

[ "$netlock_ok" = true ] || fail "security invariant check failed (network isolation / egress allow-list)"

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

# ── 10-b) 관측 스택이 "이 커밋의" 설정으로 돌고 있는지 ──
#   스크레이프 타깃·알림 규칙이 반영 안 돼도 컨테이너는 멀쩡히 뜬다 — 조용히 눈이 머는 상황을 막는다.
#   prometheus 하나만 대조하면 충분하다: 4개가 MONITORING_CONF_SHA 하나로 묶여 함께 재생성되므로
#   prometheus 가 최신이면 loki/alloy 도 최신이다.
host_prom_sha=$(sha256sum monitoring/prometheus/prometheus.yml | cut -c1-64)
live_prom_sha=$(docker exec lol-prometheus sha256sum /etc/prometheus/prometheus.yml 2>/dev/null | cut -c1-64)
if [ "$host_prom_sha" != "$live_prom_sha" ]; then
  echo "[monitoring] FAIL: prometheus is serving a stale config" >&2
  echo "        repo=$host_prom_sha" >&2
  echo "        live=$live_prom_sha" >&2
  fail "prometheus config is stale (container did not pick up this commit's conf)"
fi
log "monitoring conf OK: prometheus is running this commit's config"

# ── 10-c) 아웃바운드 프록시가 "이 커밋의" 허용목록으로 돌고 있는지 ──
#   9-b 의 CONNECT 검사는 "지금 도는 설정"이 맞게 동작하는지만 본다. 그 설정이 이 커밋의
#   것인지는 별개다 — 허용목록을 좁힌 커밋을 배포했는데 옛 넓은 목록이 그대로 도는
#   상황이 inode 함정으로 실제 가능하다.
host_squid_sha=$(sha256sum proxy/squid.conf | cut -c1-64)
live_squid_sha=$(docker exec lol-egress-proxy sha256sum /etc/squid/squid.conf 2>/dev/null | cut -c1-64)
if [ "$host_squid_sha" != "$live_squid_sha" ]; then
  echo "[egress] FAIL: egress-proxy is serving a stale allow-list" >&2
  echo "        repo=$host_squid_sha" >&2
  echo "        live=$live_squid_sha" >&2
  fail "egress proxy config is stale (container did not pick up this commit's conf)"
fi
log "egress conf OK: allow-list is this commit's config"

# ── 10-d) 컨테이너 하드닝·포트 노출 불변식 ──
#   여기까지의 검사는 전부 네트워크에 관한 것이었다. cap_drop·no-new-privileges·리소스
#   상한·read_only 는 오버레이 하나만 빠져도 조용히 사라지는데 컨테이너는 멀쩡히 뜬다.
#   설정 파일이 아니라 돌고 있는 컨테이너를 보고 판정한다.
bash "$PROJECT_DIR/scripts/check-hardening.sh" || fail "container hardening invariant check failed"

# ── 11) 감시 스크립트 cron 등록 (멱등) ──
#   워치독과 경보 릴레이는 서버에 cron 으로 상주해야 의미가 있는데, 등록을 수동 절차로
#   남기면 서버를 다시 만들 때 조용히 빠진다 — 그러면 "감시가 있다고 믿는 무인 서버"가
#   된다. 배포가 성공할 때마다 다시 심어 그 상태를 만들 수 없게 한다.
#   기존 항목은 지우고 다시 넣으므로 경로나 주기를 바꿔도 중복이 쌓이지 않는다.
install_cron(){
  local marker="# record_site monitoring (managed by scripts/deploy.sh)"
  local kept
  kept=$(crontab -l 2>/dev/null \
         | grep -vF "$marker" \
         | grep -v 'scripts/watchdog.sh' \
         | grep -v 'scripts/alert-relay.sh' || true)
  {
    [ -n "$kept" ] && printf '%s\n' "$kept"
    printf '%s\n' "$marker"
    printf '*/5 * * * * cd %s && bash scripts/watchdog.sh >> %s/logs/cron.log 2>&1\n' "$PROJECT_DIR" "$PROJECT_DIR"
    printf '*/5 * * * * cd %s && bash scripts/alert-relay.sh >> %s/logs/cron.log 2>&1\n' "$PROJECT_DIR" "$PROJECT_DIR"
  } | crontab -
}
if command -v crontab >/dev/null 2>&1; then
  install_cron && log "cron 등록 OK: watchdog + alert-relay (5분 주기)"
  # 등록만 하고 끝내면 "등록은 됐는데 실행하면 깨지는" 상태를 다음 사고 때 발견하게 된다.
  # 여기서 한 번 돌려 지금 실제로 동작하는지 확인한다(결과로 배포를 실패시키지는 않는다).
  if bash "$PROJECT_DIR/scripts/watchdog.sh"; then
    log "watchdog 첫 실행 OK"
  else
    log "WARNING: watchdog 첫 실행 실패 — logs/cron.log 확인 필요"
  fi
else
  log "WARNING: crontab 이 없어 감시 스크립트를 등록하지 못했다"
fi

log "deploy OK ($TAG) — backend health UP"
docker image prune -f >/dev/null 2>&1 || true
docker logout "$REGISTRY" >/dev/null 2>&1 || true
