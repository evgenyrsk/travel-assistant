from __future__ import annotations

from typing import Any


COMPLETED_PAYMENT_GATES = [
    "isolated_mobile_auth",
    "opaque_booking_binding",
    "decimal_safe_amount",
    "facts_freshness_window",
    "single_use_capability",
    "source_account_binding",
    "provider_identifiers_hidden",
]

PAYMENT_EXECUTION_BLOCKERS = [
    "hotel_payment_status_semantics",
    "hotel_payment_setup_contract",
    "hotel_payment_gateway_contract",
    "provider_idempotency",
    "reconciliation",
    "hotel_antifraud_profile",
    "trusted_human_confirmation",
    "non_production_approval",
]


def payment_execution_readiness() -> dict[str, Any]:
    return {
        "available": False,
        "status": "contract_evidence_required",
        "readinessVersion": "1.0",
        "completedGates": list(COMPLETED_PAYMENT_GATES),
        "blockers": list(PAYMENT_EXECUTION_BLOCKERS),
        "unknownOutcomePolicy": "do_not_retry_automatically",
        "reconciliationStatus": "not_configured",
        "providerRequestsPerformed": False,
    }
