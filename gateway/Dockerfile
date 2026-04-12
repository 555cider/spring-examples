FROM gradle:8.14.3-jdk21 AS builder
WORKDIR /workspace
COPY . .
RUN ./gradlew :gateway:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/gateway/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
