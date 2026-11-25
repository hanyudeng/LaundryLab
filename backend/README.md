# LaundryLab backend (first milestone)

Java 17+ / Spring Boot 3.5 / MySQL 8. The backend contains a 16-table fresh-install schema, demo machines, read-only machine APIs, a protected signal API, and the first account and role APIs. LINE linking, reminders, reports, and most management APIs are still to be built.

## Set up

1. Apply `sql/001_schema.sql`, then `sql/002_demo_data.sql` to MySQL 8. The first file creates a database named `laundrylab`. Demo data is for a fresh database only.
2. Set `DB_URL` (for example `jdbc:mysql://localhost:3306/laundrylab?serverTimezone=UTC`), `DB_USER`, `DB_PASSWORD`, and a nonempty `SIGNAL_API_KEY`.
3. Configure `SMTP_HOST`, `SMTP_PORT`, `MAIL_FROM`, and when required `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_AUTH=true`, and `SMTP_STARTTLS=true`. Set `PUBLIC_BASE_URL` to the frontend origin so verification links open the app. Registration rolls back if mail cannot be sent.
4. For the first administrator only, set `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` (at least 12 characters) on the first start. Remove these environment values after the administrator exists. The bootstrap does not reset an existing administrator password.
5. For localhost HTTP development only, set `AUTH_SECURE_COOKIE=false`. Keep its default `true` behind HTTPS in deployment. Set `SCHOOL_EMAIL_DOMAIN` if the accepted student email domain differs from `o365.fcu.edu.tw`.
6. Run `mvn spring-boot:run` from `backend/`. Run `mvn test` for the H2 integration tests.

Do not commit credentials. Date-times use UTC; the frontend should display local time.

## API contract

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/dormitories` | Active dormitories |
| GET | `/api/dormitories/{id}/rooms` | Active rooms in a dormitory |
| GET | `/api/rooms/{id}/machines` | Active machines and current status |
| GET | `/api/machines/{id}` | One machine and current status |
| POST | `/api/signals` | Protected signal intake |
| POST | `/api/auth/register` | Register a student and email a single-use verification link |
| POST | `/api/auth/verify` | Verify the email token |
| POST | `/api/auth/login` | Set a seven-day HttpOnly session cookie |
| GET | `/api/auth/me` | Read current account and role |
| POST | `/api/auth/logout` | Revoke session and clear cookie |
| PUT | `/api/admin/users/{id}/role` | Administrator changes a student/supervisor role; revokes sessions |
| PUT/DELETE | `/api/admin/supervisors/{id}/dormitories/{id}` | Administrator assigns or removes a supervisor's dormitory |

Student registration always creates the `STUDENT` role. Administrators can promote a verified account to `SUPERVISOR`; the user must sign in again after promotion. Machine read APIs require a session. Supervisors see only assigned dormitories and receive HTTP 403 when requesting rooms or machines outside their assignment. The signal API uses its separate server-side key.

The machine response has `id`, `machineCode`, `status`, `cycleId`, `estimatedEndAt`, and `lastSignalAt`. Timestamps are UTC ISO 8601 values ending in `Z`; the frontend displays local time. Status is `AVAILABLE`, `RUNNING`, `PENDING_CONFIRMATION`, or `MAINTENANCE`. A running cycle whose estimated end has passed appears as `PENDING_CONFIRMATION` until a COMPLETE signal arrives. The end time is an estimate, not proof of availability. This version orders machines by ID; the actual position map still needs layout data agreed with the frontend.

Example signal request:

```sh
curl -X POST http://localhost:8080/api/signals \
  -H 'Content-Type: application/json' \
  -H 'X-Signal-Key: replace-with-your-key' \
  -d '{"eventId":"A01-0001","machineId":1,"type":"START","sourceKind":"TEST_PAGE"}'
```

`type` is `START`, `COMPLETE`, or `FAULT`; `sourceKind` is `TEST_PAGE` or `IOT_SWITCH`. Use a new `eventId` for each event. A retry with the same ID returns the prior result without another cycle or log. The response includes `processStatus` (`ACCEPTED` or `REJECTED`), `reason`, and `cycleId`. Business rejections are stored in `machine_signal_log`; malformed requests return HTTP 400 and unknown machines return HTTP 404. The key must never enter a public frontend bundle. A test page must call this API through a trusted server or be restricted to an internal deployment.

## Checks and next work

`SignalFlowTest` verifies start, retry idempotency, duplicate start rejection, pending confirmation, completion, and completion without an active cycle. Apply the migration to the team's MySQL 8 instance before deployment; local tests use a minimal H2 schema and do not validate MySQL DDL.

Next: production SMTP configuration and login abuse controls, LINE account linking, reminder subscriptions and LINE Bot scheduling, issue reports, management operations, and their access checks. The SQL schema is for a fresh database; existing databases need a separately reviewed migration before applying these changes. Machine reads now require a session, but reporting and management features are not yet production-ready.
