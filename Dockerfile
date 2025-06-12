FROM --platform=linux/amd64 gradle:jdk21 AS builder
WORKDIR /app
COPY . .

# gradlew 파일에 실행 권한 추가
RUN chmod +x ./gradlew

# application-build.yml 없이 빌드 실행
RUN SPRING_PROFILES_ACTIVE=prod ./gradlew build

FROM --platform=linux/amd64 openjdk:21-jdk-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar /app/app.jar

# 최종 이미지에서는 prod 프로필 사용
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "/app/app.jar"]