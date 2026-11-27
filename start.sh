#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT/.local-run"
BACKEND_PID_FILE="$RUN_DIR/backend.pid"
FRONTEND_PID_FILE="$RUN_DIR/frontend.pid"
BACKEND_LOG="$RUN_DIR/backend.log"
FRONTEND_LOG="$RUN_DIR/frontend.log"
BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-5173}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-laundrylab}"
DB_USER="${DB_USER:-root}"
KEYCHAIN_SERVICE="${KEYCHAIN_SERVICE:-Mysql@localhost:3306}"
SMTP_KEYCHAIN_SERVICE="${SMTP_KEYCHAIN_SERVICE:-LaundryLab Gmail SMTP}"
SMTP_USER="${SMTP_USER:-sdsd51116@gmail.com}"

mkdir -p "$RUN_DIR"
chmod 700 "$RUN_DIR"

running_pid() {
  local file="$1" pid
  [[ -f "$file" ]] || return 1
  pid="$(cat "$file")"
  [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null
}

port_busy() {
  lsof -nP -iTCP:"$1" -sTCP:LISTEN -t >/dev/null 2>&1
}

if running_pid "$BACKEND_PID_FILE" || running_pid "$FRONTEND_PID_FILE"; then
  echo "LaundryLab 已由腳本啟動；請先執行 ./stop.sh。" >&2
  exit 1
fi
rm -f "$BACKEND_PID_FILE" "$FRONTEND_PID_FILE"
if port_busy "$BACKEND_PORT" || port_busy "$FRONTEND_PORT"; then
  echo "埠 $BACKEND_PORT 或 $FRONTEND_PORT 已被其他程式使用；請先關閉該程式。" >&2
  exit 1
fi

for command in security mysql mvn java npm node curl openssl screen; do
  command -v "$command" >/dev/null || { echo "找不到必要指令：$command" >&2; exit 1; }
done

if [[ ! -d "$ROOT/frontend/node_modules" ]]; then
  (cd "$ROOT/frontend" && npm ci)
fi

echo "正在讀取 macOS 鑰匙圈中的 MySQL 密碼…"
db_password="${DB_PASSWORD:-}"
if [[ -z "$db_password" ]]; then
  db_password="$(security find-generic-password -s "$KEYCHAIN_SERVICE" -a "$DB_USER" -w)" || {
    echo "無法讀取鑰匙圈項目 ${KEYCHAIN_SERVICE}（帳號 ${DB_USER}）。" >&2
    exit 1
  }
fi

table_count="$(MYSQL_PWD="$db_password" mysql --protocol=TCP --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USER" --database="$DB_NAME" --batch --skip-column-names --execute="SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()" 2>/dev/null)" || {
  echo "無法連線到 ${DB_HOST}:${DB_PORT}/${DB_NAME}，請檢查 MySQL 與鑰匙圈設定。" >&2
  exit 1
}
if [[ "$table_count" != "16" ]]; then
  echo "資料庫 ${DB_NAME} 預期有 16 張表，實際為 ${table_count}；已取消啟動。" >&2
  exit 1
fi

echo "正在建立後端程式…"
(cd "$ROOT/backend" && mvn -q -DskipTests package)
JAR="$ROOT/backend/target/backend-0.1.0.jar"
[[ -f "$JAR" ]] || { echo "找不到後端 JAR：$JAR" >&2; exit 1; }

cleanup_on_error() {
  if [[ -f "$FRONTEND_PID_FILE" ]]; then kill "$(cat "$FRONTEND_PID_FILE")" 2>/dev/null || true; rm -f "$FRONTEND_PID_FILE"; fi
  if [[ -f "$BACKEND_PID_FILE" ]]; then kill "$(cat "$BACKEND_PID_FILE")" 2>/dev/null || true; rm -f "$BACKEND_PID_FILE"; fi
}
trap 'status=$?; if (( status != 0 )); then cleanup_on_error; fi' EXIT

launch_detached() {
  local session_name="$1" pid_file="$2" log_file="$3"
  shift 3
  screen -dmS "$session_name" /bin/bash -c '
    pid_file="$1"; log_file="$2"; shift 2
    printf "%s\n" "$$" > "$pid_file"
    exec "$@" > "$log_file" 2>&1
  ' bash "$pid_file" "$log_file" "$@"
  for _ in {1..20}; do
    [[ -s "$pid_file" ]] && return 0
    sleep 0.1
  done
  echo "無法建立背景工作階段：${session_name}" >&2
  return 1
}

