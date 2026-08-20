import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { createHash, generateKeyPairSync, verify } from "node:crypto";
import test from "node:test";
import { callTool } from "../src/server.mjs";

const serverPath = new URL("../src/server.mjs", import.meta.url).pathname;

function startServer(env = {}) {
  const child = spawn(process.execPath, [serverPath], {
    stdio: ["pipe", "pipe", "pipe"],
    env: { NODE_ENV: "test", ...env },
  });
  const messages = [];
  let buffer = "";
  child.stdout.on("data", (chunk) => {
    buffer += chunk;
    const lines = buffer.split("\n");
    buffer = lines.pop();
    for (const line of lines) if (line) messages.push(JSON.parse(line));
  });
  const request = (payload) => new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error("MCP response timed out")), 2_000);
    const interval = setInterval(() => {
      const index = messages.findIndex((message) => message.id === payload.id);
      if (index >= 0) {
        clearTimeout(timer);
        clearInterval(interval);
        resolve(messages.splice(index, 1)[0]);
      }
    }, 10);
    child.stdin.write(`${JSON.stringify(payload)}\n`);
  });
  const requestAny = (payload) => new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error("MCP response timed out")), 2_000);
    const interval = setInterval(() => {
      if (messages.length) {
        clearTimeout(timer);
        clearInterval(interval);
        resolve(messages.shift());
      }
    }, 10);
    child.stdin.write(`${JSON.stringify(payload)}\n`);
  });
  return { child, request, requestAny };
}

