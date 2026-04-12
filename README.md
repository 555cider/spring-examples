# spring-examples

Spring Boot 4 sample workspace with:

- `auth`: servlet authorization server
- `gateway`: reactive API gateway
- `client`: servlet OAuth2 client
- `nacos`: service discovery for local runs

## Local infrastructure

```bash
docker compose up -d nacos postgres redis
```

## Local apps

```bash
./gradlew :auth:bootRun --args='--spring.profiles.active=local'
./gradlew :gateway:bootRun --args='--spring.profiles.active=local'
./gradlew :client:bootRun --args='--spring.profiles.active=local'
```

## Verification

```bash
./gradlew test
```
