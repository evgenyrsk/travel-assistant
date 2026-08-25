#!/usr/bin/env python3
from __future__ import annotations

import json
import hashlib
import os
import re
import secrets
import stat
import sys
import time
from pathlib import Path
from typing import Any, Callable

from .payment_intents import PaymentIntentStore
from .payment_readiness import payment_execution_readiness
from .travel_profile import build_travel_profile
from .upstream.observability import redact_text


SERVER_NAME = "tbank-banking-mcp"
SERVER_VERSION = "0.13.1"
MCP_PROTOCOL_VERSION = "2025-03-26"
MAX_PORTFOLIO_ACCOUNTS = 20
_sessions = None
payment_intents = PaymentIntentStore()
_account_ref_key = secrets.token_bytes(32)
_account_ids_by_ref: dict[str, str] = {}


def _session_path() -> Path:
    return Path(os.environ.get(
        "TBANK_BANKING_SESSION",
        os.path.expanduser("~/.local/share/tbank-banking-mcp/session.json"),
    ))


def _session_metadata() -> dict[str, Any]:
    path = _session_path()
    exists = path.exists()
    mode = stat.S_IMODE(path.stat().st_mode) if exists else None
    return {"configured": exists, "ownerOnly": mode == 0o600 if mode is not None else False}


def _sessions_manager():
    global _sessions
    if _sessions is None:
        from .session_store import SessionManager
        _sessions = SessionManager(_session_path())
    return _sessions


def _broker_client():
    socket_path = os.environ.get("TBANK_AUTH_BROKER_SOCKET")
    if not socket_path:
        return None
    from .auth_broker_client import AuthBrokerClient
    return AuthBrokerClient(Path(socket_path))


def _raw_accounts() -> list[dict[str, Any]]:
    broker = _broker_client()
    if broker is not None:
        accounts = broker.call("banking.list_accounts").get("accounts")
        return accounts if isinstance(accounts, list) else []
    return _sessions_manager().get().list_accounts()


def _json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"))


def _safe_error(error: Exception) -> str:
    text = redact_text(str(error))
    text = re.sub(r"\b[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}\b", "REDACTED_SESSION", text)
    text = re.sub(r"(?i)(authorization|bearer|access_token|refresh_token|sessionid|cookie)(\s*[:=]\s*)\S+", r"\1\2REDACTED", text)
    text = re.sub(r"(?<!\d)(?:\+7|7|8)\d{10}(?!\d)", "REDACTED_PHONE", text)
    text = re.sub(r"\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b", "REDACTED_EMAIL", text)
    text = re.sub(r"\b[A-Za-z0-9_-]{40,}\b", "REDACTED_SECRET", text)
    if len(text) > 240:
        text = text[:240] + "…"
    return _json({"ok": False, "error": text})


def _account_ref(account_id: str) -> str:
    digest = hashlib.sha256(_account_ref_key + account_id.encode()).hexdigest()[:16]
    reference = f"acct_{digest}"
    if len(_account_ids_by_ref) >= 100:
        _account_ids_by_ref.pop(next(iter(_account_ids_by_ref)))
    _account_ids_by_ref[reference] = account_id
    return reference


def _normalized_accounts(raw: list[dict[str, Any]]) -> list[dict[str, Any]]:
    result = []
    for account in raw:
        if not isinstance(account, dict):
            continue
        account_id = str(account.get("id") or account.get("accountId") or "")
        if not account_id:
            continue
        result.append(
            {
                "accountRef": _account_ref(account_id),
                "name": account.get("name") or account.get("displayName") or account.get("type"),
                "type": account.get("type") or account.get("accountType"),
                "status": account.get("status"),
            }
        )
    return result


