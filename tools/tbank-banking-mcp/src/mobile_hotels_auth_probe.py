from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any, Callable, Iterable

from .session_store import SessionManager
from .upstream.observability import redact_text


PROBE_VERSION = "1.2"
MOBILE_HOTELS_ORIGIN = "https://hotels.t-bank-app.ru"
MAX_INSPECTED_RESPONSE_BYTES = 64 * 1024
MAX_DISCOVERY_BOOKING_DETAILS = 5
IDENTIFIER = re.compile(r"^[A-Za-z0-9_-]{1,128}$")
SAFE_DIAGNOSTIC = re.compile(r"^[A-Za-z0-9_.:-]{1,80}$")
AUTH_VARIANTS = ("bearer_only", "bearer_session", "capture_compatible")
NO_AUTH_CONTROL = "no_auth_control"


@dataclass(frozen=True)
class ProbeRoute:
    name: str
    method: str
    path_template: str
    identifier: str | None = None
    body: dict[str, Any] | None = None
    inspect_json: bool = False

    def path(self, identifiers: dict[str, str]) -> str:
        if self.identifier is None:
            return self.path_template
        value = identifiers.get(self.identifier)
        if not value or not IDENTIFIER.fullmatch(value):
            raise ValueError(f"{self.identifier} must match {IDENTIFIER.pattern}")
        return self.path_template.replace(f"{{{self.identifier}}}", value)


ROUTES = (
    ProbeRoute("customer_data", "GET", "/api/v1/auth/customerdata"),
    ProbeRoute(
        "booking_list",
        "POST",
        "/api/v1/hotels/bookings/booking_list",
        body={
            "isActiveRequired": True,
            "isCancelledRequired": True,
            "isCompletedRequired": True,
        },
    ),
    ProbeRoute("booking_v1", "GET", "/api/v1/hotels/bookings/{orderId}", identifier="orderId"),
    ProbeRoute(
        "voucher",
        "GET",
        "/api/v1/hotels/bookings/voucher/{orderId}",
        identifier="orderId",
        inspect_json=False,
    ),
    ProbeRoute(
        "evo_booking",
        "GET",
        "/api/v1/hotels/bookings/evo/{orderId}",
        identifier="orderId",
        inspect_json=True,
    ),
    ProbeRoute(
        "booking_task_status",
        "GET",
        "/api/v1/hotels/bookings/tasks/{taskId}/status",
        identifier="taskId",
    ),
)


@dataclass
class TransportResponse:
    status_code: int
    headers: dict[str, str]
    body: bytes = b""
    truncated: bool = False


Transport = Callable[..., TransportResponse]


def _safe_token(value: Any) -> str | None:
    rendered = str(value or "").strip()
    return rendered if SAFE_DIAGNOSTIC.fullmatch(rendered) else None


def _safe_probe_error(error: Exception, identifiers: dict[str, str]) -> str:
    text = redact_text(str(error))
    text = re.sub(r"\b[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}\b", "REDACTED_SESSION", text)
    for identifier in identifiers.values():
        text = text.replace(identifier, "REDACTED_IDENTIFIER")
    return text[:240]


def _response_evidence(response: TransportResponse, inspect_json: bool) -> dict[str, Any]:
    raw_content_type = str(response.headers.get("content-type") or "").split(";", 1)[0].strip().lower()
    content_type = raw_content_type if re.fullmatch(r"[a-z0-9.+-]+/[a-z0-9.+-]+", raw_content_type) else None
    evidence: dict[str, Any] = {
        "httpStatus": int(response.status_code),
        "outcome": "accepted" if 200 <= response.status_code < 300 else "auth_rejected" if response.status_code in (401, 403) else "inconclusive",
        "contentType": content_type,
        "responseInspected": bool(inspect_json),
        "responseTruncated": bool(response.truncated),
    }
    request_id = _safe_token(response.headers.get("x-request-id") or response.headers.get("x-correlation-id"))
    if request_id:
        evidence["requestId"] = request_id
    if not inspect_json or not response.body:
        evidence["bodyKind"] = "not_inspected" if not inspect_json else "empty"
        return evidence
    try:
        parsed = json.loads(response.body)
    except (UnicodeDecodeError, json.JSONDecodeError):
        evidence["bodyKind"] = "non_json"
        return evidence
    if isinstance(parsed, dict):
        evidence["bodyKind"] = "object"
        evidence["topLevelKeys"] = sorted(
            key for key in parsed.keys()
            if isinstance(key, str) and re.fullmatch(r"[A-Za-z0-9_.-]{1,64}", key)
        )[:20]
        error_object = parsed.get("error") if isinstance(parsed.get("error"), dict) else {}
        for code_value in (
            parsed.get("errorCode"),
            parsed.get("code"),
            parsed.get("resultCode"),
            parsed.get("status"),
            error_object.get("errorCode"),
            error_object.get("code"),
            error_object.get("status"),
        ):
            code = _safe_token(code_value)
            if code:
                evidence["providerCode"] = code
                break
    elif isinstance(parsed, list):
        evidence["bodyKind"] = "array"
    else:
        evidence["bodyKind"] = "scalar"
    return evidence


