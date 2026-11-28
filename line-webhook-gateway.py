#!/usr/bin/env python3
"""Expose only the signed LINE webhook endpoint to a local HTTPS tunnel."""

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen
import os


BACKEND_PORT = int(os.environ.get("BACKEND_PORT", "8080"))
GATEWAY_PORT = int(os.environ.get("LINE_GATEWAY_PORT", "8090"))
WEBHOOK_PATH = "/api/line/webhook"
MAX_BODY = 1024 * 1024


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path != WEBHOOK_PATH:
            self.send_error(404)
            return
        try:
            size = int(self.headers.get("Content-Length", "-1"))
        except ValueError:
            size = -1
        if size < 0 or size > MAX_BODY:
            self.send_error(413)
            return
        body = self.rfile.read(size)
        request = Request(
            f"http://127.0.0.1:{BACKEND_PORT}{WEBHOOK_PATH}",
            data=body,
            headers={
                "Content-Type": "application/json",
                "X-Line-Signature": self.headers.get("X-Line-Signature", ""),
            },
            method="POST",
        )
        try:
            with urlopen(request, timeout=10) as response:
                status = response.status
        except HTTPError as error:
            status = error.code
        except URLError:
            status = 502
        self.send_response(status)
        self.send_header("Content-Length", "0")
        self.end_headers()

    def do_GET(self):
        self.send_error(404)

    def log_message(self, format, *args):
        pass


if __name__ == "__main__":
    ThreadingHTTPServer(("127.0.0.1", GATEWAY_PORT), Handler).serve_forever()
