# record_site

이 문서의 모든 경로·명령은 저장소 루트(`C:\match project\record_site`) 기준이며, 세션도 이 폴더에서 연다.
루트 경로에 공백이 있으므로 셸에서 경로를 쓸 때는 반드시 따옴표로 감싼다.

LoL 전적 검색 사이트(소환사·매치·챔피언 통계·팁 게시판). Spring Boot 3.5(Java 21, JPA+QueryDSL, Flyway)
+ React 19/Vite SPA + MySQL 8 / Redis(캐시 + 전적갱신 작업 큐) + nginx(엣지 TLS) + certbot.
단일 호스트 Docker Compose(백엔드 1인스턴스), GitHub Actions CI(arm64 빌드→GHCR push+Trivy)
→ CD(SSH 접속 후 `scripts/deploy.sh` 자동 실행). 로그인 기능은 없다.

## 프로젝트 규칙
- 코드와 데이터 구조 규칙은 `C:\dev-standards\standards\ARCHITECTURE.md` 를 따른다.
- 배포, 보안, 파이프라인, 관측 규칙은 `C:\dev-standards\standards\PLATFORM.md` 를 따른다.
- 각 규칙은 [절대]와 [상황]으로 표시돼 있다. [절대]는 예외 없음. [상황]은 적용 조건과 미적용 조건이 함께 있으니, 미적용 조건에 해당하면 규칙을 어기는 것이 맞다.
- [상황] 규칙의 미적용 조건을 근거로 규칙을 어길 때는 그 이유를 코드 주석이나 커밋 메시지에 한 줄 남긴다.
- 규칙끼리 충돌하거나 판단이 서지 않으면 임의로 정하지 말고 물어본다.
- 규칙 문서는 이 저장소 밖 `C:\dev-standards` 에 있다. 저장소 안에 복사하지 않는다. 이 저장소는 공개이므로 규칙 문서 내용을 커밋하거나 README에 옮겨 적지 않는다(`.gitignore` 의 `standards/` 줄은 실수로 복사됐을 때를 막기 위한 것이다).
- 이 프로젝트는 규칙 문서보다 먼저 만들어졌다. 기존 코드가 규칙과 다른 곳이 남아 있으므로, 주변 코드를 근거로 규칙을 판단하지 않는다.

### 언제 무엇을 읽는가
아래 작업을 시작하기 전에 해당 절을 먼저 읽는다. 기억에 의존해 규칙을 적용하지 않는다. `C:\dev-standards\standards\RATIONALE.md` 는 통독하지 않고 표에 적힌 절만 읽는다.

| 시작하는 작업 | 먼저 읽을 절 |
|---|---|
| 엔티티, DTO, Controller, Service 새로 만들기 | ARCHITECTURE 1, 2, 7 |
| 트랜잭션 경계 잡기, JPA·QueryDSL·네이티브 중에 고르기 | ARCHITECTURE 3, 4 |
| Riot API 호출이 끼는 흐름(전적 갱신, 크롤러, 라이브게임) 수정 | ARCHITECTURE 4, 5, RATIONALE 3-3 — 외부 호출을 트랜잭션 안에 두지 않는다 |
| 전적 갱신 재요청·중복 수집 처리, 매치 저장 재실행 | ARCHITECTURE 6, RATIONALE 3-3 |
| 팁 추천·신고 카운터, 같은 행 동시 갱신 | ARCHITECTURE 9, RATIONALE 3-2 |
| 인덱스 추가·삭제 | ARCHITECTURE 8, RATIONALE 3-1 — 실행 계획과 실측 시간을 전후로 캡처해야 한다. 나중에 만들 수 없으니 착수 전에 읽는다 |
| 매치 목록·랭킹·티어리스트 조회 성능, 페이징, N+1, 커넥션 풀 | ARCHITECTURE 11, 12 |
| Redis 캐시 추가·TTL 변경 | ARCHITECTURE 10 |
| 갱신 작업 큐(Redis List)와 워커 수정, 큐 수단 교체 검토 | ARCHITECTURE 13 |
| 예외 클래스 추가, 에러 응답 형태 변경 | ARCHITECTURE 14 |
| 테스트 작성 | ARCHITECTURE 15, RATIONALE 3-6 — 계층이 아니라 로직으로 대상을 정한다. 짠 뒤에는 일부러 깨뜨려 빨간불이 나는지 확인한다 |
| 요청 DTO 검증, 팁 비밀번호, 클라이언트 IP 기반 식별 | PLATFORM 5 |
| nginx 레이트리밋·보안헤더·CORS, DB 계정 권한 | PLATFORM 4 |
| compose 파일, Dockerfile, nginx 설정 수정 | PLATFORM 2, 3 |
| 네트워크 오버레이(netlock) 또는 새 컨테이너 추가 | PLATFORM 3 — 어느 망에 붙일지와 egress 필요 여부를 먼저 정한다 |
| CI/CD 워크플로 수정, 의존성 추가 | PLATFORM 6, 7 |
| Flyway 마이그레이션 작성과 배포 | PLATFORM 8 — 파괴적 변경은 애플리케이션 배포와 같은 릴리스에 넣지 않는다 |
| `.env` 값 추가·변경, Riot 키 교체, 유출 대응 | PLATFORM 1 |
| 지표, 로그, 알림 규칙 추가 | PLATFORM 9 |
| 부하 테스트 | PLATFORM 10, RATIONALE 3-4 — 기준선을 먼저 측정하고 합격 기준을 테스트 전에 적는다 |
| 배포 후 보안 점검 | PLATFORM 11, RATIONALE 3-5 |

