# Spring Boot 4 + Nacos Modernization Design

## Summary

This repository will be modernized from a mixed Spring Boot 3.2/3.3 and Spring Cloud Netflix Eureka setup into a Spring Boot 4 baseline centered on the latest Nacos-supported stack currently documented by Spring Cloud Alibaba:

- Spring Boot 4.0.0
- Spring Framework 7
- Spring Security 7 authorization server support
- Spring Cloud 2025.1.0
- Spring Cloud Alibaba 2025.1.0.0
- Nacos 3.1.1 for service discovery only
- Java 21

The current `discovery` module will be removed. Service registration and lookup will move to Nacos Discovery. The `auth` module will be rebuilt as a servlet-based authorization server using the Spring Security 7 authorization server model. The `gateway` module will stay reactive. The `client` module will move to a servlet MVC OAuth2 client to align with the authentication flow and reduce unnecessary reactive complexity.

Docker Compose remains a lightweight local-run convenience only. It does not define the target architecture.

## Goals

- Upgrade the project to a coherent Spring Boot 4 stack on an officially documented Nacos-compatible baseline.
- Replace Eureka with an OSS service registry that works with the chosen stack.
- Keep the `auth`, `gateway`, and `client` modules, but modernize their roles and internals.
- Move the authorization server onto the officially supported Spring Security 7 model.
- Remove reactive/blocking hybrids in the auth path.
- Keep configuration simple: local `application.yml` plus environment variables.

## Non-Goals

- Do not introduce Nacos Config in this phase.
- Do not redesign the project around Kubernetes.
- Do not replace the in-house auth server with Keycloak or another external IdP.
- Do not turn Docker Compose into the target deployment architecture.
- Do not preserve every existing endpoint or flow if it conflicts with the latest recommended model.

## Current Problems

- Spring Boot plugin versions and starter versions are inconsistent across modules.
- Spring Cloud versions are inconsistent across modules.
- Eureka is embedded into the repository via a dedicated `discovery` module, which adds platform-specific infrastructure into the app codebase.
- The `auth` module uses a reactive security stack for an authorization server, while the supported Spring Security 7 authorization server model is servlet-based.
- The `auth` module mixes reactive repositories with blocking calls, which is both brittle and misleading.
- Redis is currently used in the auth core persistence path even though the authorization server model is better served by JDBC-backed persistence.
- The `client` module uses WebFlux even though its responsibilities are login, session, and redirect-oriented browser flows.

## Target Architecture

### Module Layout

- `gateway`
  - Reactive edge service
  - Spring Cloud Gateway Server WebFlux
  - JWT resource server
  - Nacos Discovery client
- `auth`
  - Servlet authorization server
  - Spring Security 7 authorization server support
  - JDBC-backed persistence in PostgreSQL
  - Nacos Discovery client
- `client`
  - Servlet MVC OAuth2 client
  - Browser-facing demo/client app
  - Nacos Discovery client
- `discovery`
  - Removed

### Service Discovery

Service lookup will move from Eureka to Nacos Discovery.

- Each service registers itself in Nacos using `spring.application.name`.
- Gateway routes continue to use logical names such as `lb://auth`.
- Inter-service resolution is handled through Spring Cloud LoadBalancer backed by Nacos Discovery.
- Nacos Config is explicitly out of scope for this phase.

This keeps discovery externalized without forcing a full configuration-center migration.

## Runtime Model by Module

### Gateway

`gateway` remains reactive because it is an edge router and filter pipeline.

- Replace `spring-cloud-starter-gateway` with `spring-cloud-starter-gateway-server-webflux`.
- Keep JWT resource server behavior.
- Prefer `issuer-uri`-based validation and metadata discovery over custom key distribution.
- Keep Redis only if rate limiting remains enabled and justified.

### Auth

`auth` becomes a standard servlet authorization server.

- Replace WebFlux security with servlet `HttpSecurity`.
- Use the Spring Security 7 authorization server model and Boot 4 starter support.
- Expose the standard authorization server endpoints, including OIDC metadata and JWK set.
- Use PostgreSQL with JDBC-backed repositories for:
  - registered clients
  - authorizations
  - authorization consents
- Keep application-specific user authentication integrated through `UserDetailsService` and password encoding.
- Remove reactive auth persistence and any `block()`-based repository bridging.

### Client

`client` becomes a servlet MVC OAuth2 client.

