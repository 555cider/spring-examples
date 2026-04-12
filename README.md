# spring-examples

Spring Boot 4 sample workspace with:

- `auth`: servlet authorization server
- `gateway`: reactive API gateway
- `client`: servlet OAuth2 client
- `nacos`: service discovery for local runs

## Auth persistence

- Auth uses PostgreSQL schema `my_schema`.
- Runtime schema initialization lives in `auth/src/main/resources/sql/*.sql`.
- Fresh Docker-local seed data lives in `postgres/init/init.sql`.
- Current auth schema uses `oauth2_registered_client`, `oauth2_authorization`, `oauth2_authorization_consent`, `users`, and `user_authorities`.
- Legacy `my_schema.client` and `users.authorities` are cleaned up automatically during schema initialization.

Detailed notes are in `auth/README.md`.

## Local infrastructure

```bash
cp .env.example .env
docker compose up -d nacos postgres redis
```

## Local full stack

```bash
docker compose up -d --build
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
