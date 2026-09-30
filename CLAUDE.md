# record_site

이 문서의 모든 경로·명령은 저장소 루트(`C:\match project\record_site`) 기준이며, 세션도 이 폴더에서 연다.
루트 경로에 공백이 있으므로 셸에서 경로를 쓸 때는 반드시 따옴표로 감싼다.

LoL 전적 검색 사이트(소환사·매치·챔피언 통계·팁 게시판). Spring Boot 3.5(Java 21, JPA+QueryDSL, Flyway)
+ React 19/Vite SPA + MySQL 8 / Redis(캐시 + 전적갱신 작업 큐) + nginx(앱 입구, TLS 는 server-infra 의 Caddy).
단일 호스트 Docker Compose(백엔드 1인스턴스), GitHub Actions CI(arm64 빌드→GHCR push+Trivy)
→ CD(SSH 접속 후 `scripts/deploy.sh` 자동 실행). 로그인 기능은 없다.

## 프로젝트 규칙

@C:/dev-standards/templates/CLAUDE-common.md

- 위 공통 규칙 블록(규칙 문서 경로, 표시 읽는 법, "언제 무엇을 읽는가" 표)이 보이지 않으면 작업 전에 `C:\dev-standards\templates\CLAUDE-common.md` 를 직접 읽는다.
- 이 저장소는 공개이므로 규칙 문서 내용을 커밋하거나 README에 옮겨 적지 않는다(`.gitignore` 의 `standards/` 줄은 실수로 복사됐을 때를 막기 위한 것이다).
- 이 프로젝트는 규칙 문서보다 먼저 만들어졌다. 기존 코드가 규칙과 다른 곳이 남아 있으므로, 주변 코드를 근거로 규칙을 판단하지 않는다.

### 프로젝트 전제

PLATFORM 0절의 항목이다. 규칙이 이 값에 따라 갈리므로 비워두지 않는다. 값이 바뀌는 변경은 이 표를 먼저 고친 뒤 시작한다.

| 항목 | 값 |
|---|---|
| 앱 인스턴스 수 | 1 (무중단 배포 스크립트 없음, 배포 중 다운타임 허용). 갱신 워커가 전용 스레드 1개라 인스턴스를 늘리려면 이 전제부터 깨야 한다 |
| 프론트엔드 형태 | 정적 SPA. `lol-frontend` 컨테이너가 빌드 결과를 서빙하고 웹 망(`default`)에만 붙는다 |
| 사이트 경계 | 공용 무료 도메인(`kdagg.kozow.com`, 프론트와 API 가 같은 오리진). 로그인이 없어 세션 쿠키가 없다 |
| 인증 방식 | 로그인 없음. 팁 수정·삭제는 글마다 비밀번호(해시 저장)로 확인한다 |
| 결제 형태 | 없음 |
| Runner 위치 | GitHub 호스팅만. CD 는 `ubuntu-latest` 러너가 SSH 로 서버에 붙어 `scripts/deploy.sh` 를 실행한다(`DEPLOY_SSH_KEY`, 호스트키 핀 `DEPLOY_KNOWN_HOSTS`). `.env` 는 서버에 있다 |
| 엣지 프록시 | 있음. 별도 저장소 `server-infra` 의 Caddy(`edge-caddy`)가 80/443 과 TLS 를 맡고, internal 망 `edge`(10.250.0.0/24)로 `lol-nginx` 에 넘긴다. `lol-nginx` 는 그 대역에서 온 `X-Forwarded-For` 로만 실제 IP 를 복원하고(Caddy 가 클라이언트 값을 버리고 덮어쓴다), 백엔드는 계속 `X-Real-IP` 만 신뢰한다. 속도 제한 키도 복원된 IP 다 |
| 실사용자와 개인정보 | 없음(혼자 쓰는 수준, 계정 없음). 저장 항목: 팁 작성자 닉네임·본문·비밀번호 해시(`ChampionTip`), 추천·신고자 IP 를 솔트로 해시한 식별자(`ChampionTipInteraction.actorKey`), Riot 공개 게임 데이터(소환사 Riot ID·PUUID, 매치 참가자) |
| Redis 역할 | 캐시 + 전적 갱신 작업 큐(Redis List). 영속화 꺼짐(`--save ""`), 재시작하면 대기 중인 갱신 요청이 사라진다. 한 인스턴스에 `allkeys-lru` 라 메모리가 차면 큐 키도 쫓겨난다(ARCHITECTURE 10절은 큐가 있는 인스턴스에 noeviction 을 요구한다) |
| DB 엔진 | MySQL 8 (InnoDB) |
| 메시지 브로커 | Redis 작업 큐. ARCHITECTURE 13절 중 소비자 멱등, 커밋 후 발행, 재시도 횟수 제한만 적용한다 |
| 환경 구성 | 로컬 + 프로덕션. 검증 환경 없음 |
| 가상 스레드 | 사용 안 함 (`spring.threads.virtual.enabled` 설정 없음) |

