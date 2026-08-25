from __future__ import annotations

import re
import secrets
import time
import threading
from decimal import Decimal, InvalidOperation
from dataclasses import dataclass


@dataclass
class PaymentHandoff:
    booking_id: str
    amount_decimal: str
    currency: str
    hotel_payment_status: str
    created_at: float
    expires_at: float


class PaymentHandoffStore:
    """Process-local capability store shared by Hotels and Banking MCP via the broker."""

    def __init__(self, ttl_seconds: int = 300, max_items: int = 100):
        if ttl_seconds < 1 or max_items < 1:
            raise ValueError("ttl_seconds and max_items must be positive")
        self.ttl_seconds = ttl_seconds
        self.max_items = max_items
        self._items: dict[str, PaymentHandoff] = {}
        self._lock = threading.RLock()

    def _cleanup(self, now: float) -> None:
        for handoff_ref, handoff in list(self._items.items()):
            if now > handoff.expires_at:
                self._items.pop(handoff_ref, None)
        while len(self._items) >= self.max_items:
            self._items.pop(next(iter(self._items)))

    @staticmethod
    def _canonical_amount(value: object) -> str:
        if isinstance(value, bool) or not isinstance(value, (int, float, str, Decimal)):
            raise ValueError("provider paymentPrice.amount must be a positive finite decimal")
        try:
            amount = Decimal(str(value))
        except (InvalidOperation, ValueError):
            raise ValueError("provider paymentPrice.amount must be a positive finite decimal") from None
        if not amount.is_finite() or amount <= 0:
            raise ValueError("provider paymentPrice.amount must be a positive finite decimal")
        rendered = format(amount.normalize(), "f")
        if "." in rendered:
            rendered = rendered.rstrip("0").rstrip(".")
        if len(rendered.replace(".", "").lstrip("-")) > 30:
            raise ValueError("provider paymentPrice.amount exceeds the local precision limit")
        return rendered

    def create(self, booking_id: str, *, amount: object, currency: str, hotel_payment_status: str) -> dict[str, object]:
        if not re.fullmatch(r"[A-Za-z0-9_-]{1,128}", booking_id):
            raise ValueError("bookingId contains unsupported characters")
        amount_decimal = self._canonical_amount(amount)
        normalized_currency = currency.strip().upper() if isinstance(currency, str) else ""
        if not re.fullmatch(r"[A-Z]{3}", normalized_currency):
            raise ValueError("provider paymentPrice.currency must be a three-letter code")
        normalized_status = hotel_payment_status.strip() if isinstance(hotel_payment_status, str) else ""
        if not re.fullmatch(r"[A-Za-z0-9_.:-]{1,128}", normalized_status):
            raise ValueError("provider paymentStatus contains unsupported characters")
        now = time.time()
        with self._lock:
            self._cleanup(now)
            handoff_ref = f"payment_handoff_{secrets.token_hex(12)}"
            self._items[handoff_ref] = PaymentHandoff(
                booking_id=booking_id,
                amount_decimal=amount_decimal,
                currency=normalized_currency,
                hotel_payment_status=normalized_status,
                created_at=now,
                expires_at=now + self.ttl_seconds,
            )
        return {
            "paymentHandoffRef": handoff_ref,
            "singleUse": True,
            "bookingBindingVerified": True,
            "amountBindingVerified": True,
            "amountDecimal": amount_decimal,
            "currency": normalized_currency,
            "paymentStatusObservation": {
                "rawStatus": normalized_status,
                "interpretation": "not_interpreted",
                "source": "booking_v1.rateData.paymentData.paymentStatus",
            },
            "amountSource": "booking_v1.rateData.paymentData.paymentPrice",
            "createdAtEpoch": now,
            "factsObservedAtEpoch": now,
            "factsMaxAgeSeconds": self.ttl_seconds,
            "expiresAtEpoch": now + self.ttl_seconds,
            "providerRequestsPerformed": True,
        }

    def resolve(self, handoff_ref: str) -> dict[str, object]:
        if not re.fullmatch(r"payment_handoff_[a-f0-9]{24}", handoff_ref):
            raise ValueError("paymentHandoffRef has an invalid format")
        now = time.time()
        with self._lock:
            self._cleanup(now)
            handoff = self._items.pop(handoff_ref, None)
            if handoff is None:
                raise KeyError("unknown, expired, or already consumed paymentHandoffRef")
        return {
            "paymentHandoffRef": handoff_ref,
            "singleUse": True,
            "capabilityConsumed": True,
            "bookingBindingVerified": True,
            "amountBindingVerified": True,
            "amountDecimal": handoff.amount_decimal,
            "currency": handoff.currency,
            "paymentStatusObservation": {
                "rawStatus": handoff.hotel_payment_status,
                "interpretation": "not_interpreted",
                "source": "booking_v1.rateData.paymentData.paymentStatus",
            },
            "amountSource": "booking_v1.rateData.paymentData.paymentPrice",
            "createdAtEpoch": handoff.created_at,
            "factsObservedAtEpoch": handoff.created_at,
            "factsMaxAgeSeconds": self.ttl_seconds,
            "expiresAtEpoch": handoff.expires_at,
            "providerRequestsPerformed": True,
        }