## 폴더
- `backend/` Spring Boot (config/controller/domain/dto/entity/exception/repository/service/support 레이어드), 마이그레이션은 `src/main/resources/db/migration/`
- `frontend/` React + Vite SPA, `src/api/*` 도메인별 API 클라이언트, `scripts/download-ddragon.mjs` 로 Data Dragon 에셋 내려받음
- `nginx/` 엣지 설정 — `default.conf`(HTTP) / `default.https.conf`(TLS, 실사용)
- `proxy/` squid 아웃바운드 허용목록 설정(backend 의 유일한 인터넷 경로)
- `monitoring/` prometheus·loki·alloy 설정과 grafana 프로비저닝
- `scripts/` `deploy.sh`(서버 배포 본체) · `refresh-riot-key.sh`(Riot 키 교체)
- `docs/` https-setup.md(최초 인증서 발급 절차) · refresh-job-queue.md(갱신 큐 설계 근거) — 그 외 설명은 README.md 에 있다

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
- `.certbot.yml` TLS 종단 전환 + 인증서 자동갱신
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
- `TIP_ACTOR_SALT` 를 바꾸면 기존 추천·신고 이력과 매칭이 끊긴다. 고정해서 쓴다
- Riot 개발 키는 24시간마다 만료된다. 전적검색이 통째로 죽으면 먼저 키 만료를 의심하고 `scripts/refresh-riot-key.sh` 로 교체한다
- 스키마는 Flyway 만으로 관리한다(`ddl-auto: validate`). 엔티티 변경과 마이그레이션 파일은 같은 커밋에 넣고, 운영 DB 에 직접 DDL 을 치지 않는다
- 동시성 테스트(`ChampionTipConcurrencyTest`)는 실제 행 잠금을 보기 때문에 H2 로는 재현되지 않는다. 진짜 MySQL 이 필요하다
- 백엔드는 1인스턴스이고 무중단 배포 스크립트가 없다. 갱신 워커도 전용 스레드 1개다 — 이 전제에 기대는 코드가 있으므로 인스턴스를 늘리는 변경은 전제를 먼저 깬다
- `*.sh` 는 LF 로 고정돼 있다(`.gitattributes`). CRLF 로 커밋되면 리눅스 서버에서 `bad interpreter` 로 죽는다
- 배포는 서버를 해당 커밋에 고정한 뒤 실행된다. compose·nginx·monitoring 설정 변경도 반드시 커밋돼야 배포에 반영된다

## 탐색 제외
`node_modules/`, `.gradle/`, `build/`, `frontend/dist/`, `frontend/public/`(Data Dragon 에셋), `backend/src/main/generated/`(QueryDSL Q타입)
