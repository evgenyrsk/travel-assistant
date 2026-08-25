const completedGates = [
  "isolated_mobile_auth",
  "opaque_booking_binding",
  "decimal_safe_amount",
  "facts_freshness_window",
  "single_use_capability",
  "source_account_binding",
  "provider_identifiers_hidden",
];

const blockers = [
  ["hotel_payment_status_semantics", "Provider meaning and transition rules for the observed Hotels payment status."],
  ["hotel_payment_setup_contract", "Official Hotels payment-setup request and response contract."],
  ["hotel_payment_gateway_contract", "Official Hotels payment gateway origin, headers and flow identifiers."],
  ["provider_idempotency", "Provider idempotency key and duplicate-request behavior."],
  ["reconciliation", "Authoritative status lookup after timeout or an unknown outcome."],
  ["hotel_antifraud_profile", "Required Hotels device, IP and antifraud context."],
  ["trusted_human_confirmation", "A confirmation channel that cannot be self-approved by the model."],
  ["non_production_approval", "Explicit approval and credentials for a bounded non-production execution test."],
];

export function paymentReadinessReport() {
  return {
    reportVersion: "1.0",
    providerRequestsPerformed: false,
    readyForPaymentSetup: false,
    readyForPaymentExecution: false,
    completedGates: [...completedGates],
    blockers: blockers.map(([id, evidenceNeeded]) => ({ id, evidenceNeeded })),
    unknownOutcomePolicy: "do_not_retry_automatically",
    reconciliationStatus: "not_configured",
    forbiddenUntilReady: [
      "hotel_payment_setup",
      "hotel_payment_gateway_execution",
      "banking_v1_pay_as_hotel_payment",
      "automatic_retry_after_unknown_outcome",
    ],
  };
}
