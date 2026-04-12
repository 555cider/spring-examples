# RBAC and ABAC Gateway Design

## Goal

Add a narrow, testable authorization example to this workspace that demonstrates both role-based access control (RBAC) and attribute-based access control (ABAC) using the existing `auth` and `gateway` modules.

## Scope

This design intentionally keeps the surface area small.

- Reuse the existing `auth` module as the identity provider and token issuer.
- Reuse the existing `gateway` module as the protected resource server.
- Add sample protected API endpoints in `gateway` that clearly show the difference between RBAC and ABAC.
- Reuse the existing `users` and `user_authorities` data model for RBAC.
- Keep the first ABAC example small by using a narrow ownership policy instead of building a general policy engine.

Out of scope for this iteration:

- a generic policy administration UI
- a full external policy engine
- multi-tenant policy modeling
- persistent document CRUD
- client UI changes beyond what is needed for existing OAuth login flow

## Current State

The codebase already has the pieces needed to bootstrap authorization work.

- `auth` stores user authorities in `my_schema.user_authorities`.
- `auth` loads those authorities into Spring Security `UserDetails`.
- `auth` currently authenticates users but does not add role data into access token claims.
- `gateway` currently validates JWTs but only requires authentication for protected routes.
- There are no sample protected REST endpoints that show role-based or ownership-based authorization decisions.

## Recommended Approach

Implement the first end-to-end authorization example inside `gateway`, with `auth` responsible for issuing role claims.

Why this approach:

- It uses the existing module boundaries instead of adding a new service.
- It keeps the example small enough to verify with focused tests.
- It demonstrates both issuer-side and resource-server-side authorization concerns.
- It gives a clean migration path from simple RBAC to more contextual ABAC rules.

## Architecture

### Auth module responsibilities

- Continue to authenticate local users from PostgreSQL.
- Seed at least two users for demonstration:
  - `user` with `ROLE_USER`
  - `admin` with `ROLE_ADMIN`
- Add JWT customization so access tokens include a `roles` claim populated from the authenticated principal authorities.

### Gateway module responsibilities

- Continue to operate as an OAuth2 resource server.
- Add JWT authority conversion so the `roles` claim becomes Spring Security authorities.
- Expose sample protected API endpoints.
- Apply RBAC to one endpoint and ABAC to another.

### Sample resource model

The first ABAC example will use a small in-memory document catalog inside `gateway`.

Each document only needs:

- `id`
- `title`
- `ownerUsername`

This keeps the example focused on authorization rules, not persistence or CRUD mechanics.

## Data Model

### Existing schema reused

RBAC will continue to use:

- `my_schema.users`
- `my_schema.user_authorities`

No new role tables are required.

### Seed data changes

The seed process in `auth` will be extended to ensure the following users exist:

- `user` / `1234` with `ROLE_USER`
- `admin` / `1234` with `ROLE_ADMIN`

If the users already exist, the seed logic should repair missing authorities instead of duplicating rows.

### Optional schema evolution

Database schema changes are allowed, but they are not required for the first iteration.

The recommended first cut does not add a `documents` table. If the example later grows beyond a static ownership policy, a separate persistence-backed document model can be introduced in a follow-up change.

## Token and Authentication Flow

1. A user authenticates through the `auth` server.
2. `auth` loads the user and their authorities from PostgreSQL.
3. During access token issuance, `auth` adds a `roles` claim to the JWT.
4. `gateway` validates the JWT and converts the `roles` claim into `GrantedAuthority` values.
5. Endpoint authorization decisions in `gateway` use those authorities and, for ABAC, resource ownership attributes.

### JWT claim contract

The access token will include:

- `roles`: array of strings such as `["ROLE_USER"]` or `["ROLE_ADMIN"]`

This claim should be stable and explicit so the resource server does not need to reconstruct role names from unrelated claims.

## Authorization Model

### RBAC rule

`RBAC` will be demonstrated with an admin-only endpoint.

- Endpoint: `/api/admin/reports`
- Rule: caller must have `ROLE_ADMIN`

This should be enforced using standard Spring Security authorization, either by request matcher or method security. Method-level checks are preferred if the controller surface is small and explicit.

### ABAC rule

`ABAC` will be demonstrated with a document ownership rule.

- Endpoint: `/api/documents/{id}`
- Rule: caller is allowed if they own the document or if they have `ROLE_ADMIN`

Attributes used in the rule:

- subject attributes:
  - authenticated username
  - authenticated authorities
- resource attributes:
  - `document.ownerUsername`

The ABAC rule should be implemented as a dedicated policy component in `gateway`, not embedded as ad hoc controller logic. That keeps the authorization decision readable and testable.

## API Surface

### `/api/me`

Purpose:

- return basic authenticated principal details
- help verify that JWT decoding and role mapping work as expected

Expected behavior:

- authenticated callers receive their username and authorities

### `/api/admin/reports`

Purpose:

- show a pure RBAC-protected endpoint

Expected behavior:

- `ROLE_ADMIN` succeeds
- non-admin authenticated users are rejected with `403`

### `/api/documents/{id}`

Purpose:

- show an ABAC-protected endpoint based on resource ownership

Expected behavior:

- document owner succeeds
- `ROLE_ADMIN` succeeds even when not owner
- other authenticated users receive `403`
- unknown document id returns `404`

## Error Handling

Authorization failures should follow standard resource server behavior.

- unauthenticated requests: `401`
- authenticated but unauthorized requests: `403`
- missing resource: `404`

The ABAC policy should not leak unnecessary internal details about why a different user owns a resource.

## Testing Strategy

### Auth module tests

Add tests that verify:

- seeded users include expected authorities
- access tokens issued for authenticated users include the `roles` claim
- the `admin` user can be loaded with `ROLE_ADMIN`

### Gateway module tests

Add focused tests for:

- authenticated user can call `/api/me`
- `ROLE_USER` cannot call `/api/admin/reports`
- `ROLE_ADMIN` can call `/api/admin/reports`
- document owner can call `/api/documents/{id}`
- non-owner `ROLE_USER` cannot call `/api/documents/{id}`
- `ROLE_ADMIN` can call another user’s document
- unknown document id returns `404`

Tests should avoid requiring a live authorization server. JWT-backed controller or web-layer tests with locally supplied claims are sufficient for the gateway side.

## Implementation Notes

- Follow existing Spring Security patterns already present in each module.
- Keep the claim name explicit as `roles`.
- Keep the ABAC example narrow and deterministic.
- Prefer small, testable components over packing policy logic into a single config class.

## Risks and Tradeoffs

### In-memory document catalog

Pros:

- fastest path to a clear ABAC example
- no extra persistence complexity

Cons:

- not production-like persistence
- ownership data is static

This tradeoff is acceptable because the goal of this iteration is to demonstrate authorization patterns, not resource storage design.

### Custom role claim

Pros:

- explicit contract between issuer and resource server
- easier to test than inferring roles from other claims

Cons:

- requires custom mapping on the resource server

This tradeoff is acceptable because the claim contract is simple and local to this example workspace.

## Acceptance Criteria

The design is complete when all of the following are true:

- `auth` seeds both `user` and `admin` with their expected authorities
- access tokens contain a `roles` claim
- `gateway` maps `roles` into Spring Security authorities
- `/api/admin/reports` enforces admin-only RBAC
- `/api/documents/{id}` enforces owner-or-admin ABAC
- the behavior is covered by automated tests in `auth` and `gateway`
