#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import re
import socket
import socketserver
import stat
import threading
from pathlib import Path
from typing import Any

from .session_store import SessionManager, session_metadata
from .payment_handoffs import PaymentHandoffStore
from .upstream.observability import redact_text
from .voucher_store import VoucherStore


PROTOCOL_VERSION = 2
MAX_REQUEST_BYTES = 64 * 1024
MAX_CONCURRENT_CONNECTIONS = 16
CLIENT_OPERATIONS = {
    "banking": {
        "status",
        "banking.list_accounts",
        "banking.spending_categories",
        "banking.resolve_hotel_payment_handoff",
    },
    "hotels": {
        "status",
        "hotels.get_booking_v1",
        "hotels.get_customer",
        "hotels.list_bookings",
        "hotels.save_voucher_v1",
        "hotels.create_payment_handoff",
    },
}
VERIFIED_OPERATIONS = {
    "banking": set(),
    "hotels": {
        "hotels.get_booking_v1",
        "hotels.get_customer",
        "hotels.list_bookings",
        "hotels.save_voucher_v1",
    },
}


def broker_socket_path() -> Path:
    return Path(
        os.environ.get(
            "TBANK_AUTH_BROKER_SOCKET",
            os.path.expanduser("~/.local/share/tbank-auth-broker/auth.sock"),
        )
    )


def _required_text(params: dict[str, Any], name: str) -> str:
    value = params.get(name)
    if not isinstance(value, str) or not value.strip() or len(value) > 256:
        raise ValueError(f"{name} must be a non-empty string up to 256 characters")
    return value.strip()


def _required_identifier(params: dict[str, Any], name: str) -> str:
    value = _required_text(params, name)
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,128}", value):
        raise ValueError(f"{name} contains unsupported characters")
    return value


def _required_bool(params: dict[str, Any], name: str) -> bool:
    value = params.get(name)
    if not isinstance(value, bool):
        raise ValueError(f"{name} must be a boolean")
    return value


def _safe_error_message(error: Exception) -> str:
    text = redact_text(str(error))
    text = re.sub(r"\b[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}\b", "REDACTED_SESSION", text)
    text = re.sub(r"(?i)(authorization|bearer|access_token|refresh_token|sessionid|cookie)(\s*[:=]\s*)\S+", r"\1\2REDACTED", text)
    text = re.sub(r"(?<!\d)(?:\+7|7|8)\d{10}(?!\d)", "REDACTED_PHONE", text)
    text = re.sub(r"\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b", "REDACTED_EMAIL", text)
    text = re.sub(r"\b[A-Za-z0-9_-]{40,}\b", "REDACTED_SECRET", text)
    return text[:240]


def _booking_payment_facts(booking: Any) -> dict[str, Any]:
    if not isinstance(booking, dict):
        raise ValueError("Hotels booking response has an unsupported shape")
    payment_data = (booking.get("rateData") or {}).get("paymentData")
    if not isinstance(payment_data, dict):
        raise ValueError("Hotels booking response does not contain paymentData")
    price = payment_data.get("paymentPrice")
    if not isinstance(price, dict):
        raise ValueError("Hotels booking response does not contain paymentPrice")
    return {
        "amount": price.get("amount"),
        "currency": price.get("currency"),
        "hotel_payment_status": payment_data.get("paymentStatus"),
    }


