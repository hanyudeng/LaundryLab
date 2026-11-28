#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT/.local-run"

stop_process() {
  local name="$1" file="$2" expected="$3" pid command_line
  [[ -f "$file" ]] || return 0
  pid="$(cat "$file")"
  command_line="$(ps -p "$pid" -o command= 2>/dev/null || true)"
  if [[ "$pid" =~ ^[0-9]+$ && "$command_line" == *"$expected"* ]]; then
    kill "$pid" 2>/dev/null || true
    echo "${name}：已停止"
  fi
  rm -f "$file"
}

stop_process "ngrok" "$RUN_DIR/line-ngrok.pid" "ngrok http"
stop_process "LINE Webhook 閘道" "$RUN_DIR/line-gateway.pid" "line-webhook-gateway.py"
rm -f "$RUN_DIR/line-webhook-url"
