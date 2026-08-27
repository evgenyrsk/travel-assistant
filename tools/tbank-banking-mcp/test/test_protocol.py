import json
import os
import subprocess
import sys
import tomllib
import unittest
from pathlib import Path

from src import server


class ProtocolTest(unittest.TestCase):
    def _run(self, requests):
        completed = subprocess.run(
            [sys.executable, "-m", "src.server"],
            input="".join((request if isinstance(request, str) else json.dumps(request)) + "\n" for request in requests),
            text=True,
            capture_output=True,
            check=True,
            env={
                "PATH": os.environ.get("PATH", ""),
                "PYTHONPATH": os.getcwd(),
                "TBANK_BANKING_SESSION": os.path.join(os.getcwd(), "missing-session.json"),
            },
        )
        return [json.loads(line) for line in completed.stdout.splitlines()]

    def test_lists_curated_tools_and_reports_safe_status(self):
        responses = self._run([
            {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {}},
            {"jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}},
            {"jsonrpc": "2.0", "id": 3, "method": "tools/call",
             "params": {"name": "tbank_banking_connection_status", "arguments": {}}},
        ])
        self.assertEqual(responses[0]["result"]["serverInfo"]["version"], server.SERVER_VERSION)
        names = {tool["name"] for tool in responses[1]["result"]["tools"]}
        self.assertEqual(names, {
            "tbank_banking_connection_status", "tbank_banking_list_accounts",
            "tbank_banking_spending_summary", "tbank_banking_build_travel_profile",
            "tbank_banking_build_portfolio_travel_profile",
            "tbank_banking_prepare_hotel_payment_handoff_preview",
            "tbank_banking_payment_status",
        })
        payment_tool = next(tool for tool in responses[1]["result"]["tools"] if tool["name"] == "tbank_banking_prepare_hotel_payment_handoff_preview")
        profile_tool = next(tool for tool in responses[1]["result"]["tools"] if tool["name"] == "tbank_banking_build_portfolio_travel_profile")
        self.assertIn("hotelPreferences", profile_tool["description"])
        self.assertIn("tbank_hotels_plan_personalized_stay", profile_tool["description"])
        self.assertIn("travelSpendSignal", profile_tool["description"])
        self.assertIn("preferencesApplied.applied=true", profile_tool["description"])
        self.assertEqual(set(payment_tool["inputSchema"]["properties"]), {"payment_handoff_ref", "source_account_ref"})
        self.assertEqual(set(payment_tool["inputSchema"]["required"]), {"payment_handoff_ref", "source_account_ref"})
        status = responses[2]["result"]["content"][0]["text"]
        self.assertIn('"credentialsExposedToModel":false', status)
        self.assertIn('"available":false', status)
        self.assertIn('"status":"contract_evidence_required"', status)
        self.assertIn('"unknownOutcomePolicy":"do_not_retry_automatically"', status)
        self.assertIn('"reconciliationStatus":"not_configured"', status)

    def test_runtime_version_matches_package_metadata(self):
        project = tomllib.loads((Path(__file__).resolve().parents[1] / "pyproject.toml").read_text())
        self.assertEqual(project["project"]["version"], server.SERVER_VERSION)

    def test_hotel_payment_handoff_preview_requires_broker_issued_reference(self):
        responses = self._run([
            {"jsonrpc": "2.0", "id": 1, "method": "tools/call", "params": {
                "name": "tbank_banking_prepare_hotel_payment_handoff_preview",
                "arguments": {
                    "payment_handoff_ref": "443021782873",
                    "source_account_ref": "acct_0123456789abcdef",
                },
            }},
            {"jsonrpc": "2.0", "id": 2, "method": "tools/call", "params": {
                "name": "tbank_banking_prepare_hotel_payment_handoff_preview",
                "arguments": {
                    "payment_handoff_ref": "payment_handoff_0123456789abcdef01234567",
                    "source_account_ref": "acct_0123456789abcdef",
                },
            }},
        ])
        rejected = responses[0]["result"]["content"][0]["text"]
        broker_required = responses[1]["result"]["content"][0]["text"]
        self.assertIn("issued by the shared auth broker", rejected)
        self.assertIn("unknown source_account_ref", broker_required)
        self.assertNotIn("orderId", broker_required)
        self.assertNotIn("paymentToken", broker_required)

    def test_ping_and_parse_error(self):
        responses = self._run([{"jsonrpc": "2.0", "id": 1, "method": "ping"}, "not-json"])
        self.assertEqual(responses[0]["result"], {})
        self.assertEqual(responses[1]["error"]["code"], -32700)

    def test_unexpected_handler_error_is_redacted(self):
        tool = server.TOOL_BY_NAME["tbank_banking_connection_status"]
        original_handler = tool["handler"]

        def unsafe_handler(_):
            raise RuntimeError("Authorization: Bearer secret-token-value-that-must-not-leak")

        tool["handler"] = unsafe_handler
        try:
            response = server.handle({
                "jsonrpc": "2.0",
                "id": 1,
                "method": "tools/call",
                "params": {"name": "tbank_banking_connection_status", "arguments": {}},
            })
        finally:
            tool["handler"] = original_handler

        text = response["result"]["content"][0]["text"]
        self.assertTrue(response["result"]["isError"])
        self.assertIn("REDACTED", text)
        self.assertNotIn("secret-token-value", text)

        original_metadata = server._session_metadata
        server._session_metadata = lambda: (_ for _ in ()).throw(
            RuntimeError("access_token=another-secret-token-value-that-must-not-leak")
        )
        try:
            status = json.loads(server.connection_status({}))
        finally:
            server._session_metadata = original_metadata
        self.assertFalse(status["ok"])
        self.assertIn("REDACTED", status["error"])
        self.assertNotIn("another-secret-token-value", status["error"])


if __name__ == "__main__":
    unittest.main()
