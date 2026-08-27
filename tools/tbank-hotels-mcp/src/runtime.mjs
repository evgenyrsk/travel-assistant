#!/usr/bin/env node

import { createHash, createPrivateKey, randomUUID, sign } from "node:crypto";
import { readFileSync, statSync } from "node:fs";
import { createConnection } from "node:net";
import { isAbsolute } from "node:path";

import { SERVER_NAME, SERVER_VERSION, MCP_PROTOCOL_VERSION, DEFAULT_TIMEOUT_MS, MAX_TIMEOUT_MS, JOURNEY_TTL_MS, BOOKING_DRAFT_TTL_MS, CHECKOUT_VALIDATION_TTL_MS, PREPARED_CONFIRMATION_TTL_MS, AUTH_HEADER_NAME, SERVICE_JWT_REFRESH_MS, LOCATION_CACHE_TTL_MS, DEFAULT_PLAN_OPTIONS, MAX_PLAN_OPTIONS, MAX_ROOMS, MAX_ACTIVE_JOURNEYS, MAX_ACTIVE_BOOKING_DRAFTS, MAX_ACTIVE_BOOKING_REFERENCES, MAX_TRACKED_MUTATION_EXECUTIONS, MAX_LOCATION_CACHES, LOCATION_PAGE_SIZE, MAX_LOCATION_PAGES, LOCATION_COLLECTION_BUDGET_MS, MAX_PROVIDER_RESPONSE_BYTES, MAX_SERVICE_JWT_KEY_BYTES, SEARCH_PAGE_SIZE, MAX_SEARCH_REQUESTS, MAX_SEARCH_LOADING_POLLS, SEARCH_LOADING_POLL_DELAY_MS, SEARCH_COLLECTION_BUDGET_MS, MIN_SEARCH_REQUEST_BUDGET_MS, DEFAULT_MAX_PROVIDER_CONCURRENCY, MAX_PROVIDER_CONCURRENCY, MAX_PROVIDER_REQUEST_QUEUE, SEARCH_CACHE_TTL_MS, MAX_SEARCH_CACHE_ENTRIES, CHECKOUT_REQUEST_BUDGET_MS, CHECKOUT_FIRST_ATTEMPT_MS, RATES_REQUEST_BUDGET_MS, RATES_FIRST_ATTEMPT_MS, PAYMENT_FORM_CONTRACT_VERSION, PAYMENT_FORM_EXTERNAL_BLOCKERS } from "./config.mjs";
import { hostedCheckoutTarget } from "./checkout-handoff.mjs";
import { SEARCH_FILTER_IDS, tools } from "./tool-contracts.mjs";
const journeysById = new Map();
const bookingDraftsById = new Map();
const bookingReferencesById = new Map();
const mutationExecutionsByHash = new Map();
const hotelSearchCacheByKey = new Map();
const inFlightHotelSearchByKey = new Map();
let cachedServiceJwt;
const locationCatalogByCountry = new Map();
let authBrokerConnector = createConnection;
let activeProviderRequests = 0;
const providerRequestQueue = [];
let searchCacheTransport = globalThis.fetch;
let locationCatalogTransport = globalThis.fetch;

export function setAuthBrokerConnectorForTests(connector) {
  authBrokerConnector = connector ?? createConnection;
}

const text = (value) => ({ type: "text", text: typeof value === "string" ? value : JSON.stringify(value, null, 2) });

function configuredHeaders() {
  const rawHeaders = process.env.TBANK_HOTELS_AUTH_HEADERS_JSON;
  const token = process.env.TBANK_HOTELS_AUTH_TOKEN;
  const header = process.env.TBANK_HOTELS_AUTH_HEADER;
  const serviceJwtKey = serviceJwtConfigured();
  if (rawHeaders && (token || header || serviceJwtKey)) throw new Error("Configure exactly one auth profile: TBANK_HOTELS_AUTH_HEADERS_JSON, TBANK_HOTELS_AUTH_TOKEN, or TBANK_HOTELS_JWT_PRIVATE_KEY.");
  if (serviceJwtKey && (token || header)) throw new Error("Configure either TBANK_HOTELS_AUTH_TOKEN or TBANK_HOTELS_JWT_PRIVATE_KEY, not both.");
  if (rawHeaders) {
    let headers;
    try { headers = JSON.parse(rawHeaders); } catch { throw new Error("TBANK_HOTELS_AUTH_HEADERS_JSON must contain a JSON object."); }
    if (!headers || Array.isArray(headers) || typeof headers !== "object") throw new Error("TBANK_HOTELS_AUTH_HEADERS_JSON must contain a JSON object.");
    return validateHeaders(headers);
  }
  if (serviceJwtKey) {
    const resolvedHeader = process.env.TBANK_HOTELS_JWT_AUTH_HEADER ?? "Authorization";
    if (!AUTH_HEADER_NAME.test(resolvedHeader)) throw new Error("TBANK_HOTELS_JWT_AUTH_HEADER contains an invalid header name.");
    // HotelsApiPrivate's Go client concatenates the prefix and JWT directly.
    const prefix = process.env.TBANK_HOTELS_JWT_AUTH_PREFIX ?? "Bearer";
    return { [resolvedHeader]: `${prefix}${serviceJwtSignature()}` };
  }
  if (!token && !header) return {};
  if (!token) throw new Error("TBANK_HOTELS_AUTH_HEADER requires TBANK_HOTELS_AUTH_TOKEN.");
  const resolvedHeader = header ?? "Authorization";
  if (!AUTH_HEADER_NAME.test(resolvedHeader)) throw new Error("TBANK_HOTELS_AUTH_HEADER contains an invalid header name.");
  const prefix = process.env.TBANK_HOTELS_AUTH_PREFIX ?? "Bearer ";
  return { [resolvedHeader]: `${prefix}${token}` };
}

function serviceJwtSignature() {
  const now = Date.now();
  const issuer = requiredAuthSetting("TBANK_HOTELS_JWT_ISSUER");
  const audience = requiredAuthSetting("TBANK_HOTELS_JWT_AUDIENCE");
  const audiences = audience.split(",").map((value) => value.trim()).filter(Boolean);
  if (!audiences.length) throw new Error("TBANK_HOTELS_JWT_AUDIENCE must contain at least one audience.");
  const pem = normalizedServiceJwtPrivateKey(serviceJwtPrivateKeyMaterial());
  const fingerprint = createHash("sha256").update(`${issuer}\u0000${audiences.join(",")}\u0000${pem}`).digest("hex");
  if (cachedServiceJwt && cachedServiceJwt.fingerprint === fingerprint && cachedServiceJwt.expiresAt > now) return cachedServiceJwt.value;
  const header = base64UrlJson({ alg: "RS384", typ: "JWT" });
  const claims = base64UrlJson({
    iss: issuer,
    aud: audiences,
    iat: Math.floor(now / 1_000),
  });
  const signingInput = `${header}.${claims}`;
  let signature;
  try {
    signature = sign("RSA-SHA384", Buffer.from(signingInput), createPrivateKey(pem)).toString("base64url");
  } catch {
    throw new Error("Unable to create Hotels service JWT from the configured private key.");
  }
  const value = `${signingInput}.${signature}`;
  cachedServiceJwt = { value, fingerprint, expiresAt: now + SERVICE_JWT_REFRESH_MS };
  return value;
}

function requiredAuthSetting(name) {
  const value = process.env[name];
  if (!value || !value.trim()) throw new Error(`${name} is required when service JWT authentication is configured.`);
  return value.trim();
}

function serviceJwtConfigured() {
  return Boolean(process.env.TBANK_HOTELS_JWT_PRIVATE_KEY || process.env.TBANK_HOTELS_JWT_PRIVATE_KEY_FILE);
}

function serviceJwtPrivateKeyMaterial() {
  const inlineKey = process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  const keyFile = process.env.TBANK_HOTELS_JWT_PRIVATE_KEY_FILE;
  if (inlineKey && keyFile) throw new Error("Configure either TBANK_HOTELS_JWT_PRIVATE_KEY or TBANK_HOTELS_JWT_PRIVATE_KEY_FILE, not both.");
  if (inlineKey) return inlineKey;
  if (!keyFile || !keyFile.trim()) throw new Error("A service JWT private key is required.");
  const resolvedPath = keyFile.trim();
  if (!isAbsolute(resolvedPath)) throw new Error("TBANK_HOTELS_JWT_PRIVATE_KEY_FILE must be an absolute path.");
  let metadata;
  try { metadata = statSync(resolvedPath); } catch { throw new Error("Unable to read the configured service JWT private key file."); }
  if (!metadata.isFile() || metadata.size < 1 || metadata.size > MAX_SERVICE_JWT_KEY_BYTES) throw new Error("The configured service JWT private key file is invalid or too large.");
  if (process.platform !== "win32" && (metadata.mode & 0o077) !== 0) {
    throw new Error("The configured service JWT private key file must be owner-only (mode 0600 or stricter).");
  }
  try { return readFileSync(resolvedPath, "utf8"); } catch { throw new Error("Unable to read the configured service JWT private key file."); }
}

function normalizedServiceJwtPrivateKey(material) {
  const key = String(material ?? "").replace(/\\n/g, "\n").trim();
  if (!key) throw new Error("The configured service JWT private key is empty.");
  if (key.includes("-----BEGIN")) return key;
  return `-----BEGIN RSA PRIVATE KEY-----\n${key}\n-----END RSA PRIVATE KEY-----`;
}

function base64UrlJson(value) {
  return Buffer.from(JSON.stringify(value)).toString("base64url");
}

function validateHeaders(headers) {
  const result = {};
  for (const [name, value] of Object.entries(headers)) {
    if (!AUTH_HEADER_NAME.test(name) || typeof value !== "string" || !value) throw new Error("Auth headers must have valid names and non-empty string values.");
    result[name] = value;
  }
  return result;
}

function baseUrl() {
  const configured = process.env.TBANK_HOTELS_API_BASE_URL;
  if (!configured) throw new Error("TBANK_HOTELS_API_BASE_URL is required. The supplied contracts do not declare an absolute server URL.");
  let url;
  try { url = new URL(configured); } catch { throw new Error("TBANK_HOTELS_API_BASE_URL must be an absolute URL."); }
  const localHttp = url.protocol === "http:" && ["localhost", "127.0.0.1", "::1"].includes(url.hostname);
  if (url.protocol !== "https:" && !localHttp) throw new Error("TBANK_HOTELS_API_BASE_URL must use HTTPS outside localhost.");
  return url;
}

function timeoutMs() {
  const configured = process.env.TBANK_HOTELS_TIMEOUT_MS;
  if (!configured) return DEFAULT_TIMEOUT_MS;
  const value = Number(configured);
  if (!Number.isInteger(value) || value < 1_000 || value > MAX_TIMEOUT_MS) throw new Error(`TBANK_HOTELS_TIMEOUT_MS must be an integer from 1000 to ${MAX_TIMEOUT_MS}.`);
  return value;
}

function maxProviderConcurrency() {
  const configured = process.env.TBANK_HOTELS_MAX_CONCURRENT_REQUESTS;
  if (!configured) return DEFAULT_MAX_PROVIDER_CONCURRENCY;
  const value = Number(configured);
  if (!Number.isInteger(value) || value < 1 || value > MAX_PROVIDER_CONCURRENCY) {
    throw new Error(`TBANK_HOTELS_MAX_CONCURRENT_REQUESTS must be an integer from 1 to ${MAX_PROVIDER_CONCURRENCY}.`);
  }
  return value;
}

async function withProviderRequestSlot(operation) {
  const concurrency = maxProviderConcurrency();
  if (activeProviderRequests >= concurrency) {
    if (providerRequestQueue.length >= MAX_PROVIDER_REQUEST_QUEUE) {
      const error = new Error("Hotels provider request queue is full. Retry later instead of starting parallel tool calls.");
      error.code = "HOTELS_API_LOCAL_OVERLOAD";
      throw error;
    }
    await new Promise((resolve) => providerRequestQueue.push(resolve));
  }
  activeProviderRequests += 1;
  try {
    return await operation();
  } finally {
    activeProviderRequests -= 1;
    providerRequestQueue.shift()?.();
  }
}

function configuredAuthMode() {
  if (serviceJwtConfigured()) return "service_jwt";
  if (process.env.TBANK_HOTELS_AUTH_HEADERS_JSON) return "static_headers";
  if (process.env.TBANK_HOTELS_AUTH_TOKEN) return "static_token";
  return "anonymous";
}

function authBrokerSocket() {
  const configured = process.env.TBANK_AUTH_BROKER_SOCKET;
  return configured && configured.trim() ? configured.trim() : null;
}

function authBrokerTimeoutMs() {
  const configured = process.env.TBANK_AUTH_BROKER_TIMEOUT_MS;
  if (!configured) return 45_000;
  const parsed = Number(configured);
  if (!Number.isInteger(parsed) || parsed < 1_000 || parsed > 120_000) {
    throw new Error("TBANK_AUTH_BROKER_TIMEOUT_MS must be an integer from 1000 to 120000.");
  }
  return parsed;
}

function authBrokerRequest(method, params = {}, requestTimeoutMs = authBrokerTimeoutMs()) {
  const socketPath = authBrokerSocket();
  if (!socketPath) throw new Error("T-Bank auth broker is not configured.");
  return new Promise((resolve, reject) => {
    const client = authBrokerConnector(socketPath);
    let response = "";
    let settled = false;
    const finish = (callback, value) => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      client.destroy();
      callback(value);
    };
    const timer = setTimeout(() => finish(reject, new Error("T-Bank auth broker timed out.")), requestTimeoutMs);
    client.setEncoding("utf8");
    client.on("connect", () => client.write(`${JSON.stringify({ version: 2, client: "hotels", method, params })}\n`));
    client.on("data", (chunk) => {
      response += chunk;
      if (Buffer.byteLength(response) > MAX_PROVIDER_RESPONSE_BYTES) {
        finish(reject, new Error("T-Bank auth broker response exceeded the safe size limit."));
        return;
      }
      const newline = response.indexOf("\n");
      if (newline < 0) return;
      try {
        const parsed = JSON.parse(response.slice(0, newline));
        if (!parsed || parsed.ok !== true || !parsed.result || typeof parsed.result !== "object") {
          throw new Error(typeof parsed?.error === "string" ? parsed.error : "T-Bank auth broker request failed.");
        }
        finish(resolve, parsed.result);
      } catch (error) {
        finish(reject, new Error(String(error.message || "T-Bank auth broker returned an invalid response.").slice(0, 240)));
      }
    });
    client.on("error", () => finish(reject, new Error("T-Bank auth broker is unavailable.")));
    client.on("end", () => {
      if (!settled) finish(reject, new Error("T-Bank auth broker closed the connection without a response."));
    });
  });
}

function mutationRequiredHeaders(action) {
  return action === "booking" || action === "lsBooking" ? ["x-real-ip"] : [];
}

function mutationExecutionReadiness(action) {
  if (!mutationsEnabled()) {
    return { available: false, status: "not_activated", missingRequiredHeaders: mutationRequiredHeaders(action) };
  }
  if (process.env.TBANK_HOTELS_MUTATION_EXECUTION_PROFILE !== "non_production_v1_reviewed") {
    return { available: false, status: "contract_review_required", missingRequiredHeaders: mutationRequiredHeaders(action) };
  }
  try {
    baseUrl();
    const headers = configuredHeaders();
    if (configuredAuthMode() === "anonymous") {
      return { available: false, status: "authentication_not_configured", missingRequiredHeaders: mutationRequiredHeaders(action) };
    }
    const configuredNames = new Set(Object.keys(headers).map((name) => name.toLowerCase()));
    const missingRequiredHeaders = mutationRequiredHeaders(action).filter((name) => !configuredNames.has(name));
    if (missingRequiredHeaders.length) {
      return { available: false, status: "required_trusted_headers_not_configured", missingRequiredHeaders };
    }
    return { available: true, status: "configured_unverified", missingRequiredHeaders: [] };
  } catch {
    return { available: false, status: "invalid_configuration", missingRequiredHeaders: mutationRequiredHeaders(action) };
  }
}

function journeyExecutionReadiness(readiness) {
  return {
    available: readiness.available,
    status: readiness.available ? "available" : "not_available",
  };
}

function paymentFormExecutionReadiness() {
  return {
    available: false,
    status: "external_contract_evidence_required",
    flow: "hosted_payment_form",
    contractVersion: PAYMENT_FORM_CONTRACT_VERSION,
    requestContractVerifiedOffline: true,
    responseContractVerifiedOffline: true,
    taskStateContractVerifiedOffline: true,
    rawCardDataAcceptedByMcp: false,
    externalBlockers: [...PAYMENT_FORM_EXTERNAL_BLOCKERS],
  };
}

