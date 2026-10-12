# AI Academic Copilot

An authenticated planning assistant is available at `/copilot`. It can explain the selected
courses' credit load, suggest courses from the current semester catalog, and discuss schedule
tradeoffs using the existing Java CSP solver. Recommendations have links to actual catalog
records and an explicit Add course button. Advice never saves or changes schedules automatically.

## Enable the provider

The backend uses OpenAI Responses with strict structured output. There are no additional
Java libraries, database tables, external browsing tools, or fake AI fallback responses.
Set these backend environment values or add them to the ignored local `.env`, then restart:

```properties
AI_ENABLED=true
OPENAI_API_KEY=your-own-api-key
AI_MODEL=gpt-4o-mini
```

The model is configurable and must support Responses structured output. The key remains on
the server; do not use a `VITE_*` variable. Default `AI_ENABLED=false` leaves the rest of the
application working and shows a clear Not connected state. The status endpoint reports
configuration readiness, not a live upstream availability probe. Configuring a key enables
paid provider calls when students submit questions; use provider account spending limits.

Implementation follows the [official OpenAI structured output documentation](https://developers.openai.com/api/docs/guides/structured-outputs).

## Context and boundaries

The server loads the requested semester, its first 50 courses in code order, and any selected
courses outside that page. The response marks a truncated catalog. Up to eight selected
courses are checked by the Java solver with a 1000 ms search limit and one retained result.
Unpublished groups are explicitly reported as unavailable for timetable checking, without
preventing credit or course advice. UNSAT, TIMEOUT and CANCELLED retain their original meanings.
Maximum campus days, preferred hours/period, free weekdays and weights come from Settings.

Provider input includes the question, up to six recent conversation turns, public catalog
facts, selected course IDs, preferences, and reduced solver facts. It excludes JWTs, user
IDs, account details, saved schedules and instructor contact information. Conversations are
held only in page memory, clear on semester change/logout, and are not stored in PostgreSQL
or browser storage. Requests specify `store:false`; this does not override the provider's
separate abuse-monitoring or account data-retention policies. See the
[official data controls documentation](https://developers.openai.com/api/docs/guides/your-data).

Untrusted catalog text and conversation are treated as data. Structured course-ID output
is restricted to the supplied catalog and validated again server-side. React renders the
answer as escaped plain text. Model prose can still contain mistakes; prerequisite, degree
and enrollment rules are unavailable in this schema, and the assistant is instructed to say
so. The deterministic solver remains responsible for timetable validity.

## HTTP contract

Both STUDENT and ADMIN may use these endpoints; anonymous requests return 401.

| Endpoint | Response |
| --- | --- |
| GET `/api/v1/copilot/status` | `{ "available": false }` or true |
| POST `/api/v1/copilot/chat` | `answer`, `recommendedCourses`, nullable `scheduleCheck`, `catalogTruncated` |

```json
{
  "semesterId": "20000000-0000-4000-8000-000000000001",
  "message": "Help me choose programming courses with fewer campus days.",
  "courseIds": ["50000000-0000-4000-8000-000000000001"],
  "history": [],
  "preferences": {
    "preferredStart": "09:00",
    "preferredEnd": "17:00",
    "preferredPeriod": "MORNING",
    "maximumCampusDays": 4,
    "preferredFreeDays": ["FRIDAY"],
    "weights": {"gaps":1,"campusDays":1,"preferredTimes":1,"freeDays":1,"balance":0.2}
  }
}
```

Messages are at most 2000 characters, history at most six turns of 8000 characters each,
and recommended courses at most eight. The provider output is capped at 1000 tokens.
The service permits two concurrent requests and 20 request attempts per minute per backend
instance; this is a bounded, in-memory global budget rather than a per-user quota.
Provider connection/read timeouts are 3/9 seconds. Browser cancellation cancels the HTTP
fetch, but does not guarantee interruption of ongoing server or provider work.

Errors use the existing consistent JSON format: 400 invalid input or provider refusal,
429 `AI_BUSY`, 503 `AI_NOT_CONFIGURED`, 502 `AI_UNAVAILABLE`, and 504 `AI_TIMEOUT`.
Credentials and upstream error bodies are never returned. Successful responses use no-store.

Tests cover provider request shape with a mock HTTP transport, missing configuration,
invented course IDs, malformed/incomplete output, safe upstream errors, catalog grounding,
unpublished groups, cross-semester rejection, request budget, and endpoint authentication.
No live paid-provider call is required by the automated suite.