def _request_auth(session, route: ProbeRoute, variant: str) -> tuple[dict[str, str], dict[str, str]]:
    path = route.path_template
    headers = dict(session._mobile_headers(MOBILE_HOTELS_ORIGIN, path))
    params: dict[str, str] = {}
    if variant == NO_AUTH_CONTROL:
        return headers, params
    headers["Authorization"] = f"Bearer {session.access_token}"
    if variant in ("bearer_session", "capture_compatible"):
        params["sessionid"] = session.mobile_sessionid
    if variant == "capture_compatible":
        params.update({
            "deviceId": session.device_id,
            "oldDeviceId": session.old_device_id or session.device_id,
        })
        for key, value in (
            ("appName", session.app_name),
            ("appVersion", session.app_version),
            ("origin", session.origin),
            ("platform", session.platform),
            ("ccc", session.ccc),
            ("cpswc", session.cpswc),
            ("connectionType", session.connection_type),
            ("inache", session.inache),
        ):
            if value:
                params[key] = str(value)
        cookie = session._cookie_for(MOBILE_HOTELS_ORIGIN)
        if cookie:
            headers["Cookie"] = cookie
    return headers, params


def _requests_transport(session) -> Transport:
    def request(*, method: str, url: str, headers: dict[str, str], params: dict[str, str], body: dict[str, Any] | None, inspect_json: bool) -> TransportResponse:
        response = session._http.request(
            method,
            url,
            headers=headers,
            params=params,
            json=body,
            timeout=30,
            allow_redirects=False,
            stream=True,
        )
        try:
            lowered = {str(key).lower(): str(value) for key, value in response.headers.items()}
            payload = bytearray()
            truncated = False
            if inspect_json:
                for chunk in response.iter_content(chunk_size=8 * 1024):
                    if not chunk:
                        continue
                    remaining = MAX_INSPECTED_RESPONSE_BYTES + 1 - len(payload)
                    payload.extend(chunk[:remaining])
                    if len(payload) > MAX_INSPECTED_RESPONSE_BYTES:
                        truncated = True
                        del payload[MAX_INSPECTED_RESPONSE_BYTES:]
                        break
            return TransportResponse(response.status_code, lowered, bytes(payload), truncated)
        finally:
            response.close()

    return request


