# LaundryLab backend (first milestone)

Java 17+ / Spring Boot 3.5 / MySQL 8. This milestone implements the 13-table schema, demo machines, read-only machine APIs, and a protected signal API. Email, accounts, subscriptions, reports, and management APIs are planned in the schema but are not implemented yet.

## Set up

1. Apply `sql/001_schema.sql`, then `sql/002_demo_data.sql` to MySQL 8. The first file creates a database named `laundrylab`. Demo data is for a fresh database only.
2. Set `DB_URL` (for example `jdbc:mysql://localhost:3306/laundrylab?serverTimezone=UTC`), `DB_USER`, `DB_PASSWORD`, and a nonempty `SIGNAL_API_KEY`.
3. Run `mvn spring-boot:run` from `backend/`. Run `mvn test` for the H2 integration test.

Do not commit credentials. Date-times use UTC; the frontend should display local time.

## API contract

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/dormitories` | Active dormitories |
| GET | `/api/dormitories/{id}/rooms` | Active rooms in a dormitory |
| GET | `/api/rooms/{id}/machines` | Active machines and current status |
| GET | `/api/machines/{id}` | One machine and current status |
| POST | `/api/signals` | Protected signal intake |

The machine response has `id`, `machineCode`, `status`, `cycleId`, `estimatedEndAt`, and `lastSignalAt`. Status is `AVAILABLE`, `RUNNING`, `PENDING_CONFIRMATION`, or `MAINTENANCE`. A running cycle whose estimated end has passed appears as `PENDING_CONFIRMATION` until a COMPLETE signal arrives. The end time is an estimate, not proof of availability. This version orders machines by ID; the actual position map still needs layout data agreed with the frontend.

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

Next: registration and verified login, supervisor assignments, reminder subscriptions and Email scheduling, issue reports, management operations, and their access checks. Until authentication is ready, the read APIs expose demo machine data; do not deploy them as production student endpoints.
