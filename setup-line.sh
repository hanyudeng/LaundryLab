#!/usr/bin/env bash
set -euo pipefail

LINE_CHANNEL_ID="${LINE_CHANNEL_ID:-2012759531}"
LINE_SECRET_KEYCHAIN_SERVICE="${LINE_SECRET_KEYCHAIN_SERVICE:-LaundryLab LINE Channel Secret}"
LINE_TOKEN_KEYCHAIN_SERVICE="${LINE_TOKEN_KEYCHAIN_SERVICE:-LaundryLab LINE Channel Token}"

command -v security >/dev/null || { echo "此腳本需要 macOS 的 security 指令。" >&2; exit 1; }
if [[ "${1:-}" != "--token-only" ]]; then
  echo "請從 LINE Developers Console 複製 My Laundry Lab 的 Channel secret，貼到下方提示並按 Enter。輸入時不會顯示字元。"
  security add-generic-password -a "$LINE_CHANNEL_ID" -s "$LINE_SECRET_KEYCHAIN_SERVICE" -U -w
fi
echo "接著複製同一個 Channel 的 Channel access token (long-lived)，貼到下方提示並按 Enter。"
IFS= read -r -s line_token
printf '\n'
line_token="${line_token//[[:space:]]/}"
if (( ${#line_token} < 150 )); then
  echo "Token 看起來不完整（目前 ${#line_token} 字元）；請從 LINE Developers 頁面右側複製圖示重新複製。" >&2
  exit 1
fi
security add-generic-password -a "$LINE_CHANNEL_ID" -s "$LINE_TOKEN_KEYCHAIN_SERVICE" -U -w "$line_token"
unset line_token
echo "LINE Channel 憑證已存入 macOS 鑰匙圈。請執行 ./stop.sh，接著執行 ./start.sh。"
