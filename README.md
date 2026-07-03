# record_site — LoL 전적 검색 사이트

Riot Games API 를 기반으로 한 **리그 오브 레전드(LoL) 전적 검색 · 통계 서비스**입니다.
소환사 검색부터 매치 상세 · 타임라인, 실시간 인게임 정보, 챔피언 통계 · 티어리스트, 상위 티어 랭킹까지
OP.GG 스타일의 전적 사이트 기능을 직접 구현했습니다.

> 🌐 서비스 주소: **https://kdagg.kozow.com** (Oracle Cloud arm64 서버에 자동 배포)

---

## 주요 기능

| 영역 | 기능 |
|------|------|
| **전적 검색** | Riot ID(`게임명#태그`)로 소환사 조회, 랭크 티어 · 최근 전적 요약 |
| **매치 히스토리** | 매치 목록 · 상세 요약, 큐 타입 필터, **타임라인**(아이템 빌드 순서 · 스킬 선마 순서) |
| **전적 갱신** | 비동기 작업 큐 기반 갱신 + **진행률 실시간 폴링** (아래 [핵심 설계](#핵심-설계) 참고) |
| **실시간 인게임** | 스펙테이터 API 로 현재 진행 중인 게임 정보 조회 |
| **챔피언 숙련도** | 소환사별 챔피언 마스터리 |
| **챔피언 통계** | 챔피언 티어리스트, 챔피언별 상세(스킬 · 스킨 · 스탯), 무료 로테이션 |
| **챔피언 팁** | 커뮤니티 팁 작성 · 수정 · 삭제(비밀번호 기반) · 추천 · 신고 |
| **랭킹** | 상위 티어 사다리(ladder) 주기적 스냅샷 랭킹 |
| **정적 데이터** | 아이템 · 룬 정보 (Data Dragon 연동) |

---

## 기술 스택

### Backend
- **Java 21**, **Spring Boot 3.5.11** (Gradle)
- **Spring Data JPA** + Hibernate (`ddl-auto: validate` — 스키마는 코드가 아닌 마이그레이션으로만 관리)
- **QueryDSL 5.0** — 동적 · 타입 안전 조회 및 projection
- **MyBatis 3.0** — 복잡 조회/통계용
- **Flyway** — DB 스키마 마이그레이션(MySQL)
- **Redis** — 캐시(Spring Cache) + 전적 갱신 작업 큐
- **MySQL 8.0**
- Spring Boot Actuator (health), spring-dotenv, Lombok

### Frontend
- **React 19** + **Vite 7**
- **React Router 7** (SPA 라우팅)
- **axios** (도메인별 `api/*.js` + 공통 인스턴스/인터셉터)
- 도메인별 커스텀 훅(`hooks/`) 으로 서버 상태 · 로컬 저장(즐겨찾기 · 최근 검색) 관리

### Infra / DevOps
- **Docker Compose** (base = MySQL + Redis, dev/prod override 분리)
- **nginx** 엣지 리버스 프록시 (`/api` → 백엔드, 그 외 → 프론트 SPA)
- **GitHub Actions** CI/CD → **GHCR**(GitHub Container Registry) 비공개 이미지
- **arm64 네이티브 빌드**(GitHub-hosted ARM 러너 — 서버 aarch64 와 동일 아키텍처)
- **Trivy** 이미지 취약점 스캔, **Certbot**(Let's Encrypt) HTTPS

---

## 아키텍처

```
                         ┌─────────────────────────────────────────┐
   Browser  ──HTTPS──►   │  nginx (edge, :80/:443)                  │
                         │   /api/*  → backend                      │
                         │   /*      → frontend (정적 SPA)          │
                         └───────────┬───────────────┬─────────────┘
                                     │               │
                         ┌───────────▼──────┐   ┌────▼──────────────┐
                         │ backend (:8080)  │   │ frontend (nginx)  │
                         │ Spring Boot      │   │ React + Vite 빌드 │
                         └───┬────┬────┬────┘   └───────────────────┘
                             │    │    │
                 ┌───────────┘    │    └──────────────┐
                 ▼                ▼                    ▼
         ┌──────────────┐  ┌────────────┐    ┌──────────────────┐
         │  MySQL 8.0   │  │  Redis 7   │    │  Riot Games API  │
         │  (JPA/Flyway)│  │ 캐시 + 큐  │    │ (kr / asia)      │
         └──────────────┘  └────────────┘    └──────────────────┘
```

### 계층 구조 (backend)
의존 방향은 항상 안쪽을 향합니다 — `Controller → Service → Domain / Repository → Entity`.

```
com.recordsite.backend
├── controller   REST 엔드포인트 (@RestController)
├── service      흐름 제어(조율) + Riot API 클라이언트(Riot*Client) + 스케줄러/워커
├── domain       순수 도메인 모델 (LadderScore, ParticipantBuildOrder)
├── entity       JPA 엔티티 (Summoner, Match, Participant, Champion, ...)
├── repository   Spring Data JPA + QueryDSL Custom
├── dto          요청/응답 record DTO
├── config       캐시 · 레이트리미터 · 초기 데이터 로더
├── exception    도메인 커스텀 예외
└── support      보조 유틸(비밀번호 인코더 등)
```

---

## 핵심 설계

### 1. 비동기 전적 갱신 작업 큐 (Redis)
"전적 갱신"은 최근 매치 최대 20개 × (매치 상세 + 타임라인)으로 **수십 번의 Riot API 호출이 순차로** 일어나
동기로 처리하면 10~30초간 HTTP 요청이 블로킹됩니다. 이를 다음 구조로 개선했습니다.

1. `POST /api/matches/refresh` → 중복 갱신 락(`SET NX EX`) 확보 후 큐에 넣고 **즉시 `jobId` 응답**
2. 전용 워커가 `BRPOP` 으로 소비 → 신규 매치만 증분 수집(동시 N개) → 진행률(`total`/`done`) 갱신
3. 프론트가 `GET /api/matches/refresh-jobs/{jobId}` 를 2.5초 간격 폴링 → 완료 시 재조회

> 자세한 설계와 트레이드오프: [`docs/refresh-job-queue.md`](docs/refresh-job-queue.md)

### 2. 매치망 점진 크롤러
검색된 유저가 만난 동료 `puuid` 를 백그라운드로 따라가며 매치 데이터를 누적합니다.
일일 처리량 상한 · 확장 깊이를 설정으로 제한해 폭주를 방지합니다.

### 3. 공유 레이트 리미터
개발용 키 한도(20 req/s, 100 req/2분)보다 살짝 낮게(18 req/s, 95 req/2분) 잡아 429 여유를 확보하며,
모든 Riot 호출(갱신 · 크롤러 · 랭킹)이 **하나의 예산을 공유**합니다.

### 4. 랭킹 스냅샷 스케줄러
실시간이 아닌 주기적(기본 1시간) 스냅샷으로 상위 티어 사다리를 적재해 이름 해소 호출량을 제한합니다.

### 5. Redis 캐시
무거운 집계 · 정적 데이터 응답을 캐싱해 응답 지연과 DB 부하를 줄입니다.

---

## 프로젝트 구조

```
record site/
├── backend/                Spring Boot 애플리케이션
│   ├── src/main/java/com/recordsite/backend/
│   ├── src/main/resources/
│   │   ├── application.yaml
│   │   └── db/migration/    Flyway 마이그레이션 (V1__init_schema.sql ...)
│   └── build.gradle
├── frontend/               React + Vite SPA
│   ├── src/{api,components,pages,hooks,constants}/
│   ├── public/cdn/          번들된 Data Dragon 정적 에셋
│   └── package.json
├── nginx/                   엣지 nginx 설정(http / https)
├── scripts/
│   ├── deploy.sh            서버 배포 스크립트(digest 고정 · IOC 게이트 · DB 백업 · health 검증 · 자동 롤백)
│   └── refresh-riot-key.sh  Riot 개발 키 갱신 헬퍼
├── docs/                    설계 · 운영 문서
├── docker-compose*.yml      base + dev/prod/ghcr/certbot override
└── .github/workflows/       ci.yml (빌드·푸시) / cd.yml (배포)
```

---

## 로컬 개발 환경

### 사전 준비
- JDK 21, Node.js 18+
- Docker / Docker Compose
- **Riot API 개발 키** ([Riot Developer Portal](https://developer.riotgames.com/))

### 1) 인프라 기동 (MySQL + Redis)
개발 모드는 인프라만 컨테이너로 띄우고 호스트 포트를 엽니다(MySQL `3307`, Redis `6379`).

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

### 2) 백엔드 실행
`backend/.env` 를 만들고(아래 [환경 변수](#환경-변수) 참고) IDE 또는 CLI 로 실행합니다.

```bash
cd backend
./gradlew bootRun      # → http://localhost:8080
```
> `application.yaml` 기본값이 `localhost:3307`(DB) / `localhost:6379`(Redis) 라 추가 설정 없이 위 인프라에 붙습니다.
> Redis 없이 돌리려면 `SPRING_CACHE_TYPE=none` 으로 캐시를 끌 수 있습니다.

### 3) 프론트엔드 실행
```bash
cd frontend
npm install
npm run dragon:download   # (최초 1회) Data Dragon 정적 에셋 다운로드
npm run dev               # → http://localhost:5173 ( /api 는 :8080 으로 프록시)
```

---

## 환경 변수

루트 `.env`(운영) 또는 `backend/.env`(개발)에 아래 값을 둡니다. `.env.example` 참고.

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `RIOT_API_KEY` | Riot API 키 (**필수**) | — |
| `DB_PASSWORD` | MySQL root 비밀번호 (MySQL · 백엔드 공유) | `1234` |
| `DB_HOST` / `DB_PORT` | DB 호스트/포트 | `localhost` / `3307` |
| `REDIS_HOST` / `REDIS_PORT` | Redis 호스트/포트 | `localhost` / `6379` |
| `SPRING_CACHE_TYPE` | 캐시 타입 (`redis` / `none`) | `redis` |

> 시크릿(`.env`)은 git 에 커밋하지 않습니다.

---

## 데이터베이스 마이그레이션 (Flyway)

스키마는 Hibernate 가 아닌 **Flyway 마이그레이션으로만** 관리합니다(`ddl-auto: validate`).
엔티티 · 컬럼 추가 시 아래 형식으로 파일을 만들면 앱 기동 시 Flyway 가 자동 실행합니다.

```
src/main/resources/db/migration/V{yyyyMMddHHmmss}__{설명}.sql
```

운영 DB 에 직접 DDL 을 실행하지 않으며, 엔티티 변경과 마이그레이션 파일은 같은 커밋에 포함합니다.

---

## 주요 API

모든 엔드포인트는 `/api` 프리픽스를 사용합니다.

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `GET`  | `/api/summoners?...` · `/api/summoners/search` | 소환사 조회 · 검색 |
| `GET`  | `/api/matches?puuid=` | 매치 목록 |
| `GET`  | `/api/matches/{matchId}/summary` · `/{matchId}/timeline` | 매치 상세 · 타임라인 |
| `POST` | `/api/matches/refresh` | 전적 갱신 요청(작업 큐 등록) |
| `GET`  | `/api/matches/refresh-jobs/{jobId}` | 갱신 진행률 폴링 |
| `GET`  | `/api/live-game?puuid=` | 실시간 인게임 정보 |
| `GET`  | `/api/league` | 리그(랭크) 정보 |
| `GET`  | `/api/champion-mastery?puuid=` | 챔피언 숙련도 |
| `GET`  | `/api/champion-stats/tier-list` · `/{championId}/detail` | 챔피언 티어리스트 · 상세 |
| `GET`  | `/api/champion-rotation` | 무료 로테이션 |
| `GET`/`POST`/`PUT`/`DELETE` | `/api/champion-tips` (+ `/vote`, `/report`) | 챔피언 팁 CRUD · 추천 · 신고 |
| `GET`  | `/api/ranking` | 상위 티어 랭킹 |
| `GET`  | `/api/champions?name=` · `/api/items` · `/api/runePaths` | 정적 데이터 |

---

## CI/CD & 배포

**CI** — `main` 푸시 시 GitHub Actions 가 arm64 네이티브로 백엔드/프론트 이미지를 빌드해 GHCR 에 푸시합니다
(이미지 태그 = commit SHA + `latest`). Trivy 로 취약점을 스캔합니다(현재 report-only).

**CD** — CI 성공 시 이어서 서버로 SSH 자동 배포합니다. `scripts/deploy.sh` 가 다음 안전장치를 수행합니다.

1. GHCR 로그인(단기 토큰) 후 commit-sha 태그 이미지 pull
2. **digest 고정** — 스캔한 그 이미지 그대로 배포(태그 덮어쓰기 무력화)
3. **IOC 스캔 게이트** — 크립토마이너/C2 지표 검사, 발견 시 배포 차단
4. **DB 백업**(mysqldump, 최근 10개 보관)
5. 롤백용 현재 이미지 기록 → digest 고정으로 배포(서버 재빌드 없음)
6. **배포 후 health 검증**(`/actuator/health` UP), 실패 시 **자동 롤백**

수동 재배포/롤백은 `workflow_dispatch` 로 특정 태그를 지정해 실행할 수 있습니다.

### 운영 스택 기동 (참고)
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

> HTTPS 설정: [`docs/https-setup.md`](docs/https-setup.md)

---

## 라이선스

이 저장소의 라이선스는 [`LICENSE`](LICENSE) 를 참고하세요.
League of Legends 및 관련 자산은 Riot Games, Inc. 의 자산입니다. 본 프로젝트는 비공식 팬 프로젝트입니다.