def connection_status(_: dict[str, Any]) -> str:
    metadata = _session_metadata()
    broker = _broker_client()
    broker_status = "not_configured"
    broker_metadata = None
    if broker is not None:
        try:
            broker_metadata = broker.call("status")
            broker_status = "available" if broker_metadata.get("sessionConfigured") else "login_required"
        except Exception:
            broker_status = "unavailable"
    session_configured = metadata["configured"] if broker is None else broker_status == "available"
    return _json(
        {
            "serverVersion": SERVER_VERSION,
            "phoneAuth": {
                "supported": True,
                "mode": "local_cli",
                "sessionConfigured": session_configured,
                "sessionOwnerOnly": metadata["ownerOnly"] if broker is None else None,
                "sessionProvider": "auth_broker" if broker is not None else "direct_file",
                "brokerStatus": broker_status,
            },
            "readOnlyBanking": "configured" if session_configured else "broker_unavailable" if broker_status == "unavailable" else "login_required",
            "travelProfile": "available_after_login",
            "hotelPaymentHandoff": {
                "available": broker_status == "available" and "banking.resolve_hotel_payment_handoff" in (broker_metadata or {}).get("supportedOperations", []),
                "bookingBindingSupported": broker_status == "available" and "banking.resolve_hotel_payment_handoff" in (broker_metadata or {}).get("supportedOperations", []),
                "amountBindingVerified": False,
                "paymentStatusObservation": "available_at_handoff" if broker_status == "available" and "banking.resolve_hotel_payment_handoff" in (broker_metadata or {}).get("supportedOperations", []) else "not_available",
                "amountBindingAvailableAtHandoff": broker_status == "available" and "banking.resolve_hotel_payment_handoff" in (broker_metadata or {}).get("supportedOperations", []),
                "rawPaymentStatusAvailableAtHandoff": broker_status == "available" and "banking.resolve_hotel_payment_handoff" in (broker_metadata or {}).get("supportedOperations", []),
                "singleUse": True,
            },
            "paymentExecution": payment_execution_readiness(),
            "browserDependency": False,
            "credentialsExposedToModel": False,
        }
    )


def list_accounts(_: dict[str, Any]) -> str:
    try:
        return _json({"ok": True, "accounts": _normalized_accounts(_raw_accounts())})
    except Exception as error:
        return _safe_error(error)


def _spending_summary_by_account_id(account_id: str, days: int) -> dict[str, Any]:
    if days < 1 or days > 366:
        raise ValueError("days must be between 1 and 366")
    end_ms = int(time.time() * 1000)
    start_ms = end_ms - days * 86_400_000
    broker = _broker_client()
    if broker is not None:
        result = broker.call("banking.spending_categories", {
            "accountId": account_id,
            "startMs": start_ms,
            "endMs": end_ms,
        })
        summary = result.get("summary")
        return summary if isinstance(summary, dict) else {}
    return _sessions_manager().get().spending_categories(account_id, start_ms, end_ms)


def _spending_summary(account_ref: str, days: int) -> dict[str, Any]:
    account_id = _account_ids_by_ref.get(account_ref)
    if account_id is None:
        raise ValueError("unknown account_ref; call tbank_banking_list_accounts in this MCP process first")
    return _spending_summary_by_account_id(account_id, days)


def spending_summary(arguments: dict[str, Any]) -> str:
    try:
        summary = _spending_summary(str(arguments.get("account_ref") or ""), int(arguments.get("days", 90)))
        return _json({"ok": True, "summary": summary, "rawTransactionsIncluded": False})
    except Exception as error:
        return _safe_error(error)


def travel_profile(arguments: dict[str, Any]) -> str:
    try:
        days = int(arguments.get("days", 90))
        summary = _spending_summary(str(arguments.get("account_ref") or ""), days)
        return _json({"ok": True, "travelProfile": build_travel_profile(summary, days)})
    except Exception as error:
        return _safe_error(error)


def _travel_signal_band(share: float) -> str:
    if share >= 0.3:
        return "high"
    if share >= 0.1:
        return "moderate"
    if share > 0:
        return "low"
    return "none"


def portfolio_travel_profile(arguments: dict[str, Any]) -> str:
    try:
        days = int(arguments.get("days", 90))
        if days < 30 or days > 366:
            raise ValueError("days must be between 30 and 366")
        accounts = _raw_accounts()[:MAX_PORTFOLIO_ACCOUNTS]
        candidates = []
        skipped = 0
        for account in accounts:
            account_id = str(account.get("id") or account.get("accountId") or "") if isinstance(account, dict) else ""
            if not account_id:
                skipped += 1
                continue
            try:
                profile = build_travel_profile(_spending_summary_by_account_id(account_id, days), days)
                share = float(profile.get("aggregates", {}).get("travelSpendShare") or 0)
                candidates.append((share, float(profile.get("confidence") or 0), profile))
            except Exception:
                skipped += 1
        if not candidates:
            raise RuntimeError("No eligible account aggregates were available for a portfolio travel profile")
        share, _, selected = max(candidates, key=lambda candidate: (candidate[0], candidate[1]))
        return _json({
            "ok": True,
            "portfolioTravelProfile": {
                "profileType": "portfolio_spending_based_travel_preference",
                "tier": selected["tier"],
                "confidence": selected["confidence"],
                "periodDays": days,
                "currency": selected["currency"],
                "travelSignal": _travel_signal_band(share),
                "hotelDefaults": selected["hotelDefaults"],
                "coverage": {
                    "scope": "available_accounts",
                    "complete": skipped == 0,
                    "method": "strongest_eligible_travel_signal",
                },
                "privacy": {
                    "rawTransactionsIncluded": False,
                    "aggregateCategorySignalsUsedInternally": True,
                    "categoryBreakdownIncluded": False,
                    "absoluteAmountsIncluded": False,
                    "accountIdentifiersIncluded": False,
                    "accountNamesIncluded": False,
                    "bookingHistoryUsed": False,
                    "incomeTierClaimed": False,
                    "userOverrideRecommended": True,
                },
                "explanation": (
                    "Профиль вычислен по агрегированным категориям расходов среди доступных счетов. "
                    "Разбивка категорий, количество и состав счетов, абсолютные суммы и операции не раскрываются; "
                    "история бронирований не используется."
                ),
            },
        })
    except Exception as error:
        return _safe_error(error)