async function connectionStatus() {
  const hasBaseUrl = Boolean(process.env.TBANK_HOTELS_API_BASE_URL);
  const authMode = configuredAuthMode();
  let transportError = null;
  let authenticationError = null;
  if (hasBaseUrl) {
    try { baseUrl(); } catch (error) { transportError = error.message; }
  }
  if (authMode !== "anonymous") {
    try { configuredHeaders(); } catch (error) { authenticationError = error.message; }
  }
  const transport = !hasBaseUrl ? "not_configured" : transportError ? "invalid_configuration" : "configured";
  const authentication = authMode === "anonymous" ? "not_required" : authenticationError ? "invalid_configuration" : "configured";
  let configuredProviderConcurrency = null;
  let loadProtectionError = null;
  try { configuredProviderConcurrency = maxProviderConcurrency(); }
  catch (error) { loadProtectionError = error.message; }
  const searchReady = transport === "configured" && authentication !== "invalid_configuration" && !loadProtectionError;
  const sharedMobileAuth = Boolean(authBrokerSocket());
  let brokerProbe = null;
  let brokerError = null;
  if (sharedMobileAuth) {
    try { brokerProbe = await authBrokerRequest("status", {}, 1_500); }
    catch (error) { brokerError = error.message; }
  }
  const brokerReachable = Boolean(brokerProbe);
  const brokerSessionConfigured = brokerProbe?.sessionConfigured === true;
  const brokerVerifiedOperations = new Set(Array.isArray(brokerProbe?.verifiedOperations) ? brokerProbe.verifiedOperations : []);
  const brokerSupportedOperations = new Set(Array.isArray(brokerProbe?.supportedOperations) ? brokerProbe.supportedOperations : []);
  const brokerCanReadCustomer = brokerReachable && brokerSessionConfigured && brokerVerifiedOperations.has("hotels.get_customer");
  const brokerCanListBookings = brokerReachable && brokerSessionConfigured && brokerVerifiedOperations.has("hotels.list_bookings");
  const brokerCanReadBookingV1 = brokerReachable && brokerSessionConfigured && brokerVerifiedOperations.has("hotels.get_booking_v1");
  const brokerCanSaveVoucherV1 = brokerReachable && brokerSessionConfigured && brokerVerifiedOperations.has("hotels.save_voucher_v1");
  const brokerCanCreatePaymentHandoff = brokerReachable && brokerSessionConfigured && brokerSupportedOperations.has("hotels.create_payment_handoff");
  const directCustomerAuth = authMode === "static_headers" || authMode === "static_token";
  const customerReadiness = sharedMobileAuth
    ? !brokerReachable ? "broker_unavailable"
      : !brokerSessionConfigured ? "mobile_login_required"
      : brokerCanReadCustomer && brokerCanListBookings ? "mobile_read_only_ready"
      : "partial_read_only_unverified"
    : directCustomerAuth ? "unverified" : "not_configured";
  const bookingExecution = mutationExecutionReadiness("booking");
  const paymentFormExecution = paymentFormExecutionReadiness();
  return {
    serverVersion: SERVER_VERSION,
    ready: searchReady,
    searchReady,
    networkReachability: "not_checked",
    readinessScope: "local_configuration_only",
    transport,
    authentication,
    authMode,
    customerContext: customerReadiness,
    customerReadiness,
    canReadCustomer: brokerCanReadCustomer || directCustomerAuth,
    canListBookings: brokerCanListBookings || directCustomerAuth,
    canReadBookingV1: brokerCanReadBookingV1 || directCustomerAuth,
    canSaveVoucher: brokerCanSaveVoucherV1,
    canCreatePaymentHandoff: brokerCanCreatePaymentHandoff,
    paymentHandoffPreview: {
      available: brokerCanCreatePaymentHandoff,
      bookingBindingSupported: brokerCanCreatePaymentHandoff,
      amountBindingVerified: false,
      paymentStatusObservation: brokerCanCreatePaymentHandoff ? "available_at_handoff" : "not_available",
      providerRequestsPerformed: false,
      amountBindingAvailableAtHandoff: brokerCanCreatePaymentHandoff,
      rawPaymentStatusAvailableAtHandoff: brokerCanCreatePaymentHandoff,
      providerReadOnCreate: brokerCanCreatePaymentHandoff,
      singleUse: true,
    },
    mobileAuth: {
      configured: sharedMobileAuth,
      provider: sharedMobileAuth ? "local_auth_broker" : "none",
      reachable: brokerReachable,
      sessionConfigured: brokerSessionConfigured,
      sessionOwnerOnly: brokerProbe?.sessionOwnerOnly ?? null,
      verified: brokerCanReadCustomer && brokerCanListBookings && brokerCanReadBookingV1,
      verifiedOperations: [...brokerVerifiedOperations].filter((operation) => operation === "hotels.get_customer" || operation === "hotels.list_bookings" || operation === "hotels.get_booking_v1" || operation === "hotels.save_voucher_v1"),
      supportedOperations: brokerReachable ? (brokerProbe.supportedOperations ?? []) : [],
    },
    bookingExecution,
    paymentFormExecution,
    loadProtection: {
      status: loadProtectionError ? "invalid_configuration" : "configured",
      maxConcurrentProviderRequests: configuredProviderConcurrency,
      maxQueuedProviderRequests: MAX_PROVIDER_REQUEST_QUEUE,
      activeProviderRequests,
      queuedProviderRequests: providerRequestQueue.length,
      identicalSearchCacheTtlMs: SEARCH_CACHE_TTL_MS,
      cachedSearches: hotelSearchCacheByKey.size,
      inFlightSearches: inFlightHotelSearchByKey.size,
    },
    mutationsEnabled: mutationsEnabled(),
    diagnostics: {
      transport: transportError,
      authentication: authenticationError,
      authBroker: brokerError,
      loadProtection: loadProtectionError,
    },
    browserDependency: false,
    storedUserSession: false,
    sharedMobileSessionConfigured: brokerSessionConfigured,
    note: "Значения URL, токенов и auth-заголовков намеренно не раскрываются.",
  };
}

async function getCustomer() {
  if (authBrokerSocket()) {
    const result = await authBrokerRequest("hotels.get_customer");
    return result.customer;
  }
  if (configuredAuthMode() === "service_jwt") {
    throw new Error("Customer context is not configured. service_jwt authenticates the MCP service and cannot autofill booking guest data.");
  }
  if (configuredAuthMode() === "anonymous") {
    throw new Error("Customer context is not configured. Complete local mobile login and configure the auth broker, or provide an approved static customer auth profile.");
  }
  return apiRequest("GET", "/api/v1/auth/customerdata");
}

function mutationsEnabled() {
  return process.env.TBANK_HOTELS_ENABLE_MUTATIONS === "true";
}

function requireMutationsEnabled() {
  if (!mutationsEnabled()) throw new Error("Hotels API mutation execution is not available in this MCP configuration. Activation requires an integration-owner decision outside the model conversation.");
}

function requireMutationExecutionReady(action) {
  requireMutationsEnabled();
  const readiness = mutationExecutionReadiness(action);
  if (!readiness.available) {
    throw new Error(`Hotels API ${action} execution profile is not ready (${readiness.status}). Required trusted request headers and customer authorization must be configured outside the model conversation.`);
  }
}

function cleanupJourneys() {
  const now = Date.now();
  for (const [journeyId, journey] of journeysById.entries()) {
    if (journey.expiresAt <= now) journeysById.delete(journeyId);
  }
  for (const [draftId, draft] of bookingDraftsById.entries()) {
    if (draft.expiresAt <= now) bookingDraftsById.delete(draftId);
  }
  for (const [bookingRef, reference] of bookingReferencesById.entries()) {
    if (reference.expiresAt <= now) bookingReferencesById.delete(bookingRef);
  }
  for (const [requestHashValue, execution] of mutationExecutionsByHash.entries()) {
    if (execution.expiresAt <= now) mutationExecutionsByHash.delete(requestHashValue);
  }
}

function storeBounded(map, key, item, maximum) {
  cleanupJourneys();
  if (map.size >= maximum) {
    let oldestKey = null;
    let oldestExpiry = Number.POSITIVE_INFINITY;
    for (const [candidateKey, candidate] of map.entries()) {
      const expiry = Number(candidate?.expiresAt ?? 0);
      if (expiry < oldestExpiry) {
        oldestKey = candidateKey;
        oldestExpiry = expiry;
      }
    }
    if (oldestKey !== null) map.delete(oldestKey);
  }
  map.set(key, item);
}

function startTrackedMutationExecution(hash, expiresAt) {
  cleanupJourneys();
  const existing = mutationExecutionsByHash.get(hash);
  if (existing?.state === "in_flight") throw new Error("This mutation execution is already in progress.");
  if (existing?.state === "outcome_unknown") throw new Error("The previous mutation execution outcome is unknown. Do not retry it automatically; reconcile provider state first.");
  if (existing?.state === "completed") throw new Error("This prepared mutation has already completed and cannot be replayed.");
  storeBounded(mutationExecutionsByHash, hash, { state: "in_flight", expiresAt }, MAX_TRACKED_MUTATION_EXECUTIONS);
}

function finishTrackedMutationExecution(hash, state) {
  const execution = mutationExecutionsByHash.get(hash);
  if (execution) execution.state = state;
}

function bookingDraftById(bookingDraftId) {
  cleanupJourneys();
  if (typeof bookingDraftId !== "string" || !bookingDraftId) throw new Error("bookingDraftId must be a non-empty string.");
  const draft = bookingDraftsById.get(bookingDraftId);
  if (!draft) throw new Error("Unknown or expired bookingDraftId. Create a new booking draft.");
  return draft;
}

function bookingReferenceForOrderId(orderId) {
  cleanupJourneys();
  for (const [bookingRef, reference] of bookingReferencesById.entries()) {
    if (reference.orderId === orderId) return bookingRef;
  }
  const bookingRef = `booking_${randomUUID().replaceAll("-", "").slice(0, 24)}`;
  storeBounded(bookingReferencesById, bookingRef, {
    orderId,
    expiresAt: Date.now() + JOURNEY_TTL_MS,
  }, MAX_ACTIVE_BOOKING_REFERENCES);
  return bookingRef;
}

function orderIdForBookingReference(bookingRef) {
  cleanupJourneys();
  if (typeof bookingRef !== "string" || !/^booking_[a-f0-9]{24}$/.test(bookingRef)) {
    throw new Error("bookingRef must be an opaque reference returned by tbank_hotels_list_bookings.");
  }
  const reference = bookingReferencesById.get(bookingRef);
  if (!reference) throw new Error("Unknown or expired bookingRef. Call tbank_hotels_list_bookings again.");
  return reference.orderId;
}

function withoutProviderBookingIdentifiers(value) {
  if (Array.isArray(value)) return value.map(withoutProviderBookingIdentifiers);
  if (!value || typeof value !== "object") return value;
  return Object.fromEntries(Object.entries(value)
    .filter(([key]) => !providerBookingIdentifierKey(key))
    .map(([key, nested]) => [key, withoutProviderBookingIdentifiers(nested)]));
}

function providerBookingIdentifierKey(key) {
  const normalized = String(key).replace(/[^a-z0-9]/gi, "").toLowerCase();
  return [
    "orderid", "bookingid", "reservationid", "taskid", "paymentid",
    "transactionid", "customerid", "userid", "sessionid", "ssoid", "siebelid",
    "ordernumber", "bookingnumber", "reservationnumber", "confirmationnumber",
    "orderref", "bookingref", "reservationref", "providerref", "bookhash",
  ].some((suffix) => normalized.endsWith(suffix)) || normalized.includes("token");
}

function bookingListWithReferences(bookings) {
  const root = bookings?.payload && typeof bookings.payload === "object" && !Array.isArray(bookings.payload)
    ? bookings.payload
    : bookings;
  if (!root || typeof root !== "object" || Array.isArray(root)) {
    throw new Error("Hotels booking list response has an unsupported shape.");
  }
  const normalized = { ...root };
  for (const listName of ["activeList", "cancelledList", "completedList"]) {
    const list = root[listName];
    if (!Array.isArray(list)) throw new Error(`Hotels booking list response does not contain ${listName}.`);
    normalized[listName] = list.map((item) => {
      if (!item || typeof item !== "object" || Array.isArray(item)) throw new Error(`Hotels booking list ${listName} contains an invalid item.`);
      const orderId = brokerIdentifier(item.orderId, "provider orderId");
      return {
        ...withoutProviderBookingIdentifiers(item),
        bookingRef: bookingReferenceForOrderId(orderId),
      };
    });
  }
  return root === bookings ? normalized : { ...withoutProviderBookingIdentifiers(bookings), payload: normalized };
}

function bookingListSummary(bookings) {
  const root = bookings?.payload && typeof bookings.payload === "object" && !Array.isArray(bookings.payload)
    ? bookings.payload
    : bookings;
  if (!root || typeof root !== "object" || Array.isArray(root)) {
    throw new Error("Hotels booking list response has an unsupported shape.");
  }
  const counts = {};
  for (const [outputName, listName] of [["activeCount", "activeList"], ["cancelledCount", "cancelledList"], ["completedCount", "completedList"]]) {
    if (!Array.isArray(root[listName])) throw new Error(`Hotels booking list response does not contain ${listName}.`);
    counts[outputName] = root[listName].length;
  }
  return {
    status: "ready",
    ...counts,
    detailsIncluded: false,
    personalTravelFactsIncluded: false,
    bookingReferencesIncluded: false,
  };
}

function bookingWithReference(booking, bookingRef) {
  const sanitized = withoutProviderBookingIdentifiers(booking);
  if (!sanitized || typeof sanitized !== "object" || Array.isArray(sanitized)) {
    throw new Error("Hotels booking response has an unsupported shape.");
  }
  return { ...sanitized, bookingRef };
}

function journeyById(journeyId) {
  cleanupJourneys();
  if (typeof journeyId !== "string" || !journeyId) throw new Error("journeyId must be a non-empty string.");
  const journey = journeysById.get(journeyId);
  if (!journey) throw new Error("Unknown or expired journeyId. Start a new hotel stay plan.");
  return journey;
}

function optional(object, key) {
  return object && typeof object === "object" ? object[key] ?? null : null;
}

function displayedPriceBreakfastEvidence(mealName) {
  if (typeof mealName !== "string" || !mealName.trim()) return "not_confirmed_for_displayed_price";
  const normalized = normalizedText(mealName);
  const explicitlyExcluded = normalized.includes("breakfast not included")
    || normalized.includes("without breakfast")
    || normalized.includes("no breakfast")
    || normalized.includes("meal not included")
    || normalized.includes("meals not included")
    || normalized.includes("without meals")
    || normalized.includes("no meals")
    || normalized === "room only"
    || normalized.includes("завтрак не включен")
    || normalized.includes("без завтрака")
    || normalized.includes("питание не включено")
    || normalized.includes("без питания");
  if (explicitlyExcluded) return "excluded_by_meal_name";
  const explicitlyIncluded = normalized === "breakfast"
    || normalized === "завтрак"
    || normalized.includes("breakfast included")
    || normalized.includes("includes breakfast")
    || normalized.includes("завтрак включен")
    || normalized.includes("завтрак входит")
    || normalized.includes("с завтраком");
  return explicitlyIncluded ? "confirmed_by_meal_name" : "not_confirmed_for_displayed_price";
}

function stayOption(option, stayNights = 1) {
  const hotel = option.hotel;
  const rate = optional(hotel, "rateForHotelsFeed") ?? {};
  const review = optional(hotel, "review") ?? null;
  const rawStarRating = optional(hotel, "starRating");
  const price = optional(rate, "shownPrice");
  const totalPriceAmount = numericProviderFact(price);
  const priceCurrency = providerCurrency(price);
  const pricePerNightAmount = derivedPerNightAmount(totalPriceAmount, stayNights);
  const freeCancellationUntil = optional(rate, "freeCancellationUntil");
  return {
    optionId: option.optionId,
    hotelName: optional(hotel, "hotelName"),
    hotelChain: optional(hotel, "hotelChain"),
    starRating: typeof rawStarRating === "number" && rawStarRating > 0 ? rawStarRating : null,
    destination: optional(optional(hotel, "areaLocation"), "destinationName"),
    address: optional(optional(hotel, "hotelLocation"), "address"),
    price,
    priceDisplay: formatProviderMoney(totalPriceAmount, priceCurrency),
    priceBasis: "provider_total_for_stay",
    stayNights,
    totalPriceAmount,
    totalPriceCurrency: priceCurrency,
    totalPriceDisplay: formatProviderMoney(totalPriceAmount, priceCurrency),
    pricePerNightAmount,
    pricePerNightCurrency: priceCurrency,
    pricePerNightDisplay: formatProviderMoney(pricePerNightAmount, priceCurrency),
    pricePerNightSource: pricePerNightAmount === null ? null : "mcp_derived_from_provider_total_and_dates",
    freeCancellationUntil,
    freeCancellationUntilDisplay: formatProviderTimestamp(freeCancellationUntil),
    mealName: optional(rate, "mealName"),
    displayedPriceBreakfastEvidence: displayedPriceBreakfastEvidence(optional(rate, "mealName")),
    paymentPlace: optional(rate, "paymentPlace"),
    availableRoomsCount: optional(rate, "availableRoomsCount"),
    review: review ? { rating: optional(review, "rating"), ratingsCount: optional(review, "ratingsCount") } : null,
    cashback: optional(hotel, "cashback"),
  };
}

function stayRateOption(rateOption, stayNights = 1) {
  const rate = rateOption.rate;
  const shownPrice = optional(rate, "shownPrice");
  const totalPriceAmount = numericProviderFact(shownPrice);
  const priceCurrency = providerCurrency(shownPrice);
  const pricePerNightAmount = derivedPerNightAmount(totalPriceAmount, stayNights);
  const freeCancellationUntil = optional(rate, "freeCancellationUntil");
  const isNonRefundable = optional(rate, "isNonRefundable");
  return {
    rateNumber: rateOption.rateNumber,
    rateLabel: `Тариф ${rateOption.rateNumber}`,
    rateOptionId: rateOption.rateOptionId,
    shownPrice,
    shownPriceDisplay: formatProviderMoney(totalPriceAmount, priceCurrency),
    priceBasis: "provider_total_for_stay",
    stayNights,
    totalPriceAmount,
    totalPriceCurrency: priceCurrency,
    totalPriceDisplay: formatProviderMoney(totalPriceAmount, priceCurrency),
    pricePerNightAmount,
    pricePerNightCurrency: priceCurrency,
    pricePerNightDisplay: formatProviderMoney(pricePerNightAmount, priceCurrency),
    pricePerNightSource: pricePerNightAmount === null ? null : "mcp_derived_from_provider_total_and_dates",
    paymentPrice: optional(rate, "paymentPrice"),
    paymentPlace: optional(rate, "paymentPlace"),
    mealName: optional(rate, "mealName"),
    displayedPriceBreakfastEvidence: displayedPriceBreakfastEvidence(optional(rate, "mealName")),
    availableRoomsCount: optional(rate, "availableRoomsCount"),
    isNonRefundable,
    freeCancellationUntil,
    cancellationDisplay: isNonRefundable === true ? "невозвратный" : formatProviderTimestamp(freeCancellationUntil),
    isCreditCardDataRequired: optional(rate, "isCreditCardDataRequired"),
    cancellationPolicyRules: optional(rate, "cancellationPolicyRules"),
    cashback: optional(rate, "cashback"),
  };
}

