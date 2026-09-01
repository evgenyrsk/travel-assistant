export const SERVER_NAME = "tbank-hotels-api-mcp";
export const SERVER_VERSION = "0.30.0";
export const MCP_PROTOCOL_VERSION = "2025-03-26";
export const DEFAULT_TIMEOUT_MS = 15_000;
export const MAX_TIMEOUT_MS = 60_000;
export const JOURNEY_TTL_MS = 60 * 60 * 1_000;
export const BOOKING_DRAFT_TTL_MS = 60 * 60 * 1_000;
export const CHECKOUT_VALIDATION_TTL_MS = 5 * 60 * 1_000;
export const CHECKOUT_INSPECTION_TTL_MS = 5 * 60 * 1_000;
export const PREPARED_CONFIRMATION_TTL_MS = 5 * 60 * 1_000;
export const AUTH_HEADER_NAME = /^[!#$%&'*+.^_`|~0-9A-Za-z-]+$/;
export const SERVICE_JWT_REFRESH_MS = 30_000;
export const LOCATION_CACHE_TTL_MS = 5 * 60 * 1_000;
export const DEFAULT_PLAN_OPTIONS = 20;
export const MAX_PLAN_OPTIONS = 50;
export const MAX_ROOMS = 8;
export const MAX_ACTIVE_JOURNEYS = 100;
export const MAX_ACTIVE_BOOKING_DRAFTS = 100;
export const MAX_ACTIVE_BOOKING_REFERENCES = 500;
export const MAX_TRACKED_MUTATION_EXECUTIONS = 500;
export const MAX_LOCATION_CACHES = 20;
export const LOCATION_PAGE_SIZE = 100;
export const MAX_LOCATION_PAGES = 50;
export const LOCATION_COLLECTION_BUDGET_MS = 10_000;
export const MAX_PROVIDER_RESPONSE_BYTES = 2 * 1_024 * 1_024;
export const MAX_SERVICE_JWT_KEY_BYTES = 64 * 1_024;
export const SEARCH_PAGE_SIZE = 50;
export const MAX_SEARCH_REQUESTS = 20;
export const MAX_SEARCH_LOADING_POLLS = 3;
export const SEARCH_LOADING_POLL_DELAY_MS = 200;
export const SEARCH_COLLECTION_BUDGET_MS = 11_000;
export const MIN_SEARCH_REQUEST_BUDGET_MS = 1_000;
export const SUBSTANTIAL_SEARCH_COVERAGE_RATIO = 0.8;
export const DEFAULT_MAX_PROVIDER_CONCURRENCY = 2;
export const MAX_PROVIDER_CONCURRENCY = 8;
export const MAX_PROVIDER_REQUEST_QUEUE = 32;
export const SEARCH_CACHE_TTL_MS = 30_000;
export const MAX_SEARCH_CACHE_ENTRIES = 50;
export const CHECKOUT_REQUEST_BUDGET_MS = 13_000;
export const CHECKOUT_FIRST_ATTEMPT_MS = 8_000;
export const RATES_REQUEST_BUDGET_MS = 13_000;
export const RATES_FIRST_ATTEMPT_MS = 5_000;
export const PAYMENT_FORM_CONTRACT_VERSION = "swagger-2026-08-26";
export const PAYMENT_FORM_EXTERNAL_BLOCKERS = [
  "payment_task_lifecycle_semantics_unverified",
  "non_production_payment_origin_unavailable",
  "payment_customer_auth_unverified",
  "trusted_client_ip_source_unverified",
  "provider_idempotency_unverified",
  "timeout_reconciliation_unverified",
  "provider_payment_url_handoff_unverified",
  "non_production_execution_not_approved",
];
