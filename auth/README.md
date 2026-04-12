# auth

`auth` is the servlet-based authorization server module. It persists OAuth2 server state and local user credentials in PostgreSQL schema `my_schema`.

## Schema ownership

- Runtime schema source of truth: `src/main/resources/sql/oauth2-registered-client.sql`, `src/main/resources/sql/oauth2-authorization.sql`, `src/main/resources/sql/oauth2-authorization-consent.sql`, `src/main/resources/sql/users.sql`
- Fresh local Docker seed script: `../postgres/init/init.sql`
- JDBC repositories and services: `src/main/java/com/example/auth/config/AuthorizationServerConfig.java`, `src/main/java/com/example/auth/service/JdbcUserDetailsService.java`

## Active tables

- `my_schema.oauth2_registered_client`
  - Spring Authorization Server registered clients
- `my_schema.oauth2_authorization`
  - persisted authorization codes, access tokens, refresh tokens, device/user codes
- `my_schema.oauth2_authorization_consent`
  - consented authorities per client and principal
- `my_schema.users`
  - local users with `username`, `password`, `email`, `created_at`, `updated_at`
- `my_schema.user_authorities`
  - normalized user roles/authorities keyed by `(user_id, authority)`

## Initialization and migration behavior

- `spring.sql.init` runs the auth SQL scripts for `local`, `dev`, and `test`.
- The scripts are idempotent and perform compatibility cleanup:
  - drop legacy `my_schema.client` if it still exists
  - create `user_authorities` if missing
  - migrate legacy `users.authorities` values into `user_authorities`
  - remove the legacy `users.authorities` column after migration
  - normalize `created_at` and `updated_at` to `timestamp with time zone`
  - install a trigger so `updated_at` refreshes on every update
- `postgres/init/init.sql` is only used by PostgreSQL container bootstrap on a fresh data directory. It creates the same effective schema and seed rows for local Docker runs.

## Seed data

- Default local user:
  - username: `user`
  - password: `1234`
  - authority: `ROLE_USER`
- Default local admin:
  - username: `admin`
  - password: `1234`
  - authority: `ROLE_ADMIN`
- Default local OAuth2 client:
  - client id: `client_id_1`
  - client secret: `client_secret_1`
  - grant types: authorization code, refresh token

The application also seeds the same logical defaults at startup if they are absent, so an existing development database can self-heal missing rows.

## Operational notes

- If you want a completely fresh Docker-local database, remove the disposable local Postgres volume data under `postgres/data` before starting the stack again.
- If you keep an existing local database, the auth module startup scripts handle the schema drift that existed before the `user_authorities` normalization.
