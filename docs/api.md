# REST API

Base path: `/api/v1`. JSON requests are strictly validated; unknown fields return 400.
UUIDs identify resources. Never send passwords/tokens in URLs. Production requires HTTPS.

## Authentication

<<<<<<< HEAD
Administrator document import endpoints and row formats are documented in [PDF / Excel catalog import](catalog-import.md).

Owner-scoped notification endpoints, authenticated SSE and administrator section history are documented in [live updates](live-updates.md).

=======
>>>>>>> origin/main
`POST /auth/register` returns 201 and a STUDENT account with a short-lived access token:

```json
{"email":"student@example.edu","displayName":"New Student","password":"use-your-own-long-password"}
```

`POST /auth/login` returns 200 with the same response shape:

```json
{"email":"student@example.edu","password":"use-your-own-long-password"}
```

Responses contain `accessToken`, `tokenType: "Bearer"`, `expiresIn` (seconds), and `user`.
Passwords use BCrypt cost 12 and must contain 12–72 characters and at most 72 UTF-8 bytes.
The signing secret must be a base64-encoded random key of at least 32 bytes.
Tokens validate signature, issuer, audience, expiry, issued-at, UUID subject and role.
They are sent only as `Authorization: Bearer <token>`; cookie and query-token auth is disabled.
The frontend keeps tokens in memory and clears them on expiry/logout. No refresh tokens
or server-side logout/revocation are implemented. Role changes affect newly issued tokens;
old tokens remain valid until expiry (default 15 minutes). Administrative provisioning is
an operational database task; public registration cannot assign ADMIN.

## Public catalog

| Method and path | Example / behavior |
| --- | --- |
| GET `/semesters` | Up to 100 semesters, newest start first |
| GET `/courses?semesterId=<uuid>&q=database&code=CS205&page=0&size=20&sort=title,asc` | Paginated course search; all filters optional |
| GET `/courses/<uuid>` | Course plus groups, all alternative sections and meetings |
| GET `/sections/<uuid>` | Complete section details and optimistic version |

Sorting supports `code`, `title`, `credits`, `id`, with `asc`/`desc` and stable ID tie-break.
Search treats SQL wildcard characters literally. Pages are zero-based; size is 1–100.
Page responses contain `content`, `page`, `size`, `totalElements`, `totalPages`.

## Private schedules (STUDENT or ADMIN)

| Method and path | Behavior |
| --- | --- |
| GET `/schedules?page=0&size=20` | Only JWT owner's saved schedules |
| GET `/schedules/<uuid>` | Owned schedule; missing or foreign resource returns 404 |
| POST `/schedules` | Save selection; 201 with Location header |
| DELETE `/schedules/<uuid>` | Delete owned schedule; 204 |
| POST `/schedules/generate` | Up to five ranked full schedules; see [solver examples and limits](solver.md) |
<<<<<<< HEAD
| POST `/schedules/<uuid>/repair` | Preview up to five alternatives ranked by fewest section replacements; see [repair workflow and API](schedule-repair.md) |
=======
>>>>>>> origin/main

Save request (an empty `sectionIds` list saves a draft):

```json
{"semesterId":"20000000-0000-4000-8000-000000000001","name":"My autumn week","sectionIds":["70000000-0000-4000-8000-000000000001","70000000-0000-4000-8000-000000000003"]}
```

Ownership comes exclusively from the JWT. Selections must share the requested semester
and contain at most one alternative per group. Saved drafts may be incomplete; generation
enforces every required group and no conflicts. All meetings of each section are returned.
Saving references live catalog sections rather than immutable historical meeting snapshots.
Private responses and auth responses use `Cache-Control: no-store`.

## Administration (ADMIN only)

`POST /admin/courses` returns 201:

```json
{"semesterId":"20000000-0000-4000-8000-000000000001","code":"CS310","title":"Algorithms","credits":6}
```

`PUT /admin/sections/<uuid>` replaces details and the **entire** meeting list atomically.
Use the current `version` from GET section. Stale updates return 409; meetings cannot be empty.

```json
{"code":"A","capacity":60,"version":0,"meetings":[{"weekday":"MONDAY","startTime":"09:00","endTime":"10:30","instructorId":null,"roomId":null}]}
```

`GET /admin/rooms` and `GET /admin/instructors` provide reference options. Referenced IDs
must exist. Anonymous requests receive 401, authenticated STUDENT requests receive 403.
Course/group/section creation beyond the listed endpoints is not implemented.

## Errors and documentation

Authenticated AI advisor endpoints are `GET /copilot/status` and `POST /copilot/chat`.
See [AI Academic Copilot](copilot.md) for configuration, examples, privacy and limitations.

Errors use `{timestamp,status,code,message,path,fieldErrors}` with safe messages and no
stack traces or passwords. Examples: 400 validation, 401 authentication, 403 authorization,
404 missing/foreign resources, 409 duplicates or stale writes. Clients should display
`message` and per-field validation feedback without relying on internal exception text.

The development profile exposes `/v3/api-docs` and `/swagger-ui/index.html`.
Production documentation is disabled unless `OPENAPI_ENABLED=true`. Health probes remain
public at `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`.

## Campus Digital Twin

POST /api/v1/admin/digital-twin/simulate (ADMIN): compare baseline room usage with temporary meeting changes. See [examples and limitations](digital-twin.md). No catalog changes are saved.

