# Minimal-change schedule repair

When published meeting times change, a saved timetable can develop conflicts. From **Saved schedules → Repair**, students can find valid alternatives while retaining as many original sections as possible. They can also exclude a selected section when it is cancelled or no longer available to them.

## Student workflow

1. Save a complete timetable with one section for every required course group.
2. Open **Repair** on its saved card. Current published meeting times are loaded from the server.
3. Optionally check sections to exclude, then choose **Find repairs**.
4. Compare up to five options. Each includes a count of replaced sections, a **From / To** table, and a weekly calendar.
5. Save an option as a new schedule. The preview does not modify the original or the course catalog.

A zero-change result means the existing section choices remain valid. A changed group counts once even if its section has several meetings. The system does not keep historical meeting snapshots: both the original choices and their replacements show current published times.

## Optimization objective

The solver orders solutions lexicographically:

1. Minimize the number of required groups assigned a different section from the saved timetable.
2. Minimize the existing weighted preference penalties.
3. Break ties deterministically by selected section IDs.

Change count is a separate objective, not an adjustable preference weight. A better preference score cannot outweigh an extra section replacement.

Search reuses MRV, forward checking, internal meeting validation, and mandatory unavailability constraints. Original sections are visited first. In repair mode, branch and bound prunes a branch when the number of changes already made plus unavoidable changes in remaining domains exceeds the worst retained option's change count. Equal bounds remain searchable so preference scores and deterministic ties are still respected.

`metrics.complete = true` confirms the returned list is optimal under that ordering. `TIMEOUT` and `CANCELLED` retain any valid candidates discovered so far without claiming minimum changes. A complete search with no solution returns `UNSAT`. Search limits remain bounded by the server's existing solver configuration. Because branch and bound skips dominated branches, `solutionsFound` counts evaluated solutions rather than all feasible assignments.

## API

`POST /api/v1/schedules/{id}/repair`

Requires a student or administrator bearer token and ownership of the saved schedule. Missing and foreign schedules both return 404. Responses use `Cache-Control: no-store`.

Example request:

```json
{
  "excludedSectionIds": ["70000000-0000-4000-8000-000000000003"],
  "maxResults": 5,
  "timeLimitMillis": 3000
}
```

All fields are optional; `{}` repairs conflicts using default preferences. The UI sends the student's current preferences to break ties. Exclusions must be unique sections of the selected courses. `maxResults` is 1–5 and the requested time limit is 1–60,000 ms, capped by server configuration. Empty and partial drafts are rejected with a useful `INVALID_REPAIR` error.

The response includes `originalScheduleId`, `status`, solver `metrics`, and ranked `schedules`. Each option includes a generated `schedule`, `changedSectionCount`, and `changes` containing the group ID and complete `before` / `after` section records. Existing schedule creation saves the selected repair as a new plan.

## Verification

- Solver tests cover valid originals, preference priorities, new conflicts, cascading replacements, exclusions, impossible repairs, invalid baselines, timeout, and cancellation.
- A seeded randomized check compares the top 1, 3, and 5 repaired solutions against exhaustive enumeration of 60 small problems.
- Service tests check change reports, required-group coverage, invalid drafts, and exclusion validation.
- HTTP tests check authentication, owner-scoped reads, request validation, cache headers, and read-only preview behavior.
- Frontend API tests check authenticated repair requests and session handling.

For a demo, save a valid timetable and exclude a laboratory that has another published section. The first option should replace that section while keeping unrelated choices. For a cascading example, make its replacement overlap another selected section; repair must then find an alternative for that group as well, or report that no valid repair exists.
