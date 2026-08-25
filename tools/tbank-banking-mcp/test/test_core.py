import json
import os
import time
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import patch

from src.auth_broker import BrokerService, _safe_error_message as broker_safe_error
from src.auth_broker_client import AuthBrokerClient
from src.payment_intents import PaymentIntentStore
from src.payment_handoffs import PaymentHandoffStore
from src.server import (
    _account_ids_by_ref,
    _normalized_accounts,
    _safe_error,
    portfolio_travel_profile,
    prepare_hotel_payment_handoff_preview,
)
from src.session_store import exclusive_session_lock
from src.travel_profile import build_travel_profile
from src.upstream.client import MAX_HOTEL_VOUCHER_BYTES, MobileSession
from src.voucher_store import VoucherStore
from login_cli import logout


class _FakeSession:
    def list_accounts(self):
        return [{"id": "account-1"}]

    def spending_categories(self, account_id, start_ms, end_ms):
        return {"accountId": account_id, "period": [start_ms, end_ms]}

    def hotel_booking(self, booking_id):
        return {
            "bookingId": booking_id,
            "status": "CONFIRMED",
            "rateData": {"paymentData": {
                "paymentPrice": {"amount": 8_663.25, "currency": "RUB"},
                "paymentStatus": "CREATED",
            }},
        }

    def hotel_voucher(self, booking_id):
        fixture = Path(__file__).parent / "fixtures" / "voucher-sample.pdf"
        return fixture.read_bytes(), "application/pdf; charset=binary"

    def hotel_customer_data(self):
        return {"customer": {"firstName": "Ada"}, "isContactCreationNeeded": False}

    def hotel_bookings_list(self, *, active, cancelled, completed):
        return {"requested": [active, cancelled, completed], "activeList": []}


class _FakeSessionManager:
    path = None

    def get(self):
        return _FakeSession()


class TravelProfileTest(unittest.TestCase):
    def test_profile_uses_aggregates_without_raw_transactions(self):
        profile = build_travel_profile(
            {
                "total_spent": 360_000,
                "total_earned": 450_000,
                "currency": "RUB",
                "categories": [
                    {"category": "Отели", "amount": 36_000},
                    {"category": "Супермаркеты", "amount": 90_000},
                    {"category": "Рестораны", "amount": 45_000},
                ],
            },
            days=90,
        )
        self.assertEqual(profile["tier"], "comfort")
        self.assertAlmostEqual(profile["aggregates"]["travelSpendShare"], 0.1)
        self.assertFalse(profile["privacy"]["rawTransactionsIncluded"])
        self.assertFalse(profile["privacy"]["incomeTierClaimed"])

    def test_profile_validates_period(self):
        with self.assertRaises(ValueError):
            build_travel_profile({}, days=7)