- Use session-oriented browser login flows.
- Keep Thymeleaf for the browser UI unless a later requirement changes the presentation layer.
- Register with Nacos so the gateway and local tests can resolve it consistently.

## Data and Persistence

### PostgreSQL

PostgreSQL remains the primary durable store for `auth`.

Planned use:

- authorization server tables for registered clients, authorizations, and consents
- application user data already managed by `auth`

Persistence access in `auth` will be consolidated around JDBC/JPA-style blocking access rather than reactive R2DBC access. This matches the servlet runtime and avoids reactive/blocking mixing.

### Redis

Redis is removed from the auth core persistence path.

Allowed remaining uses:

- gateway rate limiting
- short-lived caching where there is a clear need
- future session externalization if required

If those uses are not needed after the refactor, Redis can be removed from the baseline later.

## Configuration Strategy

Configuration remains local-file and environment-variable driven.

- Keep `application.yml` as the main configuration source for each module.
- Keep per-environment settings in profile sections or environment variables.
- Add Nacos Discovery connection properties only.
- Do not add `bootstrap.yml`.
- Do not add Nacos Config in this phase.

This keeps the modernization focused on runtime architecture rather than distributed configuration concerns.

## Version Baseline

The repository should converge on the following baseline:

- Java 21
- Gradle wrapper in the repository root
- Spring Boot 4.0.0
- Spring Cloud 2025.1.0
- Spring Cloud Alibaba 2025.1.0.0
- Nacos Server 3.1.1

Module versions must be centralized to avoid the current drift between plugin and starter declarations.

Spring Boot 4.0.5 is the newer general Boot release as of April 11, 2026, but this design intentionally pins to the latest version line explicitly documented by Spring Cloud Alibaba for Nacos support. A later patch-level bump beyond Boot 4.0.0 is allowed only after dependency resolution and integration behavior are verified against the actual repository.

## Migration Sequence

1. Add a root Gradle wrapper and centralize dependency version management.
2. Remove the `discovery` module and all Eureka dependencies and configuration.
3. Add Nacos Discovery to `gateway`, `auth`, and `client`.
4. Upgrade `gateway` to the Boot 4.0.0 and Cloud 2025.1.0 gateway starter and verify `lb://` routing through Nacos.
5. Convert `auth` from WebFlux to servlet and rebuild its security configuration around the Spring Security 7 authorization server model.
6. Replace auth persistence internals with JDBC-backed authorization server repositories in PostgreSQL.
7. Convert `client` from WebFlux to servlet MVC OAuth2 client.
8. Retain or remove Redis based on whether gateway rate limiting remains part of the baseline.
9. Update local run assets and documentation so local execution still works after the architecture change.

## Verification Plan

The work is complete only when all of the following are true:

- Each module compiles on the new baseline.
- Tests exist for the new auth configuration and critical login flow behavior.
- `auth` exposes valid authorization server metadata and JWK set endpoints.
- Authorization code flow succeeds end to end.
- `gateway` resolves `auth` through Nacos and routes successfully.
- `gateway` validates JWTs using issuer metadata from `auth`.
- `client` can initiate login, handle the callback, and access a protected page.
- `gateway`, `auth`, and `client` appear as healthy registrations in Nacos.
- Local development instructions still work without the removed `discovery` module.

## Risks and Mitigations

### Auth Runtime Conversion Risk

Converting `auth` from WebFlux to servlet changes the entire security and persistence model.

Mitigation:

- Treat `auth` as a structured rewrite within the existing module, not an incremental tweak.
- Verify the OIDC metadata and authorization code flow before moving on to later cleanup.

### Dependency Compatibility Risk

Boot 4.0.0, Cloud 2025.1.0, and Spring Cloud Alibaba 2025.1.0.0 must be kept aligned.

Mitigation:

- Centralize versions in one place.
- Verify dependency resolution early before deeper code migration.

### Configuration Drift Risk

Removing Eureka and adding Nacos changes service registration and local wiring.

Mitigation:

- Keep config changes minimal in this phase.
- Defer Nacos Config entirely.

## Accepted Design Decisions

- Eureka is replaced with Nacos Discovery.
- Nacos Config is not included.
- `auth` remains an in-house authorization server.
- `auth` moves to servlet.
- `client` moves to servlet.
- `gateway` remains reactive.
- Docker Compose remains a local convenience, not the architecture driver.
