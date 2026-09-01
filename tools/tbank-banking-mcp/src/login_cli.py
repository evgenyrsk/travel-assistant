from __future__ import annotations

import getpass
import re
import sys
from pathlib import Path

from .session_store import DEFAULT_SESSION_PATH, blank_session, exclusive_session_lock, write_session
from .upstream.client import TbankApiError


PHONE = re.compile(r"^\+7\d{10}$")


def _confirm(session, kind: str, prompt: str) -> bool:
    value = getpass.getpass(prompt)
    try:
        session.confirm_step(kind, value)
        return True
    except TbankApiError as error:
        if getattr(error, "result_code", "") == "NEXT_STEP":
            print(f"Нужен следующий шаг: {error.message}")
            return False
        raise


def logout(path: Path = DEFAULT_SESSION_PATH) -> bool:
    with exclusive_session_lock(path):
        try:
            path.unlink()
            return True
        except FileNotFoundError:
            return False


def main() -> int:
    if len(sys.argv) == 2 and sys.argv[1] == "--logout":
        if logout():
            print(f"Локальная mobile session удалена: {DEFAULT_SESSION_PATH}")
        else:
            print("Локальная mobile session уже отсутствует.")
        return 0
    if len(sys.argv) == 1:
        phone = input("Номер телефона (+7XXXXXXXXXX): ").strip()
    elif len(sys.argv) == 2:
        phone = sys.argv[1]
    else:
        print("Usage: tbank-banking-login [+7XXXXXXXXXX] | --logout")
        return 2
    if not PHONE.fullmatch(phone):
        print("Номер должен быть в формате +7XXXXXXXXXX.")
        return 2
    session = blank_session()
    try:
        print(session.login(phone))
        if not _confirm(session, "otp", "SMS-код: "):
            if not _confirm(session, "password", "Пароль мобильного банка: "):
                _confirm(session, "pin", "PIN: ")
        if not session.access_token:
            print("Авторизация не завершена: банк запросил неподдержанный дополнительный шаг.")
            return 1
        with exclusive_session_lock(DEFAULT_SESSION_PATH):
            write_session(session)
        print(f"Готово. Сессия сохранена локально: {DEFAULT_SESSION_PATH} (0600).")
        print("Пароль, PIN и SMS-код в MCP/LLM не передавались.")
        return 0
    except Exception as error:
        print(f"Авторизация не завершена: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
