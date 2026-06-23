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
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
