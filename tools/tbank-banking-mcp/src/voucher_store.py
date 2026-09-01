from __future__ import annotations

import os
import re
import stat
import threading
import time
from datetime import datetime, timezone
from pathlib import Path
from secrets import token_hex


MAX_VOUCHER_BYTES = 5 * 1024 * 1024
DEFAULT_VOUCHER_TTL_SECONDS = 15 * 60
MIN_VOUCHER_TTL_SECONDS = 60
MAX_VOUCHER_TTL_SECONDS = 60 * 60
VOUCHER_REFERENCE = re.compile(r"^voucher_[a-f0-9]{24}$")


def _configured_ttl_seconds() -> int:
    configured = os.environ.get("TBANK_HOTELS_VOUCHER_TTL_SECONDS")
    if configured is None:
        return DEFAULT_VOUCHER_TTL_SECONDS
    try:
        value = int(configured)
    except ValueError as error:
        raise ValueError("voucher TTL must be an integer number of seconds") from error
    if value < MIN_VOUCHER_TTL_SECONDS or value > MAX_VOUCHER_TTL_SECONDS:
        raise ValueError("voucher TTL must be between 60 and 3600 seconds")
    return value


def _configured_directory() -> Path:
    configured = os.environ.get("TBANK_HOTELS_VOUCHER_DIRECTORY")
    return Path(configured).expanduser() if configured else Path(
        os.path.expanduser("~/.local/share/tbank-banking-mcp/vouchers")
    )


class VoucherStore:
    """Stores short-lived voucher PDFs without putting document bytes in MCP."""

    def __init__(self, directory: Path | None = None, ttl_seconds: int | None = None):
        self.directory = directory or _configured_directory()
        self.ttl_seconds = ttl_seconds if ttl_seconds is not None else _configured_ttl_seconds()
        if self.ttl_seconds < MIN_VOUCHER_TTL_SECONDS or self.ttl_seconds > MAX_VOUCHER_TTL_SECONDS:
            raise ValueError("voucher TTL must be between 60 and 3600 seconds")
        self._lock = threading.RLock()
        self._timers: dict[str, threading.Timer] = {}
        self._prepare_directory()
        self._restore_cleanup_schedule()

    def save(self, content: bytes, content_type: str) -> dict[str, object]:
        if not isinstance(content, bytes) or not content:
            raise ValueError("voucher response must contain PDF bytes")
        if len(content) > MAX_VOUCHER_BYTES:
            raise ValueError("voucher PDF exceeds the 5 MiB safe size limit")
        normalized_type = str(content_type or "").split(";", 1)[0].strip().lower()
        if normalized_type != "application/pdf":
            raise ValueError("voucher response content type must be application/pdf")
        if not content.startswith(b"%PDF-"):
            raise ValueError("voucher response does not have a PDF signature")

        voucher_ref = f"voucher_{token_hex(12)}"
        target = self.directory / f"{voucher_ref}.pdf"
        flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL
        if hasattr(os, "O_NOFOLLOW"):
            flags |= os.O_NOFOLLOW
        descriptor = os.open(target, flags, 0o600)
        try:
            with os.fdopen(descriptor, "wb") as stream:
                stream.write(content)
                stream.flush()
                os.fsync(stream.fileno())
            os.chmod(target, 0o600)
        except Exception:
            try:
                target.unlink()
            except FileNotFoundError:
                pass
            raise

        expires_at_epoch = time.time() + self.ttl_seconds
        self._schedule_delete(voucher_ref, self.ttl_seconds)
        return {
            "voucherRef": voucher_ref,
            "localPath": str(target.absolute()),
            "contentType": "application/pdf",
            "sizeBytes": len(content),
            "expiresAt": datetime.fromtimestamp(expires_at_epoch, timezone.utc).isoformat(),
            "ownerOnly": True,
            "containsPersonalData": True,
            "documentContentIncluded": False,
            "credentialsExposed": False,
        }

    def delete(self, voucher_ref: str) -> bool:
        if not VOUCHER_REFERENCE.fullmatch(voucher_ref):
            raise ValueError("invalid voucher reference")
        target = self.directory / f"{voucher_ref}.pdf"
        with self._lock:
            timer = self._timers.pop(voucher_ref, None)
            if timer is not None and timer is not threading.current_thread():
                timer.cancel()
            try:
                metadata = target.lstat()
            except FileNotFoundError:
                return False
            if not stat.S_ISREG(metadata.st_mode):
                raise RuntimeError("voucher path is not a regular file")
            target.unlink()
            return True

    def _prepare_directory(self) -> None:
        if self.directory.exists() and self.directory.is_symlink():
            raise RuntimeError("voucher directory must not be a symbolic link")
        self.directory.mkdir(parents=True, exist_ok=True, mode=0o700)
        if not self.directory.is_dir():
            raise RuntimeError("voucher storage path is not a directory")
        os.chmod(self.directory, 0o700)

    def _restore_cleanup_schedule(self) -> None:
        now = time.time()
        for candidate in self.directory.iterdir():
            match = re.fullmatch(r"(voucher_[a-f0-9]{24})\.pdf", candidate.name)
            if not match:
                continue
            try:
                metadata = candidate.lstat()
            except FileNotFoundError:
                continue
            if not stat.S_ISREG(metadata.st_mode):
                continue
            remaining = self.ttl_seconds - max(0.0, now - metadata.st_mtime)
            if remaining <= 0:
                candidate.unlink()
            else:
                self._schedule_delete(match.group(1), remaining)

    def _schedule_delete(self, voucher_ref: str, delay_seconds: float) -> None:
        def expire() -> None:
            try:
                self.delete(voucher_ref)
            except (FileNotFoundError, RuntimeError, ValueError):
                pass

        timer = threading.Timer(delay_seconds, expire)
        timer.daemon = True
        with self._lock:
            previous = self._timers.get(voucher_ref)
            if previous is not None:
                previous.cancel()
            self._timers[voucher_ref] = timer
        timer.start()
