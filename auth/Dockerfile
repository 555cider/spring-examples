FROM gradle:8.14.3-jdk21 AS builder
WORKDIR /workspace
COPY . .
RUN ./gradlew :auth:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/auth/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