class PaymentIntentTest(unittest.TestCase):
    def _preview(self, store):
        return store.prepare_hotel_handoff_preview(
            payment_handoff_ref="payment_handoff_0123456789abcdef01234567",
            booking_binding_verified=True,
            amount_binding_verified=True,
            payment_status_observation={"rawStatus": "CREATED", "interpretation": "not_interpreted"},
            provider_requests_performed=True,
            facts_observed_at_epoch=time.time(),
            facts_max_age_seconds=300,
            source_account_id="account-1",
            amount_decimal="8663.25",
        )

    def test_payload_hash_is_peppered_per_store(self):
        first = self._preview(PaymentIntentStore())
        second = self._preview(PaymentIntentStore())
        self.assertNotEqual(first["payloadHash"], second["payloadHash"])
        self.assertNotIn("account-1", str(first))

    @patch("src.payment_intents.time.time", return_value=1_000.0)
    def test_hotel_handoff_preview_uses_only_opaque_booking_reference(self, _time):
        store = PaymentIntentStore(ttl_seconds=300)
        result = store.prepare_hotel_handoff_preview(
            payment_handoff_ref="payment_handoff_0123456789abcdef01234567",
            booking_binding_verified=True,
            amount_binding_verified=True,
            payment_status_observation={"rawStatus": "CREATED", "interpretation": "not_interpreted"},
            provider_requests_performed=True,
            facts_observed_at_epoch=1_000.0,
            facts_max_age_seconds=300,
            source_account_id="account-1",
            amount_decimal="8663.25",
        )
        self.assertEqual(result["status"], "preview_only")
        self.assertFalse(result["executionAvailable"])
        self.assertEqual(result["preview"]["paymentHandoffRef"], "payment_handoff_0123456789abcdef01234567")
        self.assertEqual(result["preview"]["sourceAccountId"], "REDACTED")
        self.assertEqual(result["preview"]["amountDecimal"], "8663.25")
        self.assertTrue(result["handoff"]["bookingBindingVerified"])
        self.assertTrue(result["handoff"]["amountBindingVerified"])
        self.assertEqual(result["handoff"]["paymentStatusObservation"]["rawStatus"], "CREATED")
        self.assertTrue(result["providerRequestsPerformed"])
        self.assertFalse(result["providerRequestsPerformedByBankingPreview"])
        self.assertTrue(result["bookingReadPerformedByBroker"])
        self.assertFalse(result["paymentSetupPerformed"])
        self.assertFalse(result["paymentExecutionPerformed"])
        self.assertEqual(result["executionReadiness"]["status"], "contract_evidence_required")
        self.assertIn("provider_idempotency", result["executionReadiness"]["blockers"])
        self.assertEqual(result["unknownOutcomePolicy"], "do_not_retry_automatically")
        self.assertEqual(result["reconciliationStatus"], "not_configured")
        self.assertEqual(result["expiresAtEpoch"], 1_300.0)
        self.assertNotIn("orderId", str(result))
        self.assertNotIn("paymentToken", str(result))

    def test_hotel_handoff_preview_rejects_non_positive_amount(self):
        with self.assertRaisesRegex(ValueError, "positive finite decimal"):
            PaymentIntentStore().prepare_hotel_handoff_preview(
                payment_handoff_ref="payment_handoff_0123456789abcdef01234567",
                booking_binding_verified=True,
                amount_binding_verified=True,
                payment_status_observation={"rawStatus": "CREATED", "interpretation": "not_interpreted"},
                provider_requests_performed=True,
                facts_observed_at_epoch=time.time(),
                facts_max_age_seconds=300,
                source_account_id="account-1",
                amount_decimal="0",
            )

    def test_hotel_handoff_preview_rejects_unissued_reference(self):
        with self.assertRaisesRegex(ValueError, "issued by the shared auth broker"):
            PaymentIntentStore().prepare_hotel_handoff_preview(
                payment_handoff_ref="443021782873",
                booking_binding_verified=False,
                amount_binding_verified=False,
                payment_status_observation={},
                provider_requests_performed=False,
                facts_observed_at_epoch=time.time(),
                facts_max_age_seconds=300,
                source_account_id="account-1",
                amount_decimal="1",
            )

    @patch("src.payment_intents.time.time", return_value=2_000.0)
    def test_hotel_handoff_preview_rejects_stale_payment_facts(self, _time):
        with self.assertRaisesRegex(ValueError, "facts are stale"):
            PaymentIntentStore().prepare_hotel_handoff_preview(
                payment_handoff_ref="payment_handoff_0123456789abcdef01234567",
                booking_binding_verified=True,
                amount_binding_verified=True,
                payment_status_observation={"rawStatus": "CREATED", "interpretation": "not_interpreted"},
                provider_requests_performed=True,
                facts_observed_at_epoch=1_000.0,
                facts_max_age_seconds=300,
                source_account_id="account-1",
                amount_decimal="1",
            )

    def test_store_evicts_the_oldest_intent_at_its_bound(self):
        store = PaymentIntentStore(max_items=2)
        first = store.prepare_hotel_handoff_preview(
            payment_handoff_ref="payment_handoff_000000000000000000000001", booking_binding_verified=True, amount_binding_verified=True, payment_status_observation={"rawStatus": "CREATED", "interpretation": "not_interpreted"}, provider_requests_performed=True, facts_observed_at_epoch=time.time(), facts_max_age_seconds=300, source_account_id="account-1", amount_decimal="1"
        )
        store.prepare_hotel_handoff_preview(
            payment_handoff_ref="payment_handoff_000000000000000000000002", booking_binding_verified=True, amount_binding_verified=True, payment_status_observation={"rawStatus": "CREATED", "interpretation": "not_interpreted"}, provider_requests_performed=True, facts_observed_at_epoch=time.time(), facts_max_age_seconds=300, source_account_id="account-1", amount_decimal="2"
        )
        store.prepare_hotel_handoff_preview(
            payment_handoff_ref="payment_handoff_000000000000000000000003", booking_binding_verified=True, amount_binding_verified=True, payment_status_observation={"rawStatus": "CREATED", "interpretation": "not_interpreted"}, provider_requests_performed=True, facts_observed_at_epoch=time.time(), facts_max_age_seconds=300, source_account_id="account-1", amount_decimal="3"
        )
        with self.assertRaises(KeyError):
            store.describe(first["paymentIntentId"])


