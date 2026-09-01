const completedGates = [
  "isolated_mobile_auth",
  "opaque_booking_binding",
  "decimal_safe_amount",
  "facts_freshness_window",
  "single_use_capability",
  "source_account_binding",
  "provider_identifiers_hidden",
  "booking_create_request_response_schema",
  "booking_task_status_schema",
  "hosted_payment_form_request_response_schema",
  "payment_task_status_schema",
  "raw_card_endpoints_excluded_from_mcp",
  "production_payment_origin_owner_provided",
  "generic_hosted_checkout_handoff",
];

const blockers = [
  ["hotel_payment_status_semantics", "Provider meaning and transition rules for the observed Hotels payment status."],
  ["non_production_payment_origin", "Approved non-production HotelsApi.Payments origin reachable from the execution environment."],
  ["hotel_payment_customer_auth", "Accepted customer access profile for booking and hosted payment-form endpoints."],
  ["trusted_client_ip_source", "Trusted integration-owned source of x-real-ip; it must not be supplied by the model."],
  ["provider_idempotency", "Provider idempotency key and duplicate-request behavior."],
  ["reconciliation", "Authoritative recovery procedure when create times out before a taskId is observed."],
  ["provider_payment_url_handoff", "Exact owner-bound channel for opening provider paymentUrl without placing it in model-visible MCP JSON; the generic hosted checkout handoff does not transfer an exact booking or rate."],
  ["shevo_payment_credential_consumer", "Official consumer and lifecycle of the opaque credential returned by HotelsApi SHEVO payment setup."],
  ["non_production_approval", "Explicit approval and credentials for a bounded non-production execution test."],
];

export function paymentReadinessReport() {
  return {
    reportVersion: "2.0",
    providerRequestsPerformed: false,
    readyForPaymentSetup: false,
    readyForPaymentExecution: false,
    intendedPublicFlow: "hosted_payment_form",
    rawCardFlowsExposedByMcp: false,
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