SIGNAL_API_KEY="${SIGNAL_API_KEY:-$(openssl rand -hex 24)}"
LINE_BOT_URL="${LINE_BOT_URL:-https://line.me/R/ti/p/%40014utqjn}"
smtp_password="${SMTP_PASSWORD:-}"
if [[ -z "$smtp_password" ]]; then
  smtp_password="$(security find-generic-password -s "$SMTP_KEYCHAIN_SERVICE" -a "$SMTP_USER" -w 2>/dev/null)" || true
fi
smtp_password="${smtp_password//[[:space:]]/}"
if [[ -n "$smtp_password" ]]; then
  SMTP_HOST="${SMTP_HOST:-smtp.gmail.com}"
  SMTP_PORT="${SMTP_PORT:-587}"
  MAIL_FROM="${MAIL_FROM:-$SMTP_USER}"
  SMTP_AUTH=true SMTP_STARTTLS=true SMTP_STARTTLS_REQUIRED=true \
  SMTP_HOST="$SMTP_HOST" SMTP_PORT="$SMTP_PORT" SMTP_USER="$SMTP_USER" \
  SMTP_PASSWORD="$smtp_password" MAIL_FROM="$MAIL_FROM" \
  DB_URL="jdbc:mysql://$DB_HOST:$DB_PORT/$DB_NAME?serverTimezone=UTC" \
  DB_USER="$DB_USER" DB_PASSWORD="$db_password" PORT="$BACKEND_PORT" \
  AUTH_SECURE_COOKIE=false PUBLIC_BASE_URL="http://127.0.0.1:$FRONTEND_PORT" \
  SIGNAL_API_KEY="$SIGNAL_API_KEY" LINE_BOT_URL="$LINE_BOT_URL" \
    launch_detached "laundrylab_backend_${UID}" "$BACKEND_PID_FILE" "$BACKEND_LOG" java -jar "$JAR"
  echo "Gmail 驗證信寄送已啟用（${SMTP_USER}）。"
else
  echo "尚未在鑰匙圈找到 Gmail 應用程式密碼；新帳號驗證信暫不可寄送。" >&2
DB_URL="jdbc:mysql://$DB_HOST:$DB_PORT/$DB_NAME?serverTimezone=UTC" \
DB_USER="$DB_USER" DB_PASSWORD="$db_password" PORT="$BACKEND_PORT" \
AUTH_SECURE_COOKIE=false PUBLIC_BASE_URL="http://127.0.0.1:$FRONTEND_PORT" \
SIGNAL_API_KEY="$SIGNAL_API_KEY" LINE_BOT_URL="$LINE_BOT_URL" \
  launch_detached "laundrylab_backend_${UID}" "$BACKEND_PID_FILE" "$BACKEND_LOG" java -jar "$JAR"
fi
backend_pid="$(cat "$BACKEND_PID_FILE")"
unset db_password smtp_password

for _ in {1..40}; do
  if ! kill -0 "$backend_pid" 2>/dev/null; then
    echo "後端啟動失敗，請查看 $BACKEND_LOG" >&2; exit 1
  fi
  code="$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$BACKEND_PORT/api/auth/me" || true)"
  [[ "$code" == "401" || "$code" == "200" ]] && break
  sleep 1
done
if [[ "$code" != "401" && "$code" != "200" ]]; then
  echo "後端未在期限內就緒，請查看 $BACKEND_LOG" >&2; exit 1
fi

API_PROXY_TARGET="http://127.0.0.1:$BACKEND_PORT" \
  launch_detached "laundrylab_frontend_${UID}" "$FRONTEND_PID_FILE" "$FRONTEND_LOG" node "$ROOT/frontend/node_modules/vite/bin/vite.js" "$ROOT/frontend" --host 127.0.0.1 --port "$FRONTEND_PORT" --strictPort
frontend_pid="$(cat "$FRONTEND_PID_FILE")"
for _ in {1..20}; do
  if ! kill -0 "$frontend_pid" 2>/dev/null; then
    echo "前端啟動失敗，請查看 $FRONTEND_LOG" >&2; exit 1
  fi
  code="$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$FRONTEND_PORT/" || true)"
  [[ "$code" == "200" ]] && break
  sleep 1
done
if [[ "$code" != "200" ]]; then
  echo "前端未在期限內就緒，請查看 $FRONTEND_LOG" >&2; exit 1
fi

trap - EXIT
echo "前端：http://127.0.0.1:$FRONTEND_PORT/"
echo "後端：http://127.0.0.1:$BACKEND_PORT/"
echo "資料庫：${DB_HOST}:${DB_PORT}/${DB_NAME}（${table_count} 張表）"
echo "停止：./stop.sh"