class PaymentHandoffStoreTest(unittest.TestCase):
    @patch("src.payment_handoffs.time.time", return_value=1_000.0)
    def test_capability_hides_booking_identifier_and_expires(self, _time):
        store = PaymentHandoffStore(ttl_seconds=300)
        created = store.create("provider-booking-1", amount=8_663.25, currency="RUB", hotel_payment_status="CREATED")
        self.assertRegex(created["paymentHandoffRef"], r"^payment_handoff_[a-f0-9]{24}$")
        self.assertTrue(created["bookingBindingVerified"])
        self.assertNotIn("provider-booking-1", str(created))
        resolved = store.resolve(created["paymentHandoffRef"])
        self.assertTrue(resolved["capabilityConsumed"])
        self.assertTrue(resolved["bookingBindingVerified"])
        self.assertTrue(resolved["amountBindingVerified"])
        self.assertEqual(resolved["amountDecimal"], "8663.25")
        self.assertEqual(resolved["expiresAtEpoch"], 1_300.0)
        self.assertNotIn("provider-booking-1", str(resolved))
        with self.assertRaisesRegex(KeyError, "already consumed"):
            store.resolve(created["paymentHandoffRef"])
        with self.assertRaisesRegex(ValueError, "positive finite"):
            store.create("provider-booking-2", amount=0, currency="RUB", hotel_payment_status="CREATED")
        with self.assertRaisesRegex(ValueError, "three-letter"):
            store.create("provider-booking-2", amount=1, currency="RUBLE", hotel_payment_status="CREATED")
        with self.assertRaisesRegex(ValueError, "unsupported characters"):
            store.create("provider-booking-2", amount=1, currency="RUB", hotel_payment_status="pay now please")

    def test_store_evicts_the_oldest_handoff_at_its_bound(self):
        store = PaymentHandoffStore(max_items=2)
        first = store.create("provider-booking-1", amount=1, currency="RUB", hotel_payment_status="CREATED")
        store.create("provider-booking-2", amount=2, currency="RUB", hotel_payment_status="CREATED")
        store.create("provider-booking-3", amount=3, currency="RUB", hotel_payment_status="CREATED")
        with self.assertRaisesRegex(KeyError, "unknown"):
            store.resolve(first["paymentHandoffRef"])

    def test_hotel_handoff_preview_rejects_interpreted_provider_status(self):
        with self.assertRaisesRegex(ValueError, "status observation is invalid"):
            PaymentIntentStore().prepare_hotel_handoff_preview(
                payment_handoff_ref="payment_handoff_0123456789abcdef01234567",
                booking_binding_verified=True,
                amount_binding_verified=True,
                payment_status_observation={"rawStatus": "CREATED", "interpretation": "payable"},
                provider_requests_performed=True,
                facts_observed_at_epoch=time.time(),
                facts_max_age_seconds=300,
                source_account_id="account-1",
                amount_decimal="1",
            )


