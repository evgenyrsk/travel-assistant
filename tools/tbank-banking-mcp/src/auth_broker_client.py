from __future__ import annotations

import json
import math
import os
import socket
from pathlib import Path
from typing import Any


class AuthBrokerError(RuntimeError):
    pass


class AuthBrokerClient:
    def __init__(self, path: Path, client_scope: str = "banking", timeout_seconds: float | None = None):
        self.path = path
        self.client_scope = client_scope
        configured = os.environ.get("TBANK_AUTH_BROKER_TIMEOUT_MS")
        try:
            resolved = int(configured) / 1000 if configured else 45.0
        except ValueError as error:
            raise ValueError("auth broker timeout must be an integer number of milliseconds") from error
        self.timeout_seconds = timeout_seconds if timeout_seconds is not None else resolved
        if not math.isfinite(self.timeout_seconds) or self.timeout_seconds < 1 or self.timeout_seconds > 120:
            raise ValueError("auth broker timeout must be between 1 and 120 seconds")

    def call(self, method: str, params: dict[str, Any] | None = None) -> dict[str, Any]:
        request = json.dumps(
            {"version": 2, "client": self.client_scope, "method": method, "params": params or {}},
            ensure_ascii=False,
            separators=(",", ":"),
        ).encode() + b"\n"
        try:
            with socket.socket(socket.AF_UNIX, socket.SOCK_STREAM) as client:
                client.settimeout(self.timeout_seconds)
                client.connect(str(self.path))
                client.settimeout(self.timeout_seconds)
                client.sendall(request)
                chunks = bytearray()
                while not chunks.endswith(b"\n"):
                    chunk = client.recv(4096)
                    if not chunk:
                        break
                    chunks.extend(chunk)
                    if len(chunks) > 1024 * 1024:
                        raise AuthBrokerError("auth broker response is too large")
        except (socket.timeout, TimeoutError) as error:
            raise AuthBrokerError("auth broker operation timed out") from error
        except OSError as error:
            raise AuthBrokerError("auth broker is unavailable") from error
        try:
            response = json.loads(chunks)
        except (json.JSONDecodeError, UnicodeDecodeError) as error:
            raise AuthBrokerError("auth broker returned an invalid response") from error
        if not isinstance(response, dict) or response.get("ok") is not True:
            message = response.get("error") if isinstance(response, dict) else None
            raise AuthBrokerError(str(message or "auth broker request failed")[:240])
        result = response.get("result")
        if not isinstance(result, dict):
            raise AuthBrokerError("auth broker returned an invalid result")
        return result
