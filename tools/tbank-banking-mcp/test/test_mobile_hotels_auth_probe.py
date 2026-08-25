import json
import unittest
from contextlib import redirect_stderr
from io import StringIO
from unittest.mock import patch

from src.mobile_hotels_auth_probe import (
    AUTH_VARIANTS,
    NO_AUTH_CONTROL,
    ROUTES,
    MobileHotelsAuthProbe,
    TransportResponse,
    main,
)


class _FakeSession:
    access_token = "access-secret-value"
    mobile_sessionid = "session-secret-value"
    device_id = "device-secret-value"
    old_device_id = "old-device-secret-value"
    app_name = "mobile"
    app_version = "112.0.0"
    origin = "mobile"
    platform = "ios"
    ccc = "true"
    cpswc = "true"
    connection_type = "WiFi"
    inache = "drivetransitt"

    def _mobile_headers(self, _origin, _path):
        return {"Accept": "application/json", "User-Agent": "safe-test-agent"}

    def _cookie_for(self, _origin):
        return "SSO_SESSION=cookie-secret-value"


class MobileHotelsAuthProbeTest(unittest.TestCase):
    def test_discovers_only_own_identifiers_without_including_them_in_report(self):
        class DiscoverySession(_FakeSession):
            def __init__(self):
                self.detail_calls = []

            def hotel_bookings_list(self, *, active, cancelled, completed):
                self.asserted_filters = (active, cancelled, completed)
                return {
                    "payload": {
                        "activeList": [{"orderId": "own-order-1", "hotelName": "Private Hotel"}],
                        "cancelledList": [],
                        "completedList": [],
                    }
                }

            def hotel_booking(self, booking_id):
                self.detail_calls.append(booking_id)
                return {"payload": {"taskId": "own-task-1", "email": "private@example.test"}}

        session = DiscoverySession()

        def transport(**request):
            status = 200 if "Authorization" in request["headers"] else 401
            return TransportResponse(status, {"content-type": "application/json"})

        report = MobileHotelsAuthProbe(session, transport).run(
            discover_own_identifiers=True,
            variants=("bearer_only",),
        )
        rendered = json.dumps(report)

        self.assertEqual(session.asserted_filters, (True, True, True))
        self.assertEqual(session.detail_calls, ["own-order-1"])
        self.assertTrue(report["responseBodiesRead"])
        self.assertFalse(report["responseBodiesIncluded"])
        self.assertFalse(report["identifiersIncluded"])
        self.assertEqual(report["ownIdentifierDiscovery"], {
            "performed": True,
            "bookingListRead": True,
            "bookingDetailsRead": 1,
            "orderIdFound": True,
            "taskIdFound": True,
        })
        for secret in ("own-order-1", "own-task-1", "Private Hotel", "private@example.test"):
            self.assertNotIn(secret, rendered)
        for route_name in ("booking_v1", "voucher", "evo_booking", "booking_task_status"):
            route = next(result for result in report["results"] if result["route"] == route_name)
            self.assertEqual(route["status"], "auth_effect_confirmed")

    def test_route_inventory_is_fixed_and_read_only(self):
        self.assertEqual({route.name for route in ROUTES}, {
            "customer_data", "booking_list", "booking_v1", "voucher", "evo_booking", "booking_task_status",
        })
        self.assertEqual([route.name for route in ROUTES if route.method == "POST"], ["booking_list"])
        for route in ROUTES:
            self.assertNotRegex(route.path_template, r"payment|cancel|create|update|promocode|bnpl")

    def test_escalates_auth_context_only_until_a_read_is_accepted(self):
        calls = []

        def transport(**request):
            calls.append(request)
            status = 200 if "sessionid" in request["params"] else 401
            return TransportResponse(status, {"content-type": "application/json"}, b'{"payload":{}}')

        report = MobileHotelsAuthProbe(_FakeSession(), transport).run()
        tested = [result for result in report["results"] if "attempts" in result]
        self.assertEqual([result["acceptedAuthVariant"] for result in tested], ["bearer_session", "bearer_session"])
        self.assertTrue(all(len(result["attempts"]) == 3 for result in tested))
        self.assertEqual(len(calls), 6)
        self.assertFalse(report["mutationsAttempted"])

    def test_capture_compatible_credentials_never_enter_the_report(self):
        calls = []

        def transport(**request):
            calls.append(request)
            status = 200 if "Cookie" in request["headers"] else 403
            body = b'{"payload":{"email":"person@example.test","paymentToken":"provider-secret"}}'
            return TransportResponse(status, {"content-type": "application/json", "x-request-id": "req-123"}, body)

        report = MobileHotelsAuthProbe(_FakeSession(), transport).run(
            order_id="own-order-1",
            task_id="own-task-1",
        )
        rendered = json.dumps(report)
        for secret in (
            _FakeSession.access_token,
            _FakeSession.mobile_sessionid,
            _FakeSession.device_id,
            "cookie-secret-value",
            "own-order-1",
            "own-task-1",
            "person@example.test",
            "provider-secret",
        ):
            self.assertNotIn(secret, rendered)
        self.assertTrue(all(
            result.get("acceptedAuthVariant") == "capture_compatible"
            for result in report["results"] if "attempts" in result
        ))
        capture_calls = [call for call in calls if "Cookie" in call["headers"]]
        self.assertTrue(capture_calls)
        self.assertTrue(all("sessionid" in call["params"] for call in capture_calls))
        self.assertTrue(all(
            call["inspect_json"] is ("/evo/" in call["url"])
            for call in calls
        ))
        self.assertTrue(report["responseBodiesRead"])
        self.assertFalse(report["responseBodiesIncluded"])

    def test_bearer_only_success_stops_without_session_or_cookie(self):
        calls = []

        def transport(**request):
            calls.append(request)
            return TransportResponse(200, {"content-type": "application/json"}, b"[]")

        report = MobileHotelsAuthProbe(_FakeSession(), transport).run(variants=("bearer_only",))
        self.assertEqual(len(calls), 4)
        self.assertTrue(all(call["params"] == {} for call in calls))
        self.assertTrue(all("Cookie" not in call["headers"] for call in calls))
        self.assertTrue(all(result.get("acceptedAuthVariant") in (None, "bearer_only") for result in report["results"]))
        self.assertTrue(all(
            result.get("status") in ("not_tested_missing_identifier", "public_or_auth_not_required")
            for result in report["results"]
        ))

    def test_confirms_auth_effect_only_when_control_is_rejected(self):
        def transport(**request):
            status = 200 if "Authorization" in request["headers"] else 401
            return TransportResponse(status, {"content-type": "application/json"})

        report = MobileHotelsAuthProbe(_FakeSession(), transport).run(variants=("bearer_only",))
        tested = [result for result in report["results"] if "attempts" in result]
        self.assertTrue(all(result["status"] == "auth_effect_confirmed" for result in tested))
        self.assertTrue(all(result["unauthenticatedControl"] == "auth_rejected" for result in tested))
        self.assertTrue(all(result["attempts"][0]["authVariant"] == NO_AUTH_CONTROL for result in tested))

    def test_distinguishes_auth_boundary_from_successful_endpoint_contract(self):
        def transport(**request):
            if "Authorization" not in request["headers"]:
                return TransportResponse(401, {"content-type": "text/plain"})
            return TransportResponse(
                400,
                {"content-type": "application/json"},
                b'{"error":{"code":"booking_not_supported","email":"private@example.test"}}',
            )

        report = MobileHotelsAuthProbe(_FakeSession(), transport).run(
            order_id="own-order-1",
            variants=("bearer_only",),
        )
        evo = next(result for result in report["results"] if result["route"] == "evo_booking")
        rendered = json.dumps(report)

        self.assertEqual(evo["status"], "auth_boundary_passed_non_success")
        self.assertIsNone(evo["acceptedAuthVariant"])
        self.assertEqual(evo["authBoundaryVariant"], "bearer_only")
        self.assertEqual(evo["attempts"][1]["providerCode"], "booking_not_supported")
        self.assertNotIn("private@example.test", rendered)

    def test_transport_errors_and_identifiers_are_redacted(self):
        def transport(**request):
            raise RuntimeError(f"failed {request['url']}?sessionid={_FakeSession.mobile_sessionid}")

        report = MobileHotelsAuthProbe(_FakeSession(), transport).run(order_id="private-order-1")
        rendered = json.dumps(report)
        self.assertNotIn("private-order-1", rendered)
        self.assertNotIn(_FakeSession.mobile_sessionid, rendered)
        customer = next(result for result in report["results"] if result["route"] == "customer_data")
        self.assertEqual(len(customer["attempts"]), 1)
        self.assertEqual(customer["attempts"][0]["outcome"], "transport_error")

    def test_rejects_invalid_identifiers_before_transport(self):
        calls = []
        with self.assertRaisesRegex(ValueError, "orderId must match"):
            MobileHotelsAuthProbe(_FakeSession(), lambda **request: calls.append(request)).run(order_id="../order")
        self.assertEqual(calls, [])

    def test_cli_requires_explicit_acknowledgement(self):
        with self.assertRaisesRegex(SystemExit, "acknowledge-read-own-data"):
            main([])

    def test_cli_reports_missing_login_without_traceback(self):
        stderr = StringIO()
        with patch("src.mobile_hotels_auth_probe.SessionManager") as manager:
            manager.return_value.get.side_effect = RuntimeError("AUTH_REQUIRED: run the local phone login CLI first")
            with redirect_stderr(stderr):
                result = main(["--acknowledge-read-own-data"])
        self.assertEqual(result, 2)
        error = json.loads(stderr.getvalue())
        self.assertEqual(error["ok"], False)
        self.assertIn("AUTH_REQUIRED", error["error"])
        self.assertNotIn("Traceback", stderr.getvalue())

    def test_auth_variant_inventory_is_bounded(self):
        self.assertEqual(AUTH_VARIANTS, ("bearer_only", "bearer_session", "capture_compatible"))


if __name__ == "__main__":
    unittest.main()