### 이 프로젝트에서 추가로 읽을 절

| 시작하는 작업 | 먼저 읽을 절 |
|---|---|
| Riot API 호출이 끼는 흐름(전적 갱신, 크롤러, 라이브게임) 수정 | ARCHITECTURE 4, 5, 17 — 외부 호출을 트랜잭션 안에 두지 않는다 |
| 전적 갱신 재요청·중복 수집 처리, 매치 저장 재실행 | ARCHITECTURE 6 — 재현 절차는 defect-repro 스킬 |
| 팁 추천·신고 카운터, 같은 행 동시 갱신 | ARCHITECTURE 9 — 재현 절차는 defect-repro 스킬 |
| 갱신 작업 큐(Redis List)와 워커 수정, 큐 수단 교체 검토 | ARCHITECTURE 13, 10 |
| 요청 DTO 검증, 팁 비밀번호, 클라이언트 IP 기반 식별 | PLATFORM 5 |
| 네트워크 오버레이(netlock) 또는 새 컨테이너 추가 | PLATFORM 3 — 어느 망에 붙일지와 egress 필요 여부를 먼저 정한다 |
| `.env` 값 추가·변경, Riot 키 교체 | PLATFORM 1 |

## 폴더
- `backend/` Spring Boot (config/controller/domain/dto/entity/exception/repository/service/support 레이어드), 마이그레이션은 `src/main/resources/db/migration/`
- `frontend/` React + Vite SPA, `src/api/*` 도메인별 API 클라이언트, `scripts/download-ddragon.mjs` 로 Data Dragon 에셋 내려받음
- `nginx/` 앱 입구 설정 — `default.conf`(Caddy 뒤에서 평문 HTTP 로 받는다. 실제 IP 복원·속도 제한·보안 헤더·이상 Host 드롭)
- `proxy/` squid 아웃바운드 허용목록 설정(backend 의 유일한 인터넷 경로)
- `monitoring/` prometheus·loki·alloy 설정과 grafana 프로비저닝
- `scripts/` `deploy.sh`(서버 배포 본체) · `refresh-riot-key.sh`(Riot 키 교체)
- `docs/` refresh-job-queue.md(갱신 큐 설계 근거) — 그 외 설명은 README.md 에 있다

## 실행/배포
- 개발 인프라만 기동: `docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d` (MySQL 3307 / Redis 6379 호스트 공개)
- 백엔드는 IDE 또는 `cd backend && ./gradlew bootRun`, 프론트는 `cd frontend && npm run dev` (5173, `/api` → 8080 프록시)
- 테스트: `cd backend && ./gradlew test` — 동시성 테스트가 실제 MySQL 을 요구하므로 위 dev 인프라가 떠 있어야 한다
- 실제 배포는 main push 시 CI(이미지 빌드→GHCR)가 성공하면 CD 가 서버에 SSH 로 붙어 `git checkout -f <sha>` 후 `scripts/deploy.sh` 를 실행한다. 로컬에서 서버로 배포하는 경로는 없다
- **절대 하면 안 됨**: 맨손 `docker compose up` — netlock 오버레이 없이 올리면 `default`/`data` 망의 `internal` 잠금이 빠져 프론트 아웃바운드가 열리고(OTT 프로젝트에서 같은 구멍이 실제 침해로 이어졌다), backend 의 아웃바운드 허용목록(squid)도 통째로 빠진다. 운영 조합의 정본은 `scripts/deploy.sh` 의 `COMPOSE` 배열이다
- `.env`(RIOT_API_KEY, DB_PASSWORD, REDIS_PASSWORD, DB_APP_*/DB_MIGRATE_*, TIP_ACTOR_SALT)는 커밋되지 않는다. base compose 가 `:?` 로 필수화해 두어 값이 없으면 기동이 실패한다

## compose 파일 용도
- `docker-compose.yml` 베이스(mysql+redis, 망 3개 정의 — 단독 실행 금지)
- `.dev.yml` 개발용(인프라 호스트 포트만 열기) / `.prod.yml` 운영용(backend·frontend·nginx 추가)
- `.ghcr.yml` 서버 재빌드 금지 + GHCR 이미지(digest) 고정
- `.netlock.yml` `default`/`data`/`proxy` 망 egress 차단 + backend 아웃바운드 허용목록 프록시(squid) (운영 필수)
- `.hardening.yml` cap_drop·no-new-privileges·read_only·cpu 상한
- `.monitoring.yml` Prometheus/Grafana/Loki/Alloy (배포 스크립트에 항상 포함 — 빠지면 `--remove-orphans` 가 지운다)

