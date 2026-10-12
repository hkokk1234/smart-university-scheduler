# Campus Digital Twin

Administrators can open `/digital-twin` to compare the published semester timetable with temporary changes to a meeting's day, time, room or estimated attendance. No catalog or saved schedule is modified. All offered alternative sections are included, because each represents a potentially operated class.

Attendance defaults to section capacity, an upper-bound planning assumption rather than actual enrollment. This is a weekly timetable simulation, not a physical model of pedestrian traffic, room equipment, building opening hours, live occupancy or holidays.

The pure Java `CampusSimulation` module computes occupied room hours using the union of meeting intervals, double-booking alerts, capacity violations and exact daily peaks of simultaneous meetings and estimated attendance. Intervals are half-open: a class ending at 10:00 does not conflict with one beginning at 10:00. Peak attendance and peak meeting count can occur at different times. Unassigned meetings contribute to attendance peaks but not room occupancy. Unused rooms are included. Utilization is occupied hours divided by the entire 168-hour week, including weekends and overnight hours; it is not a business-hours utilization figure.

The simulation takes an internally consistent read-only catalog snapshot within a database transaction. Changes must reference meetings in the selected semester and known rooms (or null for unassigned). Duplicate overrides, invalid times and negative attendance are rejected. Requests are limited to 100 changes, 500 courses and 5000 meetings; larger catalogs are rejected rather than silently truncated. Pairwise conflict reporting can require quadratic work and output for heavily overlapping meetings; room/day sorting and sweep calculations handle union utilization and peaks without sampling.

## API

`POST /api/v1/admin/digital-twin/simulate` requires an ADMIN bearer token. Swagger exposes the request and response schemas.

```json
{
  "semesterId": "20000000-0000-4000-8000-000000000001",
  "changes": []
}
```

Empty changes return the baseline in both reports. To test a change, use a real meeting ID returned in `meetings`:

```json
{
  "semesterId": "20000000-0000-4000-8000-000000000001",
  "changes": [{
    "meetingId": "80000000-0000-4000-8000-000000000001",
    "weekday": "MONDAY",
    "startTime": "09:00:00",
    "endTime": "11:00:00",
    "roomId": null,
    "attendance": 40
  }]
}
```

The response contains `semesterId`, room inventory, original meetings, `baseline` and `scenario`. Reports include room usage, daily peaks, alerts, unassigned meeting count and total occupied room hours. Standard API errors apply: 400 invalid scenario, 401 missing authentication, 403 non-administrator and 404 unknown semester. Apply approved changes separately through the existing section administration endpoint.

Scenarios exceeding 10,000 planning alerts are rejected to bound response size and memory usage.

