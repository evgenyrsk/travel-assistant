from __future__ import annotations

import json
import os
import stat
import fcntl
from contextlib import contextmanager
from dataclasses import fields
from pathlib import Path
from typing import Any

from .upstream.client import MobileSession
from .upstream.endpoints import APP_VERSION


DEFAULT_SESSION_PATH = Path(
    os.environ.get(
        "TBANK_BANKING_SESSION",
        os.path.expanduser("~/.local/share/tbank-banking-mcp/session.json"),
    )
)


def blank_session() -> MobileSession:
    return attach_persistence(
        MobileSession(
            mobile_sessionid="",
            refresh_token="",
            client_id="gorod-app",
            client_version="112.0.0",
            vendor="t_ios",
            origin="mobile,ib5,loyalty,platform",
            platform="ios",
            app_name="mobile",
            app_version=APP_VERSION,
        )
    )


def _serializable_session(session: MobileSession) -> dict[str, Any]:
    names = {field.name for field in fields(MobileSession) if field.name != "_http"}
    return {name: getattr(session, name) for name in names if hasattr(session, name)}


def write_session(session: MobileSession, path: Path = DEFAULT_SESSION_PATH) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(f".{path.name}.tmp-{os.getpid()}")
    descriptor = os.open(temporary, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8") as output:
            json.dump(_serializable_session(session), output, ensure_ascii=False)
            output.flush()
            os.fsync(output.fileno())
        os.replace(temporary, path)
        os.chmod(path, 0o600)
    except BaseException:
        try:
            temporary.unlink()
        except OSError:
            pass
        raise


@contextmanager
def exclusive_session_lock(path: Path = DEFAULT_SESSION_PATH):
    """Serialize refresh-token rotation across local processes sharing a session."""
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    os.chmod(path.parent, 0o700)
    lock_path = path.with_name(f".{path.name}.lock")
    descriptor = os.open(lock_path, os.O_RDWR | os.O_CREAT, 0o600)
    try:
        os.chmod(lock_path, 0o600)
        fcntl.flock(descriptor, fcntl.LOCK_EX)
        yield
    finally:
        fcntl.flock(descriptor, fcntl.LOCK_UN)
        os.close(descriptor)


def load_session(path: Path = DEFAULT_SESSION_PATH) -> MobileSession | None:
    if not path.exists():
        return None
    with path.open(encoding="utf-8") as source:
        stored = json.load(source)
    accepted = {field.name for field in fields(MobileSession) if field.init}
    kwargs = {key: value for key, value in stored.items() if key in accepted}
    return attach_persistence(MobileSession(**kwargs))


def attach_persistence(session: MobileSession) -> MobileSession:
    session._on_persist = lambda: write_session(session)
    return session


def session_metadata(path: Path = DEFAULT_SESSION_PATH) -> dict[str, Any]:
    exists = path.exists()
    mode = stat.S_IMODE(path.stat().st_mode) if exists else None
    return {
        "configured": exists,
        "ownerOnly": mode == 0o600 if mode is not None else False,
        "pathSource": "TBANK_BANKING_SESSION" if os.environ.get("TBANK_BANKING_SESSION") else "default",
    }


class SessionManager:
    def __init__(self, path: Path = DEFAULT_SESSION_PATH):
        self.path = path
        self._session: MobileSession | None = None
        self._mtime_ns: int | None = None

    def get(self) -> MobileSession:
        with exclusive_session_lock(self.path):
            mtime_ns = self.path.stat().st_mtime_ns if self.path.exists() else None
            if self._session is None or mtime_ns != self._mtime_ns:
                loaded = load_session(self.path)
                if loaded is None:
                    raise RuntimeError("AUTH_REQUIRED: run the local phone login CLI first")
                loaded._on_persist = lambda: self._save_unlocked(loaded)
                self._session = loaded
                self._mtime_ns = mtime_ns
            self._session.ensure_fresh()
            return self._session

    def save(self, session: MobileSession) -> None:
        with exclusive_session_lock(self.path):
            self._save_unlocked(session)

    def _save_unlocked(self, session: MobileSession) -> None:
        write_session(session, self.path)
        self._mtime_ns = self.path.stat().st_mtime_ns