class BankingPrivacyTest(unittest.TestCase):
    def test_account_id_and_balance_are_not_returned(self):
        accounts = _normalized_accounts([
            {"id": "raw-account-123", "name": "Основной", "balance": {"value": 999_999}},
        ])
        self.assertRegex(accounts[0]["accountRef"], r"^acct_[a-f0-9]{16}$")
        self.assertNotIn("accountId", accounts[0])
        self.assertNotIn("balance", accounts[0])
        self.assertNotIn("raw-account-123", str(accounts))

    def test_error_redacts_credentials_and_identity(self):
        rendered = _safe_error(RuntimeError(
            "Authorization=BearerSecret access_token=abcdefghijklmnopqrstuvwxyz1234567890ABCDEFG "
            "phone=+79995556677 user@example.com"
        ))
        self.assertNotIn("BearerSecret", rendered)
        self.assertNotIn("+79995556677", rendered)
        self.assertNotIn("user@example.com", rendered)

    @patch("src.server._spending_summary_by_account_id")
    @patch("src.server._raw_accounts")
    def test_portfolio_profile_hides_accounts_amounts_and_booking_history(self, raw_accounts, summaries):
        raw_accounts.return_value = [
            {"id": "account-main", "name": "Sensitive Main"},
            {"id": "account-travel", "name": "Sensitive Travel"},
        ]
        summaries.side_effect = [
            {"total_spent": 2_000_000, "total_earned": 0, "currency": "RUB", "categories": [{"category": "Переводы", "amount": 2_000_000}]},
            {"total_spent": 180_000, "total_earned": 0, "currency": "RUB", "categories": [{"category": "Отели", "amount": 54_000}]},
        ]

        result = json.loads(portfolio_travel_profile({"days": 90}))

        profile = result["portfolioTravelProfile"]
        self.assertEqual(profile["tier"], "balanced")
        self.assertEqual(profile["travelSignal"], "high")
        self.assertEqual(profile["coverage"], {
            "scope": "available_accounts",
            "complete": True,
            "method": "strongest_eligible_travel_signal",
        })
        self.assertTrue(profile["privacy"]["aggregateCategorySignalsUsedInternally"])
        self.assertFalse(profile["privacy"]["categoryBreakdownIncluded"])
        self.assertFalse(profile["privacy"]["absoluteAmountsIncluded"])
        self.assertFalse(profile["privacy"]["bookingHistoryUsed"])
        rendered = json.dumps(result)
        self.assertNotIn("account-main", rendered)
        self.assertNotIn("account-travel", rendered)
        self.assertNotIn("Sensitive", rendered)
        self.assertNotIn("2000000", rendered)
        self.assertNotIn("180000", rendered)
        self.assertNotIn("Отели", rendered)
        self.assertNotIn("accountsAnalyzed", rendered)

    @patch("src.server._broker_client")
    def test_payment_preview_rejects_unknown_account_before_consuming_handoff(self, broker_client):
        _account_ids_by_ref.clear()
        result = json.loads(prepare_hotel_payment_handoff_preview({
            "payment_handoff_ref": "payment_handoff_0123456789abcdef01234567",
            "source_account_ref": "acct_0123456789abcdef",
        }))
        self.assertIn("unknown source_account_ref", result["error"])
        broker_client.assert_not_called()

    @patch("src.server._broker_client")
    def test_consumed_invalid_handoff_returns_recovery_instruction(self, broker_client):
        _account_ids_by_ref.clear()
        _account_ids_by_ref["acct_0123456789abcdef"] = "account-1"
        self.addCleanup(_account_ids_by_ref.clear)
        broker_client.return_value.call.return_value = {
            "bookingBindingVerified": True,
            "amountBindingVerified": True,
            "paymentStatusObservation": {"rawStatus": "CREATED", "interpretation": "not_interpreted"},
            "providerRequestsPerformed": True,
            "factsObservedAtEpoch": 1.0,
            "factsMaxAgeSeconds": 1,
            "amountDecimal": "8663.25",
            "currency": "RUB",
        }
        result = json.loads(prepare_hotel_payment_handoff_preview({
            "payment_handoff_ref": "payment_handoff_0123456789abcdef01234567",
            "source_account_ref": "acct_0123456789abcdef",
        }))
        self.assertIn("facts are stale", result["error"])
        self.assertIn("Create a new payment handoff preview first", result["error"])


