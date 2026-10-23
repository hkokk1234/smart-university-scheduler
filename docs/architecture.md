# Architecture

Smart University Scheduler uses a modular monolith: one Spring Boot application,
one PostgreSQL database, and one React client. npm workspaces manage the frontend;
Maven manages the Java application independently. No monorepo orchestration framework
is needed at this size.

## Backend boundaries

`com.smartuniversity.scheduler` is the application root.
`platform/security` contains cross-cutting HTTP security configuration. Actuator supplies
health probes. Future business modules belong in feature packages, for example
`<feature>/api`, `<feature>/application`, `<feature>/domain`, and `<feature>/infrastructure`.

- API classes handle HTTP input/output and delegate to application services.
- Application services coordinate use cases and transaction boundaries.
- Domain classes hold business rules independently of HTTP concerns.
- Infrastructure classes implement persistence and external adapters.

Features must not access another feature's persistence internals. Introduce explicit
application interfaces only when an actual use case needs them. The identity, catalog,
and scheduling packages now contain JPA entities, repositories, DTOs, and transactional
services and HTTP APIs. Authentication uses short-lived bearer JWTs. Scheduling generation
adapts catalog DTOs into an independent JDK-only CSP solver; the solver has no repository
or controller dependencies. See [API](api.md) and [solver design](solver.md).

Flyway owns schema changes. JPA validates mappings and does not create or update tables.
The first migration is an infrastructure-only baseline; V2 adds the scheduler schema.
Development enables an additional V2.1 demonstration-data migration. V3 adds identity
password hashes/roles and section optimistic locking. Open Session in
View is disabled. See [database.md](database.md) for constraints and persistence boundaries.

## Frontend boundaries

`src/app` composes the application. `src/features/health` owns the connectivity check.
`src/components/ui` contains local shadcn/ui components, and `src/lib` holds shared utilities.
Authentication, catalog, planner, schedules, administration, settings and dashboard live
in `src/features/<feature>`. See [frontend](frontend.md).

Vite proxies `/api` and `/actuator` during development, so no permissive CORS configuration
is required. Production needs equivalent same-origin routing at the reverse proxy.
Only variables with the `VITE_` prefix are exposed by Vite to browser code; never put secrets
in those variables. `BACKEND_PROXY_TARGET` is read only by Vite's Node configuration.

## Security and operations

Anonymous GET requests to Actuator's health endpoint and its groups are allowed.
Catalog GET endpoints and auth registration/login are public. Schedule endpoints require
STUDENT or ADMIN, while administration requires ADMIN. Both request rules and admin-service
method security enforce roles. Header-only stateless JWT authentication disables cookie/basic
authentication and CSRF; deployment requires HTTPS. There are no built-in admin credentials.

`/actuator/health/liveness` reports application liveness without database dependency.
`/actuator/health/readiness` includes PostgreSQL connectivity. Probe responses expose status
only. No other management endpoints are exposed. Missing database configuration or failed
Flyway migration prevents normal application startup.

The security test boots the actual HTTP security and Actuator configuration without a
database. The `integration` Maven profile uses Testcontainers to boot fresh PostgreSQL
databases and check migrations, constraints, JPA mappings, DTOs, and readiness. CI runs
both and requires Docker; it does not use the development database or deploy anything.

## Preserved application

`legacy/` contains the original Susun Jadwal frontend and its tooling, including archived
deployment workflows. It is excluded from the npm workspace and new CI. Its business
features are not imported into the new scaffold. The root `LICENSE.md` is preserved.

## Configuration references

- [Spring Boot 3.5 requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- [shadcn/ui with Vite](https://ui.shadcn.com/docs/installation/vite)
- [Tailwind with Vite](https://tailwindcss.com/docs/installation/using-vite)
