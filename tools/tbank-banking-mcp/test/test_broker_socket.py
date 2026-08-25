import json
import os
import signal
import socket
import stat
import subprocess
import sys
import tempfile
import time
import unittest
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path


class AuthBrokerSocketTest(unittest.TestCase):
    def _call(self, path: Path, client: str, method: str) -> dict:
        request = json.dumps({"version": 2, "client": client, "method": method, "params": {}}).encode() + b"\n"
        with socket.socket(socket.AF_UNIX, socket.SOCK_STREAM) as connection:
            connection.settimeout(2)
            connection.connect(str(path))
            connection.sendall(request)
            response = bytearray()
            while not response.endswith(b"\n"):
                response.extend(connection.recv(4096))
        return json.loads(response)

    def test_owner_only_socket_scopes_concurrency_and_stale_recovery(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            socket_path = root / "broker" / "auth.sock"
            socket_path.parent.mkdir()
            stale = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
            try:
                stale.bind(str(socket_path))
            except PermissionError:
                stale.close()
                self.skipTest("Unix sockets are blocked by the current sandbox")
            stale.close()

            environment = {
                "PATH": os.environ.get("PATH", ""),
                "PYTHONPATH": os.getcwd(),
                "PYTHONPYCACHEPREFIX": str(root / "pycache"),
                "TBANK_AUTH_BROKER_SOCKET": str(socket_path),
                "TBANK_BANKING_SESSION": str(root / "missing-session.json"),
            }
            process = subprocess.Popen(
                [sys.executable, "-m", "src.auth_broker"],
                stdout=subprocess.DEVNULL,
                stderr=subprocess.PIPE,
                text=True,
                env=environment,
            )
            try:
                deadline = time.monotonic() + 3
                ready = False
                while time.monotonic() < deadline:
                    if process.poll() is not None:
                        break
                    try:
                        ready = self._call(socket_path, "banking", "status").get("ok") is True
                    except OSError:
                        pass
                    if ready:
                        break
                    time.sleep(0.02)
                if process.poll() is not None:
                    stderr = process.stderr.read() if process.stderr else ""
                    if "Operation not permitted" in stderr:
                        self.skipTest("Unix sockets are blocked by the current sandbox")
                    self.fail(f"auth broker exited before startup: {stderr[:240]}")
                self.assertTrue(ready, "auth broker did not become ready")

                self.assertEqual(stat.S_IMODE(socket_path.parent.stat().st_mode), 0o700)
                self.assertEqual(stat.S_IMODE(socket_path.stat().st_mode), 0o600)
                banking = self._call(socket_path, "banking", "status")
                hotels = self._call(socket_path, "hotels", "status")
                self.assertTrue(banking["ok"])
                self.assertEqual(banking["result"]["clientScope"], "banking")
                self.assertIn("banking.resolve_hotel_payment_handoff", banking["result"]["supportedOperations"])
                self.assertTrue(hotels["ok"])
                self.assertEqual(hotels["result"]["clientScope"], "hotels")
                self.assertEqual(hotels["result"]["supportedOperations"], [
                    "hotels.create_payment_handoff",
                    "hotels.get_booking_v1",
                    "hotels.get_customer",
                    "hotels.list_bookings",
                    "hotels.save_voucher_v1",
                ])
                self.assertEqual(hotels["result"]["verifiedOperations"], [
                    "hotels.get_booking_v1",
                    "hotels.get_customer",
                    "hotels.list_bookings",
                    "hotels.save_voucher_v1",
                ])
                denied = self._call(socket_path, "hotels", "banking.list_accounts")
                self.assertFalse(denied["ok"])
                self.assertNotIn("account", denied.get("result", {}))
                with ThreadPoolExecutor(max_workers=2) as executor:
                    results = list(executor.map(
                        lambda client: self._call(socket_path, client, "status"),
                        ("banking", "hotels"),
                    ))
                self.assertTrue(all(result["ok"] for result in results))
            finally:
                if process.poll() is None:
                    process.send_signal(signal.SIGINT)
                process.wait(timeout=3)
                if process.stderr is not None:
                    process.stderr.close()
            self.assertFalse(socket_path.exists())


if __name__ == "__main__":
    unittest.main()
