#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT/.local-run"
GATEWAY_PORT="${LINE_GATEWAY_PORT:-8090}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
mkdir -p "$RUN_DIR"
chmod 700 "$RUN_DIR"

for command in ngrok python3 screen curl lsof; do
  command -v "$command" >/dev/null || { echo "找不到必要指令：$command" >&2; exit 1; }
done
[[ -f "$RUN_DIR/backend.pid" ]] && kill -0 "$(cat "$RUN_DIR/backend.pid")" 2>/dev/null || {
  echo "請先執行 ./start.sh 啟動後端。" >&2; exit 1;
}
if [[ -f "$RUN_DIR/line-gateway.pid" || -f "$RUN_DIR/line-ngrok.pid" ]]; then
  echo "LINE 測試入口已啟動；請先執行 ./stop-line-tunnel.sh。" >&2; exit 1
fi
if lsof -nP -iTCP:"$GATEWAY_PORT" -sTCP:LISTEN -t >/dev/null 2>&1; then
  echo "埠 $GATEWAY_PORT 已被其他程式使用。" >&2; exit 1
fi

start_detached() {
  local name="$1" pid_file="$2" log_file="$3"
  shift 3
  screen -dmS "$name" /bin/bash -c '
    pid_file="$1"; log_file="$2"; shift 2
    printf "%s\n" "$$" > "$pid_file"
    exec "$@" > "$log_file" 2>&1
  ' bash "$pid_file" "$log_file" "$@"
  for _ in {1..20}; do
    [[ -s "$pid_file" ]] && return 0
    sleep 0.1
  done
  return 1
}

cleanup() { "$ROOT/stop-line-tunnel.sh" >/dev/null 2>&1 || true; }
trap cleanup EXIT
BACKEND_PORT="$BACKEND_PORT" LINE_GATEWAY_PORT="$GATEWAY_PORT" \
  start_detached "laundrylab_line_gateway_${UID}" "$RUN_DIR/line-gateway.pid" "$RUN_DIR/line-gateway.log" \
  python3 "$ROOT/line-webhook-gateway.py"
start_detached "laundrylab_line_ngrok_${UID}" "$RUN_DIR/line-ngrok.pid" "$RUN_DIR/line-ngrok.log" \
  ngrok http "$GATEWAY_PORT" --log stdout

url=""
for _ in {1..30}; do
  if ! kill -0 "$(cat "$RUN_DIR/line-ngrok.pid")" 2>/dev/null; then break; fi
  url="$(curl -fsS http://127.0.0.1:4040/api/tunnels 2>/dev/null | python3 -c 'import json,sys; port=sys.argv[1]; print(next((t["public_url"] for t in json.load(sys.stdin).get("tunnels",[]) if t.get("public_url", "").startswith("https://") and t.get("config", {}).get("addr", "").endswith(":"+port)), ""))' "$GATEWAY_PORT" 2>/dev/null)" || true
  [[ -n "$url" ]] && break
  sleep 1
done
if [[ -z "$url" ]]; then
  echo "無法取得 ngrok HTTPS 網址；請查看 $RUN_DIR/line-ngrok.log" >&2
  exit 1
fi
python3 "$ROOT/sync-line-webhook.py" "$url/api/line/webhook"
printf '%s\n' "$url/api/line/webhook" > "$RUN_DIR/line-webhook-url"
chmod 600 "$RUN_DIR/line-webhook-url"
trap - EXIT
echo "LINE Webhook 暫時網址：$url/api/line/webhook"
echo "此入口只允許簽章有效的 /api/line/webhook POST；停止：./stop-line-tunnel.sh"
