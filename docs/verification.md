# Verification

## Initial scaffold

Executed locally on 2026-10-09 with Node.js 22.18.0, npm 10.9.3, Java 21.0.1,
Maven 3.8.9 and the generated Maven 3.9.11 wrapper, and Docker Desktop.

| Check | Result |
| --- | --- |
| `npm install --no-fund --no-audit` | Passed; generated the workspace lockfile |
| `npm run build` | Passed; TypeScript and Vite production build |
| `npm audit --audit-level=high` | Passed; zero reported vulnerabilities |
| `mvn -B -ntp verify` in `backend/` | Passed; executable JAR and four security/health tests |
| `./mvnw.cmd -version` in `backend/` | Passed; downloaded and ran Maven 3.9.11 |
| `docker compose --env-file .env.example config --quiet` | Passed |
| `docker compose up -d --wait postgres` | Passed; PostgreSQL became healthy |
| `./mvnw.cmd -B -ntp verify -Pintegration -Ddebug=false` in `backend/` | Passed; four security tests plus one PostgreSQL integration test |
| `npm run dev` and HTTP smoke checks | Passed; index, TSX module, and Tailwind CSS returned HTTP 200 |
| Packaged backend startup | Passed; JAR loaded root `.env`, connected to PostgreSQL, and validated the existing migration |
| Direct and proxied health requests | Passed; backend liveness/readiness and Vite-proxied readiness returned `UP` |
| Git ignore checks | Passed; `.env`, dependencies, and build artifacts are ignored |

The initial Docker engine connection failed while Docker Desktop was stopped. Starting
Docker Desktop resolved it. The first database integration attempt ran before PostgreSQL
was ready and failed with connection refused; rerunning after Compose reported healthy
passed. A first Vite TSX request timed out during startup; a retry passed.

The GitHub Actions workflow is configured but has not been executed on GitHub.
No business functionality or production deployment was tested.

Temporary frontend and backend servers were stopped after verification. PostgreSQL
remains running for development. A gitignored local `.env` was created with a generated
database password; it is not included in this report or the example configuration.

## Database implementation

Executed locally on 2026-10-09:

| Check | Result |
| --- | --- |
| `./mvnw.cmd -B -ntp verify -Pintegration -Ddebug=false` | Passed; executable backend JAR, 4 security tests, and 12 Testcontainers integration tests; no failures, errors, or skips |
| Clean PostgreSQL migrations with demonstration data | Passed; V1, V2, and V2.1 applied to a new container database; Hibernate validated all mappings |
| Clean PostgreSQL production migrations | Passed; V1 and V2 applied to a second empty database, no demo users, and a repeated migration run applied zero migrations |
| Database and JPA behavior | Passed; UUID persistence, all ten repositories, DTO serialization, ownership checks, uniqueness, foreign keys, time constraints, deferred triggers, concurrent deletes, and deletion cascades |
| `docker compose --env-file .env.example config --quiet` | Passed after the database changes |

The first security-test run exposed application services requiring repositories in a
database-free test context. The test now loads only the actual security and Actuator
configuration, preserving its four checks. The first integration run had one assertion
that encountered an existing selection's unique constraint before the intended foreign
key; using an empty schedule isolated that foreign-key check. The complete rerun passed.

Integration tests use disposable PostgreSQL databases on random mapped ports and do not
modify the existing Compose development database. The frontend was not changed or
rebuilt for this database task. CI configuration was updated to use Testcontainers but
has not been run on GitHub. See [database.md](database.md) for the schema and commands.

## Corrections and running application — 2026-10-12

Fixed duplicate counting of meeting minutes when a preferred time window and a preferred
morning/afternoon period are both supplied. The solver now uses their intersection.
Updated the API integration assertion for the six scoring criteria. API requests now keep
their 15-second deadline when a caller supplies a cancellation signal, and report timeout
separately from connectivity failures. Added regression tests for these behaviors.

| Executed check | Result |
| --- | --- |
| Backend unit tests during Maven verify | 32 passed, including 16 solver tests; no failures/errors/skips |
| Testcontainers integration tests during Maven verify | 24 passed: 12 API and 12 database tests; no failures/errors/skips |
| `mvn -B -ntp failsafe:verify -Pintegration -Ddebug=false` | BUILD SUCCESS; verified the completed integration-test reports |
| `npm run typecheck` | Passed |
| `npm run lint` | Passed without warnings |
| `npm run test --workspace frontend` | 9 tests passed in two files |
| `npm run build` | Passed; TypeScript and Vite production assets |
| Packaged backend, frontend and Compose PostgreSQL | Running; frontend and readiness returned HTTP 200, readiness UP |
| Live HTTP smoke through Vite proxy | Passed: catalog with three courses, temporary registration/login, five generated schedules, save/list/delete |

The first API integration run failed because its assertion still expected five score
criteria. The corrected rerun passed all 56 backend tests. The full Maven verify process
then hit its manually capped 192 MB Metaspace during final report verification; repeating
only `failsafe:verify` in a fresh JVM with a 256 MB Metaspace cap succeeded. Do not interpret
the full verify command's exit status as a successful run; the test results and subsequent
report-verification command are recorded separately here.

The initial frontend typecheck with a manually capped 192 MB Node heap exhausted that
heap; the final checks used 384 MB and passed. Backend packaging completed before test
report verification, and the resulting JAR was started and checked over HTTP.

Local startup additionally required repairing the invalid JWT signing-key configuration
and aligning the existing PostgreSQL role password with the local `.env`. Database data
was preserved. The temporary smoke account and its saved schedule were removed afterward.
Secrets and runtime logs are ignored by Git. Frontend, backend and PostgreSQL were left
running at the user's request. GitHub Actions and browser end-to-end tests were not run.

## AI Academic Copilot — 2026-10-13

Added authenticated Copilot UI and API, live catalog grounding, Java solver context,
structured OpenAI provider integration, validated course recommendations and bounded
concurrent/minute request budgets. The provider is optional and disabled by default.

| Executed check | Result |
| --- | --- |
| Maven `verify -Pintegration -Ddebug=false -DforkCount=0` | BUILD SUCCESS; 42 unit and 25 Testcontainers integration tests passed, no failures/errors/skips |
| Frontend typecheck, lint, unit tests and build | Passed; nine tests and Vite production build |
| Updated backend startup and readiness | Passed; HTTP readiness UP |
| Copilot page and TSX module over Vite | HTTP 200 |
| Live authenticated Copilot smoke | Anonymous status 401; authenticated status available=false; chat returns 503 while unconfigured |

OpenAI HTTP requests and responses were tested with mock transport, including output
validation and safe errors. A live OpenAI key was not configured, so no real model response
or paid API request was tested. The temporary runtime smoke account was deleted afterward.
Frontend and the updated backend remain running. See [Copilot setup](copilot.md).

## Campus Digital Twin — 13 October 2026

The frontend production build and nine existing frontend tests passed. The initial backend unit run passed 47 tests. The Testcontainers integration run stalled during Ryuk container startup and was stopped; it is not recorded as passing. After the final peak-time changes, Maven package -DskipTests completed successfully. The ADMIN-only API and read-only simulation are documented in digital-twin.md.


Runtime restart: frontend HTTP 200 and backend started successfully after Flyway validation. The final repeated TypeScript check ran out of host memory; the earlier typecheck and lint had passed.