function stayComparisonRow(option, allOptions, preferences, includeBestValue, stayNights) {
  const facts = stayOption(option, stayNights);
  const bestValue = includeBestValue ? bestValueAssessment(option, allOptions, preferences, stayNights) : null;
  return {
    hotelName: facts.hotelName,
    destination: facts.destination,
    starRating: facts.starRating,
    reviewRating: facts.review?.rating ?? null,
    ratingsCount: facts.review?.ratingsCount ?? null,
    priceAmount: numericProviderFact(facts.price),
    priceCurrency: providerCurrency(facts.price),
    priceDisplay: facts.priceDisplay,
    priceBasis: facts.priceBasis,
    stayNights: facts.stayNights,
    totalPriceAmount: facts.totalPriceAmount,
    totalPriceCurrency: facts.totalPriceCurrency,
    totalPriceDisplay: facts.totalPriceDisplay,
    pricePerNightAmount: facts.pricePerNightAmount,
    pricePerNightCurrency: facts.pricePerNightCurrency,
    pricePerNightDisplay: facts.pricePerNightDisplay,
    pricePerNightSource: facts.pricePerNightSource,
    freeCancellationUntil: facts.freeCancellationUntil,
    freeCancellationUntilDisplay: facts.freeCancellationUntilDisplay,
    mealName: facts.mealName,
    displayedPriceBreakfastEvidence: facts.displayedPriceBreakfastEvidence,
    bestValueScore: bestValue?.score ?? null,
    pricePreferenceFit: bestValue?.pricePreferenceFit ?? (preferences ? pricePreferenceFit(option, preferences, stayNights) : "not_requested"),
  };
}

function preferenceAlternatives(options, selectedOptionIds, preferences, stayNights) {
  if (!preferences?.pricePerNight || preferences.showAlternativesOutsideBand !== true) return null;
  const excluded = new Set(selectedOptionIds);
  const ranked = rankedOptions(options, "best_value", preferences, stayNights).filter((option) => !excluded.has(option.optionId));
  const rowsFor = (fit) => ranked
    .filter((option) => pricePreferenceFit(option, preferences, stayNights) === fit)
    .slice(0, 2)
    .map((option) => ({
      optionId: option.optionId,
      ...stayComparisonRow(option, options, preferences, true, stayNights),
    }));
  return {
    scope: "all_journey_options_outside_soft_price_range",
    belowPreferredRange: rowsFor("below_preferred_range"),
    abovePreferredRange: rowsFor("above_preferred_range"),
    presentationGuidance: "If the user requested cheaper or more expensive alternatives, show these rows separately from the top comparison. Empty arrays mean no suitable bounded-search alternative was found; do not invent one.",
  };
}

function presentedStayOption(option, allOptions, preferences, ranking, stayNights) {
  const facts = stayOption(option, stayNights);
  const assessment = ranking === "best_value" ? bestValueAssessment(option, allOptions, preferences, stayNights) : null;
  return {
    ...facts,
    bestValueScore: assessment?.score ?? null,
    pricePreferenceFit: assessment?.pricePreferenceFit ?? (preferences ? pricePreferenceFit(option, preferences, stayNights) : "not_requested"),
  };
}

function markdownCell(value) {
  if (value === null || value === undefined || value === "") return "—";
  return String(value).replaceAll("|", "\\|").replaceAll("\n", " ");
}

function comparisonTableMarkdown(rows) {
  const includeBestValue = rows.some((row) => row.bestValueScore !== null);
  const header = `| Отель | Локация | Звёзды | Рейтинг | Отзывов | За поездку | За ночь | Бесплатная отмена | Питание |${includeBestValue ? " Best value | Диапазон |" : ""}`;
  const divider = `| --- | --- | ---: | ---: | ---: | ---: | ---: | --- | --- |${includeBestValue ? " ---: | --- |" : ""}`;
  const body = rows.map((row) => {
    return `| ${markdownCell(row.hotelName)} | ${markdownCell(row.destination)} | ${markdownCell(row.starRating)} | ${markdownCell(row.reviewRating)} | ${markdownCell(row.ratingsCount)} | ${markdownCell(row.totalPriceDisplay)} | ${markdownCell(row.pricePerNightDisplay)} | ${markdownCell(row.freeCancellationUntilDisplay)} | ${markdownCell(row.mealName)} |${includeBestValue ? ` ${markdownCell(row.bestValueScore)} | ${markdownCell(row.pricePreferenceFit)} |` : ""}`;
  });
  return [header, divider, ...body].join("\n");
}

function ratePresentationRows(rateOptions, stayNights) {
  return rateOptions.map((rateOption) => {
    const facts = stayRateOption(rateOption, stayNights);
    return {
      rateNumber: facts.rateNumber,
      rateLabel: facts.rateLabel,
      rateOptionId: facts.rateOptionId,
      priceDisplay: facts.shownPriceDisplay,
      totalPriceDisplay: facts.totalPriceDisplay,
      pricePerNightDisplay: facts.pricePerNightDisplay,
      priceBasis: facts.priceBasis,
      stayNights: facts.stayNights,
      mealName: facts.mealName,
      cancellationDisplay: facts.cancellationDisplay,
      availableRoomsCount: facts.availableRoomsCount,
      displayedPriceBreakfastEvidence: facts.displayedPriceBreakfastEvidence,
    };
  });
}

function ratesTableMarkdown(rows) {
  const header = "| Тариф | За поездку | За ночь | Питание | Отмена | Доступно номеров |";
  const divider = "| ---: | ---: | ---: | --- | --- | ---: |";
  const body = rows.map((row) => `| ${row.rateNumber} | ${markdownCell(row.totalPriceDisplay)} | ${markdownCell(row.pricePerNightDisplay)} | ${markdownCell(row.mealName)} | ${markdownCell(row.cancellationDisplay)} | ${markdownCell(row.availableRoomsCount)} |`);
  return [header, divider, ...body].join("\n");
}

function normalizedText(value) {
  return String(value ?? "")
    .normalize("NFKD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLocaleLowerCase("ru-RU")
    .replace(/ё/g, "е")
    .replace(/[^\p{L}\p{N}]+/gu, " ")
    .trim()
    .replace(/\s+/g, " ");
}

function boundedInteger(value, name, fallback, minimum, maximum) {
  if (value === undefined) return fallback;
  if (!Number.isInteger(value) || value < minimum || value > maximum) throw new Error(`${name} must be an integer from ${minimum} to ${maximum}.`);
  return value;
}

function dateOnly(value, name) {
  if (typeof value !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(value)) throw new Error(`${name} must use YYYY-MM-DD format.`);
  const [year, month, day] = value.split("-").map(Number);
  const date = new Date(Date.UTC(year, month - 1, day));
  if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) throw new Error(`${name} is not a valid calendar date.`);
  return date;
}

function validatedPlanInput(args) {
  requestObject(args, "arguments");
  assertOnlyKeys(args, ["destination", "destinationId", "countryName", "checkinDate", "checkoutDate", "rooms", "hotelName", "breakfastIncluded", "hotelPreferences", "ranking", "maxOptions", "language"], "arguments");
  const destination = typeof args.destination === "string" ? args.destination.trim() : "";
  const destinationId = args.destinationId;
  if (!destination && (!Number.isInteger(destinationId) || destinationId <= 0)) throw new Error("Provide destination as a location name or a positive destinationId from destination resolution.");
  if (destination && destination.length > 200) throw new Error("destination must not exceed 200 characters.");
  if (destinationId !== undefined && (!Number.isInteger(destinationId) || destinationId <= 0)) throw new Error("destinationId must be a positive integer.");
  const checkin = dateOnly(args.checkinDate, "checkinDate");
  const checkout = dateOnly(args.checkoutDate, "checkoutDate");
  if (checkout <= checkin) throw new Error("checkoutDate must be after checkinDate.");
  const today = new Date();
  const todayUtc = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate()));
  if (checkin < todayUtc) throw new Error("checkinDate must not be in the past.");
  if (!Array.isArray(args.rooms) || args.rooms.length < 1 || args.rooms.length > MAX_ROOMS) throw new Error(`rooms must contain 1 to ${MAX_ROOMS} rooms.`);
  const rooms = args.rooms.map((room, index) => {
    requestObject(room, `rooms[${index}]`);
    if (!Number.isInteger(room.adults) || room.adults < 1 || room.adults > 16) throw new Error(`rooms[${index}].adults must be an integer from 1 to 16.`);
    const childrenAges = room.childrenAges ?? [];
    if (!Array.isArray(childrenAges) || childrenAges.length > 16 || childrenAges.some((age) => !Number.isInteger(age) || age < 0 || age > 17)) {
      throw new Error(`rooms[${index}].childrenAges must contain at most 16 integer ages from 0 to 17.`);
    }
    return { adults: room.adults, childrenAges: [...childrenAges] };
  });
  const hotelName = args.hotelName == null ? null : String(args.hotelName).trim();
  if (args.hotelName != null && (!hotelName || hotelName.length > 250)) throw new Error("hotelName must contain 1 to 250 characters.");
  const countryName = args.countryName == null ? null : String(args.countryName).trim();
  if (args.countryName != null && (!countryName || countryName.length > 120)) throw new Error("countryName must contain 1 to 120 characters.");
  let hotelPreferencesValue = null;
  if (args.hotelPreferences != null) {
    const preferences = requestObject(args.hotelPreferences, "hotelPreferences");
    assertOnlyKeys(preferences, ["pricePerNight", "ranking", "showAlternativesOutsideBand"], "hotelPreferences");
    const price = requestObject(preferences.pricePerNight, "hotelPreferences.pricePerNight");
    assertOnlyKeys(price, ["min", "max", "currency"], "hotelPreferences.pricePerNight");
    if (typeof price.min !== "number" || !Number.isFinite(price.min) || price.min < 0) throw new Error("hotelPreferences.pricePerNight.min must be a finite non-negative number.");
    if (typeof price.max !== "number" || !Number.isFinite(price.max) || price.max <= 0 || price.max < price.min) throw new Error("hotelPreferences.pricePerNight.max must be a finite number greater than or equal to min.");
    if (typeof price.currency !== "string" || !/^[A-Za-z]{3}$/.test(price.currency)) throw new Error("hotelPreferences.pricePerNight.currency must contain a three-letter currency code.");
    if (preferences.ranking !== "best_value") throw new Error("hotelPreferences.ranking must be best_value.");
    if (preferences.showAlternativesOutsideBand !== true) throw new Error("hotelPreferences.showAlternativesOutsideBand must be true because the profile range is soft.");
    hotelPreferencesValue = {
      pricePerNight: { min: price.min, max: price.max, currency: price.currency.toUpperCase() },
      ranking: preferences.ranking,
      showAlternativesOutsideBand: preferences.showAlternativesOutsideBand,
    };
  }
  const ranking = args.ranking ?? hotelPreferencesValue?.ranking ?? "provider_order";
  if (!["provider_order", "lowest_price", "highest_rating", "best_value"].includes(ranking)) throw new Error("ranking must be provider_order, lowest_price, highest_rating, or best_value.");
  if (args.breakfastIncluded !== undefined && typeof args.breakfastIncluded !== "boolean") throw new Error("breakfastIncluded must be a boolean.");
  return {
    destination,
    destinationId: destinationId ?? null,
    countryName,
    checkinDate: args.checkinDate,
    checkoutDate: args.checkoutDate,
    rooms,
    hotelName,
    breakfastIncluded: args.breakfastIncluded === true,
    hotelPreferences: hotelPreferencesValue,
    ranking,
    maxOptions: boundedInteger(args.maxOptions, "maxOptions", DEFAULT_PLAN_OPTIONS, 1, MAX_PLAN_OPTIONS),
    language: args.language,
  };
}

function validatedSearchFilters(filtersValue, name = "payload.filters") {
  if (filtersValue === undefined) return undefined;
  if (!Array.isArray(filtersValue)) throw new Error(`${name} must be an array.`);
  return filtersValue.map((filterValue, index) => {
    const itemName = `${name}[${index}]`;
    const filter = requestObject(filterValue, itemName);
    if (!SEARCH_FILTER_IDS.includes(filter.filterId)) throw new Error(`${itemName}.filterId is unsupported.`);
    switch (filter.$objectType) {
      case "array":
        assertOnlyKeys(filter, ["$objectType", "filterId", "values"], itemName);
        if (!Array.isArray(filter.values) || filter.values.some((value) => typeof value !== "string")) throw new Error(`${itemName}.values must be an array of strings.`);
        break;
      case "boolean":
        assertOnlyKeys(filter, ["$objectType", "filterId", "value"], itemName);
        if (typeof filter.value !== "boolean") throw new Error(`${itemName}.value must be a boolean.`);
        break;
      case "radio":
        assertOnlyKeys(filter, ["$objectType", "filterId", "value", "values"], itemName);
        if (typeof filter.value !== "string" || !filter.value) throw new Error(`${itemName}.value must be a non-empty string.`);
        if (filter.values !== undefined && filter.values !== null && (!Array.isArray(filter.values) || filter.values.some((value) => typeof value !== "string"))) {
          throw new Error(`${itemName}.values must be an array of strings or null.`);
        }
        break;
      case "range":
        assertOnlyKeys(filter, ["$objectType", "filterId", "min", "max"], itemName);
        if (typeof filter.min !== "number" || !Number.isFinite(filter.min) || typeof filter.max !== "number" || !Number.isFinite(filter.max)) {
          throw new Error(`${itemName}.min and ${itemName}.max must be finite numbers.`);
        }
        break;
      default:
        throw new Error(`${itemName}.$objectType must be array, boolean, radio, or range.`);
    }
    return structuredClone(filter);
  });
}

function validatedProviderSearchRequest(payloadValue) {
  const body = requestObject(payloadValue);
  assertOnlyKeys(body, ["destinationId", "checkinDate", "checkoutDate", "guests", "filters", "sort", "offset", "limit"], "payload");
  if (!Number.isInteger(body.destinationId) || body.destinationId <= 0) throw new Error("payload.destinationId must be a positive integer.");
  const checkin = dateOnly(body.checkinDate, "payload.checkinDate");
  const checkout = dateOnly(body.checkoutDate, "payload.checkoutDate");
  if (checkout <= checkin) throw new Error("payload.checkoutDate must be after payload.checkinDate.");
  if (!Array.isArray(body.guests) || body.guests.length < 1 || body.guests.length > MAX_ROOMS) throw new Error(`payload.guests must contain 1 to ${MAX_ROOMS} room guest groups.`);
  body.guests.forEach((guest, index) => {
    requestObject(guest, `payload.guests[${index}]`);
    assertOnlyKeys(guest, ["adultsCount", "childrenAge"], `payload.guests[${index}]`);
    if (!Number.isInteger(guest.adultsCount) || guest.adultsCount < 1 || guest.adultsCount > 16) throw new Error(`payload.guests[${index}].adultsCount must be an integer from 1 to 16.`);
    if (guest.childrenAge !== undefined && (!Array.isArray(guest.childrenAge) || guest.childrenAge.length > 16 || guest.childrenAge.some((age) => !Number.isInteger(age) || age < 0 || age > 17))) {
      throw new Error(`payload.guests[${index}].childrenAge must contain integer ages from 0 to 17.`);
    }
  });
  const filters = validatedSearchFilters(body.filters);
  if (body.sort !== undefined) requestObject(body.sort, "payload.sort");
  optionalNonNegativeInteger(body.offset, "payload.offset");
  optionalNonNegativeInteger(body.limit, "payload.limit");
  return { ...body, ...(filters === undefined ? {} : { filters }) };
}

function providerSearchRequest(method, path, args) {
  return apiRequest(method, path, { ...args, payload: validatedProviderSearchRequest(args.payload) });
}

function requiredStayConditions(input) {
  return {
    breakfastIncluded: input.breakfastIncluded,
  };
}

function appliedStayConditions(input) {
  return {
    breakfastIncluded: input.breakfastIncluded
      ? { required: true, applied: true, source: "provider_search_filter", filterId: "meal_types", value: "breakfast" }
      : { required: false, applied: false, source: "not_requested" },
  };
}

function appliedHotelPreferences(input) {
  if (!input.hotelPreferences) {
    return { requested: false, applied: false, source: "not_requested", providerFilterApplied: false };
  }
  return {
    requested: true,
    applied: input.ranking === "best_value",
    source: "client_supplied_privacy_profile",
    providerFilterApplied: false,
    softPreference: true,
    pricePerNight: input.hotelPreferences.pricePerNight,
    showAlternativesOutsideBand: input.hotelPreferences.showAlternativesOutsideBand,
    scoreFormula: "best_value_v2 = 60% rating + 20% review evidence + 20% band-aware price utility",
  };
}

function providerConditionFailure(error) {
  return error?.code === "HOTELS_API_TIMEOUT" || error?.code === "HOTELS_API_NETWORK" || error?.code === "HOTELS_API_HTTP";
}

function providerConditionUnavailable(error, requiredConditions) {
  const providerHttpStatus = Number.isInteger(error?.httpStatus) ? error.httpStatus : null;
  const reason = error?.code === "HOTELS_API_TIMEOUT"
    ? "provider_timeout"
    : error?.code === "HOTELS_API_NETWORK"
      ? "provider_unreachable"
      : providerHttpStatus === 401 || providerHttpStatus === 403
        ? "provider_auth_rejected"
        : (providerHttpStatus ?? 0) >= 500
          ? "provider_unavailable"
          : "provider_rejected_required_request";
  return {
    status: "requirements_unavailable",
    reason,
    requiredConditions,
    retryAllowed: false,
    lowLevelFallbackAllowed: false,
    providerHttpStatus,
    nextStep: reason === "provider_auth_rejected"
      ? "Do not retry or weaken the required condition. Check the configured Hotels search authentication profile outside the model conversation, then start a new search after readiness is restored."
      : "Do not retry by guessing low-level filter payloads and do not present unfiltered hotels as satisfying the requirement. Report that the required filtered search is temporarily unavailable.",
  };
}

function staySearchUnavailable(error, input) {
  const failure = providerConditionUnavailable(error, requiredStayConditions(input));
  return {
    ...failure,
    status: "search_unavailable",
    conditionsApplied: appliedStayConditions(input),
    preferencesApplied: appliedHotelPreferences(input),
    connectionStatusAllowedOnce: true,
    nextStep: failure.reason === "provider_auth_rejected"
      ? "Do not retry this search or inspect secrets/config from the model session. Call connection_status at most once, report that search authentication was rejected, and stop until an operator restores it."
      : "Do not retry this search, wait automatically, inspect shell/config, create a direct provider driver, or use low-level tools. Call connection_status at most once for local configuration context, report the provider connectivity failure, and stop until an operator restores network/DNS availability.",
  };
}