class MobileHotelsAuthProbe:
    def __init__(self, session, transport: Transport | None = None):
        self.session = session
        self.transport = transport or _requests_transport(session)

    def run(
        self,
        *,
        order_id: str | None = None,
        task_id: str | None = None,
        discover_own_identifiers: bool = False,
        variants: Iterable[str] = AUTH_VARIANTS,
    ) -> dict[str, Any]:
        if not self.session.access_token:
            raise RuntimeError("AUTH_REQUIRED: mobile access token is unavailable")
        selected_variants = tuple(variants)
        if not selected_variants or any(variant not in AUTH_VARIANTS for variant in selected_variants):
            raise ValueError(f"variants must be selected from: {', '.join(AUTH_VARIANTS)}")
        discovery = {
            "performed": False,
            "bookingListRead": False,
            "bookingDetailsRead": 0,
            "orderIdFound": bool(order_id),
            "taskIdFound": bool(task_id),
        }
        if discover_own_identifiers and (not order_id or not task_id):
            discovered_order_id, discovered_task_id, discovery = self._discover_own_identifiers()
            order_id = order_id or discovered_order_id
            task_id = task_id or discovered_task_id
            discovery["orderIdFound"] = bool(order_id)
            discovery["taskIdFound"] = bool(task_id)
        identifiers = {key: value for key, value in {"orderId": order_id, "taskId": task_id}.items() if value}
        for name, value in identifiers.items():
            if not IDENTIFIER.fullmatch(value):
                raise ValueError(f"{name} must match {IDENTIFIER.pattern}")
        results = []
        for route in ROUTES:
            if route.identifier and route.identifier not in identifiers:
                results.append({
                    "route": route.name,
                    "method": route.method,
                    "pathTemplate": route.path_template,
                    "status": "not_tested_missing_identifier",
                    "requiredIdentifier": route.identifier,
                })
                continue
            path = route.path(identifiers)
            attempts = []
            for variant in (NO_AUTH_CONTROL, *selected_variants):
                headers, params = _request_auth(self.session, route, variant)
                try:
                    response = self.transport(
                        method=route.method,
                        url=f"{MOBILE_HOTELS_ORIGIN}{path}",
                        headers=headers,
                        params=params,
                        body=route.body,
                        inspect_json=route.inspect_json,
                    )
                    attempt = {"authVariant": variant, **_response_evidence(response, route.inspect_json)}
                except Exception as error:
                    attempt = {
                        "authVariant": variant,
                        "outcome": "transport_error",
                        "error": _safe_probe_error(error, identifiers),
                    }
                attempts.append(attempt)
                if attempt.get("outcome") == "transport_error" or (
                    variant != NO_AUTH_CONTROL and attempt.get("outcome") == "accepted"
                ):
                    break
            control = attempts[0]
            accepted_auth = next((
                attempt["authVariant"]
                for attempt in attempts
                if attempt["authVariant"] != NO_AUTH_CONTROL and attempt.get("outcome") == "accepted"
            ), None)
            auth_boundary_variant = next((
                attempt["authVariant"]
                for attempt in attempts
                if attempt["authVariant"] != NO_AUTH_CONTROL
                and attempt.get("outcome") == "inconclusive"
                and isinstance(attempt.get("httpStatus"), int)
            ), None)
            if accepted_auth and control.get("outcome") == "auth_rejected":
                status = "auth_effect_confirmed"
            elif accepted_auth and control.get("outcome") == "accepted":
                status = "public_or_auth_not_required"
            elif accepted_auth:
                status = "http_accepted_auth_effect_unconfirmed"
            elif auth_boundary_variant and control.get("outcome") == "auth_rejected":
                status = "auth_boundary_passed_non_success"
            else:
                status = "not_confirmed"
            results.append({
                "route": route.name,
                "method": route.method,
                "pathTemplate": route.path_template,
                "status": status,
                "unauthenticatedControl": control.get("outcome"),
                "acceptedAuthVariant": accepted_auth,
                "authBoundaryVariant": auth_boundary_variant,
                "attempts": attempts,
            })
        response_bodies_read = discovery["performed"] or any(
            attempt.get("responseInspected") is True
            for result in results
            for attempt in result.get("attempts", [])
        )
        return {
            "probeVersion": PROBE_VERSION,
            "generatedAt": datetime.now(timezone.utc).isoformat(),
            "origin": MOBILE_HOTELS_ORIGIN,
            "readOnly": True,
            "mutationsAttempted": False,
            "credentialsExposed": False,
            "responseBodiesRead": response_bodies_read,
            "responseBodiesIncluded": False,
            "identifiersIncluded": False,
            "ownIdentifierDiscovery": discovery,
            "results": results,
        }

    def _discover_own_identifiers(self) -> tuple[str | None, str | None, dict[str, Any]]:
        bookings = self.session.hotel_bookings_list(active=True, cancelled=True, completed=True)
        order_ids = _identifier_values(bookings, "orderId")
        task_ids = _identifier_values(bookings, "taskId")
        details_read = 0
        if not task_ids:
            for order_id in order_ids[:MAX_DISCOVERY_BOOKING_DETAILS]:
                details = self.session.hotel_booking(order_id)
                details_read += 1
                task_ids.extend(value for value in _identifier_values(details, "taskId") if value not in task_ids)
                if task_ids:
                    break
        return (
            order_ids[0] if order_ids else None,
            task_ids[0] if task_ids else None,
            {
                "performed": True,
                "bookingListRead": True,
                "bookingDetailsRead": details_read,
                "orderIdFound": bool(order_ids),
                "taskIdFound": bool(task_ids),
            },
        )


def _identifier_values(value: Any, key_name: str) -> list[str]:
    result: list[str] = []

    def visit(node: Any) -> None:
        if isinstance(node, dict):
            for key, nested in node.items():
                if key == key_name:
                    candidate = str(nested).strip() if nested is not None else ""
                    if IDENTIFIER.fullmatch(candidate) and candidate not in result:
                        result.append(candidate)
                else:
                    visit(nested)
        elif isinstance(node, list):
            for nested in node:
                visit(nested)

    visit(value)
    return result


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Read-only mobile Bearer evidence probe for fixed T-Bank Hotels routes.",
    )
    parser.add_argument("--acknowledge-read-own-data", action="store_true", help="Required acknowledgement for reading the current user's own Hotels data.")
    parser.add_argument("--order-id", help="Own Hotels order ID; never included in the report.")
    parser.add_argument("--task-id", help="Own booking task UUID; never included in the report.")
    parser.add_argument(
        "--discover-own-identifiers",
        action="store_true",
        help="Read the current user's booking list and up to five booking details locally to discover own order/task identifiers; values are never included in the report.",
    )
    parser.add_argument("--variant", action="append", choices=AUTH_VARIANTS, help="Restrict auth variants; repeat for multiple values. Default: bounded auto sequence.")
    return parser


def main(argv: list[str] | None = None) -> int:
    args = _parser().parse_args(argv)
    if not args.acknowledge_read_own_data:
        raise SystemExit("Refusing live reads without --acknowledge-read-own-data")
    try:
        session = SessionManager().get()
        report = MobileHotelsAuthProbe(session).run(
            order_id=args.order_id,
            task_id=args.task_id,
            discover_own_identifiers=args.discover_own_identifiers,
            variants=args.variant or AUTH_VARIANTS,
        )
    except (RuntimeError, ValueError, OSError) as error:
        print(json.dumps({
            "ok": False,
            "error": _safe_probe_error(error, {}),
            "nextStep": "Run the local phone login CLI, then retry the read-only probe.",
        }, ensure_ascii=False), file=sys.stderr)
        return 2
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