class AuthBrokerTest(unittest.TestCase):
    def setUp(self):
        self.temporary_directory = TemporaryDirectory()
        self.addCleanup(self.temporary_directory.cleanup)
        self.voucher_store = VoucherStore(Path(self.temporary_directory.name) / "vouchers", ttl_seconds=60)
        self.service = BrokerService(_FakeSessionManager(), self.voucher_store)

    def test_broker_mints_and_resolves_payment_handoff_without_exposing_booking_id(self):
        created = self.service.dispatch({
            "version": 2,
            "client": "hotels",
            "method": "hotels.create_payment_handoff",
            "params": {"bookingId": "provider-booking-1"},
        })
        self.assertRegex(created["paymentHandoffRef"], r"^payment_handoff_[a-f0-9]{24}$")
        self.assertTrue(created["bookingBindingVerified"])
        self.assertNotIn("provider-booking-1", str(created))

        resolved = self.service.dispatch({
            "version": 2,
            "client": "banking",
            "method": "banking.resolve_hotel_payment_handoff",
            "params": {"paymentHandoffRef": created["paymentHandoffRef"]},
        })
        self.assertTrue(resolved["bookingBindingVerified"])
        self.assertTrue(resolved["amountBindingVerified"])
        self.assertEqual(resolved["amountDecimal"], "8663.25")
        self.assertEqual(resolved["currency"], "RUB")
        self.assertEqual(resolved["paymentStatusObservation"]["rawStatus"], "CREATED")
        with self.assertRaisesRegex(KeyError, "already consumed"):
            self.service.dispatch({
                "version": 2,
                "client": "banking",
                "method": "banking.resolve_hotel_payment_handoff",
                "params": {"paymentHandoffRef": created["paymentHandoffRef"]},
            })
        self.assertNotIn("provider-booking-1", str(resolved))

    def test_exposes_only_allowlisted_high_level_operations(self):
        booking = self.service.dispatch({
            "version": 2,
            "client": "hotels",
            "method": "hotels.get_booking_v1",
            "params": {"bookingId": "booking-1"},
        })
        self.assertEqual(booking["booking"]["bookingId"], "booking-1")
        customer = self.service.dispatch({
            "version": 2,
            "client": "hotels",
            "method": "hotels.get_customer",
            "params": {},
        })
        self.assertEqual(customer["customer"]["customer"]["firstName"], "Ada")
        bookings = self.service.dispatch({
            "version": 2,
            "client": "hotels",
            "method": "hotels.list_bookings",
            "params": {
                "isActiveRequired": True,
                "isCancelledRequired": False,
                "isCompletedRequired": True,
            },
        })
        self.assertEqual(bookings["bookings"]["requested"], [True, False, True])
        voucher = self.service.dispatch({
            "version": 2,
            "client": "hotels",
            "method": "hotels.save_voucher_v1",
            "params": {"bookingId": "booking-1"},
        })
        self.assertFalse(voucher["voucher"]["documentContentIncluded"])
        self.assertEqual(voucher["voucher"]["contentType"], "application/pdf")
        self.assertTrue(Path(voucher["voucher"]["localPath"]).is_file())
        self.assertNotIn("booking-1", str(voucher))
        self.voucher_store.delete(voucher["voucher"]["voucherRef"])
        with self.assertRaisesRegex(ValueError, "not allowed"):
            self.service.dispatch({"version": 2, "client": "hotels", "method": "banking.list_accounts", "params": {}})
        with self.assertRaisesRegex(ValueError, "not allowed"):
            self.service.dispatch({"version": 2, "client": "banking", "method": "hotels.get_booking_v1", "params": {"bookingId": "booking-1"}})
        with self.assertRaisesRegex(ValueError, "not allowed"):
            self.service.dispatch({"version": 2, "client": "banking", "method": "auth.get_token", "params": {}})

    def test_requires_boolean_booking_list_filters(self):
        with self.assertRaisesRegex(ValueError, "isActiveRequired must be a boolean"):
            self.service.dispatch({
                "version": 2,
                "client": "hotels",
                "method": "hotels.list_bookings",
                "params": {
                    "isActiveRequired": "true",
                    "isCancelledRequired": False,
                    "isCompletedRequired": True,
                },
            })

    def test_validates_spending_period_before_call(self):
        with self.assertRaisesRegex(ValueError, "invalid spending period"):
            self.service.dispatch({
                "version": 2,
                "client": "banking",
                "method": "banking.spending_categories",
                "params": {"accountId": "account-1", "startMs": 10, "endMs": 10},
            })

    def test_rejects_encoded_or_path_like_booking_identifiers(self):
        for booking_id in ("order%2F1", "../order", "order?x=1"):
            with self.subTest(booking_id=booking_id), self.assertRaisesRegex(ValueError, "unsupported characters"):
                self.service.dispatch({
                    "version": 2,
                    "client": "hotels",
                    "method": "hotels.get_booking_v1",
                    "params": {"bookingId": booking_id},
                })

    def test_redacts_identity_and_tokens_from_broker_errors(self):
        rendered = broker_safe_error(RuntimeError(
            "access_token=abcdefghijklmnopqrstuvwxyz1234567890ABCDEFG "
            "+79995556677 user@example.com"
        ))
        self.assertNotIn("abcdefghijklmnopqrstuvwxyz", rendered)
        self.assertNotIn("+79995556677", rendered)
        self.assertNotIn("user@example.com", rendered)

    def test_redacts_mobile_sessionid(self):
        rendered = broker_safe_error(RuntimeError(
            "mobile_sessionid=abcdefghijklmnopqrstuvwxyz1234567890ABCDEFG "
            "abcdefghijklmnopqrstuvwx.yzABCDEFGHIJKLMNOPQRSTUV"
        ))
        self.assertNotIn("abcdefghijklmnopqrstuvwxyz", rendered)
        self.assertNotIn("abcdefghijklmnopqrstuvwx", rendered)

    def test_client_uses_operation_sized_configurable_timeout(self):
        with patch.dict(os.environ, {}, clear=True):
            self.assertEqual(AuthBrokerClient(Path("/missing.sock")).timeout_seconds, 45.0)
        with patch.dict(os.environ, {"TBANK_AUTH_BROKER_TIMEOUT_MS": "40000"}, clear=True):
            self.assertEqual(AuthBrokerClient(Path("/missing.sock")).timeout_seconds, 40.0)
        with patch.dict(os.environ, {"TBANK_AUTH_BROKER_TIMEOUT_MS": "NaN"}, clear=True):
            with self.assertRaisesRegex(ValueError, "integer number of milliseconds"):
                AuthBrokerClient(Path("/missing.sock"))


