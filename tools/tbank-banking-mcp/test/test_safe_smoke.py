import json
import os
import subprocess
import sys
import unittest
from datetime import date

from src.safe_smoke import run_safe_smoke, shared_auth_environment


class SafeSmokeTest(unittest.TestCase):
    def test_shared_auth_environment_uses_default_and_allows_override(self):
        expected_default = os.path.expanduser("~/.local/share/tbank-auth-broker/auth.sock")
        self.assertEqual(
            shared_auth_environment({}),
            {"TBANK_AUTH_BROKER_SOCKET": expected_default},
        )
        self.assertEqual(
            shared_auth_environment({"TBANK_AUTH_BROKER_SOCKET": "/private/tmp/custom.sock"}),
            {"TBANK_AUTH_BROKER_SOCKET": "/private/tmp/custom.sock"},
        )

    def test_report_contains_only_statuses_not_personal_data_or_identifiers(self):
        banking_calls = []
        hotels_calls = []

        def banking_call(name, arguments=None):
            banking_calls.append((name, arguments))
            responses = {
                "tbank_banking_connection_status": {
                    "serverVersion": "0.7.0",
                    "phoneAuth": {"sessionConfigured": True},
                },
                "tbank_banking_list_accounts": {
                    "ok": True,
                    "accounts": [{"accountRef": "acct_0123456789abcdef", "name": "Secret account"}],
                },
                "tbank_banking_spending_summary": {
                    "ok": True,
                    "summary": {"total_spent": 999_999, "categories": [{"category": "Secret", "amount": 50_000}]},
                    "rawTransactionsIncluded": False,
                },
                "tbank_banking_build_travel_profile": {
                    "ok": True,
                    "travelProfile": {"tier": "secret-tier"},
                },
                "tbank_banking_prepare_hotel_payment_handoff_preview": {
                    "status": "preview_only",
                    "executionAvailable": False,
                    "paymentIntentId": "secret-payment-intent",
                },
                "tbank_banking_payment_status": {"status": "preview_only"},
            }
            return responses[name]

        def hotels_call(name, arguments=None):
            hotels_calls.append((name, arguments))
            responses = {
                "tbank_hotels_connection_status": {
                    "serverVersion": "0.14.0",
                    "searchReady": True,
                    "customerReadiness": "mobile_read_only_ready",
                    "bookingExecution": {"available": False},
                },
                "tbank_hotels_get_customer": {"firstName": "Secret Person", "email": "secret@example.test"},
                "tbank_hotels_list_bookings": {
                    "activeList": [{"bookingRef": "booking_0123456789abcdef01234567", "hotelName": "Secret Hotel"}],
                    "cancelledList": [],
                    "completedList": [],
                },
                "tbank_hotels_get_booking": {"bookingRef": "booking_0123456789abcdef01234567", "orderId": "secret-order"},
                "tbank_hotels_create_payment_handoff_preview": {
                    "status": "preview_ready",
                    "paymentHandoffRef": "payment_handoff_0123456789abcdef01234567",
                    "bookingBindingVerified": True,
                    "amountBindingVerified": True,
                    "providerRequestsPerformed": True,
                },
                "tbank_hotels_plan_stay": {
                    "status": "ready",
                    "journeyId": "secret-journey",
                    "returnedOptions": 2,
                    "options": [{"optionId": "secret-option", "hotelName": "Secret Hotel"}],
                },
                "tbank_hotels_select_stay_option": {"journeyId": "secret-journey"},
                "tbank_hotels_get_selected_stay_rates": {
                    "status": "ready",
                    "rateOptions": [{"rateOptionId": "secret-rate", "shownPrice": 123_456}],
                },
                "tbank_hotels_select_stay_rate": {"executionAvailable": False},
                "tbank_hotels_create_booking_preview": {
                    "status": "preview_only",
                    "personalDataCollected": False,
                    "httpRequestPerformed": False,
                },
            }
            return responses[name]

        report = run_safe_smoke(banking_call, hotels_call, today=date(2026, 8, 21))
        rendered = json.dumps(report, ensure_ascii=False)

        self.assertFalse(report["externalMutationsAttempted"])
        self.assertFalse(report["personalDataIncluded"])
        self.assertFalse(report["identifiersIncluded"])
        self.assertTrue(report["banking"]["paymentPreviewOnly"])
        self.assertFalse(report["banking"]["paymentExecutionAvailable"])
        self.assertTrue(report["hotels"]["bookingPreviewOnly"])
        self.assertFalse(report["hotels"]["personalDataCollected"])
        self.assertFalse(report["hotels"]["bookingHttpRequestPerformed"])
        for secret in (
            "acct_0123456789abcdef",
            "Secret account",
            "999999",
            "secret-tier",
            "secret-payment-intent",
            "Secret Person",
            "secret@example.test",
            "booking_0123456789abcdef01234567",
            "payment_handoff_0123456789abcdef01234567",
            "secret-order",
            "Secret Hotel",
            "secret-journey",
            "secret-option",
            "secret-rate",
            "123456",
        ):
            self.assertNotIn(secret, rendered)
        self.assertTrue(any(name == "tbank_banking_payment_status" for name, _ in banking_calls))
        self.assertTrue(any(name == "tbank_hotels_get_booking" for name, _ in hotels_calls))

    def test_cli_requires_explicit_acknowledgement_before_starting_mcp_processes(self):
        completed = subprocess.run(
            [sys.executable, "-m", "src.safe_smoke"],
            text=True,
            capture_output=True,
            env={"PATH": os.environ.get("PATH", ""), "PYTHONPATH": os.getcwd()},
        )
        self.assertEqual(completed.returncode, 2)
        self.assertIn("--acknowledge-read-own-data is required", completed.stderr)


if __name__ == "__main__":
    unittest.main()
