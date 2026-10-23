# Timetable constraint solver

`scheduling/solver` is an independent, immutable-input Java module. It imports only the
JDK. `TimetableSolver` can be constructed and tested without Spring, a database, or HTTP.
Spring's `ScheduleGenerationService` adapts catalog DTOs into this model and enriches the
result with course, instructor and room details. No generated schedule is automatically saved.

The solver also supports [minimal-change repair](schedule-repair.md) of saved schedules,
with section replacements as the primary objective and branch-and-bound pruning.

## CSP and hard constraints

The variables are **all published section groups** for the selected courses. Every group
is treated as required. A domain consists of that group's alternative sections. Each complete
assignment selects exactly one section per group and includes every meeting of that section.
The model validates course/group/semester membership, unique group/section IDs and course
coverage before searching. Courses without groups are rejected; groups without alternatives
produce UNSAT. All selected courses must belong to the requested semester.

Meetings use ISO weekdays and semester-local `LocalTime`; they are recurring weekly templates,
not dated events. Intervals are half-open `[start,end)`, so consecutive classes can touch.
Cross-midnight meetings are rejected. Internal section overlaps and mandatory unavailable
times prune alternatives before search. Every pair of selected sections must have no
overlapping meetings, including each section's second or subsequent meetings.

## Search and ranking

Backtracking selects the unassigned variable with Minimum Remaining Values (MRV), breaking
ties by canonical UUID text. Section alternatives are also sorted by UUID. Forward checking
removes conflicting options from the remaining domains after each choice; an empty domain
terminates that branch immediately. Per-call mutable state makes the solver reentrant.

Completed assignments enter a worst-first priority queue holding at most five results.
The queue discards inferior solutions rather than retaining all possible schedules. Search
continues after filling the queue to find the best ranked results, until exhaustion or interruption.
Scores are weighted penalties, **lower is better**; equal scores use sorted selected IDs as
the tie-break. Each penalty is normalized to `[0,1]` before its configurable weight is applied,
so the criteria's units do not dominate the ranking. The response explains each normalized
penalty, weight and contribution:

| Criterion | Raw penalty | Default weight |
| --- | --- | --- |
| gaps | Gap minutes / total weekly meeting minutes, capped at 1 | 1 |
| campusDays | Number of campus weekdays / 7 | 1 |
| maximumCampusDays | Excess days / available weekdays above the requested maximum; 0 when no maximum is set | 1 |
| preferredTimes | Minutes outside the intersection of the requested time window and morning/afternoon period / total weekly meeting minutes | 1 |
| freeDays | Meeting minutes on preferred free weekdays / total weekly meeting minutes | 1 |
| balance | Sum of absolute daily-load deviations / twice the total weekly meeting minutes | 0.2 |

The balance criterion includes Saturday and Sunday. Fractions of a meeting minute are supported.
All normalized penalties are bounded by 1. Weights are finite numbers from 0 to 1000; zero
disables a criterion. The same `campusDays` weight applies to campus-day count and days above
the optional maximum. Morning means 08:00–12:00; afternoon means 12:00–18:00. No preferred
window, period or free days means those penalties are zero. Every preference is soft; mandatory
unavailable times remain hard constraints. There is no unsafe branch-and-bound based on current
gaps: adding a class can shrink a gap.

## Limits, cancellation and determinism

`SOLVER_MAX_TIME_MILLIS` defaults to 3000 (allowed 1–60000). The request may choose a smaller
deadline but cannot exceed this cap. `SOLVER_MAX_GROUPS` defaults to 100 (max 100), and
`SOLVER_MAX_OPTIONS` to 5000 (max 10000). Requests contain at most 20 courses, 100 unavailable
intervals, and five retained results. Deadline checks use monotonic nanoseconds throughout
domain preparation and traversal. The budget covers solver search, not catalog SQL loading,
request parsing, or response serialization; one individual conflict/score operation may finish
after the deadline before the next check. It is a cooperative bound rather than a real-time guarantee.

| Status | Meaning |
| --- | --- |
| SOLVED | Search exhausted; the returned results are the best K valid schedules |
| UNSAT | Search exhausted and proved there is no valid assignment |
| TIMEOUT | Deadline reached; results, if present, are valid but optimality is unproved |
| CANCELLED | Cancellation requested; results, if present, are valid but incomplete |

Cancellation supports thread interruption and an explicit `BooleanSupplier` for callers.
The synchronous HTTP endpoint has no job/cancel API. Closing a browser request does not
guarantee that the servlet container interrupts the backend thread. Search ordering and
completed rankings are deterministic for identical inputs, regardless of input ordering.
Elapsed metrics and timeout prefixes depend on machine load and therefore are not guaranteed
identical; deadlines cannot guarantee both exact repeatability and bounded wall-clock work.

Metrics include visited recursive nodes, dead branches/backtracks, pruned domain entries
(including repeated pruning in different branches), completed assignments, elapsed milliseconds,
and whether exploration exhausted the search space. Assignment counts saturate at `Long.MAX_VALUE`.

## Complexity and limitations

For G groups, at most D alternatives per group and M meetings per section, worst-case
search is exponential: O(D^G × G × D × M²), plus scoring completed assignments in
O(GM log(GM)). MRV and forward checking improve typical cases without changing that bound.
Copied domains across a depth-G recursion need O(G²D) references; the input needs O(GDM),
and retained solutions O(KGM), with K ≤ 5. Only one candidate solution is scored at a time.

The solver does not model room/staff allocation, seat availability, travel time, optional
groups, linked lecture/lab combinations, holidays or examination periods. Catalog section
capacity is informational. Search limits cap each call; deployment-level rate limiting and
CPU/concurrency controls remain the hosting application's responsibility.

## HTTP example

Authenticated STUDENT and ADMIN users may call `POST /api/v1/schedules/generate`.
Use the bearer token from login; the response has `Cache-Control: no-store`.

```json
{
  "semesterId": "20000000-0000-4000-8000-000000000001",
  "courseIds": ["50000000-0000-4000-8000-000000000001"],
  "maxResults": 5,
  "timeLimitMillis": 2000,
  "preferences": {
    "unavailableTimes": [{"weekday":"MONDAY","startTime":"08:00","endTime":"09:00"}],
    "preferredStart": "09:00",
    "preferredEnd": "17:00",
    "preferredFreeDays": ["FRIDAY"],
    "maximumCampusDays": 4,
    "preferredPeriod": "MORNING",
    "weights": {"gaps":2,"campusDays":1,"preferredTimes":1,"freeDays":1,"balance":0.2}
  }
}
```

The response includes `status`, `metrics`, and `schedules`. Each schedule includes
`selectedSectionIds`, complete `sections`, flattened `weeklyMeetings`, `optimizationScore`,
and `scoreExplanation`. Invalid IDs return 404; invalid input or cross-semester selection
returns 400; absent authentication returns 401. An impossible valid problem returns 200
with UNSAT. TIMEOUT and CANCELLED also return 200 so partial valid results remain usable.

Run `./mvnw test -Dtest=TimetableSolverTest` for pure solver tests and
`./mvnw verify -Pintegration` for clean PostgreSQL and endpoint integration tests.
