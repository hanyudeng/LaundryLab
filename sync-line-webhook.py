#!/usr/bin/env python3
"""Keep LINE's webhook endpoint pointed at the current local tunnel."""

import json
import os
import subprocess
import sys


API = "https://api.line.me/v2/bot/channel/webhook"
CHANNEL_ID = os.environ.get("LINE_CHANNEL_ID", "2012759531")
TOKEN_SERVICE = os.environ.get("LINE_TOKEN_KEYCHAIN_SERVICE", "LaundryLab LINE Channel Token")


def request(method, path, token, data=None):
    config = f'header = "Authorization: Bearer {token}"\n'
    args = ["curl", "-fsS", "-X", method, "-H", "Content-Type: application/json", "-K", "-"]
    if data is not None:
        args += ["--data-binary", json.dumps(data, separators=(",", ":"))]
    args.append(API + path)
    result = subprocess.run(args, input=config, text=True, capture_output=True)
    if result.returncode:
        raise RuntimeError(f"LINE API {method} {path} 失敗（curl 代碼 {result.returncode}）")
    return json.loads(result.stdout)


def main():
    if len(sys.argv) != 2 or not sys.argv[1].startswith("https://"):
        raise SystemExit("需要 HTTPS Webhook 網址")
    endpoint = sys.argv[1]
    token = os.environ.get("LINE_CHANNEL_ACCESS_TOKEN") or subprocess.check_output(
        ["security", "find-generic-password", "-s", TOKEN_SERVICE, "-a", CHANNEL_ID, "-w"],
        text=True,
        stderr=subprocess.DEVNULL,
    ).strip()
    current = request("GET", "/endpoint", token)
    if not current.get("active"):
        raise RuntimeError("LINE 後台的 Use webhook 尚未啟用")
    if current.get("endpoint") != endpoint:
        request("PUT", "/endpoint", token, {"endpoint": endpoint})
    result = request("POST", "/test", token, {"endpoint": endpoint})
    if not result.get("success") or result.get("statusCode") != 200:
        raise RuntimeError(f"LINE Webhook 測試失敗（HTTP {result.get('statusCode')}）")
    print("LINE Webhook 網址已同步，LINE 官方測試成功。")


if __name__ == "__main__":
    try:
        main()
    except (subprocess.CalledProcessError, json.JSONDecodeError, RuntimeError) as error:
        raise SystemExit(str(error)) from None
