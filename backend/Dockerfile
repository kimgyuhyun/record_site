# jar 를 어디서 가져올지 고른다.
#   builder(기본) = 이 파일 하나로 소스에서 jar 까지 만든다. 로컬 `docker build ./backend` 와
#                   docker-compose.prod.yml 의 `build: ./backend` 가 이 경로를 쓴다.
#   ctxjar        = CI 전용. test 잡이 테스트를 통과시킨 그 bootJar 를 빌드 컨텍스트의 app.jar 로 받는다.
#                   이미지 안에서 Gradle 을 다시 돌리지 않으므로 테스트한 산출물과 배포하는 산출물이
#                   같은 빌드다(PLATFORM 7절 — 같은 산출물을 이미지 안에서 다시 만들지 않는다).
# COPY --from=${ARG} 는 buildkit 이 거부한다("variable expansion is not supported for --from").
# FROM 으로 한 번 받아 별칭을 만드는 것이 공식 우회다. 고르지 않은 스테이지는 buildkit 이 건너뛴다.
ARG JAR_SOURCE=builder

# ── 1단계(builder): Gradle 빌드 (JDK 21) ──
# 테스트는 DB가 필요하고 이미지 빌드 단계에선 띄울 수 없어 제외(-x test)한다.
# 베이스 이미지는 digest 로 고정한다. 태그가 가리키는 이미지가 바뀌어도 빌드가 조용히 달라지지 않고 Dependabot PR 로 드러난다.
FROM eclipse-temurin:24-jdk@sha256:7493205ffe6caa8074fa8a06a276bb1c5ac41d3dd0fd43a0db66d7f776e80b3e AS builder
WORKDIR /app

# 래퍼/빌드 스크립트 먼저 복사해 의존성 레이어를 캐싱한다.
COPY gradlew gradlew.bat settings.gradle build.gradle gradle.lockfile ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q || true

# 소스 복사 후 실행 가능한 단일 jar 빌드
COPY src ./src
RUN ./gradlew clean bootJar -x test --no-daemon && cp build/libs/*.jar /app/app.jar

# ── 1단계(ctxjar): CI 가 넘긴 jar ──
# 파일 하나를 담아 두는 스테이지라 베이스가 필요 없다. JAR_SOURCE=builder 이면 건너뛰므로
# 컨텍스트에 app.jar 가 없어도 기본 빌드는 영향받지 않는다.
FROM scratch AS ctxjar
COPY app.jar /app/app.jar

FROM ${JAR_SOURCE} AS jarsrc

# ── 2단계: 런타임 (JRE 21, 가벼움) ──
FROM eclipse-temurin:24-jre@sha256:8cb2387a28af84cf0db0948d9c67d4480192f4e567027a3963f145d218e8b4f2
WORKDIR /app

# 컨테이너 메모리 한도에 맞춰 힙을 자동 설정. 필요 시 compose 의 JAVA_TOOL_OPTIONS 로 override.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

COPY --from=jarsrc /app/app.jar app.jar

# 비root 실행 — 컨테이너가 뚫려도 root 권한을 얻지 못하게 한다.
# 시스템 계정(로그인 불가)으로 만들고, jar 은 읽기만 하면 되므로 소유권은 root 로 남긴다.
# compose 에서 read_only + tmpfs /tmp 로 띄우므로 앱이 쓰는 경로는 /tmp 뿐이어야 한다.
RUN groupadd --system --gid 10001 appuser \
 && useradd --system --uid 10001 --gid 10001 --no-create-home --shell /usr/sbin/nologin appuser
USER appuser

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
