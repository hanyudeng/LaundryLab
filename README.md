# LaundryLab

## 本機啟動與停止

在專案目錄執行 `./start.sh`，開啟 `http://127.0.0.1:5173/`；結束時執行 `./stop.sh`。啟動腳本會從 macOS 鑰匙圈讀取 `Mysql@localhost:3306` 的 `root` 密碼，確認 `127.0.0.1:3306/laundrylab` 有計劃書的 16 張資料表，再啟動 Spring Boot API（8080）及 Vite 前端（5173）。首次讀取鑰匙圈時，macOS 可能要求按「允許」。

執行紀錄在 `.local-run/backend.log` 和 `.local-run/frontend.log`。腳本只會停止它自己啟動的程序，也不會匯入示範資料。需要其他連線設定時，可在執行前設定 `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USER` 和 `KEYCHAIN_SERVICE`；如已在安全環境中提供 `DB_PASSWORD`，腳本也可直接使用它。

驗證信由 `sdsd51116@gmail.com` 透過 Gmail SMTP 寄出。先在 Google 帳戶建立供 LaundryLab 使用的應用程式密碼，再執行 `./setup-gmail.sh`，依提示貼到 Terminal；腳本會將它存入 macOS 鑰匙圈的 `LaundryLab Gmail SMTP` 項目，密碼不會寫入程式碼。重啟後端時使用 `./stop.sh`、`./start.sh`。若鑰匙圈尚無此項目，網站仍可啟動，但新帳號的驗證信無法寄送。Gmail 帳戶需啟用兩步驟驗證，才可建立應用程式密碼。

LINE 提醒使用「My Laundry Lab」官方帳號。執行 `./setup-line.sh`，依提示將 Channel secret 與 Channel access token 存入 macOS 鑰匙圈，再重新啟動專案。後端會讀取憑證並驗證 LINE Webhook 簽章。需要接收 LINE 綁定訊息時，再執行 `./start-line-tunnel.sh`；腳本會以 ngrok 建立暫時 HTTPS 網址，且只轉送 `/api/line/webhook` 的 POST。將輸出的網址填到 LINE Developers Console 的 Messaging API → Webhook URL，按 Verify，並啟用 Use webhook。網址在重啟 ngrok 後可能改變，需重新確認 LINE 後台設定。執行 `./stop-line-tunnel.sh` 會關閉公開入口；`./stop.sh` 也會先關閉入口再停止前後端。

學生在「個人設定 → 通知設定」取得 10 分鐘綁定碼後，將頁面的 `link ...` 傳給官方帳號並按「重新檢查」。顯示「已綁定並追蹤」後，可在使用中的機台按鈴鐺訂閱提醒。本機已驗證 A01 的「預計完成前五分鐘」提醒由 LINE API 接受且送達手機。這個 ngrok 入口適合本機測試；正式使用應部署固定的 HTTPS 後端網址。

Dormitory washing machine status prototype. The selected design uses machine signals to start and complete cycles, and LINE Bot for reminders. See [plan alignment](docs/plan-alignment.md) for the implementation target and current gaps.

- `backend/`: Spring Boot API, MySQL schema, demo data, tests, and [setup/API instructions](backend/README.md).
- `frontend/`: Vue client with student, supervisor, and administrator routes.

The frontend calls backend APIs for equipment, accounts, reports, management, and reminders. LINE notifications require server configuration and live integration testing. Existing databases require a reviewed migration before use.