test("does not inherit Hotels credentials from the parent process", async (t) => {
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  process.env.TBANK_HOTELS_API_BASE_URL = "https://production-like.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "parent-secret";
  process.env.TBANK_HOTELS_JWT_PRIVATE_KEY = "parent-private-key";
  t.after(() => {
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const server = startServer();
  t.after(() => server.child.kill());
  const result = await server.request({ jsonrpc: "2.0", id: 1, method: "tools/call", params: { name: "tbank_hotels_connection_status", arguments: {} } });
  const status = JSON.parse(result.result.content[0].text);
  assert.equal(status.serverVersion, "0.8.0");
  assert.equal(status.ready, false);
  assert.equal(status.searchReady, false);
  assert.equal(status.transport, "not_configured");
  assert.equal(status.authentication, "not_configured");
  assert.doesNotMatch(result.result.content[0].text, /production-like|parent-secret|parent-private-key/);
});

test("reports API MCP metadata and no browser tools", async (t) => {
  const server = startServer();
  t.after(() => server.child.kill());
  const initialized = await server.request({ jsonrpc: "2.0", id: 1, method: "initialize", params: { protocolVersion: "2025-03-26" } });
  assert.equal(initialized.result.serverInfo.name, "tbank-hotels-api-mcp");
  assert.equal(initialized.result.serverInfo.version, "0.8.0");
  const listed = await server.request({ jsonrpc: "2.0", id: 2, method: "tools/list" });
  const names = listed.result.tools.map((tool) => tool.name);
  assert.ok(names.includes("tbank_hotels_search"));
  assert.ok(names.includes("tbank_hotels_execute_booking"));
  assert.ok(names.includes("tbank_hotels_resolve_destination"));
  assert.ok(names.includes("tbank_hotels_plan_stay"));
  assert.ok(names.includes("tbank_hotels_compare_stay_options"));
  assert.ok(!names.some((name) => /browser|snapshot|cookie|open_/.test(name)));
  const planTool = listed.result.tools.find((tool) => tool.name === "tbank_hotels_plan_stay");
  assert.ok(planTool.inputSchema.properties.destination);
  assert.ok(planTool.inputSchema.properties.rooms);
  assert.ok(!planTool.inputSchema.properties.searchRequest);
  assert.equal(planTool.annotations.readOnlyHint, true);
  const executeBooking = listed.result.tools.find((tool) => tool.name === "tbank_hotels_execute_booking");
  assert.equal(executeBooking.annotations.readOnlyHint, false);
  assert.equal(executeBooking.annotations.destructiveHint, true);
  const selectedRates = listed.result.tools.find((tool) => tool.name === "tbank_hotels_get_selected_stay_rates");
  assert.ok(selectedRates.inputSchema.properties.filters);
  assert.ok(!selectedRates.inputSchema.properties.payload);
  const bookingPreview = listed.result.tools.find((tool) => tool.name === "tbank_hotels_create_booking_preview");
  assert.deepEqual(bookingPreview.inputSchema.required, ["journeyId"]);
  assert.ok(!bookingPreview.inputSchema.properties.bookingData);
  assert.equal(bookingPreview.annotations.readOnlyHint, true);
  const seoSearch = listed.result.tools.find((tool) => tool.name === "tbank_hotels_search_seo");
  assert.equal(seoSearch.inputSchema.type, "object");
  assert.equal(seoSearch.inputSchema.oneOf.length, 3);
  const paymentSetup = listed.result.tools.find((tool) => tool.name === "tbank_hotels_prepare_payment_setup");
  assert.ok(!paymentSetup.inputSchema.properties.payload);
  const listBookings = listed.result.tools.find((tool) => tool.name === "tbank_hotels_list_bookings");
  assert.deepEqual(listBookings.inputSchema.required, ["isActiveRequired", "isCancelledRequired", "isCompletedRequired"]);
  assert.ok(!listBookings.inputSchema.properties.payload);
  const bnplOffer = listed.result.tools.find((tool) => tool.name === "tbank_hotels_get_bnpl_offer");
  assert.ok(!bnplOffer.inputSchema.properties.payload);
  const cancelBooking = listed.result.tools.find((tool) => tool.name === "tbank_hotels_prepare_cancel_booking");
  assert.deepEqual(cancelBooking.inputSchema.required, ["orderId"]);
  assert.ok(!cancelBooking.inputSchema.properties.payload);
  const createDraft = listed.result.tools.find((tool) => tool.name === "tbank_hotels_create_booking_draft");
  assert.equal(createDraft.inputSchema.properties.bookingData.oneOf.length, 2);
  assert.ok(!createDraft.inputSchema.properties.bookingData.oneOf[0].properties.bookHash);
  const prepareBooking = listed.result.tools.find((tool) => tool.name === "tbank_hotels_prepare_booking");
  assert.equal(prepareBooking.inputSchema.properties.payload.oneOf.length, 2);
  assert.equal(prepareBooking.inputSchema.properties.payload.oneOf[0].additionalProperties, false);
});

test("reports its supported MCP protocol version instead of echoing an unsupported client version", async (t) => {
  const server = startServer();
  t.after(() => server.child.kill());
  const initialized = await server.request({ jsonrpc: "2.0", id: 1, method: "initialize", params: { protocolVersion: "2099-01-01" } });
  assert.equal(initialized.result.protocolVersion, "2025-03-26");
});

test("supports ping and explicitly rejects JSON-RPC batch input", async (t) => {
  const server = startServer();
  t.after(() => server.child.kill());
  const ping = await server.request({ jsonrpc: "2.0", id: 1, method: "ping" });
  assert.deepEqual(ping.result, {});
  const batch = await server.requestAny([{ jsonrpc: "2.0", id: 2, method: "ping" }]);
  assert.equal(batch.id, null);
  assert.equal(batch.error.code, -32600);
  assert.match(batch.error.message, /batch requests are not supported/);
});

test("rejects journey operations for an unknown context without network access", async (t) => {
  const server = startServer();
  t.after(() => server.child.kill());
  const result = await server.request({ jsonrpc: "2.0", id: 1, method: "tools/call", params: { name: "tbank_hotels_get_stay_options", arguments: { journeyId: "missing" } } });
  assert.equal(result.result.isError, true);
  assert.match(result.result.content[0].text, /Unknown or expired journeyId/);
});

test("journey flow hides provider identity while carrying a selected option to rates", async (t) => {
  const savedFetch = globalThis.fetch;
  const environmentNames = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_AUTH_HEADER", "TBANK_HOTELS_AUTH_HEADERS_JSON", "TBANK_HOTELS_JWT_PRIVATE_KEY", "TBANK_HOTELS_JWT_ISSUER", "TBANK_HOTELS_JWT_AUDIENCE", "TBANK_HOTELS_ENABLE_MUTATIONS"];
  const savedEnvironment = Object.fromEntries(environmentNames.map((name) => [name, process.env[name]]));
  const calls = [];
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  delete process.env.TBANK_HOTELS_AUTH_TOKEN;
  delete process.env.TBANK_HOTELS_AUTH_HEADER;
  process.env.TBANK_HOTELS_AUTH_HEADERS_JSON = JSON.stringify({ Authorization: "test-customer-auth", "x-real-ip": "192.0.2.1" });
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  delete process.env.TBANK_HOTELS_JWT_ISSUER;
  delete process.env.TBANK_HOTELS_JWT_AUDIENCE;
  process.env.TBANK_HOTELS_ENABLE_MUTATIONS = "true";
  globalThis.fetch = async (url, options) => {
    calls.push({ url: String(url), options });
    if (String(url).includes("/api/v1/seo/locations")) {
      return new Response(JSON.stringify({ payload: { locations: [
        { locationId: 17039, locationName: "Moscow", locationNameRu: "Москва", countryName: "Russia", countryNameRu: "Россия", hotelsCount: 105 },
      ] } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    if (String(url).endsWith("/api/v1/hotels/search")) {
      return new Response(JSON.stringify({ payload: { hotels: [
        { hotelId: "provider-1", hotelName: "First Hotel", hotelChain: null, starRating: 4, areaLocation: { destinationName: "Moscow" }, hotelLocation: { address: "Street 1" }, rateForHotelsFeed: { shownPrice: { value: 100 }, availableRoomsCount: 2, freeCancellationUntil: null, mealName: "Breakfast", paymentPlace: "ONLINE" }, review: { rating: 9.1, ratingsCount: 100 }, cashback: null },
        { hotelId: "provider-2", hotelName: "Second Hotel", hotelChain: null, starRating: 5, areaLocation: { destinationName: "Moscow" }, hotelLocation: { address: "Street 2" }, rateForHotelsFeed: { shownPrice: { value: 200 }, availableRoomsCount: 1, freeCancellationUntil: null, mealName: null, paymentPlace: "ONLINE" }, review: null, cashback: null },
      ] } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    if (String(url).endsWith("/api/v3/hotels/provider-2/rates")) {
      return new Response(JSON.stringify({ payload: { rates: [], rooms: [], otherRates: [] } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    return new Response(JSON.stringify({ payload: { rates: [
      { bookHash: "book-hash-1", roomId: "room-1", shownPrice: { value: 100 }, paymentPrice: { value: 100 }, paymentPlace: "ONLINE", cancellationPolicyRules: {}, isCreditCardDataRequired: false, isNonRefundable: false, mealName: "Breakfast", availableRoomsCount: 2, cashback: null },
    ], rooms: [], otherRates: [] } }), { status: 200, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const plan = await callTool("tbank_hotels_plan_stay", { destination: "Москва", checkinDate: "2099-09-01", checkoutDate: "2099-09-02", rooms: [{ adults: 2 }], language: "ru-RU" });
  assert.equal(plan.status, "ready");
  assert.equal(plan.resolvedDestination.destinationId, 17039);
  assert.equal(plan.totalOptions, 2);
  assert.equal("hotelId" in plan.options[0], false);
  const searchCall = calls.find((call) => call.url.endsWith("/api/v1/hotels/search"));
  assert.deepEqual(JSON.parse(searchCall.options.body), { destinationId: 17039, checkinDate: "2099-09-01", checkoutDate: "2099-09-02", guests: [{ adultsCount: 2, childrenAge: [] }], filters: [], offset: 0, limit: 50 });
  const comparison = await callTool("tbank_hotels_compare_stay_options", { journeyId: plan.journeyId, ranking: "highest_rating", limit: 2 });
  assert.equal(comparison.selectionStrategy, "highest_rating");
  assert.deepEqual(comparison.comparison.map((option) => option.hotelName), ["First Hotel", "Second Hotel"]);
  await callTool("tbank_hotels_select_stay_option", { journeyId: plan.journeyId, optionId: plan.options[0].optionId });
  const rates = await callTool("tbank_hotels_get_selected_stay_rates", { journeyId: plan.journeyId });
  assert.equal(rates.status, "ready");
  assert.equal(rates.canCreateBookingDraft, true);
  assert.equal(rates.attempts, 1);
  assert.equal(rates.failureKind, null);
  assert.equal(rates.selectedOption.hotelName, "First Hotel");
  const ratesCall = calls.find((call) => call.url.endsWith("/api/v3/hotels/provider-1/rates"));
  assert.deepEqual(JSON.parse(ratesCall.options.body), {
    checkInDate: "2099-09-01",
    checkOutDate: "2099-09-02",
    guests: [{ adultsCount: 2, childrenAge: [] }],
  });
  const selectedRate = await callTool("tbank_hotels_select_stay_rate", { journeyId: plan.journeyId, rateOptionId: rates.rateOptions[0].rateOptionId });
  assert.equal(selectedRate.selectedRate.mealName, "Breakfast");
  assert.equal(selectedRate.executionAvailable, true);
  const bookingPreview = await callTool("tbank_hotels_create_booking_preview", { journeyId: plan.journeyId });
  assert.equal(bookingPreview.status, "preview_only");
  assert.equal(bookingPreview.executionAvailable, true);
  assert.equal(bookingPreview.personalDataCollected, false);
  assert.equal(bookingPreview.httpRequestPerformed, false);
  assert.deepEqual(bookingPreview.occupancy, [{ roomIndex: 0, adults: 2, childrenAges: [] }]);
  assert.doesNotMatch(JSON.stringify(bookingPreview), /book-hash|person@example|Ada|Lovelace/);
  const draft = await callTool("tbank_hotels_create_booking_draft", { journeyId: plan.journeyId, bookingData: { guestContact: { email: "person@example.test", phone: "+70000000000" }, rooms: [{ guests: [{ firstName: "Ada", lastName: "Lovelace" }] }] } });
  assert.equal(draft.executionAvailable, true);
  assert.equal(draft.executionReadiness.status, "configured_unverified");
  assert.equal(draft.guestCoverage.namedGuestCountMatches, false);
  assert.match(draft.bookingPreview.guestContact.email, /REDACTED/);
  assert.match(draft.bookingPreview.rooms[0].guests[0].firstName, /REDACTED/);
  assert.match(draft.bookingPreview.rooms[0].guests[0].lastName, /REDACTED/);
  const checkout = await callTool("tbank_hotels_validate_checkout", { bookingDraftId: draft.bookingDraftId });
  assert.equal(checkout.attempts, 1);
  assert.ok(checkout.validatedUntil);
  const prepared = await callTool("tbank_hotels_prepare_draft_booking", { bookingDraftId: draft.bookingDraftId });
  assert.equal(prepared.executionAvailable, true);
  const providerFetch = globalThis.fetch;
  let releaseBooking;
  globalThis.fetch = async (url, options) => {
    if (String(url).endsWith("/api/v1/hotels/bookings/tasks/create")) {
      calls.push({ url: String(url), options });
      return new Promise((resolve) => {
        releaseBooking = () => resolve(new Response(JSON.stringify({ payload: { taskId: "task-1" } }), { status: 200, headers: { "content-type": "application/json" } }));
      });
    }
    return providerFetch(url, options);
  };
  const confirmationArguments = { bookingDraftId: draft.bookingDraftId, preparedRequestHash: prepared.requestHash, confirmation: prepared.confirmation, preparedAt: prepared.preparedAt, expiresAt: prepared.expiresAt };
  const pendingConfirmation = callTool("tbank_hotels_confirm_booking", confirmationArguments);
  await new Promise((resolve) => setImmediate(resolve));
  await assert.rejects(callTool("tbank_hotels_confirm_booking", confirmationArguments), /already in progress/);
  releaseBooking();
  const confirmed = await pendingConfirmation;
  assert.equal(confirmed.status, 200);
  const bookingCalls = calls.filter((call) => call.url.endsWith("/api/v1/hotels/bookings/tasks/create"));
  assert.equal(bookingCalls.length, 1);
  assert.equal(bookingCalls[0].options.headers["x-real-ip"], "192.0.2.1");
  globalThis.fetch = providerFetch;

  const uncertainDraft = await callTool("tbank_hotels_create_booking_draft", { journeyId: plan.journeyId, bookingData: { guestContact: { email: "person@example.test", phone: "+70000000000" }, rooms: [{ guests: [{ firstName: "Ada", lastName: "Lovelace" }] }] } });
  await callTool("tbank_hotels_validate_checkout", { bookingDraftId: uncertainDraft.bookingDraftId });
  const uncertainPrepared = await callTool("tbank_hotels_prepare_draft_booking", { bookingDraftId: uncertainDraft.bookingDraftId });
  globalThis.fetch = async (url, options) => {
    if (String(url).endsWith("/api/v1/hotels/bookings/tasks/create")) {
      const error = new Error("simulated unknown outcome");
      error.name = "TimeoutError";
      throw error;
    }
    return providerFetch(url, options);
  };
  const uncertainArguments = { bookingDraftId: uncertainDraft.bookingDraftId, preparedRequestHash: uncertainPrepared.requestHash, confirmation: uncertainPrepared.confirmation, preparedAt: uncertainPrepared.preparedAt, expiresAt: uncertainPrepared.expiresAt };
  await assert.rejects(callTool("tbank_hotels_confirm_booking", uncertainArguments), /timed out/);
  await assert.rejects(callTool("tbank_hotels_confirm_booking", uncertainArguments), /outcome is unknown/);
  globalThis.fetch = providerFetch;

  await callTool("tbank_hotels_select_stay_option", { journeyId: plan.journeyId, optionId: plan.options[1].optionId });
  const emptyRates = await callTool("tbank_hotels_get_selected_stay_rates", { journeyId: plan.journeyId });
  assert.equal(emptyRates.status, "no_bookable_rates");
  assert.equal(emptyRates.canCreateBookingDraft, false);
  assert.deepEqual(emptyRates.rateOptions, []);
  assert.match(emptyRates.nextStep, /Do not request guest personal data/);
  assert.match(emptyRates.note, /cannot be used as a booking rate/);
  await assert.rejects(
    callTool("tbank_hotels_create_booking_draft", { journeyId: plan.journeyId, bookingData: { guestContact: { email: "person@example.test", phone: "+70000000000" }, rooms: [] } }),
    /Select one current rate/,
  );
  const overview = await callTool("tbank_hotels_get_booking_overview", { orderId: "order-1", includeVoucher: false });
  assert.equal(overview.booking.status, 200);
  const cancellation = await callTool("tbank_hotels_preview_cancellation", { orderId: "order-1" });
  assert.match(cancellation.note, /не вычисляет сумму возврата/);
  const repeated = await callTool("tbank_hotels_repeat_stay_plan", { journeyId: plan.journeyId, checkinDate: "2099-10-01", checkoutDate: "2099-10-02" });
  assert.equal(repeated.totalOptions, 2);
  assert.ok(calls.some((call) => call.url.endsWith("/api/v3/hotels/provider-1/rates")));
});

test("keeps a booking draft usable for human interaction and returns preview-only when mutations are disabled", async (t) => {
  const savedFetch = globalThis.fetch;
  const savedNow = Date.now;
  let currentTime = savedNow();
  Date.now = () => currentTime;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY", "TBANK_HOTELS_ENABLE_MUTATIONS"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  const calls = [];
  let checkoutAttempts = 0;
  let checkoutBehavior = "retry_once";
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  delete process.env.TBANK_HOTELS_ENABLE_MUTATIONS;
  let ratesAttempts = 0;
  globalThis.fetch = async (url, options) => {
    const target = String(url);
    calls.push({ target, method: options.method });
    if (target.endsWith("/api/v1/hotels/search")) {
      return new Response(JSON.stringify({ payload: { hotels: [
        { hotelId: "hotel-1", hotelName: "Hotel One", review: { rating: 9.0 } },
      ], isLoadingCompleted: true, nextOffset: null } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    if (target.endsWith("/api/v3/hotels/hotel-1/rates")) {
      ratesAttempts += 1;
      if (ratesAttempts === 1) {
        const error = new Error("simulated rates timeout");
        error.name = "TimeoutError";
        throw error;
      }
      return new Response(JSON.stringify({ payload: { rates: [
        { bookHash: "book-1", shownPrice: { value: 10000, currency: "RUB" }, mealName: "Breakfast" },
      ] } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    if (target.endsWith("/api/v3/rates/book-1")) {
      checkoutAttempts += 1;
      if (checkoutBehavior === "budget_exhausted") {
        currentTime += 12_500;
        const error = new Error("simulated slow timeout");
        error.name = "TimeoutError";
        throw error;
      }
      if (checkoutBehavior === "http_error") {
        return new Response(JSON.stringify({ code: "provider_failure" }), { status: 503, headers: { "content-type": "application/json" } });
      }
      if (checkoutBehavior === "retry_once" && checkoutAttempts === 1) {
        const error = new Error("simulated timeout");
        error.name = "TimeoutError";
        throw error;
      }
      return new Response(JSON.stringify({ payload: { bookHash: "book-1" } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    throw new Error(`unexpected provider call: ${target}`);
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    Date.now = savedNow;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const plan = await callTool("tbank_hotels_plan_stay", {
    destinationId: 17039,
    checkinDate: "2099-09-01",
    checkoutDate: "2099-09-02",
    rooms: [{ adults: 2 }],
  });
  assert.ok(Date.parse(plan.expiresAt) - Date.now() > 50 * 60 * 1_000);
  assert.doesNotMatch(plan.note, /15 минут/);
  await callTool("tbank_hotels_select_stay_option", { journeyId: plan.journeyId, optionId: plan.options[0].optionId });
  const rates = await callTool("tbank_hotels_get_selected_stay_rates", { journeyId: plan.journeyId });
  assert.equal(rates.attempts, 2);
  assert.equal(ratesAttempts, 2);
  const selectedRate = await callTool("tbank_hotels_select_stay_rate", { journeyId: plan.journeyId, rateOptionId: rates.rateOptions[0].rateOptionId });
  assert.equal(selectedRate.executionAvailable, false);
  assert.match(selectedRate.nextStep, /without requesting guest PII/);
  const preview = await callTool("tbank_hotels_create_booking_preview", { journeyId: plan.journeyId });
  assert.equal(preview.status, "preview_only");
  assert.equal(preview.executionAvailable, false);
  assert.equal(preview.personalDataCollected, false);
  assert.equal(preview.httpRequestPerformed, false);
  assert.match(preview.nextStep, /Do not request guest personal data/);
  assert.doesNotMatch(JSON.stringify(preview), /book-1|person@example|Ada|Lovelace/);
  currentTime += 59 * 60 * 1_000;
  const draft = await callTool("tbank_hotels_create_booking_draft", {
    journeyId: plan.journeyId,
    bookingData: {
      guestContact: { email: "person@example.test", phone: "+70000000000" },
      rooms: [{ guests: [{ firstName: "Ada", lastName: "Lovelace" }] }],
    },
  });
  const budgetDraft = await callTool("tbank_hotels_create_booking_draft", {
    journeyId: plan.journeyId,
    bookingData: { guestContact: { email: "person@example.test", phone: "+70000000000" }, rooms: [{ guests: [{ firstName: "Ada", lastName: "Lovelace" }] }] },
  });
  const providerErrorDraft = await callTool("tbank_hotels_create_booking_draft", {
    journeyId: plan.journeyId,
    bookingData: { guestContact: { email: "person@example.test", phone: "+70000000000" }, rooms: [{ guests: [{ firstName: "Ada", lastName: "Lovelace" }] }] },
  });
  assert.equal(draft.executionAvailable, false);
  assert.equal(draft.guestCoverage.namedGuestCountMatches, false);
  assert.ok(Date.parse(draft.expiresAt) - Date.now() > 50 * 60 * 1_000);
  currentTime += 2 * 60 * 1_000;
  await assert.rejects(callTool("tbank_hotels_get_stay_options", { journeyId: plan.journeyId }), /Unknown or expired journeyId/);
  const checkout = await callTool("tbank_hotels_validate_checkout", { bookingDraftId: draft.bookingDraftId });
  assert.equal(checkout.attempts, 2);
  assert.ok(Date.parse(checkout.validatedUntil) - Date.now() > 4 * 60 * 1_000);
  checkoutBehavior = "budget_exhausted";
  checkoutAttempts = 0;
  await assert.rejects(callTool("tbank_hotels_validate_checkout", { bookingDraftId: budgetDraft.bookingDraftId }), /timed out/);
  assert.equal(checkoutAttempts, 1);
  checkoutBehavior = "http_error";
  checkoutAttempts = 0;
  await assert.rejects(callTool("tbank_hotels_validate_checkout", { bookingDraftId: providerErrorDraft.bookingDraftId }), /HTTP 503/);
  assert.equal(checkoutAttempts, 1);
  const prepared = await callTool("tbank_hotels_prepare_draft_booking", { bookingDraftId: draft.bookingDraftId });
  assert.equal(prepared.status, "preview_only");
  assert.equal(prepared.executionAvailable, false);
  assert.equal(prepared.executionReadiness.status, "not_activated");
  assert.equal("requestHash" in prepared, false);
  assert.equal("confirmation" in prepared, false);
  assert.equal("preparedAt" in prepared, false);
  assert.equal("expiresAt" in prepared, false);
  assert.doesNotMatch(JSON.stringify(prepared), /TBANK_HOTELS_ENABLE_MUTATIONS/);
  currentTime += 6 * 60 * 1_000;
  await assert.rejects(callTool("tbank_hotels_prepare_draft_booking", { bookingDraftId: draft.bookingDraftId }), /must be fresh/);
  await assert.rejects(
    callTool("tbank_hotels_confirm_booking", { bookingDraftId: draft.bookingDraftId }),
    /mutation execution is not available/,
  );
  assert.equal(calls.some((call) => call.target.endsWith("/api/v1/hotels/bookings/tasks/create")), false);
});

test("returns a structured rates timeout after one internal retry", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  let ratesAttempts = 0;
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async (url) => {
    const target = String(url);
    if (target.endsWith("/api/v1/hotels/search")) {
      return new Response(JSON.stringify({ payload: { hotels: [
        { hotelId: "hotel-timeout", hotelName: "Timeout Hotel" },
      ], isLoadingCompleted: true, nextOffset: null } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    if (target.endsWith("/api/v3/hotels/hotel-timeout/rates")) {
      ratesAttempts += 1;
      const error = new Error("simulated timeout");
      error.name = "TimeoutError";
      throw error;
    }
    throw new Error(`unexpected provider call: ${target}`);
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const plan = await callTool("tbank_hotels_plan_stay", {
    destinationId: 17039,
    checkinDate: "2099-09-01",
    checkoutDate: "2099-09-02",
    rooms: [{ adults: 2 }],
  });
  await callTool("tbank_hotels_select_stay_option", { journeyId: plan.journeyId, optionId: plan.options[0].optionId });
  const rates = await callTool("tbank_hotels_get_selected_stay_rates", { journeyId: plan.journeyId });
  assert.equal(rates.status, "rates_temporarily_unavailable");
  assert.equal(rates.failureKind, "timeout");
  assert.equal(rates.attempts, 2);
  assert.equal(ratesAttempts, 2);
  assert.equal(rates.canCreateBookingDraft, false);
  assert.deepEqual(rates.rateOptions, []);
  assert.match(rates.nextStep, /Do not repeat the same tool call automatically/);
  await assert.rejects(
    callTool("tbank_hotels_create_booking_preview", { journeyId: plan.journeyId }),
    /Select one current rate/,
  );
});

test("collects paginated search results, ranks locally, and inherits ranking for comparison", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  const requestBodies = [];
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async (_url, options) => {
    const body = JSON.parse(options.body);
    requestBodies.push(body);
    const payload = body.offset === 0
      ? {
          hotels: [
            { hotelId: "hotel-a", hotelName: "Hotel A", review: { rating: 8.0 }, rateForHotelsFeed: { shownPrice: { value: 5000, currency: "RUB" } } },
          ],
          hotelsTotalCount: 3,
          filteredHotelsCount: 3,
          isLoadingCompleted: false,
          nextOffset: 2,
        }
      : {
          hotels: [
            { hotelId: "hotel-b", hotelName: "Hotel B", review: { rating: 9.8 }, rateForHotelsFeed: { shownPrice: { value: 7000, currency: "RUB" } } },
            { hotelId: "hotel-c", hotelName: "Hotel C", review: { rating: 9.2 }, rateForHotelsFeed: { shownPrice: { value: 6000, currency: "RUB" } } },
          ],
          hotelsTotalCount: 3,
          filteredHotelsCount: 3,
          isLoadingCompleted: true,
          nextOffset: null,
        };
    return new Response(JSON.stringify({ payload }), { status: 200, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const plan = await callTool("tbank_hotels_plan_stay", {
    destinationId: 17039,
    checkinDate: "2099-09-01",
    checkoutDate: "2099-09-02",
    rooms: [{ adults: 2 }],
    ranking: "highest_rating",
    maxOptions: 3,
  });
  assert.deepEqual(requestBodies.map((body) => ({ offset: body.offset, limit: body.limit, hasSort: "sort" in body })), [
    { offset: 0, limit: 50, hasSort: false },
    { offset: 2, limit: 50, hasSort: false },
  ]);
  assert.deepEqual(plan.options.map((option) => option.hotelName), ["Hotel B", "Hotel C", "Hotel A"]);
  assert.deepEqual(plan.searchCoverage, {
    fetchedHotelsCount: 3,
    hotelsTotalCount: 3,
    filteredHotelsCount: 3,
    isLoadingCompleted: true,
    truncated: false,
    requestCount: 2,
    providerSort: null,
    rankingAppliedLocally: "highest_rating",
    stoppedReason: null,
  });

  const comparison = await callTool("tbank_hotels_compare_stay_options", {
    journeyId: plan.journeyId,
    optionIds: [plan.options[2].optionId, plan.options[1].optionId],
    ranking: "highest_rating",
    limit: 2,
  });
  assert.equal(comparison.selectionStrategy, "highest_rating");
  assert.equal(comparison.selectionScope, "all_journey_options");
  assert.deepEqual(comparison.comparison.map((option) => option.hotelName), ["Hotel B", "Hotel C"]);
});

test("polls a partially loaded search page and deduplicates repeated hotels", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  const requestBodies = [];
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async (_url, options) => {
    const body = JSON.parse(options.body);
    requestBodies.push(body);
    const sharedHotel = { hotelId: "hotel-a", hotelName: "Hotel A", review: { rating: 8.0 } };
    const payload = requestBodies.length === 1
      ? { hotels: [sharedHotel], filteredHotelsCount: 2, isLoadingCompleted: false, nextOffset: 0 }
      : { hotels: [sharedHotel, { hotelId: "hotel-b", hotelName: "Hotel B", review: { rating: 9.0 } }], filteredHotelsCount: 2, isLoadingCompleted: true, nextOffset: null };
    return new Response(JSON.stringify({ payload }), { status: 200, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const plan = await callTool("tbank_hotels_plan_stay", {
    destinationId: 17039,
    checkinDate: "2099-09-01",
    checkoutDate: "2099-09-02",
    rooms: [{ adults: 2 }],
  });
  assert.equal(requestBodies.length, 2);
  assert.deepEqual(requestBodies.map(({ offset, limit }) => ({ offset, limit })), [
    { offset: 0, limit: 50 },
    { offset: 0, limit: 50 },
  ]);
  assert.equal(plan.totalOptions, 2);
  assert.equal(plan.searchCoverage.fetchedHotelsCount, 2);
  assert.equal(plan.searchCoverage.isLoadingCompleted, true);
  assert.equal(plan.searchCoverage.truncated, false);
});

test("returns accumulated search results when a follow-up page exhausts the collection time budget", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  let requestCount = 0;
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async () => {
    requestCount += 1;
    if (requestCount > 1) {
      const error = new Error("simulated timeout");
      error.name = "TimeoutError";
      throw error;
    }
    return new Response(JSON.stringify({ payload: {
      hotels: [{ hotelId: "hotel-a", hotelName: "Hotel A", review: { rating: 9.1 } }],
      filteredHotelsCount: 2,
      isLoadingCompleted: false,
      nextOffset: 1,
    } }), { status: 200, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const plan = await callTool("tbank_hotels_plan_stay", {
    destinationId: 17039,
    checkinDate: "2099-09-01",
    checkoutDate: "2099-09-02",
    rooms: [{ adults: 2 }],
    ranking: "highest_rating",
  });
  assert.equal(plan.status, "ready");
  assert.equal(plan.totalOptions, 1);
  assert.equal(plan.searchCoverage.requestCount, 2);
  assert.equal(plan.searchCoverage.truncated, true);
  assert.equal(plan.searchCoverage.stoppedReason, "time_budget");
});

test("rejects an invalid semantic stay plan before any provider call", async (t) => {
  const savedFetch = globalThis.fetch;
  let calls = 0;
  globalThis.fetch = async () => { calls += 1; throw new Error("unexpected network call"); };
  t.after(() => { globalThis.fetch = savedFetch; });
  await assert.rejects(
    callTool("tbank_hotels_plan_stay", { destinationId: 17039, checkinDate: "2099-09-02", checkoutDate: "2099-09-01", rooms: [{ adults: 2 }] }),
    /checkoutDate must be after checkinDate/,
  );
  assert.equal(calls, 0);
  await assert.rejects(
    callTool("tbank_hotels_plan_stay", { destinationId: 17039, checkinDate: "2099-02-30", checkoutDate: "2099-03-02", rooms: [{ adults: 2 }] }),
    /not a valid calendar date/,
  );
  await assert.rejects(
    callTool("tbank_hotels_plan_stay", { destinationId: 17039, checkinDate: "2000-01-01", checkoutDate: "2000-01-02", rooms: [{ adults: 2 }] }),
    /must not be in the past/,
  );
  assert.equal(calls, 0);
});

test("returns destination candidates without searching hotels when location is ambiguous", async (t) => {
  const savedFetch = globalThis.fetch;
  const savedBaseUrl = process.env.TBANK_HOTELS_API_BASE_URL;
  const savedKey = process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  const calls = [];
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async (url) => {
    calls.push(String(url));
    return new Response(JSON.stringify({ payload: { locations: [
      { locationId: 11, locationName: "Springfield", locationNameRu: "Спрингфилд", countryName: "Ambiguousland", countryNameRu: "Неоднозначная страна", hotelsCount: 20 },
      { locationId: 12, locationName: "Springfield", locationNameRu: "Спрингфилд", countryName: "Ambiguousland", countryNameRu: "Неоднозначная страна", hotelsCount: 10 },
    ] } }), { status: 200, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    if (savedBaseUrl === undefined) delete process.env.TBANK_HOTELS_API_BASE_URL; else process.env.TBANK_HOTELS_API_BASE_URL = savedBaseUrl;
    if (savedKey === undefined) delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY; else process.env.TBANK_HOTELS_JWT_PRIVATE_KEY = savedKey;
  });
  const result = await callTool("tbank_hotels_plan_stay", { destination: "Springfield", checkinDate: "2099-09-01", checkoutDate: "2099-09-02", rooms: [{ adults: 2 }], countryName: "Ambiguousland" });
  assert.equal(result.status, "clarification_required");
  assert.equal(result.reason, "destination_ambiguous");
  assert.equal(result.destinationCandidates.length, 2);
  assert.equal(calls.filter((url) => url.includes("/hotels/search")).length, 0);
});

test("resolves a destination from a later bounded catalog page", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  const calls = [];
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async (url) => {
    const target = new URL(url);
    calls.push(target);
    const offset = Number(target.searchParams.get("Offset"));
    const locations = offset === 0
      ? Array.from({ length: 100 }, (_, index) => ({ locationId: index + 1, locationName: `City ${index + 1}`, countryName: "Pagedland" }))
      : [{ locationId: 501, locationName: "Target City", countryName: "Pagedland" }];
    return new Response(JSON.stringify({ payload: { locations } }), { status: 200, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const result = await callTool("tbank_hotels_resolve_destination", { query: "Target City", countryName: "Pagedland" });
  assert.equal(result.status, "resolved");
  assert.equal(result.destination.destinationId, 501);
  assert.deepEqual(calls.map((target) => target.searchParams.get("Offset")), ["0", "100"]);
  assert.ok(calls.every((target) => target.searchParams.get("Limit") === "100"));
});

test("stops a cold location catalog before exceeding the shared MCP time budget", async (t) => {
  const savedFetch = globalThis.fetch;
  const savedNow = Date.now;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  let currentTime = savedNow();
  let calls = 0;
  Date.now = () => currentTime;
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async () => {
    calls += 1;
    currentTime += 9_500;
    const locations = Array.from({ length: 100 }, (_, index) => ({
      locationId: 50_000 + index,
      locationName: `Location ${index}`,
      countryName: "Budgetland",
    }));
    return new Response(JSON.stringify({ payload: { locations } }), { status: 200, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    Date.now = savedNow;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  await assert.rejects(
    callTool("tbank_hotels_resolve_destination", { query: "Missing", countryName: "Budgetland" }),
    /safe time budget/,
  );
  assert.equal(calls, 1);
});

test("filters an explicitly named hotel inside a resolved destination", async (t) => {
  const savedFetch = globalThis.fetch;
  const savedBaseUrl = process.env.TBANK_HOTELS_API_BASE_URL;
  const savedKey = process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async () => new Response(JSON.stringify({ payload: { hotels: [
    { hotelId: "one", hotelName: "Покровка 6 Отель", rateForHotelsFeed: { shownPrice: { value: 6200 } } },
    { hotelId: "two", hotelName: "Другой отель", rateForHotelsFeed: { shownPrice: { value: 4500 } } },
  ] } }), { status: 200, headers: { "content-type": "application/json" } });
  t.after(() => {
    globalThis.fetch = savedFetch;
    if (savedBaseUrl === undefined) delete process.env.TBANK_HOTELS_API_BASE_URL; else process.env.TBANK_HOTELS_API_BASE_URL = savedBaseUrl;
    if (savedKey === undefined) delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY; else process.env.TBANK_HOTELS_JWT_PRIVATE_KEY = savedKey;
  });
  const result = await callTool("tbank_hotels_plan_stay", { destinationId: 17039, hotelName: "покровка 6 отель", checkinDate: "2099-09-01", checkoutDate: "2099-09-02", rooms: [{ adults: 2 }] });
  assert.equal(result.status, "ready");
  assert.equal(result.hotelNameMatch, "exact");
  assert.equal(result.totalOptions, 1);
  assert.equal(result.options[0].hotelName, "Покровка 6 Отель");
});

test("reports safe provider error code and request id without returning response details", async (t) => {
  const savedFetch = globalThis.fetch;
  const savedBaseUrl = process.env.TBANK_HOTELS_API_BASE_URL;
  const savedKey = process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async () => new Response(JSON.stringify({ errorCode: "invalid_body_format", message: "sensitive provider detail" }), { status: 400, headers: { "content-type": "application/json", "x-request-id": "req-123" } });
  t.after(() => {
    globalThis.fetch = savedFetch;
    if (savedBaseUrl === undefined) delete process.env.TBANK_HOTELS_API_BASE_URL; else process.env.TBANK_HOTELS_API_BASE_URL = savedBaseUrl;
    if (savedKey === undefined) delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY; else process.env.TBANK_HOTELS_JWT_PRIVATE_KEY = savedKey;
  });
  await assert.rejects(
    callTool("tbank_hotels_search", { payload: { destinationId: 17039, checkinDate: "2099-09-01", checkoutDate: "2099-09-02", guests: [{ adultsCount: 2 }] } }),
    (error) => {
      assert.match(error.message, /HTTP 400 \(code: invalid_body_format, requestId: req-123\)/);
      assert.doesNotMatch(error.message, /sensitive provider detail/);
      return true;
    },
  );
});

test("refuses to rank numeric prices across mixed currencies", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  let hotels = [
    { hotelId: "rub", hotelName: "Ruble Hotel", rateForHotelsFeed: { shownPrice: { value: 9000, currency: "RUB" } } },
    { hotelId: "usd", hotelName: "Dollar Hotel", rateForHotelsFeed: { shownPrice: { value: 100, currency: "USD" } } },
  ];
  globalThis.fetch = async () => new Response(JSON.stringify({ payload: { hotels } }), { status: 200, headers: { "content-type": "application/json" } });
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const plan = await callTool("tbank_hotels_plan_stay", { destinationId: 17039, checkinDate: "2099-09-01", checkoutDate: "2099-09-02", rooms: [{ adults: 2 }] });
  await assert.rejects(
    callTool("tbank_hotels_get_stay_options", { journeyId: plan.journeyId, ranking: "lowest_price" }),
    /different or unknown currencies/,
  );
  hotels = [
    { hotelId: "unknown-a", hotelName: "Unknown A", rateForHotelsFeed: { shownPrice: { value: 100 } } },
    { hotelId: "unknown-b", hotelName: "Unknown B", rateForHotelsFeed: { shownPrice: { value: 200 } } },
  ];
  const unknownCurrencyPlan = await callTool("tbank_hotels_plan_stay", { destinationId: 17039, checkinDate: "2099-10-01", checkoutDate: "2099-10-02", rooms: [{ adults: 2 }] });
  await assert.rejects(
    callTool("tbank_hotels_get_stay_options", { journeyId: unknownCurrencyPlan.journeyId, ranking: "lowest_price" }),
    /different or unknown currencies/,
  );
});

test("rejects an oversized provider response before parsing it", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async () => new Response("{}", { status: 200, headers: { "content-type": "application/json", "content-length": String(3 * 1_024 * 1_024) } });
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  await assert.rejects(
    callTool("tbank_hotels_search", { payload: { destinationId: 17039, checkinDate: "2099-09-01", checkoutDate: "2099-09-02", guests: [{ adultsCount: 2 }] } }),
    /response exceeded the safe size limit/,
  );
});

test("uses the confirmed request schema for every SEO API version", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  const calls = [];
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async (url, options) => {
    calls.push({ url: String(url), body: JSON.parse(options.body) });
    return new Response(JSON.stringify({ payload: { hotels: [], totalCount: 0 } }), { status: 200, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  await callTool("tbank_hotels_search_seo", { apiVersion: "v1", payload: { destinationId: 17039, hostelIsNeeded: false } });
  await callTool("tbank_hotels_search_seo", { apiVersion: "v2", payload: { locationId: 17039, offset: 0, limit: 10 } });
  await callTool("tbank_hotels_search_seo", { payload: { country: "Россия", location: "Москва", offset: 0, limit: 10 } });
  assert.deepEqual(calls.map(({ url, body }) => ({ path: new URL(url).pathname, body })), [
    { path: "/api/v1/seo/search", body: { destinationId: 17039, hostelIsNeeded: false } },
    { path: "/api/v2/seo/search", body: { locationId: 17039, offset: 0, limit: 10 } },
    { path: "/api/v3/seo/search", body: { country: "Россия", location: "Москва", offset: 0, limit: 10 } },
  ]);
  await assert.rejects(
    callTool("tbank_hotels_search_seo", { apiVersion: "v3", payload: { destinationId: 17039 } }),
    /payload.*country/,
  );
});

test("payment setup sends no request body after an exact confirmation", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY", "TBANK_HOTELS_ENABLE_MUTATIONS"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  const calls = [];
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  process.env.TBANK_HOTELS_ENABLE_MUTATIONS = "true";
  globalThis.fetch = async (url, options) => {
    calls.push({ url: String(url), options });
    return new Response(JSON.stringify({ payload: { paymentToken: "provider-token" } }), { status: 200, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const prepared = await callTool("tbank_hotels_prepare_payment_setup", { orderId: "order-1" });
  await callTool("tbank_hotels_execute_payment_setup", { orderId: "order-1", preparedRequestHash: prepared.requestHash, confirmation: prepared.confirmation, preparedAt: prepared.preparedAt, expiresAt: prepared.expiresAt });
  assert.equal(calls.length, 1);
  assert.equal(new URL(calls[0].url).pathname, "/api/v1/hotels/bookings/shevo/order-1/payment/setup");
  assert.equal(calls[0].options.body, undefined);
  assert.equal(calls[0].options.redirect, "error");
});

test("requires and forwards trusted x-real-ip for LS booking execution", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_AUTH_HEADERS_JSON", "TBANK_HOTELS_JWT_PRIVATE_KEY", "TBANK_HOTELS_ENABLE_MUTATIONS"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  const calls = [];
  let executionMode = "success";
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_HEADERS_JSON = JSON.stringify({ Authorization: "test-customer-auth", "x-real-ip": "192.0.2.1" });
  process.env.TBANK_HOTELS_ENABLE_MUTATIONS = "true";
  delete process.env.TBANK_HOTELS_AUTH_TOKEN;
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async (url, options) => {
    calls.push({ url: String(url), options });
    if (executionMode === "timeout") {
      const error = new Error("simulated unknown outcome");
      error.name = "TimeoutError";
      throw error;
    }
    return new Response(JSON.stringify({ payload: { taskId: "ls-task-1" } }), { status: 200, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const payload = {
    bookHash: "book-1",
    guestContact: { email: "person@example.test", phone: "+70000000000" },
    rooms: [{ guests: [{ firstName: "Ada", lastName: "Lovelace" }] }],
  };
  const prepared = await callTool("tbank_hotels_prepare_ls_booking", { payload });
  assert.equal(prepared.executionAvailable, true);
  await callTool("tbank_hotels_execute_ls_booking", { payload, preparedRequestHash: prepared.requestHash, confirmation: prepared.confirmation, preparedAt: prepared.preparedAt, expiresAt: prepared.expiresAt });
  assert.equal(calls.length, 1);
  assert.equal(calls[0].options.headers["x-real-ip"], "192.0.2.1");
  await assert.rejects(
    callTool("tbank_hotels_execute_ls_booking", { payload, preparedRequestHash: prepared.requestHash, confirmation: prepared.confirmation, preparedAt: prepared.preparedAt, expiresAt: prepared.expiresAt }),
    /already completed/,
  );
  assert.equal(calls.length, 1);

  const uncertainPayload = { ...payload, bookHash: "book-2" };
  const uncertainPrepared = await callTool("tbank_hotels_prepare_ls_booking", { payload: uncertainPayload });
  executionMode = "timeout";
  const uncertainArguments = { payload: uncertainPayload, preparedRequestHash: uncertainPrepared.requestHash, confirmation: uncertainPrepared.confirmation, preparedAt: uncertainPrepared.preparedAt, expiresAt: uncertainPrepared.expiresAt };
  await assert.rejects(callTool("tbank_hotels_execute_ls_booking", uncertainArguments), /timed out/);
  await assert.rejects(callTool("tbank_hotels_execute_ls_booking", uncertainArguments), /outcome is unknown/);
  assert.equal(calls.length, 2);

  process.env.TBANK_HOTELS_AUTH_HEADERS_JSON = JSON.stringify({ Authorization: "test-customer-auth" });
  const blocked = await callTool("tbank_hotels_prepare_ls_booking", { payload });
  assert.equal(blocked.status, "preview_only");
  assert.equal(blocked.executionReadiness.status, "required_trusted_headers_not_configured");
  assert.deepEqual(blocked.executionReadiness.missingRequiredHeaders, ["x-real-ip"]);
});

test("uses typed booking-list and tranche inputs and sends no BNPL request body", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  const calls = [];
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async (url, options) => {
    calls.push({ path: new URL(url).pathname, options });
    return new Response(JSON.stringify({ payload: {} }), { status: 200, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  await callTool("tbank_hotels_list_bookings", { isActiveRequired: true, isCancelledRequired: false, isCompletedRequired: true });
  await callTool("tbank_hotels_get_available_tranche_amount", { accounts: [{ accountId: "account-1", type: "Debit", balance: 1250.5 }] });
  await callTool("tbank_hotels_get_bnpl_offer", { orderId: "order-1", language: "ru-RU" });
  assert.deepEqual(JSON.parse(calls[0].options.body), { isActiveRequired: true, isCancelledRequired: false, isCompletedRequired: true });
  assert.deepEqual(JSON.parse(calls[1].options.body), { accounts: [{ accountId: "account-1", type: "Debit", balance: 1250.5 }] });
  assert.equal(calls[2].options.body, undefined);
  assert.deepEqual(calls.map((call) => call.path), [
    "/api/v1/hotels/bookings/booking_list",
    "/api/v1/tranches/available/amount",
    "/api/v1/hotels/bookings/evo/order-1/bnpl_offer",
  ]);
  await assert.rejects(
    callTool("tbank_hotels_list_bookings", { isActiveRequired: true, isCancelledRequired: false }),
    /isCompletedRequired must be a boolean/,
  );
  await assert.rejects(
    callTool("tbank_hotels_get_available_tranche_amount", { accounts: [{ accountId: "account-1", type: "Debit", balance: "1250" }] }),
    /balance must be a finite number/,
  );
  assert.equal(calls.length, 3);
});

test("prepares typed cancellation, promocode, and extra-services bodies without HTTP", async () => {
  const cancellation = await callTool("tbank_hotels_prepare_cancel_booking", { orderId: "order-1" });
  assert.deepEqual(cancellation.payloadPreview, { orderId: "order-1" });
  const promocode = await callTool("tbank_hotels_prepare_apply_promocode", { bookHash: "book-1", promocode: "SUMMER" });
  assert.deepEqual(promocode.payloadPreview, { promocode: "SUMMER" });
  const extraServices = await callTool("tbank_hotels_prepare_update_extra_services", { bookHash: "book-1", extraServiceIds: ["breakfast", "transfer"] });
  assert.deepEqual(extraServices.payloadPreview, { extraServiceIds: ["breakfast", "transfer"] });
  await assert.rejects(
    callTool("tbank_hotels_prepare_update_extra_services", { bookHash: "book-1", extraServiceIds: [""] }),
    /extraServiceIds must be an array of non-empty strings/,
  );
});

test("validates the documented booking and LS booking request shapes before HTTP", async () => {
  const bookingPayload = {
    bookHash: "book-1",
    guestContact: { email: "person@example.test", phone: "+70000000000" },
    rooms: [{ guests: [{ firstName: "Ada", lastName: "Lovelace" }] }],
    userData: { ssoId: "sso-1", siebelId: null, phoneNumber: "+70000000000" },
    userIp: "192.0.2.1",
  };
  const prepared = await callTool("tbank_hotels_prepare_booking", { payload: bookingPayload });
  assert.match(prepared.payloadPreview.guestContact.email, /REDACTED/);
  assert.match(prepared.payloadPreview.userData.ssoId, /REDACTED/);
  assert.match(prepared.payloadPreview.userIp, /REDACTED/);
  await assert.rejects(
    callTool("tbank_hotels_prepare_booking", { payload: { ...bookingPayload, guestContact: { email: "person@example.test" } } }),
    /guestContact.phone must be a non-empty string/,
  );
  await assert.rejects(
    callTool("tbank_hotels_prepare_ls_booking", { payload: { ...bookingPayload, userData: undefined, userIp: undefined, paymentMeans: "dolyame" } }),
    /unsupported fields/,
  );
  const lsPrepared = await callTool("tbank_hotels_prepare_ls_booking", {
    payload: {
      bookHash: "book-1",
      guestContact: { email: "person@example.test", phone: "+70000000000" },
      rooms: [{ guests: [{ firstName: "Ada", lastName: "Lovelace" }] }],
    },
  });
  assert.equal(lsPrepared.action, "lsBooking");
});

test("does not expose configured auth secrets", async (t) => {
  const server = startServer({ TBANK_HOTELS_API_BASE_URL: "https://hotels.example.test", TBANK_HOTELS_AUTH_TOKEN: "super-secret" });
  t.after(() => server.child.kill());
  const result = await server.request({ jsonrpc: "2.0", id: 1, method: "tools/call", params: { name: "tbank_hotels_connection_status", arguments: {} } });
  assert.equal(result.result.isError, false);
  assert.match(result.result.content[0].text, /configured/);
  assert.equal(JSON.parse(result.result.content[0].text).ready, true);
  assert.doesNotMatch(result.result.content[0].text, /super-secret|Authorization/);
});

test("connection status validates service JWT configuration without making a network request", async (t) => {
  const server = startServer({
    TBANK_HOTELS_API_BASE_URL: "https://hotels.example.test",
    TBANK_HOTELS_JWT_PRIVATE_KEY: "not-a-private-key",
    TBANK_HOTELS_JWT_ISSUER: "HOTELSSEARCHAPI",
    TBANK_HOTELS_JWT_AUDIENCE: "HOTELSAPI",
    TBANK_HOTELS_AUTH_TOKEN: "",
    TBANK_HOTELS_AUTH_HEADERS_JSON: "",
  });
  t.after(() => server.child.kill());
  const result = await server.request({ jsonrpc: "2.0", id: 1, method: "tools/call", params: { name: "tbank_hotels_connection_status", arguments: {} } });
  const status = JSON.parse(result.result.content[0].text);
  assert.equal(status.ready, false);
  assert.equal(status.transport, "configured");
  assert.equal(status.authentication, "invalid_configuration");
  assert.equal(status.customerContext, "not_configured");
  assert.equal(status.canReadCustomer, false);
  assert.equal(status.bookingExecution.available, false);
  assert.equal(status.bookingExecution.status, "not_activated");
  assert.match(status.diagnostics.authentication, /Unable to create Hotels service JWT/);
  assert.doesNotMatch(result.result.content[0].text, /not-a-private-key/);
  const customer = await server.request({ jsonrpc: "2.0", id: 2, method: "tools/call", params: { name: "tbank_hotels_get_customer", arguments: {} } });
  assert.equal(customer.result.isError, true);
  assert.match(customer.result.content[0].text, /service_jwt.*cannot autofill booking guest data/);
  assert.doesNotMatch(customer.result.content[0].text, /not-a-private-key/);
});

test("creates the configured HotelsApiPrivate RS384 service JWT without exposing its key", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = [
    "TBANK_HOTELS_API_BASE_URL",
    "TBANK_HOTELS_AUTH_TOKEN",
    "TBANK_HOTELS_AUTH_HEADER",
    "TBANK_HOTELS_AUTH_HEADERS_JSON",
    "TBANK_HOTELS_JWT_PRIVATE_KEY",
    "TBANK_HOTELS_JWT_ISSUER",
    "TBANK_HOTELS_JWT_AUDIENCE",
    "TBANK_HOTELS_JWT_AUTH_HEADER",
    "TBANK_HOTELS_JWT_AUTH_PREFIX",
    "TBANK_HOTELS_ENABLE_MUTATIONS",
  ];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  const { privateKey, publicKey } = generateKeyPairSync("rsa", { modulusLength: 2048 });
  const calls = [];
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels-private.example.test/";
  process.env.TBANK_HOTELS_JWT_PRIVATE_KEY = privateKey.export({ type: "pkcs1", format: "pem" }).toString();
  process.env.TBANK_HOTELS_JWT_ISSUER = "HOTELSSEARCHAPI";
  process.env.TBANK_HOTELS_JWT_AUDIENCE = "HOTELSAPI";
  delete process.env.TBANK_HOTELS_AUTH_TOKEN;
  delete process.env.TBANK_HOTELS_AUTH_HEADER;
  delete process.env.TBANK_HOTELS_AUTH_HEADERS_JSON;
  delete process.env.TBANK_HOTELS_JWT_AUTH_HEADER;
  delete process.env.TBANK_HOTELS_JWT_AUTH_PREFIX;
  process.env.TBANK_HOTELS_ENABLE_MUTATIONS = "true";
  globalThis.fetch = async (_url, options) => {
    calls.push(options);
    return new Response(JSON.stringify({ payload: {} }), { status: 200, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const status = await callTool("tbank_hotels_connection_status");
  assert.equal(status.authMode, "service_jwt");
  assert.equal(status.searchReady, true);
  assert.equal(status.bookingExecution.available, false);
  assert.equal(status.bookingExecution.status, "required_trusted_headers_not_configured");
  assert.deepEqual(status.bookingExecution.missingRequiredHeaders, ["x-real-ip"]);
  await callTool("tbank_hotels_search", { payload: { destinationId: 1, checkinDate: "2099-09-01", checkoutDate: "2099-09-02", guests: [{ adultsCount: 2 }] } });
  const authorization = calls[0].headers.Authorization;
  assert.match(authorization, /^BearereyJ/);
  const [encodedHeader, encodedClaims, encodedSignature] = authorization.slice("Bearer".length).split(".");
  assert.deepEqual(JSON.parse(Buffer.from(encodedHeader, "base64url")), { alg: "RS384", typ: "JWT" });
  const claims = JSON.parse(Buffer.from(encodedClaims, "base64url"));
  assert.equal(claims.iss, "HOTELSSEARCHAPI");
  assert.deepEqual(claims.aud, ["HOTELSAPI"]);
  assert.equal(typeof claims.iat, "number");
  assert.equal("exp" in claims, false);
  assert.equal(verify("RSA-SHA384", Buffer.from(`${encodedHeader}.${encodedClaims}`), publicKey, Buffer.from(encodedSignature, "base64url")), true);
  assert.doesNotMatch(JSON.stringify(status), /BEGIN RSA PRIVATE KEY/);
});

test("does not attempt a provider request until API base URL is configured", async (t) => {
  const server = startServer();
  t.after(() => server.child.kill());
  const result = await server.request({ jsonrpc: "2.0", id: 1, method: "tools/call", params: { name: "tbank_hotels_search", arguments: { language: "ru-RU", payload: { destinationId: 1, checkinDate: "2099-09-01", checkoutDate: "2099-09-02", guests: [{ adultsCount: 2 }] } } } });
  assert.equal(result.result.isError, true);
  assert.match(result.result.content[0].text, /TBANK_HOTELS_API_BASE_URL is required/);
});

test("prepare is stateless and execute rejects a changed booking payload", async (t) => {
  const server = startServer({
    TBANK_HOTELS_API_BASE_URL: "https://hotels.example.test/",
    TBANK_HOTELS_AUTH_HEADERS_JSON: JSON.stringify({ Authorization: "test-customer-auth", "x-real-ip": "192.0.2.1" }),
    TBANK_HOTELS_ENABLE_MUTATIONS: "true",
  });
  t.after(() => server.child.kill());
  const payload = { bookHash: "hash", guestContact: { email: "person@example.test", phone: "+70000000000" }, rooms: [] };
  const prepared = await server.request({ jsonrpc: "2.0", id: 1, method: "tools/call", params: { name: "tbank_hotels_prepare_booking", arguments: { payload } } });
  assert.equal(prepared.result.isError, false);
  const preview = JSON.parse(prepared.result.content[0].text);
  assert.match(prepared.result.content[0].text, /REDACTED/);
  const executed = await server.request({ jsonrpc: "2.0", id: 2, method: "tools/call", params: { name: "tbank_hotels_execute_booking", arguments: { payload: { ...payload, bookHash: "changed" }, preparedRequestHash: preview.requestHash, confirmation: preview.confirmation, preparedAt: preview.preparedAt, expiresAt: preview.expiresAt } } });
  assert.equal(executed.result.isError, true);
  assert.match(executed.result.content[0].text, /does not match/);
});

test("matching prepared confirmation is blocked while mutations are disabled", async (t) => {
  const server = startServer();
  t.after(() => server.child.kill());
  const payload = { bookHash: "hash", guestContact: { email: "person@example.test", phone: "+70000000000" }, rooms: [] };
  const prepared = await server.request({ jsonrpc: "2.0", id: 1, method: "tools/call", params: { name: "tbank_hotels_prepare_booking", arguments: { payload } } });
  const preview = JSON.parse(prepared.result.content[0].text);
  assert.equal(preview.status, "preview_only");
  assert.equal(preview.executionAvailable, false);
  assert.equal("requestHash" in preview, false);
  assert.equal("confirmation" in preview, false);
  assert.doesNotMatch(JSON.stringify(preview), /TBANK_HOTELS_ENABLE_MUTATIONS/);
  const executed = await server.request({ jsonrpc: "2.0", id: 2, method: "tools/call", params: { name: "tbank_hotels_execute_booking", arguments: { payload, preparedRequestHash: "0".repeat(64), confirmation: "not-used", preparedAt: new Date().toISOString(), expiresAt: new Date(Date.now() + 60_000).toISOString() } } });
  assert.equal(executed.result.isError, true);
  assert.match(executed.result.content[0].text, /mutation execution is not available/);
});

test("rejects an expired prepared mutation before reaching transport", async (t) => {
  const server = startServer({
    TBANK_HOTELS_API_BASE_URL: "https://hotels.example.test/",
    TBANK_HOTELS_AUTH_HEADERS_JSON: JSON.stringify({ Authorization: "test-customer-auth", "x-real-ip": "192.0.2.1" }),
    TBANK_HOTELS_ENABLE_MUTATIONS: "true",
  });
  t.after(() => server.child.kill());
  const payload = { bookHash: "hash", guestContact: { email: "person@example.test", phone: "+70000000000" }, rooms: [] };
  const preparedAt = new Date(Date.now() - 10 * 60 * 1_000).toISOString();
  const expiresAt = new Date(Date.now() - 5 * 60 * 1_000).toISOString();
  const material = JSON.stringify({ action: "booking", path: "/api/v1/hotels/bookings/tasks/create", payload, preparedAt, expiresAt });
  const requestHash = createHash("sha256").update(material).digest("hex");
  const confirmation = `CONFIRM_TBANK_HOTELS_BOOKING_${requestHash.slice(0, 12)}`;
  const executed = await server.request({ jsonrpc: "2.0", id: 1, method: "tools/call", params: { name: "tbank_hotels_execute_booking", arguments: { payload, preparedRequestHash: requestHash, confirmation, preparedAt, expiresAt } } });
  assert.equal(executed.result.isError, true);
  assert.match(executed.result.content[0].text, /has expired/);
  assert.doesNotMatch(executed.result.content[0].text, /BASE_URL/);
});
