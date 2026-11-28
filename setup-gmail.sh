#!/usr/bin/env bash
set -euo pipefail

SMTP_USER="${SMTP_USER:-sdsd51116@gmail.com}"
SMTP_KEYCHAIN_SERVICE="${SMTP_KEYCHAIN_SERVICE:-LaundryLab Gmail SMTP}"

command -v security >/dev/null || { echo "此腳本需要 macOS 的 security 指令。" >&2; exit 1; }
echo "請貼上為 LaundryLab 建立的 Gmail 應用程式密碼。輸入時不會顯示字元。"
security add-generic-password -a "$SMTP_USER" -s "$SMTP_KEYCHAIN_SERVICE" -U -w
echo "已存入 macOS 鑰匙圈。請執行 ./stop.sh，接著執行 ./start.sh。"
