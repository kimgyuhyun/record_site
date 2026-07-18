# ── 1단계: Gradle 빌드 (JDK 21) ──
# 테스트는 DB가 필요하고 이미지 빌드 단계에선 띄울 수 없어 제외(-x test)한다.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 래퍼/빌드 스크립트 먼저 복사해 의존성 레이어를 캐싱한다.
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q || true

# 소스 복사 후 실행 가능한 단일 jar 빌드
COPY src ./src
RUN ./gradlew clean bootJar -x test --no-daemon

# ── 2단계: 런타임 (JRE 21, 가벼움) ──
FROM eclipse-temurin:21-jre
WORKDIR /app

# 컨테이너 메모리 한도에 맞춰 힙을 자동 설정. 필요 시 compose 의 JAVA_TOOL_OPTIONS 로 override.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

COPY --from=build /app/build/libs/*.jar app.jar

# 비root 실행 — 컨테이너가 뚫려도 root 권한을 얻지 못하게 한다.
# 시스템 계정(로그인 불가)으로 만들고, jar 은 읽기만 하면 되므로 소유권은 root 로 남긴다.
# compose 에서 read_only + tmpfs /tmp 로 띄우므로 앱이 쓰는 경로는 /tmp 뿐이어야 한다.
RUN groupadd --system --gid 10001 appuser \
 && useradd --system --uid 10001 --gid 10001 --no-create-home --shell /usr/sbin/nologin appuser
USER appuser

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