async function guardedProviderSearchRequest(method, path, args) {
  const body = validatedProviderSearchRequest(args.payload);
  try {
    return await apiRequest(method, path, { ...args, payload: body });
  } catch (error) {
    if (body.filters?.length && providerConditionFailure(error)) {
      return providerConditionUnavailable(error, { providerFilters: body.filters });
    }
    throw error;
  }
}

function locationCandidate(location) {
  const destinationId = optional(location, "locationId");
  if (!Number.isInteger(destinationId) || destinationId <= 0) return null;
  return {
    destinationId,
    name: optional(location, "locationNameRu") || optional(location, "locationName"),
    internationalName: optional(location, "locationName"),
    countryName: optional(location, "countryNameRu") || optional(location, "countryName"),
    internationalCountryName: optional(location, "countryName"),
    hotelsCount: optional(location, "hotelsCount"),
  };
}

async function locationCatalog(countryName) {
  if (locationCatalogTransport !== globalThis.fetch) {
    locationCatalogByCountry.clear();
    locationCatalogTransport = globalThis.fetch;
  }
  const cacheKey = normalizedText(countryName || "*");
  const cached = locationCatalogByCountry.get(cacheKey);
  if (cached && cached.expiresAt > Date.now()) return cached.locations;
  const locationsById = new Map();
  const startedAt = Date.now();
  for (let page = 0; page < MAX_LOCATION_PAGES; page += 1) {
    const remainingBudgetMs = LOCATION_COLLECTION_BUDGET_MS - (Date.now() - startedAt);
    if (page > 0 && remainingBudgetMs < MIN_SEARCH_REQUEST_BUDGET_MS) {
      throw new Error("Hotels API locations catalog could not be loaded completely within the safe time budget. Retry destination resolution.");
    }
    const query = { Sort: "name", Offset: page * LOCATION_PAGE_SIZE, Limit: LOCATION_PAGE_SIZE };
    if (countryName) query.CountryName = countryName;
    let result;
    try {
      result = await apiRequest("GET", "/api/v1/seo/locations", {
        query,
        requestTimeoutMs: Math.max(MIN_SEARCH_REQUEST_BUDGET_MS, remainingBudgetMs),
      });
    } catch (error) {
      if (error.code === "HOTELS_API_TIMEOUT") {
        throw new Error("Hotels API locations catalog could not be loaded completely within the safe time budget. Retry destination resolution.");
      }
      throw error;
    }
    const rawLocations = result.data?.payload?.locations;
    if (!Array.isArray(rawLocations)) throw new Error("Hotels API locations response does not contain the expected payload.locations array.");
    let added = 0;
    for (const candidate of rawLocations.map(locationCandidate).filter(Boolean)) {
      if (!locationsById.has(candidate.destinationId)) added += 1;
      locationsById.set(candidate.destinationId, candidate);
    }
    if (rawLocations.length < LOCATION_PAGE_SIZE || added === 0) break;
    if (page === MAX_LOCATION_PAGES - 1) throw new Error("Hotels API locations catalog exceeded the safe pagination limit.");
  }
  const locations = [...locationsById.values()];
  if (locationCatalogByCountry.size >= MAX_LOCATION_CACHES) {
    const oldest = [...locationCatalogByCountry.entries()].sort((left, right) => left[1].expiresAt - right[1].expiresAt)[0];
    if (oldest) locationCatalogByCountry.delete(oldest[0]);
  }
  locationCatalogByCountry.set(cacheKey, { expiresAt: Date.now() + LOCATION_CACHE_TTL_MS, locations });
  return locations;
}

function locationMatchScore(candidate, query) {
  const names = [candidate.name, candidate.internationalName].map(normalizedText).filter(Boolean);
  const countries = [candidate.countryName, candidate.internationalCountryName].map(normalizedText).filter(Boolean);
  if (names.includes(query)) return 0;
  if (names.some((name) => `${name} ${countries[0] ?? ""}`.trim() === query)) return 1;
  if (names.some((name) => name.startsWith(query))) return 2;
  if (names.some((name) => name.includes(query))) return 3;
  const tokens = query.split(" ").filter(Boolean);
  if (tokens.length > 1 && names.some((name) => tokens.every((token) => name.includes(token)))) return 4;
  return null;
}

function locationMatchesCountry(candidate, countryName) {
  if (!countryName) return true;
  const requested = normalizedText(countryName);
  return [candidate.countryName, candidate.internationalCountryName]
    .map(normalizedText)
    .filter(Boolean)
    .includes(requested);
}

function rankedLocationMatches(locations, query, countryName) {
  const countryMatches = locations.filter((candidate) => locationMatchesCountry(candidate, countryName));
  const candidates = countryMatches.length ? countryMatches : locations;
  return candidates
    .map((candidate) => ({ candidate, score: locationMatchScore(candidate, query) }))
    .filter(({ score }) => score !== null)
    .sort((left, right) => left.score - right.score || (right.candidate.hotelsCount ?? 0) - (left.candidate.hotelsCount ?? 0) || String(left.candidate.name).localeCompare(String(right.candidate.name), "ru"));
}

async function resolveDestination(args) {
  if (typeof args.query !== "string" || !args.query.trim() || args.query.length > 200) throw new Error("query must contain 1 to 200 characters.");
  if (args.countryName != null && (typeof args.countryName !== "string" || !args.countryName.trim() || args.countryName.length > 120)) {
    throw new Error("countryName must contain 1 to 120 characters.");
  }
  const query = normalizedText(args.query);
  const maxCandidates = boundedInteger(args.maxCandidates, "maxCandidates", 5, 1, 10);
  let locations = await locationCatalog(args.countryName);
  let matches = rankedLocationMatches(locations, query, args.countryName);
  let countryCatalogFallbackApplied = false;
  if (!matches.length && args.countryName) {
    locations = await locationCatalog(null);
    matches = rankedLocationMatches(locations, query, args.countryName);
    countryCatalogFallbackApplied = true;
  }
  const candidates = matches.slice(0, maxCandidates).map(({ candidate }) => candidate);
  if (!matches.length) return { status: "not_found", query: args.query, candidates: [], catalogSize: locations.length, countryCatalogFallbackApplied };
  const top = matches[0];
  const next = matches[1];
  const uniquelyResolved = !next || (top.score <= 2 && top.score < next.score);
  return {
    status: uniquelyResolved ? "resolved" : "ambiguous",
    query: args.query,
    countryCatalogFallbackApplied,
    destination: uniquelyResolved ? top.candidate : null,
    candidates,
  };
}

function numericProviderFact(value) {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (!value || typeof value !== "object") return null;
  for (const key of ["amount", "value", "price"]) {
    if (typeof value[key] === "number" && Number.isFinite(value[key])) return value[key];
  }
  return null;
}

function providerCurrency(value) {
  if (!value || typeof value !== "object") return null;
  for (const key of ["currency", "currencyCode", "currencyType"]) {
    if (typeof value[key] === "string" && value[key].trim()) return value[key].trim().toUpperCase();
  }
  return null;
}

function formatProviderMoney(amount, currency) {
  if (typeof amount !== "number" || !Number.isFinite(amount)) return null;
  const [integer, fraction] = String(amount).split(".");
  const grouped = integer.replace(/\B(?=(\d{3})+(?!\d))/g, " ");
  const number = fraction ? `${grouped},${fraction}` : grouped;
  const symbols = { RUB: "₽", USD: "$", EUR: "€", GBP: "£" };
  return `${number} ${symbols[currency] ?? currency ?? "(валюта не указана)"}`;
}

function formatProviderTimestamp(value) {
  if (typeof value !== "string" || !value.trim()) return "нет данных";
  const match = value.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::\d{2}(?:\.\d+)?)?(Z|[+-]\d{2}:\d{2})$/);
  if (!match) return `${value} (как передал поставщик)`;
  const [, year, month, day, hour, minute, offset] = match;
  const timezone = offset === "Z" || offset === "+00:00" ? "UTC" : `UTC${offset}`;
  return `${day}.${month}.${year} ${hour}:${minute} (${timezone})`;
}

function stayNightCount(checkinDate, checkoutDate) {
  const checkin = dateOnly(checkinDate, "checkinDate");
  const checkout = dateOnly(checkoutDate, "checkoutDate");
  return Math.max(1, Math.round((checkout.getTime() - checkin.getTime()) / 86_400_000));
}

function derivedPerNightAmount(totalAmount, stayNights) {
  if (typeof totalAmount !== "number" || !Number.isFinite(totalAmount)) return null;
  if (!Number.isInteger(stayNights) || stayNights < 1) return null;
  return Number((totalAmount / stayNights).toFixed(2));
}

function optionComparablePrice(option, stayNights) {
  return derivedPerNightAmount(numericProviderFact(stayOption(option).price), stayNights);
}

function pricePreferenceFit(option, preferences, stayNights) {
  if (!preferences?.pricePerNight) return "not_requested";
  const facts = stayOption(option);
  const amount = derivedPerNightAmount(numericProviderFact(facts.price), stayNights);
  const currency = providerCurrency(facts.price);
  if (amount === null || currency === null) return "unknown";
  const band = preferences.pricePerNight;
  if (currency !== band.currency) return "currency_mismatch";
  if (amount < band.min) return "below_preferred_range";
  if (amount > band.max) return "above_preferred_range";
  return "within_preferred_range";
}

function bestValueAssessment(option, options, preferences, stayNights) {
  const facts = stayOption(option);
  const amount = optionComparablePrice(option, stayNights);
  const currency = providerCurrency(facts.price);
  const rating = numericProviderFact(facts.review?.rating);
  const ratingsCount = numericProviderFact(facts.review?.ratingsCount);
  const fit = pricePreferenceFit(option, preferences, stayNights);
  const pricedFacts = options
    .map((candidate) => stayOption(candidate).price)
    .map((price) => ({ amount: derivedPerNightAmount(numericProviderFact(price), stayNights), currency: providerCurrency(price) }))
    .filter((fact) => fact.amount !== null);
  const pricedCurrencies = new Set(pricedFacts.map((fact) => fact.currency ?? "UNKNOWN"));
  const unsafeUnprofiledCurrencies = !preferences?.pricePerNight && (pricedCurrencies.size !== 1 || pricedCurrencies.has("UNKNOWN"));
  if (amount === null || currency === null || rating === null || unsafeUnprofiledCurrencies || (preferences?.pricePerNight && currency !== preferences.pricePerNight.currency)) {
    return { score: null, pricePreferenceFit: fit, source: "mcp_derived_from_provider_facts", formulaVersion: "best_value_v2" };
  }
  const comparablePrices = pricedFacts
    .filter((fact) => fact.currency === currency)
    .map((fact) => fact.amount);
  const sampleMin = Math.min(...comparablePrices);
  const sampleMax = Math.max(...comparablePrices);
  let priceUtility;
  if (preferences?.pricePerNight && currency === preferences.pricePerNight.currency) {
    const { min, max } = preferences.pricePerNight;
    const width = Math.max(1, max - min);
    if (amount < min) priceUtility = Math.max(0, 0.65 - ((min - amount) / width));
    else if (amount <= max) priceUtility = 1 - 0.2 * ((amount - min) / width);
    else priceUtility = Math.max(0, 0.65 - ((amount - max) / width));
  } else {
    priceUtility = sampleMax === sampleMin ? 1 : 1 - ((amount - sampleMin) / (sampleMax - sampleMin));
  }
  const ratingUtility = Math.max(0, Math.min(1, rating / 10));
  const reviewEvidenceUtility = ratingsCount === null ? 0 : Math.min(1, Math.log10(ratingsCount + 1) / 3);
  const score = 0.6 * ratingUtility + 0.2 * reviewEvidenceUtility + 0.2 * priceUtility;
  return {
    score: Number(score.toFixed(4)),
    pricePreferenceFit: fit,
    source: "mcp_derived_from_provider_facts",
    formulaVersion: "best_value_v2",
  };
}

function rankedOptions(options, strategy = "provider_order", preferences = null, stayNights = 1) {
  if (!["provider_order", "lowest_price", "highest_rating", "best_value"].includes(strategy)) throw new Error("ranking must be provider_order, lowest_price, highest_rating, or best_value.");
  const ranked = [...options];
  if (strategy === "lowest_price") {
    const currencies = new Set(ranked
      .map((option) => stayOption(option).price)
      .filter((price) => numericProviderFact(price) !== null)
      .map((price) => providerCurrency(price) ?? "UNKNOWN"));
    if (currencies.size > 1 || currencies.has("UNKNOWN")) throw new Error("Cannot rank hotel prices across different or unknown currencies. Use provider_order and compare currency fields explicitly.");
    ranked.sort((left, right) => (numericProviderFact(stayOption(left).price) ?? Number.POSITIVE_INFINITY) - (numericProviderFact(stayOption(right).price) ?? Number.POSITIVE_INFINITY));
  } else if (strategy === "highest_rating") {
    ranked.sort((left, right) => (numericProviderFact(stayOption(right).review?.rating) ?? Number.NEGATIVE_INFINITY) - (numericProviderFact(stayOption(left).review?.rating) ?? Number.NEGATIVE_INFINITY));
  } else if (strategy === "best_value") {
    const assessments = new Map(options.map((option) => [option.optionId, bestValueAssessment(option, options, preferences, stayNights)]));
    ranked.sort((left, right) => {
      const scoreDifference = (assessments.get(right.optionId)?.score ?? Number.NEGATIVE_INFINITY) - (assessments.get(left.optionId)?.score ?? Number.NEGATIVE_INFINITY);
      if (scoreDifference !== 0) return scoreDifference;
      const ratingDifference = (numericProviderFact(stayOption(right).review?.rating) ?? Number.NEGATIVE_INFINITY) - (numericProviderFact(stayOption(left).review?.rating) ?? Number.NEGATIVE_INFINITY);
      if (ratingDifference !== 0) return ratingDifference;
      const countDifference = (numericProviderFact(stayOption(right).review?.ratingsCount) ?? Number.NEGATIVE_INFINITY) - (numericProviderFact(stayOption(left).review?.ratingsCount) ?? Number.NEGATIVE_INFINITY);
      if (countDifference !== 0) return countDifference;
      const leftPrice = stayOption(left).price;
      const rightPrice = stayOption(right).price;
      const leftCurrency = providerCurrency(leftPrice);
      const rightCurrency = providerCurrency(rightPrice);
      if (!leftCurrency || leftCurrency !== rightCurrency) return 0;
      return (numericProviderFact(leftPrice) ?? Number.POSITIVE_INFINITY) - (numericProviderFact(rightPrice) ?? Number.POSITIVE_INFINITY);
    });
  }
  return ranked;
}

function editDistance(left, right) {
  const previous = Array.from({ length: right.length + 1 }, (_, index) => index);
  for (let i = 1; i <= left.length; i += 1) {
    let diagonal = previous[0];
    previous[0] = i;
    for (let j = 1; j <= right.length; j += 1) {
      const old = previous[j];
      previous[j] = Math.min(previous[j] + 1, previous[j - 1] + 1, diagonal + (left[i - 1] === right[j - 1] ? 0 : 1));
      diagonal = old;
    }
  }
  return previous[right.length];
}

function hotelsByName(hotels, hotelName) {
  if (!hotelName) return { matchMode: null, hotels, suggestions: [] };
  const query = normalizedText(hotelName);
  const exact = hotels.filter((hotel) => normalizedText(optional(hotel, "hotelName")) === query);
  if (exact.length) return { matchMode: "exact", hotels: exact, suggestions: [] };
  const partial = hotels.filter((hotel) => normalizedText(optional(hotel, "hotelName")).includes(query));
  if (partial.length) return { matchMode: "partial", hotels: partial, suggestions: [] };
  const suggestions = [...hotels]
    .filter((hotel) => optional(hotel, "hotelName"))
    .sort((left, right) => editDistance(normalizedText(left.hotelName), query) - editDistance(normalizedText(right.hotelName), query))
    .slice(0, 5)
    .map((hotel) => ({ hotelName: hotel.hotelName, destination: optional(optional(hotel, "areaLocation"), "destinationName") }));
  return { matchMode: "not_found", hotels: [], suggestions };
}

function hotelDeduplicationKey(hotel) {
  const hotelId = optional(hotel, "hotelId");
  if (typeof hotelId === "string" && hotelId) return `id:${hotelId}`;
  return `content:${createHash("sha256").update(JSON.stringify(hotel)).digest("hex")}`;
}

function optionalCount(value) {
  return Number.isInteger(value) && value >= 0 ? value : null;
}

