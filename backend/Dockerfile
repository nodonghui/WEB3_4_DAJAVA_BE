# 첫 번째 스테이지: 빌드 스테이지
FROM gradle:jdk-21-and-23-graal-jammy AS builder

# 작업 디렉토리 설정
WORKDIR /app

# ✅ Gradle 래퍼 관련 파일 복사
COPY gradlew .
COPY gradle gradle
RUN chmod +x ./gradlew

# ✅ 프로젝트 설정 파일 복사
COPY build.gradle .
COPY settings.gradle .

# ✅ 소스 코드 복사
COPY src src

# ✅ 애플리케이션 빌드
RUN ./gradlew build -x test -x sentryBundleSourcesJava --no-daemon

# 두 번째 스테이지: 실행 스테이지
FROM ghcr.io/graalvm/jdk-community:21

# 작업 디렉토리 설정
WORKDIR /app

# 첫 번째 스테이지에서 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 실행할 JAR 파일 지정
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