class BrokerService:
    """Owns refreshable mobile credentials and exposes only allowlisted operations."""

    def __init__(
        self,
        sessions: SessionManager | None = None,
        voucher_store: VoucherStore | None = None,
        payment_handoffs: PaymentHandoffStore | None = None,
    ):
        self.sessions = sessions or SessionManager()
        self._voucher_store = voucher_store
        self.payment_handoffs = payment_handoffs or PaymentHandoffStore()
        self._operation_lock = threading.RLock()

    @property
    def voucher_store(self) -> VoucherStore:
        if self._voucher_store is None:
            self._voucher_store = VoucherStore()
        return self._voucher_store

    def dispatch(self, request: dict[str, Any]) -> dict[str, Any]:
        if request.get("version") != PROTOCOL_VERSION:
            raise ValueError("unsupported auth broker protocol version")
        client = request.get("client")
        if client not in CLIENT_OPERATIONS:
            raise ValueError("unknown auth broker client scope")
        method = request.get("method")
        if method not in CLIENT_OPERATIONS[client]:
            raise ValueError("operation is not allowed for this auth broker client scope")
        params = request.get("params") if isinstance(request.get("params"), dict) else {}
        if method == "status":
            metadata = session_metadata(self.sessions.path)
            return {
                "protocolVersion": PROTOCOL_VERSION,
                "sessionConfigured": metadata["configured"],
                "sessionOwnerOnly": metadata["ownerOnly"],
                "credentialsExposed": False,
                "clientScope": client,
                "supportedOperations": sorted(CLIENT_OPERATIONS[client] - {"status"}),
                "verifiedOperations": sorted(VERIFIED_OPERATIONS[client]),
            }
        if method == "banking.resolve_hotel_payment_handoff":
            return self.payment_handoffs.resolve(_required_text(params, "paymentHandoffRef"))
        with self._operation_lock:
            session = self.sessions.get()
            if method == "banking.list_accounts":
                return {"accounts": session.list_accounts()}
            if method == "banking.spending_categories":
                account_id = _required_text(params, "accountId")
                start_ms = int(params.get("startMs"))
                end_ms = int(params.get("endMs"))
                if start_ms < 0 or end_ms <= start_ms:
                    raise ValueError("invalid spending period")
                return {"summary": session.spending_categories(account_id, start_ms, end_ms)}
            if method == "hotels.get_booking_v1":
                return {"booking": session.hotel_booking(_required_identifier(params, "bookingId"))}
            if method == "hotels.create_payment_handoff":
                booking_id = _required_identifier(params, "bookingId")
                booking = session.hotel_booking(booking_id)
                return self.payment_handoffs.create(booking_id, **_booking_payment_facts(booking))
            if method == "hotels.save_voucher_v1":
                booking_id = _required_identifier(params, "bookingId")
                try:
                    content, content_type = session.hotel_voucher(booking_id)
                    voucher = self.voucher_store.save(content, content_type)
                except Exception as error:
                    raise RuntimeError("Hotels voucher could not be saved safely") from error
                return {
                    "voucher": voucher,
                    "handling": "Show the local path to the user. Do not read, parse, attach, summarize, or upload the PDF unless the user explicitly asks.",
                }
            if method == "hotels.get_customer":
                return {"customer": session.hotel_customer_data()}
            if method == "hotels.list_bookings":
                return {"bookings": session.hotel_bookings_list(
                    active=_required_bool(params, "isActiveRequired"),
                    cancelled=_required_bool(params, "isCancelledRequired"),
                    completed=_required_bool(params, "isCompletedRequired"),
                )}
        raise RuntimeError("allowlisted auth broker operation has no handler")


class _BrokerHandler(socketserver.StreamRequestHandler):
    timeout = 5

    def handle(self) -> None:
        line = self.rfile.readline(MAX_REQUEST_BYTES + 1)
        if not line or len(line) > MAX_REQUEST_BYTES:
            self._reply({"ok": False, "error": "invalid request size"})
            return
        try:
            request = json.loads(line)
            if not isinstance(request, dict):
                raise ValueError("request must be an object")
            result = self.server.service.dispatch(request)  # type: ignore[attr-defined]
            self._reply({"ok": True, "result": result})
        except Exception as error:
            self._reply({"ok": False, "error": _safe_error_message(error)})

    def _reply(self, payload: dict[str, Any]) -> None:
        self.wfile.write(json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode() + b"\n")


class _BrokerServer(socketserver.ThreadingUnixStreamServer):
    daemon_threads = True
    request_queue_size = MAX_CONCURRENT_CONNECTIONS

    def __init__(self, *args, **kwargs):
        self._connection_slots = threading.BoundedSemaphore(MAX_CONCURRENT_CONNECTIONS)
        super().__init__(*args, **kwargs)

    def process_request(self, request, client_address) -> None:
        if not self._connection_slots.acquire(blocking=False):
            self.shutdown_request(request)
            return
        super().process_request(request, client_address)

    def process_request_thread(self, request, client_address) -> None:
        try:
            super().process_request_thread(request, client_address)
        finally:
            self._connection_slots.release()


def main() -> None:
    path = broker_socket_path()
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    os.chmod(path.parent, 0o700)
    if path.exists():
        if not stat.S_ISSOCK(path.lstat().st_mode):
            raise RuntimeError("auth broker path exists and is not a Unix socket")
        probe = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        try:
            probe.settimeout(0.2)
            probe.connect(str(path))
        except OSError:
            path.unlink()
        else:
            raise RuntimeError("auth broker is already running")
        finally:
            probe.close()
    old_umask = os.umask(0o177)
    try:
        server = _BrokerServer(str(path), _BrokerHandler)
    finally:
        os.umask(old_umask)
    server.service = BrokerService()  # type: ignore[attr-defined]
    os.chmod(path, 0o600)
    try:
        try:
            server.serve_forever()
        except KeyboardInterrupt:
            pass
    finally:
        server.server_close()
        try:
            path.unlink()
        except FileNotFoundError:
            pass


if __name__ == "__main__":
    main()
