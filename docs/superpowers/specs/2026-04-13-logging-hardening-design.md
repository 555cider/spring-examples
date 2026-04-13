# Logging Hardening Design

## Goal

Standardize application logging for `auth`, `client`, and `gateway` so log naming, rolling, and environment behavior are predictable and production-friendly.

## Current State

- Each module has its own `logback-spring.xml`, but all three files are duplicated.
- Logs write to `../logs`, which depends on the launch directory.
- Rolling files are not compressed and do not set `maxHistory`.
- The config declares duplicate `root` loggers and uses `DEBUG` as a default root level.

## Decision

Keep per-module `logback-spring.xml` files, but normalize them to one shared convention.

- Use `spring.application.name` as the active log filename: `${app}.log`
- Use compressed archive files under an `archive/` subdirectory:
  - `${app}.yyyy-MM-dd.i.log.gz`
- Drive log directory from `app.logging.path`, defaulting to `./logs`
- Use profile-specific roots:
  - `default | local | test`: console only
  - `dev | prod`: console + async rolling file
- Keep root level at `INFO`
- Let `com.example` log at `DEBUG` only in `local`

## Rolling Policy

- `SizeAndTimeBasedRollingPolicy`
- `maxFileSize`: `20MB`
- `maxHistory`: `30`
- `totalSizeCap`: `1GB`
- `cleanHistoryOnStart`: `true`

## Naming and Pattern

- Active file: `${LOG_PATH}/${springAppName}.log`
- Archive file: `${LOG_PATH}/archive/${springAppName}.%d{yyyy-MM-dd}.%i.log.gz`
- Line format includes timestamp, level, application name, thread, logger, and message

## Testing

Add a small test per module that reads `logback-spring.xml` from the classpath and asserts:

- archive naming uses `.log.gz`
- history and size limits are declared
- console-only vs file-enabled profile sections are present
- the legacy `../logs` path is gone
