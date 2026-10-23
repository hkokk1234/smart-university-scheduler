# Live timetable updates and change history

When an administrator edits a section, the application records immutable before/after snapshots and creates one inbox notification for every user whose saved schedules contain that section. Users remain eligible while offline. Selecting the same section in several saved schedules still produces only one notification per change.

## Interface

- The workspace header includes a notification bell and an unread count. Its status dot indicates whether live updates are connected or reconnecting.
- **Timetable updates** shows each change's author, timestamp, original values and replacement values. Users can mark one notification or their entire inbox as read and open their saved plans to check for new conflicts or use Repair.
- **Administration → select course → History** shows a section's paginated edit history. It is available only to administrators.
- Live catalog events refresh course details, saved plans, history and campus simulation queries. Generated alternatives are cleared after a change rather than retained with obsolete meeting data.

Snapshot fields include section code, capacity, weekday, start/end times, room and instructor. Meeting IDs, version increments and list ordering are excluded from comparison: an edit that leaves these meaningful values unchanged creates no audit entry or notification. Historic details are preserved even when the current catalog later changes. New imports create sections; they do not overwrite existing sections and therefore do not create edit notifications. There is no backfill for edits made before this feature was enabled.

## Transaction and delivery model

Migration `V4__section_history_and_notifications.sql` adds `section_changes` and `user_notifications`. Existing section editing, the audit insert and all recipient inserts share the same database transaction. If any write fails, the edit and its history/inbox entries roll back together. Author identity comes from the authenticated administrator's JWT subject and server-side user record.

Recipient selection uses distinct owners of saved plans. A unique `(user_id, change_id)` constraint reinforces deduplication. Read operations include the authenticated owner in their SQL predicate; foreign and missing notification IDs both return 404.

Live signals use authenticated Server-Sent Events through `SseEmitter`. They are published using Spring's [transaction-bound event listener](https://docs.spring.io/spring-framework/reference/7.1/data-access/transaction/event.html) after commit. Delivery uses a bounded worker pool and queue, so slow clients do not block the catalog transaction thread. Signals carry cache invalidation information; sensitive inbox contents are fetched separately from owner-scoped endpoints.

The browser opens the stream using `fetch` with an Authorization header. Tokens are never passed in a URL. Connections last at most 60 seconds, never beyond token expiry, and send a heartbeat every 15 seconds. There are limits of five streams per user and 500 per application instance. Streams close on logout or component unmount. The client reconnects with capped exponential backoff and resynchronizes queries when connected.

The inbox is the durable source of truth. It refreshes every 30 seconds while the app is active, including when a live signal was dropped. A newly discovered notification also refreshes affected plans and clears generated alternatives. Read-state changes synchronize between connected tabs. There are no email or operating-system push notifications.

## Deployment limits

The live connection registry is in memory on each application instance. Immediate push therefore targets clients connected to the instance handling the edit. Across multiple instances, the shared database inbox and 30-second polling still recover notifications; a shared broker would be needed for immediate cross-instance broadcasts. Signals themselves are not a replayable event log.

Reverse proxies should allow `text/event-stream`, disable response buffering for this endpoint and allow idle connections beyond the heartbeat interval. The controller sends `X-Accel-Buffering: no` and `Cache-Control: no-store`. JWT expiry is still enforced when each new stream opens.

## API

| Endpoint | Access | Behavior |
| --- | --- | --- |
| GET `/api/v1/notifications?page=0&size=20` | STUDENT / ADMIN | Owner's paginated inbox plus total unread count |
| POST `/api/v1/notifications/{id}/read` | Owner | Idempotently mark one notification read |
| POST `/api/v1/notifications/read-all` | STUDENT / ADMIN | Mark the authenticated user's current unread notifications read |
| GET `/api/v1/notifications/stream` | STUDENT / ADMIN | Authenticated SSE with `ready`, `catalog-changed`, `inbox-changed` and `heartbeat` events |
| GET `/api/v1/admin/sections/{id}/history?page=0&size=20` | ADMIN | Section edit history, newest first |

All JSON endpoints return `Cache-Control: no-store`. Page size is limited to 100. Notification DTOs contain `id`, `readAt`, and `change`; change DTOs include immutable `before` and `after` snapshots, course and section IDs, author display name and creation timestamp.

## Verification

Tests cover no-op comparison, immutable snapshots, recipient events, owner-scoped access, administrator-only history and pagination validation. A transaction test confirms SSE delivery handlers run after commit, are suppressed on rollback, and do not run outside a transaction. Frontend tests cover fragmented UTF-8/CRLF SSE frames, cancellation, buffering limits and bearer-header authentication.

`ApiIT.sectionHistoryAndNotificationsAreDurableDeduplicatedAndOwnerScoped` exercises migration-backed persistence, duplicate saved plans, unread state, privacy and failed stale updates using PostgreSQL/Testcontainers. Run the integration profile with a working Docker daemon.