async function collectHotelSearchUncached(searchRequest, ranking, language) {
  const hotelsByKey = new Map();
  const visitedOffsets = new Set([0]);
  const startedAt = Date.now();
  let offset = 0;
  let requestCount = 0;
  let loadingPolls = 0;
  let isLoadingCompleted = false;
  let hotelsTotalCount = null;
  let filteredHotelsCount = null;
  let stoppedReason = null;

  while (requestCount < MAX_SEARCH_REQUESTS) {
    const remainingBudgetMs = SEARCH_COLLECTION_BUDGET_MS - (Date.now() - startedAt);
    if (requestCount > 0 && remainingBudgetMs < MIN_SEARCH_REQUEST_BUDGET_MS) {
      stoppedReason = "time_budget";
      break;
    }
    const payload = {
      ...searchRequest,
      offset,
      limit: SEARCH_PAGE_SIZE,
    };
    requestCount += 1;
    let search;
    try {
      search = await apiRequest("POST", "/api/v1/hotels/search", {
        payload,
        language,
        requestTimeoutMs: Math.max(MIN_SEARCH_REQUEST_BUDGET_MS, remainingBudgetMs),
      });
    } catch (error) {
      if (error.code === "HOTELS_API_TIMEOUT" && hotelsByKey.size > 0) {
        stoppedReason = "time_budget";
        break;
      }
      throw error;
    }
    const page = search.data?.payload;
    const hotels = page?.hotels;
    if (!Array.isArray(hotels)) throw new Error("Hotels API search response does not contain the expected payload.hotels array.");
    for (const hotel of hotels) hotelsByKey.set(hotelDeduplicationKey(hotel), hotel);
    hotelsTotalCount = optionalCount(page.hotelsTotalCount) ?? hotelsTotalCount;
    filteredHotelsCount = optionalCount(page.filteredHotelsCount) ?? filteredHotelsCount;
    isLoadingCompleted = page.isLoadingCompleted !== false;
    const nextOffset = optionalCount(page.nextOffset);

    if (nextOffset !== null && nextOffset > offset) {
      if (visitedOffsets.has(nextOffset)) {
        stoppedReason = "repeated_next_offset";
        break;
      }
      if (requestCount >= MAX_SEARCH_REQUESTS) {
        stoppedReason = "request_limit";
        break;
      }
      visitedOffsets.add(nextOffset);
      offset = nextOffset;
      loadingPolls = 0;
      continue;
    }
    if (isLoadingCompleted) break;
    if (nextOffset !== null && nextOffset < offset) {
      stoppedReason = "repeated_next_offset";
      break;
    }
    if (loadingPolls >= MAX_SEARCH_LOADING_POLLS) {
      stoppedReason = "loading_poll_limit";
      break;
    }
    loadingPolls += 1;
    if (SEARCH_COLLECTION_BUDGET_MS - (Date.now() - startedAt) <= SEARCH_LOADING_POLL_DELAY_MS + MIN_SEARCH_REQUEST_BUDGET_MS) {
      stoppedReason = "time_budget";
      break;
    }
    await new Promise((resolve) => setTimeout(resolve, SEARCH_LOADING_POLL_DELAY_MS));
  }

  if (requestCount >= MAX_SEARCH_REQUESTS && !isLoadingCompleted && stoppedReason === null) stoppedReason = "request_limit";
  const hotels = [...hotelsByKey.values()];
  const expectedCount = filteredHotelsCount ?? hotelsTotalCount;
  const truncated = stoppedReason !== null || !isLoadingCompleted || (expectedCount !== null && hotels.length < expectedCount);
  return {
    hotels,
    metadata: {
      fetchedHotelsCount: hotels.length,
      hotelsTotalCount,
      filteredHotelsCount,
      isLoadingCompleted,
      truncated,
      requestCount,
      providerSort: null,
      rankingAppliedLocally: ranking,
      stoppedReason,
    },
  };
}

function resetSearchCacheForChangedTransport() {
  if (searchCacheTransport === globalThis.fetch) return;
  hotelSearchCacheByKey.clear();
  inFlightHotelSearchByKey.clear();
  searchCacheTransport = globalThis.fetch;
}

function hotelSearchCacheKey(searchRequest, language) {
  const origin = baseUrl().href;
  return createHash("sha256")
    .update(JSON.stringify({ origin, authMode: configuredAuthMode(), searchRequest, language: language ?? null }))
    .digest("hex");
}

function collectedSearchCopy(search, cacheStatus, ranking) {
  return {
    hotels: structuredClone(search.hotels),
    metadata: { ...search.metadata, rankingAppliedLocally: ranking, cacheStatus },
  };
}

async function collectHotelSearch(searchRequest, ranking, language) {
  resetSearchCacheForChangedTransport();
  const key = hotelSearchCacheKey(searchRequest, language);
  const now = Date.now();
  const cached = hotelSearchCacheByKey.get(key);
  if (cached?.expiresAt > now) return collectedSearchCopy(cached.search, "hit", ranking);
  if (cached) hotelSearchCacheByKey.delete(key);

  const inFlight = inFlightHotelSearchByKey.get(key);
  if (inFlight) return collectedSearchCopy(await inFlight, "coalesced", ranking);

  const pending = collectHotelSearchUncached(searchRequest, ranking, language);
  inFlightHotelSearchByKey.set(key, pending);
  try {
    const search = await pending;
    storeBounded(hotelSearchCacheByKey, key, {
      expiresAt: Date.now() + SEARCH_CACHE_TTL_MS,
      search: collectedSearchCopy(search, "stored", ranking),
    }, MAX_SEARCH_CACHE_ENTRIES);
    return collectedSearchCopy(search, "miss", ranking);
  } finally {
    inFlightHotelSearchByKey.delete(key);
  }
}

async function planStay(args) {
  const input = validatedPlanInput(args);
  let destination = input.destinationId ? { destinationId: input.destinationId, name: input.destination || null, countryName: input.countryName } : null;
  if (!destination) {
    let resolution;
    try {
      resolution = await resolveDestination({ query: input.destination, countryName: input.countryName, maxCandidates: 5 });
    } catch (error) {
      if (providerConditionFailure(error)) return staySearchUnavailable(error, input);
      throw error;
    }
    if (resolution.status !== "resolved") {
      return {
        status: "clarification_required",
        reason: resolution.status === "ambiguous" ? "destination_ambiguous" : "destination_not_found",
        destinationQuery: input.destination,
        destinationCandidates: resolution.candidates,
        retryAllowed: false,
        automaticResolutionFallbackAllowed: false,
        nextStep: resolution.status === "ambiguous" ? "Ask the user to choose a candidate, then call plan_stay again with destinationId. Do not call resolve_destination automatically." : "Ask the user to clarify the city or country. Do not retry with translated or expanded location names automatically.",
      };
    }
    destination = { ...resolution.destination, countryCatalogFallbackApplied: resolution.countryCatalogFallbackApplied };
  }
  const searchRequest = {
    destinationId: destination.destinationId,
    checkinDate: input.checkinDate,
    checkoutDate: input.checkoutDate,
    guests: input.rooms.map((room) => ({ adultsCount: room.adults, childrenAge: room.childrenAges })),
    filters: input.breakfastIncluded
      ? [{ $objectType: "array", filterId: "meal_types", values: ["breakfast"] }]
      : [],
  };
  let search;
  try {
    search = await collectHotelSearch(searchRequest, input.ranking, input.language);
  } catch (error) {
    if (input.breakfastIncluded && providerConditionFailure(error)) {
      return providerConditionUnavailable(error, requiredStayConditions(input));
    }
    if (providerConditionFailure(error)) return staySearchUnavailable(error, input);
    throw error;
  }
  const hotels = search.hotels;
  if (input.breakfastIncluded && hotels.length === 0) {
    return {
      status: "no_matching_stays",
      reason: "no_hotels_matched_required_conditions",
      resolvedDestination: destination,
      requiredConditions: requiredStayConditions(input),
      conditionsApplied: appliedStayConditions(input),
      searchCoverage: search.metadata,
      retryAllowed: false,
      lowLevelFallbackAllowed: false,
      nextStep: "Report that no matching hotels were returned. Do not remove the breakfast requirement or run low-level filter experiments unless the user explicitly changes the request.",
    };
  }
  const nameMatch = hotelsByName(hotels, input.hotelName);
  if (nameMatch.matchMode === "not_found") {
    return {
      status: "no_matching_hotel",
      resolvedDestination: destination,
      hotelNameQuery: input.hotelName,
      searchedHotelsCount: hotels.length,
      searchCoverage: search.metadata,
      requiredConditions: requiredStayConditions(input),
      conditionsApplied: appliedStayConditions(input),
      suggestions: nameMatch.suggestions,
      nextStep: "Ask the user to confirm one suggested provider hotel name or search the location without hotelName.",
    };
  }
  const journeyId = randomUUID();
  const expiresAt = Date.now() + JOURNEY_TTL_MS;
  const options = nameMatch.hotels.map((hotel) => ({ optionId: randomUUID(), hotel }));
  storeBounded(journeysById, journeyId, { expiresAt, searchRequest, searchMetadata: search.metadata, planInput: input, destination, language: input.language, options, selectedOptionId: null, rateOptions: [], selectedRateOptionId: null }, MAX_ACTIVE_JOURNEYS);
  const stayNights = stayNightCount(input.checkinDate, input.checkoutDate);
  const displayedOptions = rankedOptions(options, input.ranking, input.hotelPreferences, stayNights).slice(0, input.maxOptions);
  return {
    status: "ready",
    journeyId,
    expiresAt: new Date(expiresAt).toISOString(),
    resolvedDestination: destination,
    hotelNameMatch: nameMatch.matchMode,
    ranking: input.ranking,
    preferencesApplied: appliedHotelPreferences(input),
    requiredConditions: requiredStayConditions(input),
    conditionsApplied: appliedStayConditions(input),
    totalOptions: options.length,
    returnedOptions: displayedOptions.length,
    stayNights,
    searchCoverage: search.metadata,
    options: displayedOptions.map((option) => presentedStayOption(option, options, input.hotelPreferences, input.ranking, stayNights)),
    note: "Контекст хранится только в текущем MCP-процессе до expiresAt и не содержит токен или auth headers. Provider shownPrice — полная цена поездки; pricePerNightDisplay вычислен MCP делением на stayNights. preferencesApplied.applied=true — единственное основание утверждать, что Banking-профиль применён. conditionsApplied подтверждает provider-фильтр ко всей journey; утверждайте включение завтрака в показанную цену только при displayedPriceBreakfastEvidence=confirmed_by_meal_name.",
  };
}

function getStayOptions(args) {
  const journey = journeyById(args.journeyId);
  const ranking = args.ranking ?? journey.planInput.ranking ?? "provider_order";
  const limit = boundedInteger(args.limit, "limit", DEFAULT_PLAN_OPTIONS, 1, MAX_PLAN_OPTIONS);
  const stayNights = stayNightCount(journey.planInput.checkinDate, journey.planInput.checkoutDate);
  const options = rankedOptions(journey.options, ranking, journey.planInput.hotelPreferences, stayNights).slice(0, limit);
  return {
    journeyId: args.journeyId,
    expiresAt: new Date(journey.expiresAt).toISOString(),
    selectedOptionId: journey.selectedOptionId,
    ranking,
    preferencesApplied: appliedHotelPreferences(journey.planInput),
    totalOptions: journey.options.length,
    returnedOptions: options.length,
    stayNights,
    searchCoverage: journey.searchMetadata,
    requiredConditions: requiredStayConditions(journey.planInput),
    conditionsApplied: appliedStayConditions(journey.planInput),
    options: options.map((option) => presentedStayOption(option, journey.options, journey.planInput.hotelPreferences, ranking, stayNights)),
  };
}

function selectedJourneyOption(journey, optionId) {
  const option = journey.options.find((candidate) => candidate.optionId === optionId);
  if (!option) throw new Error("optionId is not part of this journey.");
  return option;
}

function compareStayOptions(args) {
  assertOnlyKeys(args, ["journeyId", "optionIds", "scope", "ranking", "limit"], "arguments");
  if (args.scope !== undefined && !["previous_comparison", "all_journey_options"].includes(args.scope)) {
    throw new Error("scope must be previous_comparison or all_journey_options.");
  }
  if (args.optionIds !== undefined && args.scope !== undefined) {
    throw new Error("Pass either optionIds or scope, not both.");
  }
  const journey = journeyById(args.journeyId);
  const stayNights = stayNightCount(journey.planInput.checkinDate, journey.planInput.checkoutDate);
  const previousComparison = journey.lastComparison ?? null;
  let eligibleOptions;
  let selected;
  let selectionScope;
  let selectionStrategy;
  let basedOnComparisonId = null;
  if (args.optionIds !== undefined) {
    if (!Array.isArray(args.optionIds) || args.optionIds.length < 2 || args.optionIds.length > 5 || new Set(args.optionIds).size !== args.optionIds.length) {
      throw new Error("optionIds must contain 2 to 5 distinct optionIds.");
    }
    eligibleOptions = args.optionIds.map((optionId) => selectedJourneyOption(journey, optionId));
    selectionScope = "explicit_options";
    selectionStrategy = args.ranking ?? "explicit_order";
    selected = args.ranking === undefined
      ? eligibleOptions
      : rankedOptions(eligibleOptions, args.ranking, journey.planInput.hotelPreferences, stayNights);
  } else {
    const ranking = args.ranking ?? journey.planInput.ranking ?? "provider_order";
    const limit = boundedInteger(args.limit, "limit", 5, 2, 5);
    const usePreviousComparison = args.scope === "previous_comparison" || (args.scope === undefined && previousComparison !== null);
    if (usePreviousComparison) {
      if (!previousComparison) throw new Error("scope=previous_comparison requires an earlier tbank_hotels_compare_stay_options result in this journey.");
      eligibleOptions = previousComparison.optionIds.map((optionId) => selectedJourneyOption(journey, optionId));
      selectionScope = "previous_comparison";
      basedOnComparisonId = previousComparison.comparisonId;
    } else {
      eligibleOptions = journey.options;
      selectionScope = "all_journey_options";
    }
    selected = rankedOptions(eligibleOptions, ranking, journey.planInput.hotelPreferences, stayNights).slice(0, limit);
    selectionStrategy = ranking;
    if (selected.length < 2) throw new Error("At least two stay options are required for comparison.");
  }
  const comparisonId = randomUUID();
  journey.lastComparison = { comparisonId, optionIds: selected.map((option) => option.optionId) };
  const comparisonRows = selected.map((option) => stayComparisonRow(option, eligibleOptions, journey.planInput.hotelPreferences, selectionStrategy === "best_value", stayNights));
  return {
    journeyId: args.journeyId,
    comparisonId,
    basedOnComparisonId,
    selectionStrategy,
    selectionScope,
    eligibleOptionCount: eligibleOptions.length,
    stayNights,
    searchCoverage: journey.searchMetadata,
    requiredConditions: requiredStayConditions(journey.planInput),
    conditionsApplied: appliedStayConditions(journey.planInput),
    preferencesApplied: appliedHotelPreferences(journey.planInput),
    comparison: selected.map((option) => stayOption(option, stayNights)),
    comparisonRows,
    comparisonTableMarkdown: comparisonTableMarkdown(comparisonRows),
    preferenceAlternatives: preferenceAlternatives(journey.options, selected.map((option) => option.optionId), journey.planInput.hotelPreferences, stayNights),
    presentationGuidance: {
      source: "Copy comparisonTableMarkdown into the user-facing answer and explain it from comparisonRows.",
      scope: "Use only hotels in comparisonRows unless the user explicitly asks for alternatives.",
      continuation: "For a follow-up phrased as among these or previously shown options, preserve selectionScope=previous_comparison. Use all_journey_options only on an explicit request to reconsider the full search result.",
      fields: ["hotelName", "destination", "starRating", "reviewRating", "ratingsCount", "totalPriceDisplay", "pricePerNightDisplay", "freeCancellationUntilDisplay", "mealName", "displayedPriceBreakfastEvidence", "bestValueScore", "pricePreferenceFit"],
      breakfastFacts: "confirmed_by_meal_name means included in the displayed price; excluded_by_meal_name means explicitly excluded; not_confirmed_for_displayed_price means unknown for that price.",
      factIntegrity: "Copy totalPriceDisplay, pricePerNightDisplay and freeCancellationUntilDisplay exactly. Provider shownPrice is totalPriceDisplay for the full stay; pricePerNightDisplay is MCP-derived from total/stayNights. Never compare a total stay price with a per-night preference and never call UTC a hotel-local time. bestValueScore is MCP-derived, not a provider fact. reviewRating is a review score, while starRating is the hotel category: never append a star-category symbol to reviewRating. Missing starRating means no confirmed category; do not infer hotel format, quality class or amenities from its name.",
    },
    note: "null означает, что соответствующий provider fact отсутствует или не был однозначно извлечён из search response. Используйте displayedPriceBreakfastEvidence как трёхсостоянийное доказательство для показанной цены; conditionsApplied относится только к отбору journey.",
  };
}

function selectStayOption(args) {
  const journey = journeyById(args.journeyId);
  const option = selectedJourneyOption(journey, args.optionId);
  const stayNights = stayNightCount(journey.planInput.checkinDate, journey.planInput.checkoutDate);
  journey.selectedOptionId = option.optionId;
  return {
    journeyId: args.journeyId,
    selectedOption: presentedStayOption(option, journey.options, journey.planInput.hotelPreferences, journey.planInput.ranking, stayNights),
    nextStep: "Use tbank_hotels_get_selected_stay_rates to inspect current rooms and rates before creating a booking draft.",
  };
}

