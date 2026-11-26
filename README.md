# LaundryLab

Dormitory washing machine status prototype. The selected design uses machine signals to start and complete cycles, and LINE Bot for reminders. See [plan alignment](docs/plan-alignment.md) for the implementation target and current gaps.

- `backend/`: Spring Boot API, MySQL schema, demo data, tests, and [setup/API instructions](backend/README.md).
- `frontend/`: Vue client with student, supervisor, and administrator routes.

The frontend calls backend APIs for equipment, accounts, reports, management, and reminders. LINE notifications require server configuration and live integration testing. Existing databases require a reviewed migration before use.