class VoucherStoreTest(unittest.TestCase):
    def test_saves_pdf_owner_only_without_returning_content(self):
        fixture = (Path(__file__).parent / "fixtures" / "voucher-sample.pdf").read_bytes()
        with TemporaryDirectory() as directory:
            store = VoucherStore(Path(directory) / "vouchers", ttl_seconds=60)
            result = store.save(fixture, "application/pdf")
            target = Path(result["localPath"])
            self.assertEqual(target.read_bytes(), fixture)
            self.assertEqual(target.stat().st_mode & 0o777, 0o600)
            self.assertEqual(target.parent.stat().st_mode & 0o777, 0o700)
            self.assertFalse(result["documentContentIncluded"])
            self.assertFalse(result["credentialsExposed"])
            self.assertNotIn("content", result)
            self.assertTrue(store.delete(result["voucherRef"]))

    def test_rejects_non_pdf_content(self):
        with TemporaryDirectory() as directory:
            store = VoucherStore(Path(directory) / "vouchers", ttl_seconds=60)
            with self.assertRaisesRegex(ValueError, "content type"):
                store.save(b"%PDF-1.4\n", "text/plain")
            with self.assertRaisesRegex(ValueError, "PDF signature"):
                store.save(b"not a pdf", "application/pdf")


class LocalSessionSafetyTest(unittest.TestCase):
    def test_lock_file_is_owner_only_and_logout_is_idempotent(self):
        with TemporaryDirectory() as directory:
            path = Path(directory) / "session.json"
            path.write_text("{}", encoding="utf-8")
            path.chmod(0o600)
            with exclusive_session_lock(path):
                lock_path = path.with_name(f".{path.name}.lock")
                self.assertEqual(lock_path.stat().st_mode & 0o777, 0o600)
            self.assertTrue(logout(path))
            self.assertFalse(path.exists())
            self.assertFalse(logout(path))


