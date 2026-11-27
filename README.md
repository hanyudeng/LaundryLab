# LaundryLab

## 本機啟動與停止

在專案目錄執行 `./start.sh`，開啟 `http://127.0.0.1:5173/`；結束時執行 `./stop.sh`。啟動腳本會從 macOS 鑰匙圈讀取 `Mysql@localhost:3306` 的 `root` 密碼，確認 `127.0.0.1:3306/laundrylab` 有計劃書的 16 張資料表，再啟動 Spring Boot API（8080）及 Vite 前端（5173）。首次讀取鑰匙圈時，macOS 可能要求按「允許」。

執行紀錄在 `.local-run/backend.log` 和 `.local-run/frontend.log`。腳本只會停止它自己啟動的程序。新版資料庫目前沒有帳號或宿舍資料；腳本不會匯入示範資料。需要其他連線設定時，可在執行前設定 `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USER` 和 `KEYCHAIN_SERVICE`；如已在安全環境中提供 `DB_PASSWORD`，腳本也可直接使用它。

Dormitory washing machine status prototype. The selected design uses machine signals to start and complete cycles, and LINE Bot for reminders. See [plan alignment](docs/plan-alignment.md) for the implementation target and current gaps.

- `backend/`: Spring Boot API, MySQL schema, demo data, tests, and [setup/API instructions](backend/README.md).
- `frontend/`: Vue client with student, supervisor, and administrator routes.

The frontend calls backend APIs for equipment, accounts, reports, management, and reminders. LINE notifications require server configuration and live integration testing. Existing databases require a reviewed migration before use.