def prepare_hotel_payment_handoff_preview(arguments: dict[str, Any]) -> str:
    try:
        handoff_ref = str(arguments.get("payment_handoff_ref") or "")
        if not re.fullmatch(r"payment_handoff_[a-f0-9]{24}", handoff_ref):
            raise ValueError("paymentHandoffRef must be issued by the shared auth broker")
        source_account_ref = str(arguments.get("source_account_ref") or "")
        source_account_id = _account_ids_by_ref.get(source_account_ref)
        if source_account_id is None:
            raise ValueError("unknown source_account_ref; call tbank_banking_list_accounts in this MCP process first")
        broker = _broker_client()
        if broker is None:
            raise ValueError("Hotel payment handoff preview requires the shared local auth broker")
        binding = broker.call(
            "banking.resolve_hotel_payment_handoff",
            {"paymentHandoffRef": handoff_ref},
        )
        try:
            preview = payment_intents.prepare_hotel_handoff_preview(
                payment_handoff_ref=handoff_ref,
                booking_binding_verified=binding.get("bookingBindingVerified") is True,
                amount_binding_verified=binding.get("amountBindingVerified") is True,
                payment_status_observation=binding.get("paymentStatusObservation"),
                provider_requests_performed=binding.get("providerRequestsPerformed") is True,
                facts_observed_at_epoch=binding.get("factsObservedAtEpoch"),
                facts_max_age_seconds=binding.get("factsMaxAgeSeconds"),
                source_account_id=source_account_id,
                amount_decimal=str(binding.get("amountDecimal") or ""),
                currency=str(binding.get("currency") or ""),
            )
        except Exception as error:
            raise ValueError(f"{error} Create a new payment handoff preview first.") from error
        return _json(preview)
    except Exception as error:
        return _safe_error(error)


def payment_status(arguments: dict[str, Any]) -> str:
    try:
        return _json(payment_intents.describe(str(arguments.get("payment_intent_id") or "")))
    except Exception as error:
        return _safe_error(error)


def _object_schema(properties: dict[str, Any], required: list[str] | None = None) -> dict[str, Any]:
    return {"type": "object", "properties": properties, "required": required or [], "additionalProperties": False}


ToolHandler = Callable[[dict[str, Any]], str]


def _tool(name: str, description: str, schema: dict[str, Any], handler: ToolHandler,
          *, read_only: bool = True, idempotent: bool = True) -> dict[str, Any]:
    return {
        "name": name,
        "description": description,
        "inputSchema": schema,
        "annotations": {
            "readOnlyHint": read_only,
            "destructiveHint": False,
            "idempotentHint": idempotent,
            "openWorldHint": True,
        },
        "handler": handler,
    }