class _FakeResponse:
    status_code = 200
    text = ""

    def __init__(self, payload):
        self.payload = payload

    def json(self):
        return {"payload": self.payload}

    def raise_for_status(self):
        return None


class _RecordingHttp:
    def __init__(self):
        self.calls = []
        self.headers = {}
        self.proxies = {}
        self.verify = True

    def mount(self, _prefix, _adapter):
        return None

    def get(self, url, **kwargs):
        self.calls.append(("GET", url, kwargs))
        return _FakeResponse({"customer": {"firstName": "Ada"}})

    def post(self, url, **kwargs):
        self.calls.append(("POST", url, kwargs))
        return _FakeResponse({"activeList": [], "cancelledList": [], "completedList": []})


class _BinaryResponse:
    status_code = 200
    text = ""

    def __init__(self, chunks, content_type="application/pdf", content_length=None):
        self._chunks = chunks
        self.headers = {"content-type": content_type}
        if content_length is not None:
            self.headers["content-length"] = str(content_length)
        self.closed = False

    def raise_for_status(self):
        return None

    def iter_content(self, chunk_size):
        self.chunk_size = chunk_size
        yield from self._chunks

    def close(self):
        self.closed = True


class _VoucherHttp(_RecordingHttp):
    def __init__(self, response):
        super().__init__()
        self.response = response

    def get(self, url, **kwargs):
        self.calls.append(("GET", url, kwargs))
        return self.response


class HotelsBearerProfileTest(unittest.TestCase):
    def test_customer_and_booking_list_use_bearer_without_session_device_or_cookie(self):
        transport = _RecordingHttp()
        session = MobileSession(
            mobile_sessionid="mobile-session-secret",
            refresh_token="refresh-secret",
            access_token="access-secret",
            device_id="device-secret",
            cookie_str="cookie-secret",
            _http=transport,
        )

        customer = session.hotel_customer_data()
        bookings = session.hotel_bookings_list(active=True, cancelled=False, completed=True)

        self.assertEqual(customer["customer"]["firstName"], "Ada")
        self.assertEqual(bookings["activeList"], [])
        self.assertEqual(len(transport.calls), 2)
        for _, _, kwargs in transport.calls:
            self.assertEqual(kwargs["params"], {})
            self.assertEqual(kwargs["headers"]["Authorization"], "Bearer access-secret")
            self.assertNotIn("Cookie", kwargs["headers"])
            rendered = str(kwargs)
            self.assertNotIn("mobile-session-secret", rendered)
            self.assertNotIn("device-secret", rendered)
            self.assertNotIn("cookie-secret", rendered)
        self.assertEqual(transport.calls[1][2]["json"], {
            "isActiveRequired": True,
            "isCancelledRequired": False,
            "isCompletedRequired": True,
        })

    def test_voucher_is_streamed_with_bearer_and_bounded_before_broker_storage(self):
        response = _BinaryResponse([b"%PDF-1.4\n", b"fixture"], content_length=16)
        transport = _VoucherHttp(response)
        session = MobileSession(
            mobile_sessionid="mobile-session-secret",
            refresh_token="refresh-secret",
            access_token="access-secret",
            device_id="device-secret",
            cookie_str="cookie-secret",
            _http=transport,
        )

        content, content_type = session.hotel_voucher("booking-1")

        self.assertEqual(content, b"%PDF-1.4\nfixture")
        self.assertEqual(content_type, "application/pdf")
        self.assertTrue(response.closed)
        _, url, kwargs = transport.calls[0]
        self.assertTrue(url.endswith("/api/v1/hotels/bookings/voucher/booking-1"))
        self.assertTrue(kwargs["stream"])
        self.assertEqual(kwargs["params"], {})
        self.assertEqual(kwargs["headers"]["Authorization"], "Bearer access-secret")
        self.assertNotIn("Cookie", kwargs["headers"])

        oversized = _BinaryResponse([], content_length=MAX_HOTEL_VOUCHER_BYTES + 1)
        session._http = _VoucherHttp(oversized)
        with self.assertRaisesRegex(ValueError, "5 MiB"):
            session.hotel_voucher("booking-1")
        self.assertTrue(oversized.closed)


if __name__ == "__main__":
    unittest.main()
