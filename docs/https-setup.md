# HTTPS 활성화 (3단계)

record_site 를 HTTP 로 정상 배포한 뒤, **도메인(Dynu)이 이 서버의 공인 IP 를 가리키고**
**80·443 포트가 열려 있는**(OCI 보안리스트 + 인스턴스 방화벽 둘 다) 상태에서 진행한다.

관련 파일: [`nginx/default.https.conf`](../nginx/default.https.conf), [`docker-compose.certbot.yml`](../docker-compose.certbot.yml)

명령이 길어서 아래 별칭을 먼저 만들어두면 편하다:

```bash
alias dc='docker compose -f docker-compose.yml -f docker-compose.prod.yml -f docker-compose.certbot.yml'
DOMAIN=myapp.freeddns.org      # ← 실제 Dynu 도메인으로
EMAIL=you@example.com          # ← 갱신 알림 받을 메일
```

## 0. 도메인 치환
`nginx/default.https.conf` 안의 `YOUR_DOMAIN` 3군데를 실제 도메인으로 바꾼다.

## 1. 최초 인증서 발급 (딱 한 번)
아직 인증서가 없어 nginx(443)가 못 뜨는 닭-달걀 문제가 있다.
임시 self-signed 로 우선 부팅 → 진짜 인증서로 교체하는 순서로 푼다.

```bash
# (a) 임시 더미 인증서 생성 — nginx 가 443 블록을 로드할 수 있게
dc run --rm --entrypoint "sh -c 'mkdir -p /etc/letsencrypt/live/$DOMAIN && \
  openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
  -keyout /etc/letsencrypt/live/$DOMAIN/privkey.pem \
  -out /etc/letsencrypt/live/$DOMAIN/fullchain.pem -subj /CN=localhost'" certbot

# (b) nginx 기동 — 더미 인증서로 뜨고, 80 챌린지 응답이 가능해진다
dc up -d nginx

# (c) 더미 삭제 후 진짜 인증서 발급(webroot)
dc run --rm --entrypoint "sh -c 'rm -rf /etc/letsencrypt/live/$DOMAIN && \
  certbot certonly --webroot -w /var/www/certbot -d $DOMAIN \
  --email $EMAIL --agree-tos --no-eff-email'" certbot

# (d) nginx 리로드 — 진짜 인증서 반영
dc exec nginx nginx -s reload
```

## 2. 전체 기동 (이후 상시)
```bash
dc up -d
```
- `certbot` 컨테이너: 12시간마다 갱신 시도(만료 30일 이내에만 실제 갱신)
- `nginx` 컨테이너: 6시간마다 자동 reload → 갱신된 인증서를 무중단 반영
- 즉 이후 인증서 갱신은 사람이 손댈 필요 없음.

## 3. 확인
- `https://<도메인>` 접속 → 자물쇠 확인
- `http://<도메인>` 접속 → https 로 자동 리다이렉트되는지 확인

## HTTP(2단계)로 되돌리려면
`docker-compose.certbot.yml` 를 빼고 prod 만 기동하면 된다:
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```