TOOLS = [
    _tool("tbank_banking_connection_status", "Локальная readiness-диагностика без обращения к банковскому API.",
          _object_schema({}), connection_status),
    _tool("tbank_banking_list_accounts", "Счета текущего пользователя без токенов, cookies и реквизитов карт.",
          _object_schema({}), list_accounts),
    _tool(
        "tbank_banking_spending_summary",
        "Агрегированные расходы, поступления и категории; raw transactions не возвращаются.",
        _object_schema({"account_ref": {"type": "string", "pattern": "^acct_[a-f0-9]{16}$"},
                        "days": {"type": "integer", "minimum": 1, "maximum": 366, "default": 90}},
                       ["account_ref"]),
        spending_summary,
    ),
    _tool(
        "tbank_banking_build_travel_profile",
        "Профиль по одному явно выбранному account_ref; может вернуть агрегированные абсолютные суммы. Для обычного обезличенного профиля по всем счетам используйте tbank_banking_build_portfolio_travel_profile.",
        _object_schema({"account_ref": {"type": "string", "pattern": "^acct_[a-f0-9]{16}$"},
                        "days": {"type": "integer", "minimum": 30, "maximum": 366, "default": 90}},
                       ["account_ref"]),
        travel_profile,
    ),
    _tool(
        "tbank_banking_build_portfolio_travel_profile",
        "Privacy-first профиль предпочтений по всем доступным счетам. Сам получает bounded агрегаты, использует агрегированные категории внутри процесса и возвращает только tier, confidence, ценовой диапазон и ranking — без accountRef, количества/названий счетов, абсолютных сумм, разбивки категорий и истории бронирований. Не утверждайте, что категории не использовались: они использованы, но не раскрыты. Используйте для обычных просьб про обезличенный travel-профиль; не вызывайте list_accounts/spending_summary дополнительно.",
        _object_schema({"days": {"type": "integer", "minimum": 30, "maximum": 366, "default": 90}}),
        portfolio_travel_profile,
    ),
    _tool(
        "tbank_banking_prepare_hotel_payment_handoff_preview",
        "Безопасный меж-MCP preview оплаты отеля. Принимает только одноразовый короткоживущий paymentHandoffRef и account_ref; сумму, валюту и raw paymentStatus получает из связанной booking v1 карточки через broker. Первый вызов атомарно поглощает capability; для повторного preview нужен новый handoff. Не принимает bookingRef, provider orderId, paymentToken или сумму от модели, не выполняет payment setup и не переводит деньги. paymentStatus не интерпретируется как разрешение оплаты.",
        _object_schema({
            "payment_handoff_ref": {"type": "string", "pattern": "^payment_handoff_[a-f0-9]{24}$"},
            "source_account_ref": {"type": "string", "pattern": "^acct_[a-f0-9]{16}$"},
        }, ["payment_handoff_ref", "source_account_ref"]),
        prepare_hotel_payment_handoff_preview,
        read_only=False,
        idempotent=False,
    ),
    _tool(
        "tbank_banking_payment_status",
        "Состояние локального payment intent; банковский payment status пока не запрашивается.",
        _object_schema({"payment_intent_id": {"type": "string", "minLength": 1}}, ["payment_intent_id"]),
        payment_status,
    ),
]
TOOL_BY_NAME = {tool["name"]: tool for tool in TOOLS}


def _response(request_id: Any, result: Any) -> dict[str, Any]:
    return {"jsonrpc": "2.0", "id": request_id, "result": result}


def _error(request_id: Any, code: int, message: str) -> dict[str, Any]:
    return {"jsonrpc": "2.0", "id": request_id, "error": {"code": code, "message": message}}


def handle(request: dict[str, Any]) -> dict[str, Any] | None:
    if "id" not in request:
        return None
    request_id = request.get("id")
    if request.get("jsonrpc") != "2.0":
        return _error(request_id, -32600, "Invalid JSON-RPC version.")
    method = request.get("method")
    if method == "initialize":
        return _response(request_id, {
            "protocolVersion": MCP_PROTOCOL_VERSION,
            "capabilities": {"tools": {"listChanged": False}},
            "serverInfo": {"name": SERVER_NAME, "version": SERVER_VERSION},
            "instructions": (
                "Phone auth runs only in the local login CLI. The MCP exposes a curated read-only banking "
                "surface and aggregate travel profile. Payment execution is unavailable; the payment tool "
                "creates preview-only intents and never requests OTP."
            ),
        })
    if method == "ping":
        return _response(request_id, {})
    if method == "tools/list":
        return _response(request_id, {"tools": [{key: value for key, value in tool.items() if key != "handler"}
                                                  for tool in TOOLS]})
    if method == "tools/call":
        params = request.get("params") if isinstance(request.get("params"), dict) else {}
        tool = TOOL_BY_NAME.get(params.get("name"))
        if tool is None:
            return _response(request_id, {"content": [{"type": "text", "text": "Unknown tool."}], "isError": True})
        arguments = params.get("arguments") if isinstance(params.get("arguments"), dict) else {}
        try:
            text = tool["handler"](arguments)
            return _response(request_id, {"content": [{"type": "text", "text": text}], "isError": False})
        except Exception as error:
            return _response(request_id, {"content": [{"type": "text", "text": str(error)[:240]}], "isError": True})
    return _error(request_id, -32601, "Method not found.")


def main() -> None:
    for line in sys.stdin:
        try:
            parsed = json.loads(line)
            message = handle(parsed) if isinstance(parsed, dict) else _error(None, -32600, "JSON-RPC batch requests are not supported.")
            if message is not None:
                print(json.dumps(message, ensure_ascii=False, separators=(",", ":")), flush=True)
        except json.JSONDecodeError:
            print(json.dumps(_error(None, -32700, "Parse error."), separators=(",", ":")), flush=True)


if __name__ == "__main__":
    main()
