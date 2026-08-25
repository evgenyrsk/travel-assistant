from __future__ import annotations

import argparse
import json
import os
import selectors
import subprocess
import sys
from datetime import date, timedelta
from pathlib import Path
from typing import Any, Callable


SMOKE_VERSION = "1.0"
MCP_TIMEOUT_SECONDS = 70
DEFAULT_AUTH_BROKER_SOCKET = "~/.local/share/tbank-auth-broker/auth.sock"


class McpSmokeError(RuntimeError):
    pass


class McpProcess:
    def __init__(self, command: list[str], cwd: Path, env_overrides: dict[str, str] | None = None):
        self._next_id = 1
        child_env = os.environ.copy()
        child_env.update(env_overrides or {})
        self._process = subprocess.Popen(
            command,
            cwd=cwd,
            env=child_env,
            text=True,
            bufsize=1,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL,
        )
        if self._process.stdin is None or self._process.stdout is None:
            raise McpSmokeError("unable to open MCP stdio")
        self._selector = selectors.DefaultSelector()
        self._selector.register(self._process.stdout, selectors.EVENT_READ)

    def call(self, name: str, arguments: dict[str, Any] | None = None) -> Any:
        request_id = self._next_id
        self._next_id += 1
        request = {
            "jsonrpc": "2.0",
            "id": request_id,
            "method": "tools/call",
            "params": {"name": name, "arguments": arguments or {}},
        }
        self._process.stdin.write(json.dumps(request, separators=(",", ":")) + "\n")
        self._process.stdin.flush()
        if not self._selector.select(MCP_TIMEOUT_SECONDS):
            raise McpSmokeError("MCP response timed out")
        line = self._process.stdout.readline()
        if not line:
            raise McpSmokeError("MCP process returned no response")
        try:
            response = json.loads(line)
        except json.JSONDecodeError as error:
            raise McpSmokeError("MCP returned invalid JSON") from error
        if response.get("id") != request_id or "error" in response:
            raise McpSmokeError("MCP returned a protocol error")
        result = response.get("result") if isinstance(response.get("result"), dict) else {}
        if result.get("isError") is True:
            raise McpSmokeError("MCP tool returned an error")
        content = result.get("content") if isinstance(result.get("content"), list) else []
        text = content[0].get("text") if content and isinstance(content[0], dict) else None
        if not isinstance(text, str):
            raise McpSmokeError("MCP tool returned no text payload")
        try:
            return json.loads(text)
        except json.JSONDecodeError as error:
            raise McpSmokeError("MCP tool returned an invalid payload") from error

    def close(self) -> None:
        self._selector.close()
        if self._process.stdin is not None:
            self._process.stdin.close()
        self._process.terminate()
        try:
            self._process.wait(timeout=2)
        except subprocess.TimeoutExpired:
            self._process.kill()


def _attempt(section: dict[str, Any], name: str, operation: Callable[[], Any]) -> Any | None:
    try:
        value = operation()
        section[name] = {"status": "passed"}
        return value
    except Exception as error:
        section[name] = {"status": "failed", "errorType": type(error).__name__}
        return None


def _booking_lists(value: Any) -> list[dict[str, Any]]:
    root = value.get("payload") if isinstance(value, dict) and isinstance(value.get("payload"), dict) else value
    if not isinstance(root, dict):
        return []
    result: list[dict[str, Any]] = []
    for name in ("activeList", "cancelledList", "completedList"):
        values = root.get(name)
        if isinstance(values, list):
            result.extend(item for item in values if isinstance(item, dict))
    return result


def shared_auth_environment(environment: dict[str, str] | None = None) -> dict[str, str]:
    source = environment if environment is not None else os.environ
    broker_socket = source.get("TBANK_AUTH_BROKER_SOCKET", DEFAULT_AUTH_BROKER_SOCKET)
    return {"TBANK_AUTH_BROKER_SOCKET": os.path.expanduser(broker_socket)}