async function selectedStayRates(args) {
  const journey = journeyById(args.journeyId);
  if (!journey.selectedOptionId) throw new Error("Select one stay option before requesting rates.");
  const option = selectedJourneyOption(journey, journey.selectedOptionId);
  const hotelId = optional(option.hotel, "hotelId");
  if (typeof hotelId !== "string" || !hotelId) throw new Error("Selected option does not contain a usable provider hotelId.");
  const apiVersion = version(args, "v3", ["v2", "v3"]);
  if (args.filters !== undefined && !Array.isArray(args.filters)) throw new Error("filters must be an array when provided.");
  const ratesRequest = {
    checkInDate: journey.searchRequest.checkinDate,
    checkOutDate: journey.searchRequest.checkoutDate,
    guests: journey.searchRequest.guests,
    ...(args.filters === undefined ? {} : { filters: args.filters }),
  };
  const path = `/api/${apiVersion}/hotels/${value(hotelId, "hotelId")}/rates`;
  const startedAt = Date.now();
  const timeoutResult = (attempts) => {
    journey.rateOptions = [];
    journey.selectedRateOptionId = null;
    return {
      status: "rates_temporarily_unavailable",
      journeyId: args.journeyId,
      selectedOption: stayOption(option),
      rateOptions: [],
      canCreateBookingDraft: false,
      otherRatesCount: null,
      roomsCount: null,
      sourceStatus: null,
      attempts,
      durationMs: Math.max(0, Date.now() - startedAt),
      failureKind: "timeout",
      nextStep: "The internal timeout retry budget is exhausted. Do not repeat the same tool call automatically; tell the user rates are temporarily unavailable and offer another hotel or a later explicit retry.",
      note: "No booking rate was selected and the search-feed price cannot be used for a booking preview or draft.",
    };
  };
  let attempts = 1;
  let result;
  try {
    result = await apiRequest("POST", path, {
      payload: ratesRequest,
      language: args.language,
      requestTimeoutMs: RATES_FIRST_ATTEMPT_MS,
    });
  } catch (error) {
    const remainingMs = RATES_REQUEST_BUDGET_MS - (Date.now() - startedAt);
    if (error.code !== "HOTELS_API_TIMEOUT") throw error;
    if (remainingMs < MIN_SEARCH_REQUEST_BUDGET_MS) return timeoutResult(attempts);
    attempts = 2;
    try {
      result = await apiRequest("POST", path, {
        payload: ratesRequest,
        language: args.language,
        requestTimeoutMs: remainingMs,
      });
    } catch (retryError) {
      if (retryError.code === "HOTELS_API_TIMEOUT") return timeoutResult(attempts);
      throw retryError;
    }
  }
  const rates = result.data?.payload?.rates;
  if (!Array.isArray(rates)) throw new Error("Hotels API rates response does not contain the expected payload.rates array.");
  journey.rateOptions = rates.map((rate, index) => ({ rateOptionId: randomUUID(), rateNumber: index + 1, rate }));
  journey.selectedRateOptionId = null;
  const canCreateBookingDraft = journey.rateOptions.length > 0;
  const otherRates = result.data?.payload?.otherRates;
  const rooms = result.data?.payload?.rooms;
  const stayNights = stayNightCount(journey.planInput.checkinDate, journey.planInput.checkoutDate);
  const presentationRows = ratePresentationRows(journey.rateOptions, stayNights);
  return {
    status: canCreateBookingDraft ? "ready" : "no_bookable_rates",
    journeyId: args.journeyId,
    stayNights,
    selectedOption: presentedStayOption(option, journey.options, journey.planInput.hotelPreferences, journey.planInput.ranking, stayNights),
    rateOptions: journey.rateOptions.map((rateOption) => stayRateOption(rateOption, stayNights)),
    ratePresentationRows: presentationRows,
    ratesTableMarkdown: ratesTableMarkdown(presentationRows),
    presentationGuidance: {
      table: "Show ratesTableMarkdown exactly once and preserve its stable rate numbers. Do not renumber a breakfast-only, refundable-only or other subset.",
      selection: "When continuing to preview in the same user turn, select the matching rateOptionId by its original rateNumber, finish select_stay_rate and create_booking_preview, then give one consolidated answer instead of a preliminary table plus a second table.",
      facts: "Copy price, meal and cancellation facts from the original row. cancellationDisplay=нет данных must remain unknown.",
    },
    canCreateBookingDraft,
    otherRatesCount: Array.isArray(otherRates) ? otherRates.length : null,
    roomsCount: Array.isArray(rooms) ? rooms.length : null,
    sourceStatus: result.status,
    attempts,
    durationMs: Math.max(0, Date.now() - startedAt),
    failureKind: null,
    nextStep: canCreateBookingDraft
      ? "If the user has not provided selection criteria, ask them to choose one stable rateNumber. If their request already defines how to choose, select the matching rateOptionId, call preview, and present ratesTableMarkdown only once in the final answer. Do not call preview concurrently with rates."
      : "No selectable rateOption/bookHash is available. Do not request guest personal data and do not create a booking draft; choose another hotel or retry rates later.",
    note: canCreateBookingDraft
      ? "Only returned rateOptions are selectable for booking. Copy ratesTableMarkdown for user-facing facts; cancellationDisplay=нет данных must not be rewritten as non-refundable or free cancellation."
      : "The search-feed price is informational and cannot be used as a booking rate without a provider rateOption/bookHash.",
  };
}

function selectedStayRate(args) {
  const journey = journeyById(args.journeyId);
  if (!journey.selectedOptionId) throw new Error("Select one stay option before selecting a rate.");
  const rateOption = journey.rateOptions.find((candidate) => candidate.rateOptionId === args.rateOptionId);
  if (!rateOption) throw new Error("rateOptionId is not part of this journey. Load current rates again.");
  journey.selectedRateOptionId = rateOption.rateOptionId;
  const stayNights = stayNightCount(journey.planInput.checkinDate, journey.planInput.checkoutDate);
  const executionReadiness = mutationExecutionReadiness("booking");
  return {
    journeyId: args.journeyId,
    stayNights,
    selectedRate: stayRateOption(rateOption, stayNights),
    selectedRateNumber: rateOption.rateNumber,
    executionAvailable: executionReadiness.available,
    executionReadiness: journeyExecutionReadiness(executionReadiness),
    nextStep: executionReadiness.available
      ? "For a preview without personal data, call tbank_hotels_create_booking_preview. Collect guest PII and call tbank_hotels_create_booking_draft only after the user explicitly chooses real booking."
      : "Call tbank_hotels_create_booking_preview without requesting guest PII. Execution is unavailable, so do not create a booking draft or ask for final confirmation.",
  };
}

function createBookingPreview(args) {
  const journey = journeyById(args.journeyId);
  if (!journey.selectedOptionId) throw new Error("Select one stay option before creating a booking preview.");
  if (!journey.selectedRateOptionId) throw new Error("Select one current rate before creating a booking preview.");
  const rateOption = journey.rateOptions.find((candidate) => candidate.rateOptionId === journey.selectedRateOptionId);
  if (!rateOption) throw new Error("Selected rate is no longer part of this journey. Load current rates again.");
  const bookHash = optional(rateOption.rate, "bookHash");
  if (typeof bookHash !== "string" || !bookHash) throw new Error("Selected rate does not contain a usable bookHash.");
  const stayNights = stayNightCount(journey.planInput.checkinDate, journey.planInput.checkoutDate);
  const executionReadiness = mutationExecutionReadiness("booking");
  return {
    status: "preview_only",
    journeyId: args.journeyId,
    executionAvailable: executionReadiness.available,
    executionReadiness: journeyExecutionReadiness(executionReadiness),
    stayNights,
    selectedStay: presentedStayOption(selectedJourneyOption(journey, journey.selectedOptionId), journey.options, journey.planInput.hotelPreferences, journey.planInput.ranking, stayNights),
    selectedRate: stayRateOption(rateOption, stayNights),
    selectedRateNumber: rateOption.rateNumber,
    occupancy: journey.searchRequest.guests.map((guest, roomIndex) => ({
      roomIndex,
      adults: guest.adultsCount,
      childrenAges: guest.childrenAge ?? [],
    })),
    personalDataCollected: false,
    httpRequestPerformed: false,
    bookingDataRequiredForExecution: ["guestContact.email", "guestContact.phone", "rooms[].guests[].firstName", "rooms[].guests[].lastName"],
    nextStep: executionReadiness.available
      ? "Show this preview without requesting personal data. Only if the user explicitly asks to make a real booking, collect required guest data and create a booking draft."
      : "Show this preview and stop. Do not request guest personal data or final confirmation while booking execution is unavailable.",
    note: "Локальный preview не резервирует номер, не проверяет checkout и не выполняет HTTP-запрос к Hotels API.",
  };
}

function createPaymentFormPreview(args) {
  const bookingPreview = createBookingPreview(args);
  const readiness = paymentFormExecutionReadiness();
  return {
    status: "preview_only",
    journeyId: bookingPreview.journeyId,
    stayNights: bookingPreview.stayNights,
    selectedStay: bookingPreview.selectedStay,
    selectedRate: bookingPreview.selectedRate,
    occupancy: bookingPreview.occupancy,
    paymentFlow: {
      type: "hosted_payment_form",
      createEndpoint: "/api/v1/hotels/bookings/prepay_task/pf/create",
      statusEndpointTemplate: "/api/v1/hotels/bookings/payment_tasks/{taskId}/status",
      documentedStates: ["in_progress", "give_card_data", "waiting_booking", "failed", "completed", "card_replacement"],
      lifecycleSemanticsVerified: false,
      providerReturns: ["taskId", "orderId", "paymentUrl"],
      paymentUrlIncluded: false,
    },
    executionAvailable: false,
    executionReadiness: { available: false, status: "not_available" },
    contractEvidence: {
      contractVersion: readiness.contractVersion,
      requestVerifiedOffline: readiness.requestContractVerifiedOffline,
      responseVerifiedOffline: readiness.responseContractVerifiedOffline,
      taskStatesVerifiedOffline: readiness.taskStateContractVerifiedOffline,
      externalBlockerCount: readiness.externalBlockers.length,
    },
    personalDataCollected: false,
    paymentCredentialsCollected: false,
    httpRequestPerformed: false,
    excludedFromMcp: ["pan", "card_expiry", "cvv_cvc", "pin", "otp", "3ds_challenge_data", "browser_fingerprint"],
    dataRequiredOnlyAfterExecutionApproval: [
      "guestContact.email",
      "guestContact.phone",
      "rooms[].guests[].firstName",
      "rooms[].guests[].lastName",
    ],
    nextStep: "Show the preview and stop. Do not request personal data, payment credentials or final confirmation until every external payment execution gate is closed outside the model conversation.",
    note: "Hosted payment form is the intended public payment boundary. Raw-card and 3-D Secure provider endpoints are deliberately not exposed as MCP tools.",
  };
}

function createCheckoutHandoff(args) {
  const bookingPreview = createBookingPreview(args);
  const journey = journeyById(args.journeyId);
  const selectedOption = selectedJourneyOption(journey, journey.selectedOptionId);
  const target = hostedCheckoutTarget(optional(selectedOption.hotel, "hotelId"), {
    checkinDate: journey.planInput.checkinDate,
    checkoutDate: journey.planInput.checkoutDate,
    occupancy: bookingPreview.occupancy,
  });
  return {
    status: "ready",
    journeyId: bookingPreview.journeyId,
    handoffMode: "hosted_checkout",
    hostedCheckoutUrl: target.url,
    hostedCheckoutUrlSource: target.source,
    selectedStay: bookingPreview.selectedStay,
    selectedRate: bookingPreview.selectedRate,
    occupancy: bookingPreview.occupancy,
    stayNights: bookingPreview.stayNights,
    selectionPreserved: target.selectionPreserved,
    selectionPreservationScope: target.selectionPreserved ? "selected_hotel_page_with_safe_search_context" : "none",
    searchCriteriaPreserved: target.searchCriteriaPreserved,
    searchCriteriaPreservationScope: target.searchCriteriaPreservationScope,
    datesPreserved: target.datesPreserved,
    guestCountPreserved: target.guestCountPreserved,
    roomCompositionPreserved: target.roomCompositionPreserved,
    childrenAgesPreserved: target.childrenAgesPreserved,
    exactRatePreserved: false,
    exactRateHandoffStatus: "not_supported_by_verified_public_contract",
    personalDataIncluded: false,
    paymentCredentialsIncluded: false,
    bookingCreated: false,
    paymentStarted: false,
    httpRequestPerformed: false,
    userActionRequired: true,
    nextStep: "Show the selected stay and stable rateNumber summary, then offer the hostedCheckoutUrl. The URL opens the selected hotel and preserves dates plus guest count only when the corresponding preservation fields are true. The user still selects a current rate, enters guest data and pays only in the trusted external interface. Do not claim that the exact MCP rate is reserved or transferred.",
    note: "This handoff transfers no token, bookHash, guest identity, card data, payment state, exact rate or reservation. The official public page accepts dateFrom/dateTo and a simple one-room adults-only guests count; complex room/child composition is deliberately not encoded. Availability and price must always be confirmed again externally.",
  };
}

function bookingGuestCoverage(journey, bookingPayload) {
  const rooms = journey.searchRequest.guests.map((requested, index) => {
    const namedGuests = bookingPayload.rooms[index]?.guests ?? [];
    const namedChildrenAges = namedGuests.filter((guest) => Number.isInteger(guest.childAge)).map((guest) => guest.childAge);
    return {
      roomIndex: index,
      requestedAdults: requested.adultsCount,
      requestedChildrenAges: requested.childrenAge ?? [],
      namedAdults: namedGuests.filter((guest) => !Number.isInteger(guest.childAge)).length,
      namedChildrenAges,
    };
  });
  const roomCountMatches = bookingPayload.rooms.length === journey.searchRequest.guests.length;
  const namedGuestCountMatches = roomCountMatches && rooms.every((room) => (
    room.namedAdults === room.requestedAdults
    && JSON.stringify([...room.namedChildrenAges].sort((a, b) => a - b)) === JSON.stringify([...room.requestedChildrenAges].sort((a, b) => a - b))
  ));
  return {
    roomCountMatches,
    namedGuestCountMatches,
    rooms,
    note: namedGuestCountMatches
      ? "Named guests match the searched occupancy."
      : "Named guests do not match the searched occupancy. OpenAPI describes rooms.guests as residents but does not declare an exact cross-field count constraint; review before execution.",
  };
}

function createBookingDraft(args) {
  const journey = journeyById(args.journeyId);
  if (!journey.selectedRateOptionId) throw new Error("Select one current rate before creating a booking draft.");
  const executionReadiness = mutationExecutionReadiness("booking");
  if (!executionReadiness.available) {
    return {
      status: "execution_unavailable",
      bookingDraftCreated: false,
      personalDataStored: false,
      executionAvailable: false,
      executionReadiness: journeyExecutionReadiness(executionReadiness),
      nextStep: "Use tbank_hotels_create_booking_preview and stop. Do not submit or retain guest personal data while booking execution is unavailable.",
      note: "bookingData не разобраны, не сохранены и не возвращены; bookingDraftId не создан.",
    };
  }
  requestObject(args.bookingData, "bookingData");
  if (Object.hasOwn(args.bookingData, "bookHash")) throw new Error("bookingData must not contain bookHash; it is bound to the selected rate.");
  validatedBookingPayload(args.bookingData, { requireBookHash: false, name: "bookingData" });
  const rateOption = journey.rateOptions.find((candidate) => candidate.rateOptionId === journey.selectedRateOptionId);
  const bookHash = optional(rateOption?.rate, "bookHash");
  if (typeof bookHash !== "string" || !bookHash) throw new Error("Selected rate does not contain a usable bookHash.");
  const bookingDraftId = randomUUID();
  const bookingPayload = { ...args.bookingData, bookHash };
  const expiresAt = Date.now() + BOOKING_DRAFT_TTL_MS;
  const stayNights = stayNightCount(journey.planInput.checkinDate, journey.planInput.checkoutDate);
  storeBounded(bookingDraftsById, bookingDraftId, {
    expiresAt,
    bookingPayload,
    journeyId: args.journeyId,
    rateOption,
    stayNights,
    confirmationState: "ready",
  }, MAX_ACTIVE_BOOKING_DRAFTS);
  return {
    bookingDraftId,
    expiresAt: new Date(expiresAt).toISOString(),
    executionAvailable: true,
    executionReadiness,
    stayNights,
    selectedStay: presentedStayOption(selectedJourneyOption(journey, journey.selectedOptionId), journey.options, journey.planInput.hotelPreferences, journey.planInput.ranking, stayNights),
    selectedRate: stayRateOption(rateOption, stayNights),
    guestCoverage: bookingGuestCoverage(journey, bookingPayload),
    bookingPreview: redactPreview(bookingPayload),
    nextStep: "Call tbank_hotels_validate_checkout immediately before asking for the final booking confirmation.",
  };
}

async function validateCheckout(args) {
  const draft = bookingDraftById(args.bookingDraftId);
  const bookHash = value(draft.bookingPayload.bookHash, "bookHash");
  const path = `/api/v3/rates/${bookHash}`;
  const startedAt = Date.now();
  let attempts = 1;
  let result;
  try {
    result = await apiRequest("GET", path, { requestTimeoutMs: CHECKOUT_FIRST_ATTEMPT_MS });
  } catch (error) {
    const remainingMs = CHECKOUT_REQUEST_BUDGET_MS - (Date.now() - startedAt);
    if (error.code !== "HOTELS_API_TIMEOUT" || remainingMs < MIN_SEARCH_REQUEST_BUDGET_MS) throw error;
    attempts = 2;
    result = await apiRequest("GET", path, { requestTimeoutMs: remainingMs });
  }
  draft.validationExpiresAt = Date.now() + CHECKOUT_VALIDATION_TTL_MS;
  return {
    bookingDraftId: args.bookingDraftId,
    attempts,
    validatedUntil: new Date(draft.validationExpiresAt).toISOString(),
    selectedRate: stayRateOption(draft.rateOption, draft.stayNights ?? 1),
    checkout: result,
    note: "Provider checkout response is authoritative. Review it with the user before preparing final confirmation.",
  };
}

function preparedDraftBooking(args) {
  const draft = bookingDraftById(args.bookingDraftId);
  if (draft.confirmationState === "confirming") throw new Error("Booking confirmation is already in progress for this draft.");
  if (draft.confirmationState === "outcome_unknown") throw new Error("The previous booking confirmation outcome is unknown. Do not retry creation; reconcile the provider task or order status outside this draft.");
  if (!draft.validationExpiresAt || draft.validationExpiresAt <= Date.now()) throw new Error("Checkout validation is required and must be fresh before booking confirmation.");
  const path = "/api/v1/hotels/bookings/tasks/create";
  const executionReadiness = mutationExecutionReadiness("booking");
  if (!executionReadiness.available) {
    return {
      status: "preview_only",
      bookingDraftId: args.bookingDraftId,
      executionAvailable: false,
      executionReadiness: journeyExecutionReadiness(executionReadiness),
      endpoint: path,
      payloadPreview: redactPreview(draft.bookingPayload),
      nextStep: "Execution is unavailable in this configuration. Do not ask the user for final execution confirmation; an integration owner must configure and approve the execution profile outside the model conversation.",
      note: "HTTP-запрос не выполнен; confirmation и requestHash намеренно не выданы.",
    };
  }
  const window = preparationWindow();
  const hash = requestHash("booking", path, { payload: draft.bookingPayload, ...window });
  return {
    bookingDraftId: args.bookingDraftId,
    executionAvailable: true,
    executionReadiness,
    requestHash: hash,
    confirmation: confirmationPhrase("booking", hash),
    ...window,
    endpoint: path,
    payloadPreview: redactPreview(draft.bookingPayload),
    note: "HTTP-запрос не выполнен. Получите явное подтверждение пользователя непосредственно перед confirm_booking.",
  };
}

