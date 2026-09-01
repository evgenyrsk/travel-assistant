from __future__ import annotations

import hashlib
import hmac
import json
import re
import secrets
import time
import uuid
from decimal import Decimal, InvalidOperation
from dataclasses import dataclass
from typing import Any

from .payment_readiness import payment_execution_readiness


@dataclass
class PaymentIntent:
    intent_id: str
    created_at: float
    expires_at: float
    payload_hash: str
    preview: dict[str, Any]
    handoff: dict[str, Any] | None = None
    status: str = "preview_only"


class PaymentIntentStore:
    def __init__(self, ttl_seconds: int = 300, max_items: int = 100):
        if ttl_seconds < 1 or max_items < 1:
            raise ValueError("ttl_seconds and max_items must be positive")
        self.ttl_seconds = ttl_seconds
        self.max_items = max_items
        self._items: dict[str, PaymentIntent] = {}
        self._hash_pepper = secrets.token_bytes(32)

    def _cleanup(self, now: float) -> None:
        for intent_id, intent in list(self._items.items()):
            if now > intent.expires_at:
                self._items.pop(intent_id, None)
        while len(self._items) >= self.max_items:
            self._items.pop(next(iter(self._items)))

    def prepare_hotel_handoff_preview(
        self,
        *,
        payment_handoff_ref: str,
        booking_binding_verified: bool,
        amount_binding_verified: bool,
        payment_status_observation: dict[str, Any],
        provider_requests_performed: bool,
        facts_observed_at_epoch: float,
        facts_max_age_seconds: int,
        source_account_id: str,
        amount_decimal: str,
        currency: str = "RUB",
    ) -> dict[str, Any]:
        normalized_handoff_ref = payment_handoff_ref.strip()
        if not re.fullmatch(r"payment_handoff_[a-f0-9]{24}", normalized_handoff_ref):
            raise ValueError("paymentHandoffRef must be issued by the shared auth broker")
        if booking_binding_verified is not True:
            raise ValueError("paymentHandoffRef booking binding is not verified")
        if amount_binding_verified is not True:
            raise ValueError("paymentHandoffRef amount binding is not verified")
        if not isinstance(payment_status_observation, dict) or payment_status_observation.get("interpretation") != "not_interpreted":
            raise ValueError("paymentHandoffRef payment status observation is invalid")
        now = time.time()
        if not isinstance(facts_observed_at_epoch, (int, float)) or isinstance(facts_observed_at_epoch, bool):
            raise ValueError("paymentHandoffRef facts observation time is invalid")
        if not isinstance(facts_max_age_seconds, int) or isinstance(facts_max_age_seconds, bool) or facts_max_age_seconds < 1:
            raise ValueError("paymentHandoffRef facts freshness window is invalid")
        if float(facts_observed_at_epoch) > now + 5:
            raise ValueError("paymentHandoffRef facts observation time is invalid")
        facts_age_seconds = max(0.0, now - float(facts_observed_at_epoch))
        if facts_age_seconds > facts_max_age_seconds:
            raise ValueError("paymentHandoffRef facts are stale")
        if not source_account_id.strip():
            raise ValueError("sourceAccountId is required")
        try:
            normalized_amount = Decimal(str(amount_decimal))
        except (InvalidOperation, ValueError):
            raise ValueError("amountDecimal must be a positive finite decimal") from None
        if not normalized_amount.is_finite() or normalized_amount <= 0:
            raise ValueError("amountDecimal must be a positive finite decimal")
        canonical_amount = format(normalized_amount.normalize(), "f")
        if "." in canonical_amount:
            canonical_amount = canonical_amount.rstrip("0").rstrip(".")
        normalized_currency = currency.strip().upper()
        if not re.fullmatch(r"[A-Z]{3}", normalized_currency):
            raise ValueError("currency must be a three-letter ISO 4217 code")

        payload = {
            "paymentHandoffRef": normalized_handoff_ref,
            "sourceAccountId": source_account_id.strip(),
            "amountDecimal": canonical_amount,
            "currency": normalized_currency,
        }
        canonical = json.dumps(payload, sort_keys=True, separators=(",", ":"))
        self._cleanup(now)
        intent = PaymentIntent(
            intent_id=uuid.uuid4().hex,
            created_at=now,
            expires_at=now + self.ttl_seconds,
            payload_hash=hmac.new(self._hash_pepper, canonical.encode(), hashlib.sha256).hexdigest(),
            preview={
                "paymentHandoffRef": payload["paymentHandoffRef"],
                "sourceAccountId": "REDACTED",
                "amountDecimal": payload["amountDecimal"],
                "currency": payload["currency"],
            },
            handoff={
                "version": "1.0",
                "source": "shared_auth_broker_capability",
                "bookingBindingVerified": True,
                "amountBindingVerified": True,
                "sourceAccountBindingVerified": True,
                "paymentStatusObservation": payment_status_observation,
                "amountSource": "booking_v1.rateData.paymentData.paymentPrice",
                "providerRequestsPerformed": provider_requests_performed,
                "factsObservedAtEpoch": float(facts_observed_at_epoch),
                "factsMaxAgeSeconds": facts_max_age_seconds,
                "factsAgeSecondsAtPreparation": facts_age_seconds,
                "factsFreshAtPreparation": True,
            },
        )
        self._items[intent.intent_id] = intent
        return self.describe(intent.intent_id)

    def describe(self, intent_id: str) -> dict[str, Any]:
        intent = self._items.get(intent_id)
        if intent is None:
            raise KeyError("unknown payment intent")
        expired = time.time() > intent.expires_at
        result = {
            "paymentIntentId": intent.intent_id,
            "status": "expired" if expired else intent.status,
            "executionAvailable": False,
            "preview": intent.preview,
            "payloadHash": intent.payload_hash,
            "createdAtEpoch": intent.created_at,
            "expiresAtEpoch": intent.expires_at,
            "executionReadiness": payment_execution_readiness(),
            "unknownOutcomePolicy": "do_not_retry_automatically",
            "reconciliationStatus": "not_configured",
        }
        if intent.handoff is not None:
            result.update({
                "handoff": intent.handoff,
                "providerRequestsPerformed": intent.handoff.get("providerRequestsPerformed") is True,
                "providerRequestsPerformedByBankingPreview": False,
                "bookingReadPerformedByBroker": intent.handoff.get("providerRequestsPerformed") is True,
                "paymentSetupPerformed": False,
                "paymentExecutionPerformed": False,
            })
        return result