def run_safe_smoke(
    banking_call: Callable[[str, dict[str, Any] | None], Any],
    hotels_call: Callable[[str, dict[str, Any] | None], Any],
    *,
    today: date | None = None,
) -> dict[str, Any]:
    report: dict[str, Any] = {
        "smokeVersion": SMOKE_VERSION,
        "readOnlyProviderOperations": True,
        "externalMutationsAttempted": False,
        "credentialsIncluded": False,
        "personalDataIncluded": False,
        "identifiersIncluded": False,
        "banking": {},
        "hotels": {},
    }
    banking = report["banking"]
    hotels = report["hotels"]
    account_ref = None

    banking_status = _attempt(banking, "connectionStatus", lambda: banking_call("tbank_banking_connection_status", None))
    if isinstance(banking_status, dict):
        banking["serverVersion"] = banking_status.get("serverVersion")
        banking["sessionReady"] = banking_status.get("phoneAuth", {}).get("sessionConfigured") is True

    accounts_payload = _attempt(banking, "listAccounts", lambda: banking_call("tbank_banking_list_accounts", None))
    accounts = accounts_payload.get("accounts") if isinstance(accounts_payload, dict) else None
    banking["accountsAvailable"] = isinstance(accounts, list) and bool(accounts)
    if isinstance(accounts, list) and accounts and isinstance(accounts[0], dict):
        account_ref = accounts[0].get("accountRef")
        summary = _attempt(banking, "spendingSummary", lambda: banking_call(
            "tbank_banking_spending_summary", {"account_ref": account_ref, "days": 90}
        ))
        banking["rawTransactionsIncluded"] = summary.get("rawTransactionsIncluded") if isinstance(summary, dict) else None
        profile = _attempt(banking, "travelProfile", lambda: banking_call(
            "tbank_banking_build_travel_profile", {"account_ref": account_ref, "days": 90}
        ))
        banking["travelProfileReturned"] = isinstance(profile, dict) and isinstance(profile.get("travelProfile"), dict)

    hotels_status = _attempt(hotels, "connectionStatus", lambda: hotels_call("tbank_hotels_connection_status", None))
    if isinstance(hotels_status, dict):
        hotels["serverVersion"] = hotels_status.get("serverVersion")
        hotels["searchReady"] = hotels_status.get("searchReady") is True
        hotels["customerReadiness"] = hotels_status.get("customerReadiness")
        hotels["bookingExecutionAvailable"] = hotels_status.get("bookingExecution", {}).get("available") is True

    customer = _attempt(hotels, "getCustomer", lambda: hotels_call("tbank_hotels_get_customer", None))
    hotels["customerPayloadReturned"] = customer is not None
    bookings = _attempt(hotels, "listBookings", lambda: hotels_call(
        "tbank_hotels_list_bookings",
        {"isActiveRequired": True, "isCancelledRequired": True, "isCompletedRequired": True},
    ))
    booking_items = _booking_lists(bookings)
    hotels["bookingsAvailable"] = bool(booking_items)
    if booking_items:
        booking_ref = booking_items[0].get("bookingRef")
        if isinstance(booking_ref, str):
            booking = _attempt(hotels, "getBookingByRef", lambda: hotels_call(
                "tbank_hotels_get_booking", {"bookingRef": booking_ref, "apiVersion": "v1"}
            ))
            hotels["bookingPayloadReturned"] = booking is not None
            handoff = _attempt(hotels, "paymentHandoff", lambda: hotels_call(
                "tbank_hotels_create_payment_handoff_preview", {"bookingRef": booking_ref}
            ))
            handoff_ref = handoff.get("paymentHandoffRef") if isinstance(handoff, dict) else None
            if isinstance(account_ref, str) and isinstance(handoff_ref, str):
                payment = _attempt(banking, "paymentPreview", lambda: banking_call(
                    "tbank_banking_prepare_hotel_payment_handoff_preview",
                    {"payment_handoff_ref": handoff_ref, "source_account_ref": account_ref},
                ))
                if isinstance(payment, dict):
                    banking["paymentPreviewOnly"] = payment.get("status") == "preview_only"
                    banking["paymentExecutionAvailable"] = payment.get("executionAvailable") is True
                    payment_intent_id = payment.get("paymentIntentId")
                    if isinstance(payment_intent_id, str):
                        _attempt(banking, "paymentStatus", lambda: banking_call(
                            "tbank_banking_payment_status", {"payment_intent_id": payment_intent_id}
                        ))

    if isinstance(hotels_status, dict) and hotels_status.get("searchReady") is True:
        checkin = (today or date.today()) + timedelta(days=60)
        checkout = checkin + timedelta(days=1)
        plan = _attempt(hotels, "planStay", lambda: hotels_call(
            "tbank_hotels_plan_stay",
            {
                "destination": "Москва",
                "checkinDate": checkin.isoformat(),
                "checkoutDate": checkout.isoformat(),
                "rooms": [{"adults": 2, "childrenAges": []}],
                "maxOptions": 2,
                "ranking": "provider_order",
            },
        ))
        if isinstance(plan, dict):
            hotels["searchStatus"] = plan.get("status")
            hotels["returnedOptions"] = plan.get("returnedOptions")
            options = plan.get("options") if isinstance(plan.get("options"), list) else []
            journey_id = plan.get("journeyId")
            if options and isinstance(options[0], dict) and isinstance(journey_id, str):
                option_id = options[0].get("optionId")
                _attempt(hotels, "selectStayOption", lambda: hotels_call(
                    "tbank_hotels_select_stay_option", {"journeyId": journey_id, "optionId": option_id}
                ))
                rates = _attempt(hotels, "getSelectedStayRates", lambda: hotels_call(
                    "tbank_hotels_get_selected_stay_rates", {"journeyId": journey_id}
                ))
                rate_options = rates.get("rateOptions") if isinstance(rates, dict) and isinstance(rates.get("rateOptions"), list) else []
                hotels["ratesAvailable"] = bool(rate_options)
                if rate_options and isinstance(rate_options[0], dict):
                    rate_option_id = rate_options[0].get("rateOptionId")
                    _attempt(hotels, "selectStayRate", lambda: hotels_call(
                        "tbank_hotels_select_stay_rate", {"journeyId": journey_id, "rateOptionId": rate_option_id}
                    ))
                    preview = _attempt(hotels, "bookingPreview", lambda: hotels_call(
                        "tbank_hotels_create_booking_preview", {"journeyId": journey_id}
                    ))
                    if isinstance(preview, dict):
                        hotels["bookingPreviewOnly"] = preview.get("status") == "preview_only"
                        hotels["personalDataCollected"] = preview.get("personalDataCollected") is True
                        hotels["bookingHttpRequestPerformed"] = preview.get("httpRequestPerformed") is True

    return report


def main() -> None:
    parser = argparse.ArgumentParser(description="Privacy-safe read-only/preview-only smoke for Banking and Hotels MCP.")
    parser.add_argument("--acknowledge-read-own-data", action="store_true")
    args = parser.parse_args()
    if not args.acknowledge_read_own_data:
        parser.error("--acknowledge-read-own-data is required")

    banking_root = Path(__file__).resolve().parent.parent
    hotels_root = banking_root.parent / "tbank-hotels-mcp"
    shared_auth_env = shared_auth_environment()
    banking = McpProcess([sys.executable, "-m", "src.server"], banking_root, shared_auth_env)
    hotels = McpProcess(["node", str(hotels_root / "src" / "server.mjs")], hotels_root, shared_auth_env)
    try:
        report = run_safe_smoke(banking.call, hotels.call)
    finally:
        banking.close()
        hotels.close()
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
