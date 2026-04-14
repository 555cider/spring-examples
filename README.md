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

## Demo defaults

- `.env.example` contains runnable demo defaults for Docker Compose.
- It only keeps the environment variables currently used by Compose and the app modules.
- Demo users:
  - `user` / `1234`
  - `teammate` / `1234`
  - `outsider` / `1234`
  - `admin` / `1234`
- Demo tenants:
  - `tenant-alpha`: `user`, `teammate`, `admin`
  - `tenant-bravo`: `outsider`
- Demo OAuth client:
  - `AUTH_CLIENT_ID=client_id_1`
  - `AUTH_CLIENT_SECRET=client_secret_1`
- For stricter non-demo runs, set `AUTH_DEMO_USERS_ENABLED=false` and `AUTH_DEMO_CLIENT_ENABLED=false`.

## Local infrastructure

```bash
cp .env.example .env
docker compose up -d nacos postgres redis
```

## Local full stack

```bash
docker compose up -d --build
```

Compose uses the values from `.env`. The provided example keeps the stack demo-friendly while still allowing you to turn off demo users and demo client seeding explicitly.

## Local apps

```bash
./gradlew :auth:bootRun --args='--spring.profiles.active=local'
./gradlew :gateway:bootRun --args='--spring.profiles.active=local'
./gradlew :client:bootRun --args='--spring.profiles.active=local'
```

`local` keeps demo users and demo client enabled with built-in defaults, so it works even without exporting extra environment variables.

## Verification

```bash
./gradlew test
```