## 함정
- 단일 파일 bind mount 는 inode 로 고정된다. CD 의 `git checkout -f` 가 파일을 새 inode 로 갈아끼우면 컨테이너는 삭제된 옛 파일을 계속 읽는다(`nginx -s reload` 도 소용없다). 그래서 `deploy.sh` 가 내용 해시를 `NGINX_CONF_SHA`/`MONITORING_CONF_SHA`/`EGRESS_CONF_SHA` 로 주입해 컨테이너를 재생성시킨다 — **단일 파일 마운트를 새로 추가하면 그 파일도 해시 대상에 넣어야 한다**
- 해시 계산에 들어가는 파일 목록은 순서를 고정한다. 와일드카드로 순서가 흔들리면 내용이 같아도 매 배포마다 재생성된다
- 오버레이를 새로 만들면 `deploy.sh` 의 `COMPOSE` 배열에 반드시 추가한다. 빠지면 반영이 안 되는 정도가 아니라 `--remove-orphans` 가 그 컨테이너를 지운다
- `scripts/refresh-riot-key.sh` 의 `COMPOSE` 배열은 deploy.sh 와 동일하게 유지한다. 빠지면 backend 가 그 오버레이 없이 재생성되는데, netlock 이 빠지면 `proxy` 망이 없어 Riot 호출이 통째로 실패한다(하드닝도 같이 벗겨진다)
- nginx 는 upstream 호스트명을 기동 시 1회만 IP 로 해석한다. 배포로 backend/frontend 가 재생성되면 옛 IP 를 붙들어 502 가 난다 — `deploy.sh` 가 up 직후 `lol-nginx` 를 재시작해서 푼다. health 체크는 backend 직결이라 이 고장을 못 잡는다
- 클라이언트 IP 는 `X-Real-IP` 만 신뢰한다. `X-Forwarded-For` 는 nginx 가 클라이언트가 보낸 값 뒤에 덧붙이는 방식이라 위조된 앞쪽 값을 그대로 읽게 된다
- `lol-nginx` 가 보는 접속 주소는 Caddy 다. `nginx/default.conf` 의 `set_real_ip_from`(edge 대역)이 빠지거나 대역이 server-infra 와 어긋나면 모든 요청이 Caddy IP 하나로 수렴한다 — 속도 제한이 서비스 전체 총량이 되고, 팁 추천·신고가 전원 한 사람으로 묶인다. 에러는 나지 않는다
- `edge` 망은 server-infra 가 만드는 external 망이다. 서버에 server-infra 가 먼저 떠 있지 않으면(최소 `docker compose up --no-start`) 배포가 compose 단계에서 실패한다
- `TIP_ACTOR_SALT` 를 바꾸면 기존 추천·신고 이력과 매칭이 끊긴다. 고정해서 쓴다
- Riot 개발 키는 24시간마다 만료된다. 전적검색이 통째로 죽으면 먼저 키 만료를 의심하고 `scripts/refresh-riot-key.sh` 로 교체한다
- 스키마는 Flyway 만으로 관리한다(`ddl-auto: validate`). 엔티티 변경과 마이그레이션 파일은 같은 커밋에 넣고, 운영 DB 에 직접 DDL 을 치지 않는다
- 동시성 테스트(`ChampionTipConcurrencyTest`)는 실제 행 잠금을 보기 때문에 H2 로는 재현되지 않는다. 진짜 MySQL 이 필요하다
- 백엔드는 1인스턴스이고 무중단 배포 스크립트가 없다. 갱신 워커도 전용 스레드 1개다 — 이 전제에 기대는 코드가 있으므로 인스턴스를 늘리는 변경은 전제를 먼저 깬다
- `*.sh` 는 LF 로 고정돼 있다(`.gitattributes`). CRLF 로 커밋되면 리눅스 서버에서 `bad interpreter` 로 죽는다
- 배포는 서버를 해당 커밋에 고정한 뒤 실행된다. compose·nginx·monitoring 설정 변경도 반드시 커밋돼야 배포에 반영된다

## 탐색 제외
`node_modules/`, `.gradle/`, `build/`, `frontend/dist/`, `frontend/public/`(Data Dragon 에셋), `backend/src/main/generated/`(QueryDSL Q타입)