async function confirmBooking(args) {
  requireMutationExecutionReady("booking");
  const draft = bookingDraftById(args.bookingDraftId);
  if (draft.confirmationState === "confirming") throw new Error("Booking confirmation is already in progress for this draft.");
  if (draft.confirmationState === "outcome_unknown") throw new Error("The previous booking confirmation outcome is unknown. Do not retry creation; reconcile the provider task or order status outside this draft.");
  if (!draft.validationExpiresAt || draft.validationExpiresAt <= Date.now()) throw new Error("Checkout validation is required and must be fresh before booking confirmation.");
  validatePreparationWindow(args);
  const path = "/api/v1/hotels/bookings/tasks/create";
  const hash = requestHash("booking", path, { payload: draft.bookingPayload, preparedAt: args.preparedAt, expiresAt: args.expiresAt });
  if (args.preparedRequestHash !== hash) throw new Error("preparedRequestHash does not match this booking draft.");
  if (args.confirmation !== confirmationPhrase("booking", hash)) throw new Error("confirmation must exactly match the phrase returned by tbank_hotels_prepare_draft_booking.");
  draft.confirmationState = "confirming";
  try {
    const result = await apiRequest("POST", path, { payload: draft.bookingPayload });
    bookingDraftsById.delete(args.bookingDraftId);
    return result;
  } catch (error) {
    if (error.code === "HOTELS_API_TIMEOUT" || error.code === "HOTELS_API_NETWORK" || (error.httpStatus ?? 0) >= 500) {
      draft.confirmationState = "outcome_unknown";
    } else {
      draft.confirmationState = "ready";
    }
    throw error;
  }
}

async function readExistingBooking(args) {
  const apiVersion = version(args, authBrokerSocket() ? "v1" : "v3", ["v1", "v2", "v3"]);
  if (authBrokerSocket()) {
    if (apiVersion !== "v1") throw new Error("Mobile auth broker supports existing booking reads only with apiVersion=v1.");
    if (args.orderId !== undefined) throw new Error("Mobile auth broker mode does not accept provider orderId. Use bookingRef from tbank_hotels_list_bookings.");
    const bookingRef = args.bookingRef;
    const orderId = orderIdForBookingReference(bookingRef);
    const result = await authBrokerRequest("hotels.get_booking_v1", { bookingId: orderId });
    return bookingWithReference(result.booking, bookingRef);
  }
  if (args.bookingRef !== undefined) throw new Error("bookingRef is available only with the mobile auth broker. Direct API profiles require orderId.");
  return apiRequest("GET", `/api/${apiVersion}/hotels/bookings/${value(args.orderId, "orderId")}`, args);
}

async function bookingOverview(args) {
  const booking = await readExistingBooking(args);
  if (args.includeVoucher !== true) return { booking, voucher: { requested: false } };
  return {
    booking,
    voucher: {
      requested: true,
      documentContentIncluded: false,
      separateHandoffRequired: true,
      availableViaTool: authBrokerSocket() ? "tbank_hotels_save_voucher" : null,
      note: "Binary voucher is never fetched or embedded by booking_overview. Use the local broker handoff only after an explicit user request.",
    },
  };
}

async function previewCancellation(args) {
  const booking = await readExistingBooking(args);
  return { booking, note: "MCP не вычисляет сумму возврата. Используйте только фактически возвращённые provider условия, затем tbank_hotels_prepare_cancel_booking при явном намерении отменить." };
}

async function repeatStayPlan(args) {
  const journey = journeyById(args.journeyId);
  return planStay({ ...journey.planInput, destinationId: journey.destination.destinationId, checkinDate: args.checkinDate, checkoutDate: args.checkoutDate });
}

function value(value, name) {
  if (typeof value !== "string" || !value.trim()) throw new Error(`${name} must be a non-empty string.`);
  if (value.length > 512 || value.includes("/") || value.includes("?")) throw new Error(`${name} contains unsupported path characters.`);
  return encodeURIComponent(value);
}

function brokerIdentifier(identifier, name) {
  if (typeof identifier !== "string" || !/^[A-Za-z0-9_-]{1,128}$/.test(identifier)) {
    throw new Error(`${name} contains unsupported characters.`);
  }
  return identifier;
}

function requestObject(value, name = "payload") {
  if (!value || Array.isArray(value) || typeof value !== "object") throw new Error(`${name} must be an object.`);
  return value;
}

function requireNonEmptyString(value, name) {
  if (typeof value !== "string" || !value.trim()) throw new Error(`${name} must be a non-empty string.`);
}

function optionalNullableString(value, name) {
  if (value !== undefined && value !== null && typeof value !== "string") throw new Error(`${name} must be a string or null.`);
}

function optionalNullableBoolean(value, name) {
  if (value !== undefined && value !== null && typeof value !== "boolean") throw new Error(`${name} must be a boolean or null.`);
}

function optionalNullableUuid(value, name) {
  optionalNullableString(value, name);
  if (value !== undefined && value !== null && !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value)) {
    throw new Error(`${name} must be a UUID or null.`);
  }
}

function validatedBookingPayload(payloadValue, { requireBookHash = true, ls = false, name = "payload" } = {}) {
  const body = requestObject(payloadValue, name);
  const commonKeys = ["checkOutId", "guestContact", "rooms", "contactData", "arrivalTime", "promocode", "extraServices", "isBusinessTrip"];
  const allowed = [...(requireBookHash ? ["bookHash"] : []), ...commonKeys, ...(ls ? [] : ["paymentData", "paymentMeans", "userData", "userIp"] )];
  assertOnlyKeys(body, allowed, name);
  if (requireBookHash) requireNonEmptyString(body.bookHash, `${name}.bookHash`);
  optionalNullableString(body.checkOutId, `${name}.checkOutId`);

  const guestContact = requestObject(body.guestContact, `${name}.guestContact`);
  assertOnlyKeys(guestContact, ["email", "phone", "comment"], `${name}.guestContact`);
  requireNonEmptyString(guestContact.email, `${name}.guestContact.email`);
  requireNonEmptyString(guestContact.phone, `${name}.guestContact.phone`);
  optionalNullableString(guestContact.comment, `${name}.guestContact.comment`);

  if (!Array.isArray(body.rooms)) throw new Error(`${name}.rooms must be an array.`);
  body.rooms.forEach((room, roomIndex) => {
    requestObject(room, `${name}.rooms[${roomIndex}]`);
    assertOnlyKeys(room, ["guests"], `${name}.rooms[${roomIndex}]`);
    if (!Array.isArray(room.guests)) throw new Error(`${name}.rooms[${roomIndex}].guests must be an array.`);
    room.guests.forEach((guest, guestIndex) => {
      const guestName = `${name}.rooms[${roomIndex}].guests[${guestIndex}]`;
      requestObject(guest, guestName);
      assertOnlyKeys(guest, ["firstName", "lastName", "childAge"], guestName);
      requireNonEmptyString(guest.firstName, `${guestName}.firstName`);
      requireNonEmptyString(guest.lastName, `${guestName}.lastName`);
      if (guest.childAge !== undefined && guest.childAge !== null && !Number.isInteger(guest.childAge)) throw new Error(`${guestName}.childAge must be an integer or null.`);
    });
  });

  if (body.contactData !== undefined && body.contactData !== null) {
    const contact = requestObject(body.contactData, `${name}.contactData`);
    assertOnlyKeys(contact, ["firstName", "lastName", "email"], `${name}.contactData`);
    if (typeof contact.firstName !== "string" || typeof contact.lastName !== "string") throw new Error(`${name}.contactData requires string firstName and lastName.`);
    optionalNullableString(contact.email, `${name}.contactData.email`);
  }
  if (body.arrivalTime !== undefined && body.arrivalTime !== null) {
    const arrival = requestObject(body.arrivalTime, `${name}.arrivalTime`);
    assertOnlyKeys(arrival, ["type", "from", "to"], `${name}.arrivalTime`);
    for (const key of ["type", "from", "to"]) optionalNullableString(arrival[key], `${name}.arrivalTime.${key}`);
  }
  optionalNullableString(body.promocode, `${name}.promocode`);
  optionalNullableBoolean(body.isBusinessTrip, `${name}.isBusinessTrip`);
  if (body.extraServices !== undefined && body.extraServices !== null) {
    const extras = requestObject(body.extraServices, `${name}.extraServices`);
    assertOnlyKeys(extras, ["earlyCheckInId", "lateCheckOutId", "guaranteedRefundSelected"], `${name}.extraServices`);
    optionalNullableString(extras.earlyCheckInId, `${name}.extraServices.earlyCheckInId`);
    optionalNullableString(extras.lateCheckOutId, `${name}.extraServices.lateCheckOutId`);
    if (extras.guaranteedRefundSelected !== undefined && extras.guaranteedRefundSelected !== null && typeof extras.guaranteedRefundSelected !== "boolean") {
      throw new Error(`${name}.extraServices.guaranteedRefundSelected must be a boolean or null.`);
    }
  }

  if (!ls) {
    if (body.paymentData !== undefined && body.paymentData !== null) {
      const paymentData = requestObject(body.paymentData, `${name}.paymentData`);
      assertOnlyKeys(paymentData, ["creditCardId"], `${name}.paymentData`);
      optionalNullableUuid(paymentData.creditCardId, `${name}.paymentData.creditCardId`);
    }
    if (body.paymentMeans !== undefined && body.paymentMeans !== null && !["payment_form", "on_us", "off_us", "dolyame", "pos"].includes(body.paymentMeans)) {
      throw new Error(`${name}.paymentMeans is unsupported.`);
    }
    const hasUserData = Object.hasOwn(body, "userData");
    const hasUserIp = Object.hasOwn(body, "userIp");
    if (hasUserData !== hasUserIp) throw new Error(`${name}.userData and ${name}.userIp must be provided together.`);
    if (hasUserData) {
      const userData = requestObject(body.userData, `${name}.userData`);
      assertOnlyKeys(userData, ["ssoId", "siebelId", "phoneNumber"], `${name}.userData`);
      for (const key of ["ssoId", "siebelId", "phoneNumber"]) optionalNullableString(userData[key], `${name}.userData.${key}`);
      requireNonEmptyString(body.userIp, `${name}.userIp`);
    }
  }
  return body;
}

function validatedBookingsListArgs(args) {
  requestObject(args, "arguments");
  assertOnlyKeys(args, ["isActiveRequired", "isCancelledRequired", "isCompletedRequired"], "arguments");
  for (const name of ["isActiveRequired", "isCancelledRequired", "isCompletedRequired"]) {
    if (typeof args[name] !== "boolean") throw new Error(`${name} must be a boolean.`);
  }
  return {
    isActiveRequired: args.isActiveRequired,
    isCancelledRequired: args.isCancelledRequired,
    isCompletedRequired: args.isCompletedRequired,
  };
}

function validatedTrancheAmountArgs(args) {
  requestObject(args, "arguments");
  assertOnlyKeys(args, ["accounts"], "arguments");
  if (!Array.isArray(args.accounts) || args.accounts.length > 100) throw new Error("accounts must be an array with at most 100 items.");
  return {
    accounts: args.accounts.map((account, index) => {
      requestObject(account, `accounts[${index}]`);
      assertOnlyKeys(account, ["accountId", "type", "balance"], `accounts[${index}]`);
      if (typeof account.accountId !== "string" || !account.accountId.trim()) throw new Error(`accounts[${index}].accountId must be a non-empty string.`);
      if (typeof account.type !== "string" || !account.type.trim()) throw new Error(`accounts[${index}].type must be a non-empty string.`);
      if (typeof account.balance !== "number" || !Number.isFinite(account.balance)) throw new Error(`accounts[${index}].balance must be a finite number.`);
      return { accountId: account.accountId, type: account.type, balance: account.balance };
    }),
  };
}

function requestHash(action, path, args) {
  const material = JSON.stringify({ action, path, payload: args.payload, orderId: args.orderId, bookHash: args.bookHash, preparedAt: args.preparedAt, expiresAt: args.expiresAt });
  return createHash("sha256").update(material).digest("hex");
}

function preparationWindow() {
  const preparedAt = Date.now();
  return {
    preparedAt: new Date(preparedAt).toISOString(),
    expiresAt: new Date(preparedAt + PREPARED_CONFIRMATION_TTL_MS).toISOString(),
  };
}

function validatePreparationWindow(args) {
  const preparedAt = Date.parse(args.preparedAt);
  const expiresAt = Date.parse(args.expiresAt);
  if (!Number.isFinite(preparedAt) || !Number.isFinite(expiresAt)) throw new Error("preparedAt and expiresAt must be valid timestamps from the prepare response.");
  if (expiresAt <= preparedAt || expiresAt - preparedAt > PREPARED_CONFIRMATION_TTL_MS) throw new Error("Prepared confirmation window is invalid.");
  const now = Date.now();
  if (preparedAt > now + 30_000) throw new Error("Prepared confirmation timestamp is in the future.");
  if (expiresAt <= now) throw new Error("Prepared confirmation has expired. Prepare and review the action again.");
}

function confirmationPhrase(action, hash) {
  return `CONFIRM_TBANK_HOTELS_${action.toUpperCase()}_${hash.slice(0, 12)}`;
}

function mutationPath(action, args) {
  switch (action) {
    case "booking": return "/api/v1/hotels/bookings/tasks/create";
    case "lsBooking": return "/api/v1/hotels/bookings/ls/tasks/create";
    case "cancel": return "/api/v1/hotels/bookings/cancel";
    case "paymentSetup": return `/api/v1/hotels/bookings/shevo/${value(args.orderId, "orderId")}/payment/setup`;
    case "applyPromocode": return `/api/v1/hotels/rates/${value(args.bookHash, "bookHash")}/promocode`;
    case "extraServices": return `/api/v1/hotels/rates/${value(args.bookHash, "bookHash")}/extraServices`;
    default: throw new Error("Unsupported mutation.");
  }
}

function mutationRequestBody(tool, args) {
  switch (tool._action) {
    case "booking":
      return validatedBookingPayload(args.payload);
    case "lsBooking":
      return validatedBookingPayload(args.payload, { ls: true });
    case "cancel":
      assertOnlyKeys(args, tool._execute
        ? ["orderId", "preparedRequestHash", "confirmation", "preparedAt", "expiresAt"]
        : ["orderId"], "arguments");
      if (typeof args.orderId !== "string" || !args.orderId.trim()) throw new Error("orderId must be a non-empty string.");
      return { orderId: args.orderId };
    case "paymentSetup":
      assertOnlyKeys(args, tool._execute
        ? ["orderId", "preparedRequestHash", "confirmation", "preparedAt", "expiresAt"]
        : ["orderId"], "arguments");
      return undefined;
    case "applyPromocode":
      assertOnlyKeys(args, tool._execute
        ? ["bookHash", "promocode", "preparedRequestHash", "confirmation", "preparedAt", "expiresAt"]
        : ["bookHash", "promocode"], "arguments");
      if (args.promocode !== null && typeof args.promocode !== "string") throw new Error("promocode must be a string or null.");
      return { promocode: args.promocode };
    case "extraServices":
      assertOnlyKeys(args, tool._execute
        ? ["bookHash", "extraServiceIds", "preparedRequestHash", "confirmation", "preparedAt", "expiresAt"]
        : ["bookHash", "extraServiceIds"], "arguments");
      if (!Array.isArray(args.extraServiceIds) || !args.extraServiceIds.every((item) => typeof item === "string" && item.length > 0)) {
        throw new Error("extraServiceIds must be an array of non-empty strings.");
      }
      return { extraServiceIds: args.extraServiceIds };
    default:
      throw new Error("Unsupported mutation.");
  }
}

function redactPreview(payloadValue) {
  if (Array.isArray(payloadValue)) return payloadValue.map(redactPreview);
  if (!payloadValue || typeof payloadValue !== "object") return payloadValue;
  const secretNames = /password|token|authorization|card|pan|cvv|cvc|phone|email|passport|birth|document/i;
  const guestNameKeys = new Set(["firstname", "lastname", "middlename", "surname", "givenname", "familyname", "patronymic", "fullname", "guestname", "ssoid", "siebelid", "userip", "ipaddress"]);
  return Object.fromEntries(Object.entries(payloadValue).map(([key, item]) => {
    const normalizedKey = key.replace(/[^A-Za-z]/g, "").toLowerCase();
    return [key, secretNames.test(key) || guestNameKeys.has(normalizedKey) ? "[REDACTED]" : redactPreview(item)];
  }));
}

function assertOnlyKeys(object, allowed, name) {
  const unexpected = Object.keys(object).filter((key) => !allowed.includes(key));
  if (unexpected.length) throw new Error(`${name} contains unsupported fields: ${unexpected.join(", ")}.`);
}

function optionalNonNegativeInteger(value, name, minimum = 0) {
  if (value !== undefined && (!Number.isInteger(value) || value < minimum)) throw new Error(`${name} must be an integer greater than or equal to ${minimum}.`);
}

function validatedSeoSearchArgs(args) {
  const apiVersion = version(args, "v3", ["v1", "v2", "v3"]);
  const body = requestObject(args.payload);
  if (apiVersion === "v1") {
    if (!Number.isInteger(body.destinationId) || body.destinationId <= 0) throw new Error("payload.destinationId must be a positive integer for SEO v1.");
    assertOnlyKeys(body, ["destinationId", "hostelIsNeeded", "guesthouseIsNeeded"], "payload");
    for (const key of ["hostelIsNeeded", "guesthouseIsNeeded"]) {
      if (body[key] !== undefined && typeof body[key] !== "boolean") throw new Error(`payload.${key} must be boolean.`);
    }
  } else if (apiVersion === "v2") {
    if (!Number.isInteger(body.locationId) || body.locationId <= 0) throw new Error("payload.locationId must be a positive integer for SEO v2.");
    assertOnlyKeys(body, ["locationId", "offset", "limit", "filter"], "payload");
    optionalNonNegativeInteger(body.offset, "payload.offset");
    optionalNonNegativeInteger(body.limit, "payload.limit", 1);
    if (body.filter !== undefined) requestObject(body.filter, "payload.filter");
  } else {
    if (typeof body.country !== "string" || !body.country.trim()) throw new Error("payload.country must be a non-empty string for SEO v3.");
    if (typeof body.location !== "string" || !body.location.trim()) throw new Error("payload.location must be a non-empty string for SEO v3.");
    assertOnlyKeys(body, ["country", "location", "offset", "limit", "filter"], "payload");
    optionalNonNegativeInteger(body.offset, "payload.offset");
    optionalNonNegativeInteger(body.limit, "payload.limit", 1);
    if (body.filter !== undefined) requestObject(body.filter, "payload.filter");
  }
  return { apiVersion, payload: body };
}

