# LaundryLab implementation against the 2026-10-28 project plan

Source: `洗衣管理系統_最新計劃書.pdf` (42 pages). This file records the implementation target; it does not replace the plan.

## Product rules

- Roles: student, dorm supervisor, and system administrator. The backend must enforce role and assigned-dormitory access.
- Students verify ownership of a school email address once. A revocable, expiring session persists across browser restarts.
- Machine status comes from validated START, COMPLETE, and FAULT signals. An elapsed estimate without COMPLETE is pending confirmation, not available.
- A student may subscribe to several active cycles; several students may subscribe to the same cycle. A subscription grants no reservation or queue position.
- A linked and following LINE account is required for reminders. Send one five-minute warning and one estimated-completion notice per subscription and cycle. Late subscriptions with under five minutes remaining receive the warning immediately. Cancellation or unlinking stops pending sends.
- Reminders indicate estimates. LINE API acceptance does not prove delivery or reading.
- Supervisors handle only assigned dormitories. Manual changes require an audit record and a reason.
- Demo signals and demo data must be visibly identified in the UI.

## Current implementation gap

| Area | Current code | Plan target |
| --- | --- | --- |
| Machine reads | Four read endpoints and a START/COMPLETE/FAULT signal endpoint exist; frontend filters use API data | Add server time; expose signal and stale-status detail |
| Registration and login | Student registration, school-email verification, bcrypt passwords, expiring cookie sessions, and role-based routing implemented locally | Configure SMTP, add login abuse controls and deployment checks |
| Reminder subscription | Authenticated list/create/cancel API and frontend bell persist subscriptions | Verify against MySQL and LINE test account |
| Notifications | Scheduled LINE push uses retry keys and deduplication | Test delivery, cancellation races, and deployment monitoring |
| LINE linking | Ten-minute code, signed webhook, follow/unfollow, unlink | Test with a configured LINE channel and public HTTPS webhook |
| Reports and management | Scoped text reports, maintenance, assignments, facilities, and seven-day cycle counts have APIs | Add photo upload, full audit reasons for administrator facility changes, and MySQL integration checks |
| Frontend | Account, report, equipment, reminder, management, and statistics views call APIs | Add delivery status and end-to-end browser checks |

## Suggested delivery order

1. Bring the schema in line with the plan, including LINE link, request, and webhook event tables plus notification retry fields. Use a migration for existing databases; keep fresh-install schema synchronized.
2. Configure SMTP and create the initial administrator; verify registration, one-time email verification, login sessions, and backend role/assignment checks against the deployed MySQL instance.
3. Add the LINE account linking flow and webhook signature verification. Keep LINE secrets server-side.
4. Add authenticated reminder APIs and wire the student bell and account page to them.
5. Add scheduled notification creation and LINE dispatch with deduplication, bounded retries, and tests for cancellation, unlinking, and time changes.
6. Complete report, supervisor, and administrator APIs; replace remaining frontend mock state. Run end-to-end checks against MySQL and a LINE test account.

The plan excludes QR scanning, resident roster checks, reservations, pickup confirmation, penalties, and personal laundry history.
