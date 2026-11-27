#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT/.local-run"
"$ROOT/stop-line-tunnel.sh"

stop_process() {
  local name="$1" file="$2" expected="$3" pid command_line
  if [[ ! -f "$file" ]]; then echo "${name}：未由腳本啟動"; return; fi
  pid="$(cat "$file")"
  if [[ ! "$pid" =~ ^[0-9]+$ ]] || [[ -z "$(ps -p "$pid" -o command= 2>/dev/null)" ]]; then
    rm -f "$file"
    echo "${name}：已停止"
    return
  fi
  command_line="$(ps -p "$pid" -o command= 2>/dev/null || true)"
  if [[ "$command_line" != *"$expected"* ]]; then
    echo "${name}：PID ${pid} 不屬於此專案，未停止" >&2
    rm -f "$file"
    return
  fi
  if ! kill "$pid"; then
    echo "${name}：無法停止 PID $pid，請檢查執行權限" >&2
    return 1
  fi
  for _ in {1..20}; do
    if [[ -z "$(ps -p "$pid" -o command= 2>/dev/null)" ]]; then break; fi
    sleep 0.25
  done
  if [[ -n "$(ps -p "$pid" -o command= 2>/dev/null)" ]]; then
    echo "${name}：停止逾時，請檢查 PID $pid" >&2
    return 1
  fi
  rm -f "$file"
  echo "${name}：已停止"
}

stop_process "前端" "$RUN_DIR/frontend.pid" "$ROOT/frontend/node_modules/vite/bin/vite.js"
stop_process "後端" "$RUN_DIR/backend.pid" "$ROOT/backend/target/backend-0.1.0.jar"