function safeDiagnosticToken(value) {
  return typeof value === "string" && /^[A-Za-z0-9_.:-]{1,128}$/.test(value) ? value : null;
}

function providerErrorCode(responseBody) {
  if (!responseBody || typeof responseBody !== "object") return null;
  const queue = [{ value: responseBody, depth: 0 }];
  while (queue.length) {
    const { value, depth } = queue.shift();
    if (!value || typeof value !== "object" || depth > 3) continue;
    for (const [key, item] of Object.entries(value)) {
      if (/^(errorCode|code|type)$/i.test(key)) {
        const safe = safeDiagnosticToken(item);
        if (safe) return safe;
      }
      if (item && typeof item === "object") queue.push({ value: item, depth: depth + 1 });
    }
  }
  return null;
}

async function boundedResponseText(response) {
  const declaredLength = Number(response.headers.get("content-length"));
  if (Number.isFinite(declaredLength) && declaredLength > MAX_PROVIDER_RESPONSE_BYTES) {
    await response.body?.cancel();
    throw new Error("Hotels API response exceeded the safe size limit.");
  }
  if (!response.body) return "";
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let size = 0;
  let result = "";
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    size += value.byteLength;
    if (size > MAX_PROVIDER_RESPONSE_BYTES) {
      await reader.cancel();
      throw new Error("Hotels API response exceeded the safe size limit.");
    }
    result += decoder.decode(value, { stream: true });
  }
  return result + decoder.decode();
}

async function apiRequest(method, path, { payload: body, query, language, requestTimeoutMs } = {}) {
  const configuredBase = baseUrl();
  const target = new URL(path, `${configuredBase.origin}/`);
  if (query) {
    requestObject(query, "query");
    for (const [key, item] of Object.entries(query)) {
      if (item === undefined || item === null) continue;
      if (typeof item === "object") throw new Error("query values must be scalar.");
      target.searchParams.set(key, String(item));
    }
  }
  if (body !== undefined) requestObject(body);
  const headers = { Accept: "application/json", ...configuredHeaders() };
  if (language) {
    if (typeof language !== "string" || language.length < 2 || language.length > 35) throw new Error("language must be a string from 2 to 35 characters.");
    headers["X-User-Language"] = language;
  }
  if (body !== undefined) headers["Content-Type"] = "application/json";
  return withProviderRequestSlot(async () => {
    let response;
    try {
      const effectiveTimeoutMs = requestTimeoutMs === undefined ? timeoutMs() : Math.min(timeoutMs(), Math.max(1, Math.floor(requestTimeoutMs)));
      response = await fetch(target, { method, headers, body: body === undefined ? undefined : JSON.stringify(body), redirect: "error", signal: AbortSignal.timeout(effectiveTimeoutMs) });
    } catch (error) {
      if (error.name === "TimeoutError") {
        const timeoutError = new Error("Hotels API request timed out.");
        timeoutError.code = "HOTELS_API_TIMEOUT";
        throw timeoutError;
      }
      const networkCode = safeDiagnosticToken(error?.cause?.code);
      const networkError = new Error(`Unable to reach Hotels API${networkCode ? ` (${networkCode})` : ""}.`);
      networkError.code = "HOTELS_API_NETWORK";
      throw networkError;
    }
    const responseText = await boundedResponseText(response);
    let responseBody = null;
    if (responseText) {
      try { responseBody = JSON.parse(responseText); } catch { responseBody = responseText; }
    }
    if (!response.ok) {
      const code = providerErrorCode(responseBody);
      const requestId = safeDiagnosticToken(response.headers.get("x-request-id")) || safeDiagnosticToken(response.headers.get("x-correlation-id"));
      const details = [code ? `code: ${code}` : null, requestId ? `requestId: ${requestId}` : null].filter(Boolean);
      const providerError = new Error(`Hotels API returned HTTP ${response.status}${details.length ? ` (${details.join(", ")})` : ""}.`);
      providerError.code = "HOTELS_API_HTTP";
      providerError.httpStatus = response.status;
      throw providerError;
    }
    return { status: response.status, data: responseBody };
  });
}

function version(args, fallback, allowed) {
  const selected = args.apiVersion ?? fallback;
  if (!allowed.includes(selected)) throw new Error(`apiVersion must be one of: ${allowed.join(", ")}.`);
  return selected;
}

const directToolHandlers = new Map([
  ["tbank_hotels_connection_status", () => connectionStatus()],
  ["tbank_hotels_get_customer", () => getCustomer()],
  ["tbank_hotels_search", (args) => guardedProviderSearchRequest("POST", "/api/v1/hotels/search", args)],
  ["tbank_hotels_resolve_destination", (args) => resolveDestination(args)],
  ["tbank_hotels_plan_stay", (args) => planStay(args)],
  ["tbank_hotels_plan_personalized_stay", (args) => {
    if (args.hotelPreferences === undefined) {
      throw new Error("hotelPreferences from tbank_banking_build_portfolio_travel_profile are required for personalized stay planning.");
    }
    return planStay(args);
  }],
  ["tbank_hotels_get_stay_options", (args) => getStayOptions(args)],
  ["tbank_hotels_compare_stay_options", (args) => compareStayOptions(args)],
  ["tbank_hotels_select_stay_option", (args) => selectStayOption(args)],
  ["tbank_hotels_get_selected_stay_rates", (args) => selectedStayRates(args)],
  ["tbank_hotels_select_stay_rate", (args) => selectedStayRate(args)],
  ["tbank_hotels_create_booking_preview", (args) => createBookingPreview(args)],
  ["tbank_hotels_create_payment_form_preview", (args) => createPaymentFormPreview(args)],
  ["tbank_hotels_create_checkout_handoff", (args) => createCheckoutHandoff(args)],
  ["tbank_hotels_create_booking_draft", (args) => createBookingDraft(args)],
  ["tbank_hotels_validate_checkout", (args) => validateCheckout(args)],
  ["tbank_hotels_prepare_draft_booking", (args) => preparedDraftBooking(args)],
  ["tbank_hotels_confirm_booking", (args) => confirmBooking(args)],
  ["tbank_hotels_get_booking_overview", (args) => bookingOverview(args)],
  ["tbank_hotels_preview_cancellation", (args) => previewCancellation(args)],
  ["tbank_hotels_repeat_stay_plan", (args) => repeatStayPlan(args)],
  ["tbank_hotels_get_search_filters", (args) => apiRequest("GET", `/api/${version(args, "v1", ["v1", "v2"])}/hotels/search-filters`)],
  ["tbank_hotels_get_filter_availability", (args) => guardedProviderSearchRequest("POST", "/api/v1/hotels/search-filters-availability", args)],
  ["tbank_hotels_search_map", (args) => providerSearchRequest("POST", "/api/v1/hotels/map/search", args)],
  ["tbank_hotels_get_map_hotels", (args) => apiRequest("POST", "/api/v1/hotels/map/hotels", args)],
  ["tbank_hotels_search_points_of_interest", (args) => {
    if (!["search", "landmarks", "groups"].includes(args.mode)) throw new Error("mode must be search, landmarks, or groups.");
    return apiRequest("POST", `/api/v1/points_of_interest/${args.mode}`, args);
  }],
  ["tbank_hotels_get_hotel", (args) => apiRequest("GET", `/api/v1/hotels/${value(args.hotelId, "hotelId")}`, args)],
  ["tbank_hotels_get_hotel_rates", (args) => apiRequest("POST", `/api/${version(args, "v3", ["v2", "v3"])}/hotels/${value(args.hotelId, "hotelId")}/rates`, args)],
  ["tbank_hotels_get_rate", (args) => apiRequest("GET", `/api/${version(args, "v3", ["v2", "v3"])}/rates/${value(args.bookHash, "bookHash")}`, args)],
  ["tbank_hotels_get_cashback_percent", (args) => apiRequest("GET", `/api/v1/hotels/cashback/percent-by-account/${value(args.bookHash, "bookHash")}`)],
  ["tbank_hotels_get_max_cashback", () => apiRequest("GET", "/api/v1/hotels/cashback/max-percent")],
  ["tbank_hotels_validate_promocode", (args) => apiRequest("POST", "/api/v1/hotels/promocodes/validate", args)],
  ["tbank_hotels_get_rate_upgrade", (args) => apiRequest("POST", `/api/v1/hotels/rates/${value(args.bookHash, "bookHash")}/upgrade`, args)],
  ["tbank_hotels_get_booking", async (args) => {
    const v = version(args, authBrokerSocket() ? "v1" : "v3", ["v1", "v2", "v3"]);
    if (authBrokerSocket()) {
      if (v !== "v1") throw new Error("Mobile auth broker supports tbank_hotels_get_booking only with apiVersion=v1.");
      if (args.orderId !== undefined) throw new Error("Mobile auth broker mode does not accept provider orderId. Use bookingRef from tbank_hotels_list_bookings.");
      const bookingRef = args.bookingRef;
      const orderId = orderIdForBookingReference(bookingRef);
      const result = await authBrokerRequest("hotels.get_booking_v1", { bookingId: orderId });
      return bookingWithReference(result.booking, bookingRef);
    }
    if (args.bookingRef !== undefined) throw new Error("bookingRef is available only with the mobile auth broker. Direct API profiles require orderId.");
    return apiRequest("GET", `/api/${v}/hotels/bookings/${value(args.orderId, "orderId")}`, args);
  }],
  ["tbank_hotels_list_bookings", async (args) => {
    const payload = validatedBookingsListArgs(args);
    if (authBrokerSocket()) {
      const result = await authBrokerRequest("hotels.list_bookings", payload);
      return bookingListWithReferences(result.bookings);
    }
    return apiRequest("POST", "/api/v1/hotels/bookings/booking_list", { payload });
  }],
  ["tbank_hotels_summarize_bookings", async () => {
    const payload = { isActiveRequired: true, isCancelledRequired: true, isCompletedRequired: true };
    if (authBrokerSocket()) {
      const result = await authBrokerRequest("hotels.list_bookings", payload);
      return bookingListSummary(result.bookings);
    }
    return bookingListSummary(await apiRequest("POST", "/api/v1/hotels/bookings/booking_list", { payload }));
  }],
  ["tbank_hotels_get_voucher", () => { throw new Error("Inline voucher delivery is disabled because PDF content must not enter MCP JSON. Use tbank_hotels_save_voucher with bookingRef and the local auth broker."); }],
  ["tbank_hotels_save_voucher", async (args) => {
    if (!authBrokerSocket()) throw new Error("Safe voucher handoff requires the local mobile auth broker.");
    if (args.orderId !== undefined) throw new Error("Provider orderId is not accepted. Use bookingRef from tbank_hotels_list_bookings.");
    const bookingRef = args.bookingRef;
    const orderId = orderIdForBookingReference(bookingRef);
    const result = await authBrokerRequest("hotels.save_voucher_v1", { bookingId: orderId });
    if (!result?.voucher || result.voucher.documentContentIncluded !== false) throw new Error("Auth broker returned an unsafe voucher response.");
    return { status: "saved_locally", bookingRef, ...result };
  }],
  ["tbank_hotels_create_payment_handoff_preview", async (args) => {
    if (!authBrokerSocket()) throw new Error("Hotel payment handoff preview requires the shared local auth broker.");
    if (args.orderId !== undefined) throw new Error("Provider orderId is not accepted. Use bookingRef from tbank_hotels_list_bookings.");
    const bookingRef = args.bookingRef;
    const orderId = orderIdForBookingReference(bookingRef);
    const result = await authBrokerRequest("hotels.create_payment_handoff", { bookingId: orderId });
    if (!result || result.bookingBindingVerified !== true || typeof result.paymentHandoffRef !== "string") {
      throw new Error("Auth broker returned an invalid hotel payment handoff.");
    }
    return { status: "preview_ready", bookingRef, ...result, paymentSetupPerformed: false, paymentExecutionPerformed: false };
  }],
  ["tbank_hotels_get_reservation", (args) => apiRequest("GET", "/api/v1/hotels/bookings/getReservation", args)],
  ["tbank_hotels_get_evo_booking", (args) => apiRequest("GET", `/api/v1/hotels/bookings/evo/${value(args.orderId, "orderId")}`)],
  ["tbank_hotels_get_bnpl_offer", (args) => apiRequest("POST", `/api/v1/hotels/bookings/evo/${value(args.orderId, "orderId")}/bnpl_offer`, { language: args.language })],
  ["tbank_hotels_get_booking_task_status", (args) => apiRequest("GET", `/api/v1/hotels/bookings/tasks/${value(args.taskId, "taskId")}/status`)],
  ["tbank_hotels_check_ls_order", (args) => apiRequest("GET", `/api/v1/hotels/bookings/ls/check_orders/${value(args.orderId, "orderId")}`)],
  ["tbank_hotels_get_reviews", (args) => {
    if (!["ratings", "summary", "feedback", "feedback-filters"].includes(args.resource)) throw new Error("resource is unsupported.");
    return apiRequest("GET", `/api/v1/review/${value(args.hotelId, "hotelId")}/${args.resource}`, args);
  }],
  ["tbank_hotels_get_review_order_status", (args) => apiRequest("GET", `/api/v1/review/order-status/${value(args.orderId, "orderId")}`)],
  ["tbank_hotels_search_seo", (args) => {
    const validated = validatedSeoSearchArgs(args);
    return apiRequest("POST", `/api/${validated.apiVersion}/seo/search`, { payload: validated.payload });
  }],
  ["tbank_hotels_search_urls", (args) => apiRequest("POST", `/api/${version(args, "v3", ["v1", "v2", "v3"])}/hotels/urls/search`, args)],
  ["tbank_hotels_get_seo_resource", (args) => seoResource(args)],
  ["tbank_hotels_get_deeplink_token", (args) => {
    if (args.kind === "general") return apiRequest("GET", "/api/v1/get-link-token");
    if (args.kind === "hotels-urls") return apiRequest("GET", "/api/v1/hotels/urls/link-token");
    throw new Error("kind must be general or hotels-urls.");
  }],
  ["tbank_hotels_get_available_tranche_amount", (args) => apiRequest("POST", "/api/v1/tranches/available/amount", { payload: validatedTrancheAmountArgs(args) })],
  ["tbank_hotels_get_partner_redirect_url", (args) => apiRequest("POST", `/api/v1/partners/${value(args.partnerAlias, "partnerAlias")}/redirectUrl`, args)],
]);

export const runtimeHandledToolNames = Object.freeze(new Set([
  ...directToolHandlers.keys(),
  ...tools.filter((tool) => tool._action).map((tool) => tool.name),
]));

export async function callTool(name, args = {}) {
  if (!args || typeof args !== "object" || Array.isArray(args)) throw new Error("Tool arguments must be an object.");
  const mutation = tools.find((tool) => tool.name === name && tool._action);
  if (mutation) return callMutation(mutation, args);
  const handler = directToolHandlers.get(name);
  if (!handler) throw new Error(`Unknown tool: ${name}`);
  return handler(args);
}

function seoResource(args) {
  switch (args.resource) {
    case "hotel": return apiRequest("GET", `/api/v1/seo/hotels/${value(args.id, "id")}`, args);
    case "region": return apiRequest("GET", `/api/v1/seo/regions/${value(args.id, "id")}`, args);
    case "available-filters": return args.id ? apiRequest("GET", `/api/v1/seo/available-filters/${value(args.id, "id")}`, args) : apiRequest("GET", "/api/v1/seo/available-filters", args);
    case "locations": return apiRequest("GET", "/api/v1/seo/locations", args);
    case "location-by-slug": return apiRequest("GET", "/api/v1/seo/location-by-slug", args);
    case "rooms": return apiRequest("GET", `/api/v1/seo/rooms/${value(args.id, "id")}`, args);
    case "slug-by-hotel": return apiRequest("GET", `/api/v1/seo/slug-by-hotel/${value(args.id, "id")}`, args);
    default: throw new Error("resource is unsupported.");
  }
}

async function callMutation(tool, args) {
  const path = mutationPath(tool._action, args);
  const body = mutationRequestBody(tool, args);
  if (!tool._execute) {
    const executionReadiness = mutationExecutionReadiness(tool._action);
    if (!executionReadiness.available) {
      return {
        status: "preview_only",
        action: tool._action,
        executionAvailable: false,
        executionReadiness: journeyExecutionReadiness(executionReadiness),
        endpoint: path,
        payloadPreview: redactPreview(body),
        nextStep: "Execution is unavailable in this configuration. Do not ask the user for confirmation; configuration and approval belong outside the model conversation.",
        note: "HTTP-запрос не выполнен; confirmation и requestHash намеренно не выданы.",
      };
    }
    const window = preparationWindow();
    const hash = requestHash(tool._action, path, { payload: body, orderId: args.orderId, bookHash: args.bookHash, ...window });
    return {
      action: tool._action,
      executionAvailable: true,
      executionReadiness,
      requestHash: hash,
      confirmation: confirmationPhrase(tool._action, hash),
      ...window,
      endpoint: path,
      payloadPreview: redactPreview(body),
      note: "HTTP-запрос не выполнен. Получите явное подтверждение пользователя непосредственно перед execute-вызовом.",
    };
  }
  requireMutationExecutionReady(tool._action);
  validatePreparationWindow(args);
  const hash = requestHash(tool._action, path, { payload: body, orderId: args.orderId, bookHash: args.bookHash, preparedAt: args.preparedAt, expiresAt: args.expiresAt });
  const phrase = confirmationPhrase(tool._action, hash);
  if (args.preparedRequestHash !== hash) throw new Error("preparedRequestHash does not match this exact request. Prepare and review the action again.");
  if (args.confirmation !== phrase) throw new Error("confirmation must exactly match the phrase returned by the corresponding prepare call.");
  startTrackedMutationExecution(hash, Date.parse(args.expiresAt) + PREPARED_CONFIRMATION_TTL_MS);
  try {
    const result = await apiRequest("POST", path, body === undefined ? {} : { payload: body });
    finishTrackedMutationExecution(hash, "completed");
    return result;
  } catch (error) {
    if (error.code === "HOTELS_API_TIMEOUT" || error.code === "HOTELS_API_NETWORK" || (error.httpStatus ?? 0) >= 500) {
      finishTrackedMutationExecution(hash, "outcome_unknown");
    } else {
      mutationExecutionsByHash.delete(hash);
    }
    throw error;
  }
}
