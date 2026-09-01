import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { createHash, generateKeyPairSync, verify } from "node:crypto";
import { EventEmitter } from "node:events";
import { chmodSync, mkdtempSync, readFileSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { resolve } from "node:path";
import test from "node:test";
import { SERVER_VERSION } from "../src/config.mjs";
import { hostedCheckoutTarget } from "../src/checkout-handoff.mjs";
import { normalizeCheckoutInspection } from "../src/checkout-inspection.mjs";
import { runtimeHandledToolNames } from "../src/runtime.mjs";
import { callTool, setAuthBrokerConnectorForTests } from "../src/server.mjs";
import { tools } from "../src/tool-contracts.mjs";

const serverPath = new URL("../src/server.mjs", import.meta.url).pathname;
const packageVersion = JSON.parse(readFileSync(new URL("../package.json", import.meta.url), "utf8")).version;

test("builds a selected-hotel handoff with only verified public search parameters", () => {
  const simple = hostedCheckoutTarget("1488990", {
    checkinDate: "2026-09-15",
    checkoutDate: "2026-09-16",
    occupancy: [{ roomIndex: 0, adults: 2, childrenAges: [] }],
  }, {});
  assert.equal(simple.url, "https://www.tbank.ru/travel/hotels/new/hotels/1488990/?dateFrom=2026-09-15&dateTo=2026-09-16&guests=2");
  assert.equal(simple.searchCriteriaPreserved, true);
  assert.equal(simple.searchCriteriaPreservationScope, "dates_and_single_room_adults");
  assert.equal(simple.roomCompositionPreserved, true);

  const family = hostedCheckoutTarget("1488990", {
    checkinDate: "2026-09-15",
    checkoutDate: "2026-09-16",
    occupancy: [{ roomIndex: 0, adults: 2, childrenAges: [7] }],
  }, {});
  assert.equal(family.url, "https://www.tbank.ru/travel/hotels/new/hotels/1488990/?dateFrom=2026-09-15&dateTo=2026-09-16");
  assert.equal(family.searchCriteriaPreserved, false);
  assert.equal(family.searchCriteriaPreservationScope, "dates_only");
  assert.equal(family.guestCountPreserved, false);
  assert.equal(family.childrenAgesPreserved, false);
});

const checkoutNormalizerOptions = (expectedBookHash) => ({
  expectedBookHash,
  serviceReference: (kind) => `checkout_extra_${kind === "early_check_in" ? "1" : "2"}`,
  formatMoney: (amount, currency) => amount === null ? null : `${amount} ${currency ?? ""}`.trim(),
  formatTimestamp: (value) => value ?? null,
});

test("normalizes the v1 checkout wrapper and rejects a foreign wrapper rate", () => {
  const normalized = normalizeCheckoutInspection({ payload: {
    rate: {
      bookHash: "expected-book",
      shownPrice: { amount: 12000, currency: "RUB" },
      paymentPrice: { amount: 11500, currency: "RUB" },
      cancellationPolicyRules: { freeCancellationUntil: "2099-09-14T00:00:00+03:00", policies: [] },
    },
    promocodeInfo: { status: "applied", value: { amount: 500, currency: "RUB" } },
    cashbackInfo: { cbServiceName: "Hotels", accounts: [{ accountNumber: "must-not-leak", cashbackAmount: 1000 }] },
  } }, checkoutNormalizerOptions("expected-book"));

  assert.equal(normalized.prices.shown.amount, 12000);
  assert.equal(normalized.prices.payment.amount, 11500);
  assert.equal(normalized.promocode.present, true);
  assert.equal(normalized.cashback.options[0].cashbackAmount, 1000);
  assert.doesNotMatch(JSON.stringify(normalized), /expected-book|must-not-leak/);
  assert.throws(
    () => normalizeCheckoutInspection({ payload: { rate: { bookHash: "foreign-book" } } }, checkoutNormalizerOptions("expected-book")),
    /does not contain a checkout rate/,
  );
});

test("rejects a v3 checkout response without the selected bookHash", () => {
  assert.throws(
    () => normalizeCheckoutInspection({ payload: { roomsForBooking: { rooms: [{ rates: [
      { bookHash: "foreign-book", shownPrice: { amount: 1, currency: "RUB" } },
    ] }] } } }, checkoutNormalizerOptions("expected-book")),
    /does not contain a checkout rate/,
  );
});

// Direct callTool tests execute in this process. Remove all real Hotels/broker
// settings before any test can observe a developer's credentials or local
// auth broker; individual tests install only their own fixtures afterwards.
for (const name of Object.keys(process.env)) {
  if (name.startsWith("TBANK_HOTELS_") || name.startsWith("TBANK_AUTH_BROKER_")) {
    delete process.env[name];
  }
}

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
  assert.equal(status.serverVersion, SERVER_VERSION);
  assert.equal(status.ready, false);
  assert.equal(status.searchReady, false);
  assert.equal(status.transport, "not_configured");
  assert.equal(status.authentication, "not_required");
  assert.equal(status.authMode, "anonymous");
  assert.doesNotMatch(result.result.content[0].text, /production-like|parent-secret|parent-private-key/);
});

test("reports API MCP metadata and no browser tools", async (t) => {
  const server = startServer();
  t.after(() => server.child.kill());
  const initialized = await server.request({ jsonrpc: "2.0", id: 1, method: "initialize", params: { protocolVersion: "2025-03-26" } });
  assert.equal(initialized.result.serverInfo.name, "tbank-hotels-api-mcp");
  assert.equal(initialized.result.serverInfo.version, SERVER_VERSION);
  assert.match(initialized.result.instructions, /tbank_hotels_create_checkout_handoff/);
  assert.match(initialized.result.instructions, /remains available when direct booking execution is unavailable/);
  assert.match(initialized.result.instructions, /destination, checkinDate, checkoutDate and rooms/);
  assert.match(initialized.result.instructions, /Compatibility aliases location, guests, limit/);
  const listed = await server.request({ jsonrpc: "2.0", id: 2, method: "tools/list" });
  const names = listed.result.tools.map((tool) => tool.name);
  assert.ok(names.includes("tbank_hotels_search"));
  assert.ok(names.includes("tbank_hotels_execute_booking"));
  assert.ok(names.includes("tbank_hotels_resolve_destination"));
  assert.ok(names.includes("tbank_hotels_plan_stay"));
  assert.ok(names.includes("tbank_hotels_continue_stay_search"));
  assert.ok(names.includes("tbank_hotels_compare_stay_options"));
  assert.ok(!names.some((name) => /browser|snapshot|cookie|open_/.test(name)));
  assert.ok(!names.some((name) => /card_data|save_credit_card|finger.?print|3ds|tds/i.test(name)));
  const planTool = listed.result.tools.find((tool) => tool.name === "tbank_hotels_plan_stay");
  assert.ok(planTool.inputSchema.properties.destination);
  assert.ok(planTool.inputSchema.properties.location);
  assert.ok(planTool.inputSchema.properties.rooms);
  assert.ok(planTool.inputSchema.properties.guests);
  assert.ok(planTool.inputSchema.properties.limit);
  assert.ok(planTool.inputSchema.properties.rooms.items.properties.adultsCount);
  assert.match(planTool.description, /destination, checkinDate, checkoutDate, rooms/);
  assert.match(planTool.description, /location→destination, guests→rooms, limit→maxOptions/);
  assert.equal(planTool.inputSchema.properties.breakfastIncluded.type, "boolean");
  assert.ok(!planTool.inputSchema.properties.searchRequest);
  assert.equal(planTool.annotations.readOnlyHint, true);
  const continueSearch = listed.result.tools.find((tool) => tool.name === "tbank_hotels_continue_stay_search");
  assert.deepEqual(continueSearch.inputSchema.required, ["journeyId"]);
  assert.equal(continueSearch.annotations.readOnlyHint, true);
  assert.match(continueSearch.description, /не более одного раза/);
  const lowLevelSearch = listed.result.tools.find((tool) => tool.name === "tbank_hotels_search");
  const filterSchema = lowLevelSearch.inputSchema.properties.payload.properties.filters.items;
  assert.equal(filterSchema.oneOf.length, 4);
  assert.deepEqual(filterSchema.oneOf.map((schema) => schema.properties.$objectType.const), ["array", "boolean", "radio", "range"]);
  assert.ok(filterSchema.oneOf.every((schema) => schema.additionalProperties === false));
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
  assert.match(bookingPreview.description, /tbank_hotels_create_checkout_handoff/);
  const inspectCheckout = listed.result.tools.find((tool) => tool.name === "tbank_hotels_inspect_checkout");
  assert.deepEqual(inspectCheckout.inputSchema.required, ["journeyId"]);
  assert.deepEqual(Object.keys(inspectCheckout.inputSchema.properties), ["journeyId", "promocode", "includeUpgradeOffer", "language"]);
  assert.equal(inspectCheckout.annotations.readOnlyHint, true);
  assert.doesNotMatch(JSON.stringify(inspectCheckout.inputSchema), /bookHash|checkOutId|hotelId|extraServiceIds|email|phone|pan|cvv|otp/i);
  const previewCheckoutChanges = listed.result.tools.find((tool) => tool.name === "tbank_hotels_preview_checkout_changes");
  assert.deepEqual(previewCheckoutChanges.inputSchema.required, ["journeyId"]);
  assert.equal(previewCheckoutChanges.annotations.readOnlyHint, true);
  assert.deepEqual(previewCheckoutChanges.inputSchema.properties.promocodeAction.enum, ["unchanged", "apply_validated"]);
  assert.match(previewCheckoutChanges.inputSchema.properties.extraServiceOptionRefs.items.pattern, /checkout_extra_/);
  assert.doesNotMatch(JSON.stringify(previewCheckoutChanges.inputSchema), /bookHash|checkOutId|hotelId|extraServiceIds|email|phone|pan|cvv|otp/i);
  const paymentFormPreview = listed.result.tools.find((tool) => tool.name === "tbank_hotels_create_payment_form_preview");
  assert.deepEqual(paymentFormPreview.inputSchema.required, ["journeyId"]);
  assert.deepEqual(Object.keys(paymentFormPreview.inputSchema.properties), ["journeyId"]);
  assert.equal(paymentFormPreview.annotations.readOnlyHint, true);
  assert.doesNotMatch(JSON.stringify(paymentFormPreview.inputSchema), /pan|cvv|cvc|otp|pin|cardNumber|successUrl|failUrl/i);
  const checkoutHandoff = listed.result.tools.find((tool) => tool.name === "tbank_hotels_create_checkout_handoff");
  assert.deepEqual(checkoutHandoff.inputSchema.required, ["journeyId"]);
  assert.deepEqual(Object.keys(checkoutHandoff.inputSchema.properties), ["journeyId"]);
  assert.equal(checkoutHandoff.annotations.readOnlyHint, true);
  assert.match(checkoutHandoff.description, /direct booking execution недоступен/);
  assert.match(checkoutHandoff.description, /Покажите ссылку пользователю/);
  assert.doesNotMatch(JSON.stringify(checkoutHandoff.inputSchema), /pan|cvv|cvc|otp|pin|cardNumber|email|phone|bookHash/i);
  const saveVoucher = listed.result.tools.find((tool) => tool.name === "tbank_hotels_save_voucher");
  assert.deepEqual(saveVoucher.inputSchema.required, ["bookingRef"]);
  assert.ok(!saveVoucher.inputSchema.properties.orderId);
  assert.equal(saveVoucher.annotations.readOnlyHint, false);
  assert.equal(saveVoucher.annotations.destructiveHint, false);
  assert.equal(saveVoucher.annotations.idempotentHint, false);
  const paymentHandoff = listed.result.tools.find((tool) => tool.name === "tbank_hotels_create_payment_handoff_preview");
  assert.deepEqual(paymentHandoff.inputSchema.required, ["bookingRef"]);
  assert.ok(!paymentHandoff.inputSchema.properties.orderId);
  assert.equal(paymentHandoff.annotations.readOnlyHint, false);
  assert.equal(paymentHandoff.annotations.destructiveHint, false);
  assert.equal(paymentHandoff.annotations.idempotentHint, false);
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
  assert.ok(prepareBooking.inputSchema.properties.payload.oneOf[0].properties.isBusinessTrip);
  assert.deepEqual(prepareBooking.inputSchema.properties.payload.oneOf[0].properties.paymentMeans.anyOf[0].enum, ["payment_form", "on_us", "off_us", "dolyame", "pos"]);
});

test("keeps package, runtime and handler registry in sync", () => {
  assert.equal(packageVersion, SERVER_VERSION);
  assert.deepEqual(
    [...runtimeHandledToolNames].sort(),
    tools.map((tool) => tool.name).sort(),
  );
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

test("normalizes common LLM plan aliases without provider-contract retries", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  const searchBodies = [];
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async (url, options) => {
    const target = new URL(url);
    if (target.pathname === "/api/v1/seo/locations") {
      return new Response(JSON.stringify({ payload: { locations: [
        { locationId: 17039, locationName: "Moscow", locationNameRu: "Москва", countryName: "Russia", countryNameRu: "Россия", hotelsCount: 1 },
      ] } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    if (target.pathname === "/api/v1/hotels/search") {
      searchBodies.push(JSON.parse(options.body));
      return new Response(JSON.stringify({ payload: {
        hotels: [{ hotelId: `hotel-${searchBodies.length}`, hotelName: "Alias Hotel", review: { rating: 9.1 } }],
        hotelsTotalCount: 1,
        filteredHotelsCount: 1,
        isLoadingCompleted: true,
      } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    throw new Error(`unexpected provider call: ${target}`);
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const nestedAlias = await callTool("tbank_hotels_plan_stay", {
    location: "Москва",
    checkinDate: "2099-09-01",
    checkoutDate: "2099-09-02",
    rooms: [{ adultsCount: 2, childrenAge: [] }],
    limit: 1,
  });
  const topLevelAlias = await callTool("tbank_hotels_plan_stay", {
    destinationId: 17039,
    checkinDate: "2099-10-01",
    checkoutDate: "2099-10-02",
    guests: [{ adultsCount: 2 }],
    maxOptions: 1,
  });

  assert.equal(nestedAlias.status, "ready");
  assert.equal(topLevelAlias.status, "ready");
  assert.equal(nestedAlias.options.length, 1);
  assert.deepEqual(searchBodies.map((body) => body.guests), [
    [{ adultsCount: 2, childrenAge: [] }],
    [{ adultsCount: 2, childrenAge: [] }],
  ]);
});

test("rejects conflicting or unknown plan aliases before provider access", async (t) => {
  const savedFetch = globalThis.fetch;
  let providerCalls = 0;
  globalThis.fetch = async () => {
    providerCalls += 1;
    throw new Error("provider access is forbidden for invalid plan input");
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
  });

  const base = {
    destination: "Москва",
    checkinDate: "2099-09-01",
    checkoutDate: "2099-09-02",
    rooms: [{ adults: 2 }],
  };
  const cases = [
    [{ ...base, location: "Казань" }, /either destination.*location/i],
    [{ ...base, guests: [{ adultsCount: 1 }] }, /either rooms.*guests/i],
    [{ ...base, maxOptions: 10, limit: 5 }, /either maxOptions.*limit/i],
    [{ ...base, rooms: [{ adults: 2, adultsCount: 1 }] }, /either adults.*adultsCount/i],
    [{ ...base, rooms: [{ adults: 2, childrenAges: [5], childrenAge: [5] }] }, /either childrenAges.*childrenAge/i],
    [{ ...base, guestCount: 2 }, /unsupported fields: guestCount/i],
  ];

  for (const [argumentsValue, expectedMessage] of cases) {
    await assert.rejects(
      callTool("tbank_hotels_plan_stay", argumentsValue),
      (error) => {
        assert.match(error.message, expectedMessage);
        assert.doesNotMatch(error.message, /guess|retry|try another|provider DTO/i);
        return true;
      },
    );
  }
  assert.equal(providerCalls, 0);
});

test("requires an adult count in canonical or compatibility form in journey room schemas", () => {
  for (const toolName of ["tbank_hotels_plan_stay", "tbank_hotels_plan_personalized_stay"]) {
    const tool = tools.find((candidate) => candidate.name === toolName);
    assert.ok(tool, `${toolName} must be present`);
    for (const propertyName of ["rooms", "guests"]) {
      assert.deepEqual(tool.inputSchema.properties[propertyName].items.anyOf, [
        { required: ["adults"] },
        { required: ["adultsCount"] },
      ]);
    }
  }
});

test("applies the breakfast requirement before search and preserves it during comparison", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  const requestBodies = [];
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async (_url, options) => {
    requestBodies.push(JSON.parse(options.body));
    return new Response(JSON.stringify({ payload: {
      hotels: [
        { hotelId: "hotel-a", hotelName: "Hotel A", review: { rating: 9.1 }, rateForHotelsFeed: { mealName: null } },
        { hotelId: "hotel-b", hotelName: "Hotel B", review: { rating: 9.7 }, rateForHotelsFeed: { mealName: "Breakfast" } },
      ],
      hotelsTotalCount: 2,
      filteredHotelsCount: 2,
      isLoadingCompleted: true,
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
    breakfastIncluded: true,
    ranking: "highest_rating",
  });
  assert.equal(plan.status, "ready");
  assert.equal(requestBodies.length, 1);
  assert.deepEqual(requestBodies[0].filters, [{ $objectType: "array", filterId: "meal_types", values: ["breakfast"] }]);
  assert.deepEqual(plan.requiredConditions, { breakfastIncluded: true });
  assert.deepEqual(plan.conditionsApplied.breakfastIncluded, {
    required: true,
    applied: true,
    source: "provider_search_filter",
    filterId: "meal_types",
    value: "breakfast",
  });
  assert.equal(plan.options[0].displayedPriceBreakfastEvidence, "confirmed_by_meal_name");
  assert.equal(plan.options[1].displayedPriceBreakfastEvidence, "not_confirmed_for_displayed_price");
  const listedOptions = await callTool("tbank_hotels_get_stay_options", { journeyId: plan.journeyId, limit: 2 });
  assert.deepEqual(listedOptions.requiredConditions, plan.requiredConditions);
  assert.deepEqual(listedOptions.conditionsApplied, plan.conditionsApplied);
  const comparison = await callTool("tbank_hotels_compare_stay_options", { journeyId: plan.journeyId, limit: 2 });
  assert.deepEqual(comparison.comparison.map((option) => option.hotelName), ["Hotel B", "Hotel A"]);
  assert.deepEqual(comparison.requiredConditions, plan.requiredConditions);
  assert.deepEqual(comparison.conditionsApplied, plan.conditionsApplied);
  const repeated = await callTool("tbank_hotels_repeat_stay_plan", {
    journeyId: plan.journeyId,
    checkinDate: "2099-10-01",
    checkoutDate: "2099-10-02",
  });
  assert.equal(requestBodies.length, 2);
  assert.deepEqual(requestBodies[1].filters, [{ $objectType: "array", filterId: "meal_types", values: ["breakfast"] }]);
  assert.deepEqual(repeated.requiredConditions, { breakfastIncluded: true });
  assert.equal(repeated.conditionsApplied.breakfastIncluded.source, "provider_search_filter");
});

test("applies a privacy-safe portfolio profile as soft best-value ranking without provider price filters", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  const requestBodies = [];
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async (_url, options) => {
    requestBodies.push(JSON.parse(options.body));
    return new Response(JSON.stringify({ payload: {
      hotels: [
        { hotelId: "premium", hotelName: "Premium", review: { rating: 9.5, ratingsCount: 1000 }, rateForHotelsFeed: { shownPrice: { value: 26000, currency: "RUB" }, freeCancellationUntil: "2099-08-31T21:00:00+00:00" } },
        { hotelId: "balanced", hotelName: "Balanced", review: { rating: 9.2, ratingsCount: 500 }, rateForHotelsFeed: { shownPrice: { value: 16000, currency: "RUB" }, freeCancellationUntil: "2099-08-31T21:00:00+03:00" } },
        { hotelId: "few-reviews", hotelName: "Few Reviews", review: { rating: 9.7, ratingsCount: 5 }, rateForHotelsFeed: { shownPrice: { value: 12000, currency: "RUB" } } },
        { hotelId: "far-below", hotelName: "Far Below", starRating: 0, review: { rating: 9.2, ratingsCount: 2623 }, rateForHotelsFeed: { shownPrice: { value: 3400, currency: "RUB" } } },
        { hotelId: "outside", hotelName: "Outside", review: { rating: 9.0, ratingsCount: 200 }, rateForHotelsFeed: { shownPrice: { value: 40000, currency: "RUB" } } },
      ],
      hotelsTotalCount: 5,
      filteredHotelsCount: 5,
      isLoadingCompleted: true,
    } }), { status: 200, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const hotelPreferences = {
    pricePerNight: { min: 6000, max: 13000, currency: "RUB" },
    ranking: "best_value",
    showAlternativesOutsideBand: true,
  };
  const plan = await callTool("tbank_hotels_plan_stay", {
    destinationId: 17039,
    checkinDate: "2099-09-01",
    checkoutDate: "2099-09-03",
    rooms: [{ adults: 2 }],
    hotelPreferences,
    maxOptions: 5,
  });
  assert.equal(requestBodies.length, 1);
  assert.deepEqual(requestBodies[0].filters, []);
  assert.equal(JSON.stringify(requestBodies[0]).includes("pricePerNight"), false);
  assert.equal(plan.ranking, "best_value");
  assert.equal(plan.preferencesApplied.providerFilterApplied, false);
  assert.equal(plan.preferencesApplied.softPreference, true);
  assert.equal(plan.preferencesApplied.applied, true);
  assert.equal(plan.stayNights, 2);
  assert.equal(plan.options.length, 5);
  assert.equal(plan.options[0].hotelName, "Premium");
  assert.equal(plan.options[0].totalPriceDisplay, "26 000 ₽");
  assert.equal(plan.options[0].pricePerNightDisplay, "13 000 ₽");
  assert.equal(plan.options[0].priceBasis, "provider_total_for_stay");
  const farBelow = plan.options.find((option) => option.hotelName === "Far Below");
  assert.equal(farBelow.pricePreferenceFit, "below_preferred_range");
  assert.equal(farBelow.starRating, null);
  assert.notEqual(plan.options[0].hotelName, "Far Below");
  assert.equal(plan.options.at(-1).pricePreferenceFit, "above_preferred_range");
  assert.match(plan.preferencesApplied.scoreFormula, /best_value_v2/);

  const comparison = await callTool("tbank_hotels_compare_stay_options", { journeyId: plan.journeyId, limit: 3 });
  assert.equal(comparison.selectionStrategy, "best_value");
  assert.equal(comparison.stayNights, 2);
  assert.equal(comparison.comparisonRows[0].hotelName, "Premium");
  assert.equal(comparison.comparisonRows[0].priceDisplay, "26 000 ₽");
  assert.equal(comparison.comparisonRows[0].totalPriceDisplay, "26 000 ₽");
  assert.equal(comparison.comparisonRows[0].pricePerNightDisplay, "13 000 ₽");
  assert.equal(comparison.comparisonRows[0].freeCancellationUntilDisplay, "31.08.2099 21:00 (UTC)");
  assert.equal(comparison.comparisonRows[1].freeCancellationUntilDisplay, "31.08.2099 21:00 (UTC+03:00)");
  assert.equal(comparison.comparisonRows.every((row) => row.bestValueScore === null || typeof row.bestValueScore === "number"), true);
  assert.match(comparison.comparisonTableMarkdown, /За поездку \| За ночь/);
  assert.match(comparison.comparisonTableMarkdown, /Best value \| Диапазон/);
  assert.match(comparison.comparisonTableMarkdown, /within_preferred_range/);
  assert.match(comparison.presentationGuidance.factIntegrity, /never call UTC/);
  assert.match(comparison.presentationGuidance.factIntegrity, /reviewRating is a review score/);
  assert.equal(comparison.preferenceAlternatives.belowPreferredRange[0].hotelName, "Far Below");
  assert.equal(comparison.preferenceAlternatives.abovePreferredRange[0].hotelName, "Outside");
});

test("personalized stay planning requires an explicit privacy-safe profile", async () => {
  let requests = 0;
  const savedFetch = globalThis.fetch;
  globalThis.fetch = async () => { requests += 1; throw new Error("unexpected provider request"); };
  try {
    await assert.rejects(callTool("tbank_hotels_plan_personalized_stay", {
      destinationId: 17039,
      checkinDate: "2099-09-01",
      checkoutDate: "2099-09-02",
      rooms: [{ adults: 2 }],
    }), /hotelPreferences.*required/);
    assert.equal(requests, 0);
  } finally {
    globalThis.fetch = savedFetch;
  }
});

test("rejects malformed hotel preferences before provider search", async () => {
  let requests = 0;
  const savedFetch = globalThis.fetch;
  globalThis.fetch = async () => { requests += 1; throw new Error("unexpected provider request"); };
  try {
    await assert.rejects(callTool("tbank_hotels_plan_stay", {
      destinationId: 17039,
      checkinDate: "2099-09-01",
      checkoutDate: "2099-09-02",
      rooms: [{ adults: 2 }],
      hotelPreferences: { pricePerNight: { min: 13000, max: 6000, currency: "RUB" }, ranking: "best_value", showAlternativesOutsideBand: true },
    }), /max must be a finite number greater than or equal to min/);
    await assert.rejects(callTool("tbank_hotels_plan_stay", {
      destinationId: 17039,
      checkinDate: "2099-09-01",
      checkoutDate: "2099-09-02",
      rooms: [{ adults: 2 }],
      hotelPreferences: { pricePerNight: { min: 6000, max: 13000, currency: "RUB" }, ranking: "best_value", showAlternativesOutsideBand: false },
    }), /must be true because the profile range is soft/);
    assert.equal(requests, 0);
  } finally {
    globalThis.fetch = savedFetch;
  }
});

test("validates all search filter discriminator variants locally", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  let requests = 0;
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async () => {
    requests += 1;
    return new Response(JSON.stringify({ payload: {} }), { status: 200, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });
  const base = { destinationId: 17039, checkinDate: "2099-09-01", checkoutDate: "2099-09-02", guests: [{ adultsCount: 2 }] };
  const filters = [
    { $objectType: "array", filterId: "meal_types", values: ["breakfast"] },
    { $objectType: "boolean", filterId: "free_cancellation_allowed", value: true },
    { $objectType: "radio", filterId: "payment_places", value: "now", values: null },
    { $objectType: "range", filterId: "price", min: 5000, max: 10000 },
  ];
  for (const filter of filters) await callTool("tbank_hotels_search", { payload: { ...base, filters: [filter] } });
  assert.equal(requests, 4);

  await assert.rejects(
    callTool("tbank_hotels_search", { payload: { ...base, filters: [{ filterId: "meal_types", values: ["breakfast"] }] } }),
    /\$objectType must be array, boolean, radio, or range/,
  );
  await assert.rejects(
    callTool("tbank_hotels_search", { payload: { ...base, filters: [{ $objectType: "array", filterId: "meal_types", values: ["breakfast"], value: true }] } }),
    /unsupported fields: value/,
  );
  assert.equal(requests, 4);
});

test("does not fall back or invite retries when a required breakfast search fails", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  let requests = 0;
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async () => {
    requests += 1;
    return new Response(JSON.stringify({ errorCode: "filter_unavailable" }), { status: 500, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const result = await callTool("tbank_hotels_plan_stay", {
    destinationId: 17039,
    checkinDate: "2099-09-01",
    checkoutDate: "2099-09-02",
    rooms: [{ adults: 2 }],
    breakfastIncluded: true,
  });
  assert.equal(requests, 1);
  assert.equal(result.status, "requirements_unavailable");
  assert.equal(result.reason, "provider_unavailable");
  assert.equal(result.retryAllowed, false);
  assert.equal(result.lowLevelFallbackAllowed, false);
  assert.deepEqual(result.requiredConditions, { breakfastIncluded: true });
  assert.match(result.nextStep, /Do not retry/);
});

test("distinguishes provider auth rejection for a required breakfast search", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  let requests = 0;
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async () => {
    requests += 1;
    return new Response(JSON.stringify({ errorCode: "unauthorized" }), { status: 401, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const result = await callTool("tbank_hotels_plan_stay", {
    destinationId: 17039,
    checkinDate: "2099-09-01",
    checkoutDate: "2099-09-02",
    rooms: [{ adults: 2 }],
    breakfastIncluded: true,
  });
  assert.equal(requests, 1);
  assert.equal(result.status, "requirements_unavailable");
  assert.equal(result.reason, "provider_auth_rejected");
  assert.equal(result.providerHttpStatus, 401);
  assert.equal(result.retryAllowed, false);
  assert.equal(result.lowLevelFallbackAllowed, false);
  assert.equal(result.options, undefined);
  assert.match(result.nextStep, /authentication profile/);
});

test("fails closed after a network error in a required breakfast search", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  let requests = 0;
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async () => {
    requests += 1;
    throw new TypeError("simulated network failure");
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const result = await callTool("tbank_hotels_plan_stay", {
    destinationId: 17039,
    checkinDate: "2099-09-01",
    checkoutDate: "2099-09-02",
    rooms: [{ adults: 2 }],
    breakfastIncluded: true,
  });
  assert.equal(requests, 1);
  assert.equal(result.status, "requirements_unavailable");
  assert.equal(result.reason, "provider_unreachable");
  assert.equal(result.retryAllowed, false);
  assert.equal(result.lowLevelFallbackAllowed, false);
  assert.equal(result.options, undefined);
});

test("returns a terminal no-retry result for ordinary search DNS or network failure", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  let requests = 0;
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async () => {
    requests += 1;
    throw new TypeError("getaddrinfo ENOTFOUND hotels.example.test");
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const searchFailure = await callTool("tbank_hotels_plan_stay", {
    destinationId: 17039,
    checkinDate: "2099-09-01",
    checkoutDate: "2099-09-02",
    rooms: [{ adults: 2 }],
  });
  assert.equal(searchFailure.status, "search_unavailable");
  assert.equal(searchFailure.reason, "provider_unreachable");
  assert.equal(searchFailure.retryAllowed, false);
  assert.equal(searchFailure.lowLevelFallbackAllowed, false);
  assert.equal(searchFailure.connectionStatusAllowedOnce, true);
  assert.match(searchFailure.nextStep, /Do not retry this search/);
  assert.match(searchFailure.nextStep, /direct provider driver/);
  assert.equal(requests, 1);

  const resolutionFailure = await callTool("tbank_hotels_plan_stay", {
    destination: "Unique Network Failure City",
    countryName: "Unique Network Failure Country",
    checkinDate: "2099-09-01",
    checkoutDate: "2099-09-02",
    rooms: [{ adults: 2 }],
  });
  assert.equal(resolutionFailure.status, "search_unavailable");
  assert.equal(resolutionFailure.reason, "provider_unreachable");
  assert.equal(resolutionFailure.retryAllowed, false);
  assert.equal(requests, 2);

  const status = await callTool("tbank_hotels_connection_status");
  assert.equal(status.searchReady, true);
  assert.equal(status.networkReachability, "not_checked");
  assert.equal(status.readinessScope, "local_configuration_only");
  assert.equal(status.paymentFormExecution.available, false);
  assert.equal(status.paymentFormExecution.status, "external_contract_evidence_required");
  assert.equal(status.paymentFormExecution.flow, "hosted_payment_form");
  assert.equal(status.paymentFormExecution.rawCardDataAcceptedByMcp, false);
  assert.ok(status.paymentFormExecution.externalBlockers.includes("provider_idempotency_unverified"));
  assert.equal(requests, 2);
});

test("returns a no-retry result for a failed low-level filtered availability request", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  let requests = 0;
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  let responseStatus = 503;
  globalThis.fetch = async () => {
    requests += 1;
    return new Response(JSON.stringify({ errorCode: "temporary_failure" }), { status: responseStatus, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });
  const result = await callTool("tbank_hotels_get_filter_availability", { payload: {
    destinationId: 17039,
    checkinDate: "2099-09-01",
    checkoutDate: "2099-09-02",
    guests: [{ adultsCount: 2 }],
    filters: [{ $objectType: "array", filterId: "meal_types", values: ["breakfast"] }],
  } });
  assert.equal(requests, 1);
  assert.equal(result.status, "requirements_unavailable");
  assert.equal(result.retryAllowed, false);
  assert.equal(result.lowLevelFallbackAllowed, false);
  assert.equal(result.providerHttpStatus, 503);
  assert.equal(result.reason, "provider_unavailable");

  responseStatus = 400;
  const rejected = await callTool("tbank_hotels_search", { payload: {
    destinationId: 17039,
    checkinDate: "2099-10-01",
    checkoutDate: "2099-10-02",
    guests: [{ adultsCount: 2 }],
    filters: [{ $objectType: "array", filterId: "meal_types", values: ["breakfast"] }],
  } });
  assert.equal(requests, 2);
  assert.equal(rejected.reason, "provider_rejected_required_request");
  assert.equal(rejected.providerHttpStatus, 400);
});

test("returns no_matching_stays without weakening an empty breakfast search", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  let requests = 0;
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async () => {
    requests += 1;
    return new Response(JSON.stringify({ payload: {
      hotels: [], hotelsTotalCount: 0, filteredHotelsCount: 0, isLoadingCompleted: true,
    } }), { status: 200, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });
  const result = await callTool("tbank_hotels_plan_stay", {
    destinationId: 17039,
    checkinDate: "2099-09-01",
    checkoutDate: "2099-09-02",
    rooms: [{ adults: 2 }],
    breakfastIncluded: true,
  });
  assert.equal(requests, 1);
  assert.equal(result.status, "no_matching_stays");
  assert.equal(result.retryAllowed, false);
  assert.equal(result.lowLevelFallbackAllowed, false);
  assert.deepEqual(result.requiredConditions, { breakfastIncluded: true });
});

test("journey flow hides provider identity while carrying a selected option to rates", async (t) => {
  const savedFetch = globalThis.fetch;
  const environmentNames = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_AUTH_HEADER", "TBANK_HOTELS_AUTH_HEADERS_JSON", "TBANK_HOTELS_JWT_PRIVATE_KEY", "TBANK_HOTELS_JWT_ISSUER", "TBANK_HOTELS_JWT_AUDIENCE", "TBANK_HOTELS_ENABLE_MUTATIONS", "TBANK_HOTELS_MUTATION_EXECUTION_PROFILE"];
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
  process.env.TBANK_HOTELS_MUTATION_EXECUTION_PROFILE = "non_production_v1_reviewed";
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
        { hotelId: "provider-2", hotelName: "Second Hotel", hotelChain: null, starRating: 5, areaLocation: { destinationName: "Moscow" }, hotelLocation: { address: "Street 2" }, rateForHotelsFeed: { shownPrice: { value: 200 }, availableRoomsCount: 1, freeCancellationUntil: null, mealName: "Meals not included", paymentPlace: "ONLINE" }, review: null, cashback: null },
      ] } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    if (String(url).endsWith("/api/v3/hotels/provider-2/rates")) {
      return new Response(JSON.stringify({ payload: { rates: [], rooms: [], otherRates: [] } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    return new Response(JSON.stringify({ payload: { rates: [
      { bookHash: "book-hash-1", roomId: "room-1", shownPrice: { value: 90 }, paymentPrice: { value: 90 }, paymentPlace: "ONLINE", cancellationPolicyRules: {}, isCreditCardDataRequired: false, isNonRefundable: false, mealName: "Meals not included", availableRoomsCount: 2, cashback: null },
      { bookHash: "book-hash-2", roomId: "room-2", shownPrice: { value: 100 }, paymentPrice: { value: 100 }, paymentPlace: "ONLINE", cancellationPolicyRules: {}, isCreditCardDataRequired: false, isNonRefundable: false, mealName: "Breakfast", availableRoomsCount: 2, cashback: null },
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
  assert.equal(comparison.comparison[0].displayedPriceBreakfastEvidence, "confirmed_by_meal_name");
  assert.equal(comparison.comparison[1].displayedPriceBreakfastEvidence, "excluded_by_meal_name");
  assert.deepEqual(comparison.comparisonRows, [
    {
      hotelName: "First Hotel",
      destination: "Moscow",
      starRating: 4,
      reviewRating: 9.1,
      ratingsCount: 100,
      priceAmount: 100,
      priceCurrency: null,
      priceDisplay: "100 (валюта не указана)",
      priceBasis: "provider_total_for_stay",
      stayNights: 1,
      totalPriceAmount: 100,
      totalPriceCurrency: null,
      totalPriceDisplay: "100 (валюта не указана)",
      pricePerNightAmount: 100,
      pricePerNightCurrency: null,
      pricePerNightDisplay: "100 (валюта не указана)",
      pricePerNightSource: "mcp_derived_from_provider_total_and_dates",
      freeCancellationUntil: null,
      freeCancellationUntilDisplay: "нет данных",
      mealName: "Breakfast",
      displayedPriceBreakfastEvidence: "confirmed_by_meal_name",
      bestValueScore: null,
      pricePreferenceFit: "not_requested",
    },
    {
      hotelName: "Second Hotel",
      destination: "Moscow",
      starRating: 5,
      reviewRating: null,
      ratingsCount: null,
      priceAmount: 200,
      priceCurrency: null,
      priceDisplay: "200 (валюта не указана)",
      priceBasis: "provider_total_for_stay",
      stayNights: 1,
      totalPriceAmount: 200,
      totalPriceCurrency: null,
      totalPriceDisplay: "200 (валюта не указана)",
      pricePerNightAmount: 200,
      pricePerNightCurrency: null,
      pricePerNightDisplay: "200 (валюта не указана)",
      pricePerNightSource: "mcp_derived_from_provider_total_and_dates",
      freeCancellationUntil: null,
      freeCancellationUntilDisplay: "нет данных",
      mealName: "Meals not included",
      displayedPriceBreakfastEvidence: "excluded_by_meal_name",
      bestValueScore: null,
      pricePreferenceFit: "not_requested",
    },
  ]);
  assert.equal(comparison.comparisonTableMarkdown, [
    "| Отель | Локация | Звёзды | Рейтинг | Отзывов | За поездку | За ночь | Бесплатная отмена | Питание |",
    "| --- | --- | ---: | ---: | ---: | ---: | ---: | --- | --- |",
    "| First Hotel | Moscow | 4 | 9.1 | 100 | 100 (валюта не указана) | 100 (валюта не указана) | нет данных | Breakfast |",
    "| Second Hotel | Moscow | 5 | — | — | 200 (валюта не указана) | 200 (валюта не указана) | нет данных | Meals not included |",
  ].join("\n"));
  assert.equal(comparison.presentationGuidance.source, "Copy comparisonTableMarkdown into the user-facing answer and explain it from comparisonRows.");
  assert.equal(comparison.presentationGuidance.scope, "Use only hotels in comparisonRows unless the user explicitly asks for alternatives.");
  assert.match(comparison.presentationGuidance.factIntegrity, /never call UTC/);
  assert.deepEqual(comparison.presentationGuidance.fields, ["hotelName", "destination", "starRating", "reviewRating", "ratingsCount", "totalPriceDisplay", "pricePerNightDisplay", "freeCancellationUntilDisplay", "mealName", "displayedPriceBreakfastEvidence", "bestValueScore", "pricePreferenceFit"]);
  await callTool("tbank_hotels_select_stay_option", { journeyId: plan.journeyId, optionId: plan.options[0].optionId });
  const rates = await callTool("tbank_hotels_get_selected_stay_rates", { journeyId: plan.journeyId });
  assert.equal(rates.status, "ready");
  assert.equal(rates.canCreateBookingDraft, true);
  assert.equal(rates.attempts, 1);
  assert.equal(rates.failureKind, null);
  assert.equal(rates.selectedOption.hotelName, "First Hotel");
  assert.equal(rates.rateOptions[0].displayedPriceBreakfastEvidence, "excluded_by_meal_name");
  assert.equal(rates.rateOptions[0].rateNumber, 1);
  assert.equal(rates.rateOptions[0].rateLabel, "Тариф 1");
  assert.equal(rates.rateOptions[1].displayedPriceBreakfastEvidence, "confirmed_by_meal_name");
  assert.equal(rates.rateOptions[1].rateNumber, 2);
  assert.equal(rates.rateOptions[1].rateLabel, "Тариф 2");
  assert.equal(rates.ratePresentationRows[0].priceDisplay, "90 (валюта не указана)");
  assert.equal(rates.ratePresentationRows[0].totalPriceDisplay, "90 (валюта не указана)");
  assert.equal(rates.ratePresentationRows[0].pricePerNightDisplay, "90 (валюта не указана)");
  assert.equal(rates.ratePresentationRows[0].cancellationDisplay, "нет данных");
  assert.match(rates.ratesTableMarkdown, /нет данных/);
  assert.match(rates.presentationGuidance.table, /exactly once/);
  assert.match(rates.presentationGuidance.table, /Do not renumber/);
  const ratesCall = calls.find((call) => call.url.endsWith("/api/v3/hotels/provider-1/rates"));
  assert.deepEqual(JSON.parse(ratesCall.options.body), {
    checkInDate: "2099-09-01",
    checkOutDate: "2099-09-02",
    guests: [{ adultsCount: 2, childrenAge: [] }],
  });
  const selectedRate = await callTool("tbank_hotels_select_stay_rate", { journeyId: plan.journeyId, rateOptionId: rates.rateOptions[1].rateOptionId });
  assert.equal(selectedRate.selectedRate.mealName, "Breakfast");
  assert.equal(selectedRate.selectedRateNumber, 2);
  assert.equal(selectedRate.selectedRate.rateNumber, 2);
  assert.equal(selectedRate.selectedRate.rateLabel, "Тариф 2");
  assert.equal(selectedRate.selectedRate.displayedPriceBreakfastEvidence, "confirmed_by_meal_name");
  assert.equal(selectedRate.executionAvailable, true);
  assert.deepEqual(selectedRate.executionReadiness, { available: true, status: "available" });
  const bookingPreview = await callTool("tbank_hotels_create_booking_preview", { journeyId: plan.journeyId });
  assert.equal(bookingPreview.status, "preview_only");
  assert.equal(bookingPreview.selectedRateNumber, 2);
  assert.equal(bookingPreview.selectedRate.rateLabel, "Тариф 2");
  assert.equal(bookingPreview.executionAvailable, true);
  assert.deepEqual(bookingPreview.executionReadiness, { available: true, status: "available" });
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

test("does not retain guest PII while execution is unavailable and keeps an enabled draft usable", async (t) => {
  const savedFetch = globalThis.fetch;
  const savedNow = Date.now;
  let currentTime = savedNow();
  Date.now = () => currentTime;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_AUTH_HEADERS_JSON", "TBANK_HOTELS_JWT_PRIVATE_KEY", "TBANK_HOTELS_ENABLE_MUTATIONS", "TBANK_HOTELS_MUTATION_EXECUTION_PROFILE", "TBANK_HOTELS_HOSTED_CHECKOUT_URL", "TBANK_HOTELS_HOTEL_PAGE_URL_TEMPLATE"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  const calls = [];
  let checkoutAttempts = 0;
  let checkoutBehavior = "retry_once";
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  delete process.env.TBANK_HOTELS_ENABLE_MUTATIONS;
  delete process.env.TBANK_HOTELS_MUTATION_EXECUTION_PROFILE;
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
      if (checkoutBehavior === "timeout_twice") {
        const error = new Error("simulated checkout timeout");
        error.name = "TimeoutError";
        throw error;
      }
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
  assert.deepEqual(selectedRate.executionReadiness, { available: false, status: "not_available" });
  assert.doesNotMatch(JSON.stringify(selectedRate), /x-real-ip|missingRequiredHeaders/);
  assert.match(selectedRate.nextStep, /tbank_hotels_create_checkout_handoff/);
  assert.match(selectedRate.nextStep, /remains available while direct execution is unavailable/);
  checkoutBehavior = "timeout_twice";
  checkoutAttempts = 0;
  const unavailableCheckout = await callTool("tbank_hotels_inspect_checkout", { journeyId: plan.journeyId });
  assert.equal(unavailableCheckout.status, "checkout_temporarily_unavailable");
  assert.equal(unavailableCheckout.failureKind, "provider_timeout");
  assert.equal(unavailableCheckout.checkoutAttempts, 2);
  assert.equal(unavailableCheckout.providerRequestCount, 2);
  assert.equal(unavailableCheckout.retryAllowed, false);
  assert.equal(unavailableCheckout.lowLevelFallbackAllowed, false);
  assert.match(unavailableCheckout.nextStep, /Do not repeat the same checkout inspection automatically/);
  checkoutBehavior = "retry_once";
  checkoutAttempts = 0;
  const preview = await callTool("tbank_hotels_create_booking_preview", { journeyId: plan.journeyId });
  assert.equal(preview.status, "preview_only");
  assert.equal(preview.executionAvailable, false);
  assert.deepEqual(preview.executionReadiness, { available: false, status: "not_available" });
  assert.doesNotMatch(JSON.stringify(preview), /x-real-ip|missingRequiredHeaders/);
  assert.equal(preview.personalDataCollected, false);
  assert.equal(preview.httpRequestPerformed, false);
  assert.match(preview.nextStep, /tbank_hotels_create_checkout_handoff/);
  assert.match(preview.nextStep, /hostedCheckoutUrl/);
  assert.doesNotMatch(preview.nextStep, /^Show this preview and stop/);
  assert.doesNotMatch(JSON.stringify(preview), /book-1|person@example|Ada|Lovelace/);
  const paymentPreview = await callTool("tbank_hotels_create_payment_form_preview", { journeyId: plan.journeyId });
  assert.equal(paymentPreview.status, "preview_only");
  assert.equal(paymentPreview.paymentFlow.type, "hosted_payment_form");
  assert.equal(paymentPreview.paymentFlow.paymentUrlIncluded, false);
  assert.equal(paymentPreview.executionAvailable, false);
  assert.deepEqual(paymentPreview.executionReadiness, { available: false, status: "not_available" });
  assert.equal(paymentPreview.contractEvidence.requestVerifiedOffline, true);
  assert.equal(paymentPreview.contractEvidence.externalBlockerCount, 8);
  assert.equal(paymentPreview.personalDataCollected, false);
  assert.equal(paymentPreview.paymentCredentialsCollected, false);
  assert.equal(paymentPreview.httpRequestPerformed, false);
  assert.ok(paymentPreview.excludedFromMcp.includes("cvv_cvc"));
  assert.doesNotMatch(JSON.stringify(paymentPreview), /book-1|person@example|Ada|Lovelace|x-real-ip|provider_idempotency|paymentUrl\s*:\s*https/i);
  const checkoutHandoff = await callTool("tbank_hotels_create_checkout_handoff", { journeyId: plan.journeyId });
  assert.equal(checkoutHandoff.status, "ready");
  assert.equal(checkoutHandoff.handoffMode, "hosted_checkout");
  assert.equal(checkoutHandoff.hostedCheckoutUrl, "https://www.tbank.ru/travel/hotels/new/hotels/hotel-1/?dateFrom=2099-09-01&dateTo=2099-09-02&guests=2");
  assert.equal(checkoutHandoff.selectionPreserved, true);
  assert.equal(checkoutHandoff.selectionPreservationScope, "selected_hotel_page_with_safe_search_context");
  assert.equal(checkoutHandoff.searchCriteriaPreserved, true);
  assert.equal(checkoutHandoff.searchCriteriaPreservationScope, "dates_and_single_room_adults");
  assert.equal(checkoutHandoff.datesPreserved, true);
  assert.equal(checkoutHandoff.guestCountPreserved, true);
  assert.equal(checkoutHandoff.roomCompositionPreserved, true);
  assert.equal(checkoutHandoff.childrenAgesPreserved, true);
  assert.equal(checkoutHandoff.exactRatePreserved, false);
  assert.equal(checkoutHandoff.exactRateHandoffStatus, "not_supported_by_verified_public_contract");
  assert.equal(checkoutHandoff.personalDataIncluded, false);
  assert.equal(checkoutHandoff.paymentCredentialsIncluded, false);
  assert.equal(checkoutHandoff.bookingCreated, false);
  assert.equal(checkoutHandoff.paymentStarted, false);
  assert.equal(checkoutHandoff.httpRequestPerformed, false);
  assert.doesNotMatch(JSON.stringify(checkoutHandoff), /book-1|person@example|Ada|Lovelace|x-real-ip|cvv|otp/i);
  process.env.TBANK_HOTELS_HOSTED_CHECKOUT_URL = "https://www.tbank.ru/travel/hotels/new/";
  const staleGenericCheckoutHandoff = await callTool("tbank_hotels_create_checkout_handoff", { journeyId: plan.journeyId });
  assert.equal(staleGenericCheckoutHandoff.hostedCheckoutUrl, "https://www.tbank.ru/travel/hotels/new/hotels/hotel-1/?dateFrom=2099-09-01&dateTo=2099-09-02&guests=2");
  assert.equal(staleGenericCheckoutHandoff.selectionPreserved, true);
  delete process.env.TBANK_HOTELS_HOSTED_CHECKOUT_URL;
  process.env.TBANK_HOTELS_HOTEL_PAGE_URL_TEMPLATE = "https://user:secret@example.test/hotels/{hotelId}/?token=secret";
  await assert.rejects(
    callTool("tbank_hotels_create_checkout_handoff", { journeyId: plan.journeyId }),
    /absolute HTTPS URL without credentials, query parameters, or a fragment/,
  );
  delete process.env.TBANK_HOTELS_HOTEL_PAGE_URL_TEMPLATE;
  process.env.TBANK_HOTELS_HOTEL_PAGE_URL_TEMPLATE = "https://checkout.example.test/hotels/{hotelId}/";
  const configuredCheckoutHandoff = await callTool("tbank_hotels_create_checkout_handoff", { journeyId: plan.journeyId });
  assert.equal(configuredCheckoutHandoff.hostedCheckoutUrl, "https://checkout.example.test/hotels/hotel-1/?dateFrom=2099-09-01&dateTo=2099-09-02&guests=2");
  assert.equal(configuredCheckoutHandoff.selectionPreserved, true);
  delete process.env.TBANK_HOTELS_HOTEL_PAGE_URL_TEMPLATE;
  currentTime += 59 * 60 * 1_000;
  const unavailableDraft = await callTool("tbank_hotels_create_booking_draft", {
    journeyId: plan.journeyId,
    bookingData: {
      guestContact: { email: "person@example.test", phone: "+70000000000" },
      rooms: [{ guests: [{ firstName: "Ada", lastName: "Lovelace" }] }],
    },
  });
  assert.equal(unavailableDraft.status, "execution_unavailable");
  assert.equal(unavailableDraft.bookingDraftCreated, false);
  assert.equal(unavailableDraft.personalDataStored, false);
  assert.equal("bookingDraftId" in unavailableDraft, false);
  assert.deepEqual(unavailableDraft.executionReadiness, { available: false, status: "not_available" });
  assert.doesNotMatch(JSON.stringify(unavailableDraft), /x-real-ip|missingRequiredHeaders/);
  assert.doesNotMatch(JSON.stringify(unavailableDraft), /person@example|70000000000|Ada|Lovelace/);

  delete process.env.TBANK_HOTELS_AUTH_TOKEN;
  process.env.TBANK_HOTELS_AUTH_HEADERS_JSON = JSON.stringify({ Authorization: "test-customer-auth", "x-real-ip": "192.0.2.1" });
  process.env.TBANK_HOTELS_ENABLE_MUTATIONS = "true";
  process.env.TBANK_HOTELS_MUTATION_EXECUTION_PROFILE = "non_production_v1_reviewed";
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
  assert.equal(draft.executionAvailable, true);
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
  delete process.env.TBANK_HOTELS_ENABLE_MUTATIONS;
  const prepared = await callTool("tbank_hotels_prepare_draft_booking", { bookingDraftId: draft.bookingDraftId });
  assert.equal(prepared.status, "preview_only");
  assert.equal(prepared.executionAvailable, false);
  assert.deepEqual(prepared.executionReadiness, { available: false, status: "not_available" });
  assert.doesNotMatch(JSON.stringify(prepared), /x-real-ip|missingRequiredHeaders/);
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

test("collects paginated search results and preserves the previous comparison scope", async (t) => {
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
    coverageLevel: "complete",
    coverageRatio: 1,
    continuationAvailable: false,
    continuationRecommended: false,
    requestCount: 2,
    maxRequestCount: 20,
    providerSort: null,
    rankingAppliedLocally: "highest_rating",
    stoppedReason: null,
    cacheStatus: "miss",
  });

  const initialComparison = await callTool("tbank_hotels_compare_stay_options", {
    journeyId: plan.journeyId,
    ranking: "highest_rating",
    limit: 2,
  });
  assert.equal(initialComparison.selectionScope, "all_journey_options");
  assert.deepEqual(initialComparison.comparison.map((option) => option.hotelName), ["Hotel B", "Hotel C"]);

  const continuedComparison = await callTool("tbank_hotels_compare_stay_options", {
    journeyId: plan.journeyId,
    ranking: "lowest_price",
    limit: 2,
  });
  assert.equal(continuedComparison.selectionScope, "previous_comparison");
  assert.equal(continuedComparison.basedOnComparisonId, initialComparison.comparisonId);
  assert.deepEqual(continuedComparison.comparison.map((option) => option.hotelName), ["Hotel C", "Hotel B"]);

  const fullJourneyComparison = await callTool("tbank_hotels_compare_stay_options", {
    journeyId: plan.journeyId,
    scope: "all_journey_options",
    ranking: "lowest_price",
    limit: 2,
  });
  assert.equal(fullJourneyComparison.selectionScope, "all_journey_options");
  assert.deepEqual(fullJourneyComparison.comparison.map((option) => option.hotelName), ["Hotel A", "Hotel C"]);

  const explicitComparison = await callTool("tbank_hotels_compare_stay_options", {
    journeyId: plan.journeyId,
    optionIds: [plan.options[2].optionId, plan.options[1].optionId],
    ranking: "highest_rating",
  });
  assert.equal(explicitComparison.selectionStrategy, "highest_rating");
  assert.equal(explicitComparison.selectionScope, "explicit_options");
  assert.deepEqual(explicitComparison.comparison.map((option) => option.hotelName), ["Hotel C", "Hotel A"]);
  await assert.rejects(callTool("tbank_hotels_compare_stay_options", {
    journeyId: plan.journeyId,
    optionIds: [plan.options[0].optionId, plan.options[1].optionId],
    scope: "all_journey_options",
  }), /either optionIds or scope/);
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
  assert.equal(plan.searchCoverage.coverageLevel, "partial");
  assert.equal(plan.searchCoverage.coverageRatio, 0.5);
  assert.equal(plan.searchCoverage.continuationAvailable, true);
  assert.equal(plan.searchCoverage.continuationRecommended, true);
  assert.equal(plan.searchCoverage.stoppedReason, "time_budget");
});

test("classifies an incomplete eighty-percent sample as substantial without automatic continuation", async (t) => {
  const savedFetch = globalThis.fetch;
  const savedBaseUrl = process.env.TBANK_HOTELS_API_BASE_URL;
  let requests = 0;
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels-substantial.example.test/";
  globalThis.fetch = async () => {
    requests += 1;
    if (requests > 1) {
      const error = new Error("simulated timeout");
      error.name = "TimeoutError";
      throw error;
    }
    return new Response(JSON.stringify({ payload: {
      hotels: Array.from({ length: 4 }, (_, index) => ({ hotelId: `hotel-${index}`, hotelName: `Hotel ${index}` })),
      filteredHotelsCount: 5,
      isLoadingCompleted: false,
      nextOffset: 4,
    } }), { status: 200, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    if (savedBaseUrl === undefined) delete process.env.TBANK_HOTELS_API_BASE_URL;
    else process.env.TBANK_HOTELS_API_BASE_URL = savedBaseUrl;
  });

  const plan = await callTool("tbank_hotels_plan_stay", {
    destinationId: 17039,
    checkinDate: "2099-09-10",
    checkoutDate: "2099-09-11",
    rooms: [{ adults: 2 }],
  });
  assert.equal(plan.searchCoverage.coverageRatio, 0.8);
  assert.equal(plan.searchCoverage.coverageLevel, "substantial");
  assert.equal(plan.searchCoverage.continuationAvailable, true);
  assert.equal(plan.searchCoverage.continuationRecommended, false);
  assert.match(plan.nextStep, /Compare the current journey options/);
});

test("continues a partial journey without reloading its first page and preserves option ids", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  const offsets = [];
  let secondPageAttempts = 0;
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels-resumable.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async (_url, options) => {
    const body = JSON.parse(options.body);
    offsets.push(body.offset);
    if (body.offset === 0) {
      return new Response(JSON.stringify({ payload: {
        hotels: [{ hotelId: "hotel-a", hotelName: "Hotel A", review: { rating: 8.9 } }],
        filteredHotelsCount: 2,
        hotelsTotalCount: 2,
        isLoadingCompleted: false,
        nextOffset: 1,
      } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    secondPageAttempts += 1;
    if (secondPageAttempts === 1) {
      const error = new Error("simulated timeout");
      error.name = "TimeoutError";
      throw error;
    }
    return new Response(JSON.stringify({ payload: {
      hotels: [{ hotelId: "hotel-b", hotelName: "Hotel B", review: { rating: 9.5 } }],
      filteredHotelsCount: 2,
      hotelsTotalCount: 2,
      isLoadingCompleted: true,
      nextOffset: null,
    } }), { status: 200, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const args = {
    destinationId: 17039,
    checkinDate: "2099-11-01",
    checkoutDate: "2099-11-02",
    rooms: [{ adults: 2 }],
    ranking: "highest_rating",
    maxOptions: 2,
  };
  const plan = await callTool("tbank_hotels_plan_stay", args);
  const firstOptionId = plan.options.find((option) => option.hotelName === "Hotel A").optionId;
  assert.deepEqual(offsets, [0, 1]);
  assert.equal(plan.searchCoverage.continuationRecommended, true);

  const [continued, coalescedContinuation] = await Promise.all([
    callTool("tbank_hotels_continue_stay_search", { journeyId: plan.journeyId }),
    callTool("tbank_hotels_continue_stay_search", { journeyId: plan.journeyId }),
  ]);
  assert.deepEqual(offsets, [0, 1, 1]);
  assert.deepEqual(coalescedContinuation, continued);
  assert.equal(continued.addedOptions, 1);
  assert.equal(continued.totalOptions, 2);
  assert.equal(continued.searchCoverage.coverageLevel, "complete");
  assert.equal(continued.searchCoverage.coverageRatio, 1);
  assert.equal(continued.searchCoverage.requestCount, 3);
  assert.equal(continued.searchCoverage.continuationAvailable, false);
  assert.equal(continued.options.find((option) => option.hotelName === "Hotel A").optionId, firstOptionId);

  const alreadyComplete = await callTool("tbank_hotels_continue_stay_search", { journeyId: plan.journeyId });
  assert.equal(alreadyComplete.status, "already_complete");
  assert.deepEqual(offsets, [0, 1, 1]);

  const cached = await callTool("tbank_hotels_plan_stay", args);
  assert.equal(cached.searchCoverage.cacheStatus, "hit");
  assert.equal(cached.totalOptions, 2);
  assert.deepEqual(offsets, [0, 1, 1]);
});

test("does not cache a truncated search as a final result", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  const offsets = [];
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels-partial-cache.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async (_url, options) => {
    const body = JSON.parse(options.body);
    offsets.push(body.offset);
    if (body.offset > 0) {
      const error = new Error("simulated timeout");
      error.name = "TimeoutError";
      throw error;
    }
    return new Response(JSON.stringify({ payload: {
      hotels: [{ hotelId: "hotel-partial", hotelName: "Partial Hotel" }],
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
  const args = {
    destinationId: 17039,
    checkinDate: "2099-12-01",
    checkoutDate: "2099-12-02",
    rooms: [{ adults: 2 }],
  };

  const first = await callTool("tbank_hotels_plan_stay", args);
  const second = await callTool("tbank_hotels_plan_stay", args);
  assert.equal(first.searchCoverage.cacheStatus, "miss");
  assert.equal(second.searchCoverage.cacheStatus, "miss");
  assert.deepEqual(offsets, [0, 1, 0, 1]);
});

test("keeps the provider request cap across initial and continued search", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  let requests = 0;
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels-request-cap.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async (_url, options) => {
    const body = JSON.parse(options.body);
    requests += 1;
    return new Response(JSON.stringify({ payload: {
      hotels: [{ hotelId: `hotel-${body.offset}`, hotelName: `Hotel ${body.offset}` }],
      filteredHotelsCount: 100,
      isLoadingCompleted: false,
      nextOffset: body.offset + 1,
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
    checkinDate: "2099-12-10",
    checkoutDate: "2099-12-11",
    rooms: [{ adults: 2 }],
  });
  assert.equal(requests, 20);
  assert.equal(plan.searchCoverage.requestCount, 20);
  assert.equal(plan.searchCoverage.stoppedReason, "request_limit");
  assert.equal(plan.searchCoverage.continuationAvailable, false);
  const continuation = await callTool("tbank_hotels_continue_stay_search", { journeyId: plan.journeyId });
  assert.equal(continuation.status, "continuation_unavailable");
  assert.equal(requests, 20);
});

test("makes a repeated continuation offset terminal without another provider request", async (t) => {
  const savedFetch = globalThis.fetch;
  const savedBaseUrl = process.env.TBANK_HOTELS_API_BASE_URL;
  const offsets = [];
  let secondPageAttempts = 0;
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels-repeated-offset.example.test/";
  globalThis.fetch = async (_url, options) => {
    const body = JSON.parse(options.body);
    offsets.push(body.offset);
    if (body.offset === 0) {
      return new Response(JSON.stringify({ payload: {
        hotels: [{ hotelId: "hotel-a", hotelName: "Hotel A" }],
        filteredHotelsCount: 3,
        isLoadingCompleted: false,
        nextOffset: 1,
      } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    secondPageAttempts += 1;
    if (secondPageAttempts === 1) {
      const error = new Error("simulated timeout");
      error.name = "TimeoutError";
      throw error;
    }
    return new Response(JSON.stringify({ payload: {
      hotels: [{ hotelId: "hotel-b", hotelName: "Hotel B" }],
      filteredHotelsCount: 3,
      isLoadingCompleted: false,
      nextOffset: 0,
    } }), { status: 200, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    if (savedBaseUrl === undefined) delete process.env.TBANK_HOTELS_API_BASE_URL;
    else process.env.TBANK_HOTELS_API_BASE_URL = savedBaseUrl;
  });

  const plan = await callTool("tbank_hotels_plan_stay", {
    destinationId: 17039,
    checkinDate: "2099-12-12",
    checkoutDate: "2099-12-13",
    rooms: [{ adults: 2 }],
  });
  const continued = await callTool("tbank_hotels_continue_stay_search", { journeyId: plan.journeyId });
  assert.equal(continued.status, "ready");
  assert.equal(continued.searchCoverage.stoppedReason, "repeated_next_offset");
  assert.equal(continued.searchCoverage.continuationAvailable, false);
  assert.equal(continued.searchCoverage.continuationRecommended, false);
  const requestsAfterTerminalResult = offsets.length;

  const terminal = await callTool("tbank_hotels_continue_stay_search", { journeyId: plan.journeyId });
  assert.equal(terminal.status, "continuation_unavailable");
  assert.equal(terminal.retryAllowed, false);
  assert.equal(offsets.length, requestsAfterTerminalResult);
});

test("makes a provider failure during continuation terminal", async (t) => {
  const savedFetch = globalThis.fetch;
  const savedBaseUrl = process.env.TBANK_HOTELS_API_BASE_URL;
  let requests = 0;
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels-continuation-failure.example.test/";
  globalThis.fetch = async () => {
    requests += 1;
    if (requests === 1) {
      return new Response(JSON.stringify({ payload: {
        hotels: [{ hotelId: "hotel-a", hotelName: "Hotel A" }],
        filteredHotelsCount: 2,
        isLoadingCompleted: false,
        nextOffset: 1,
      } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    if (requests === 2) {
      const error = new Error("simulated timeout");
      error.name = "TimeoutError";
      throw error;
    }
    return new Response(JSON.stringify({ code: "temporarily_unavailable" }), {
      status: 503,
      headers: { "content-type": "application/json" },
    });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    if (savedBaseUrl === undefined) delete process.env.TBANK_HOTELS_API_BASE_URL;
    else process.env.TBANK_HOTELS_API_BASE_URL = savedBaseUrl;
  });

  const plan = await callTool("tbank_hotels_plan_stay", {
    destinationId: 17039,
    checkinDate: "2099-12-14",
    checkoutDate: "2099-12-15",
    rooms: [{ adults: 2 }],
  });
  const continuation = await callTool("tbank_hotels_continue_stay_search", { journeyId: plan.journeyId });
  assert.equal(continuation.status, "continuation_unavailable");
  assert.equal(continuation.searchCoverage.stoppedReason, "continuation_provider_failure");
  assert.equal(continuation.searchCoverage.continuationAvailable, false);
  assert.equal(continuation.searchCoverage.continuationRecommended, false);
  assert.equal(continuation.retryAllowed, false);
  const requestsAfterFailure = requests;
  const terminal = await callTool("tbank_hotels_continue_stay_search", { journeyId: plan.journeyId });
  assert.equal(terminal.status, "continuation_unavailable");
  assert.equal(requests, requestsAfterFailure);
});

test("continues successfully after the bounded loading poll limit", async (t) => {
  const savedFetch = globalThis.fetch;
  const savedBaseUrl = process.env.TBANK_HOTELS_API_BASE_URL;
  let requests = 0;
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels-loading-polls.example.test/";
  globalThis.fetch = async () => {
    requests += 1;
    const payload = requests <= 4
      ? {
          hotels: [{ hotelId: "hotel-a", hotelName: "Hotel A" }],
          filteredHotelsCount: 2,
          isLoadingCompleted: false,
          nextOffset: 0,
        }
      : {
          hotels: [
            { hotelId: "hotel-a", hotelName: "Hotel A" },
            { hotelId: "hotel-b", hotelName: "Hotel B" },
          ],
          filteredHotelsCount: 2,
          isLoadingCompleted: true,
          nextOffset: null,
        };
    return new Response(JSON.stringify({ payload }), { status: 200, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    if (savedBaseUrl === undefined) delete process.env.TBANK_HOTELS_API_BASE_URL;
    else process.env.TBANK_HOTELS_API_BASE_URL = savedBaseUrl;
  });

  const plan = await callTool("tbank_hotels_plan_stay", {
    destinationId: 17039,
    checkinDate: "2099-12-16",
    checkoutDate: "2099-12-17",
    rooms: [{ adults: 2 }],
  });
  assert.equal(plan.searchCoverage.stoppedReason, "loading_poll_limit");
  assert.equal(plan.searchCoverage.continuationAvailable, true);
  const continuation = await callTool("tbank_hotels_continue_stay_search", { journeyId: plan.journeyId });
  assert.equal(continuation.status, "ready");
  assert.equal(continuation.totalOptions, 2);
  assert.equal(continuation.searchCoverage.coverageLevel, "complete");
  assert.equal(continuation.searchCoverage.continuationAvailable, false);
  assert.equal(requests, 5);
});

test("resets a stale selection and suppresses automatic continuation after the first continuation", async (t) => {
  const savedFetch = globalThis.fetch;
  const savedBaseUrl = process.env.TBANK_HOTELS_API_BASE_URL;
  const offsets = [];
  let secondPageAttempts = 0;
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels-selection-reset.example.test/";
  globalThis.fetch = async (_url, options) => {
    const body = JSON.parse(options.body);
    offsets.push(body.offset);
    if (body.offset === 0) {
      return new Response(JSON.stringify({ payload: {
        hotels: [{ hotelId: "renamed-hotel", hotelName: "Original Hotel" }],
        filteredHotelsCount: 4,
        isLoadingCompleted: false,
        nextOffset: 1,
      } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    if (body.offset === 1) {
      secondPageAttempts += 1;
      if (secondPageAttempts === 1) {
        const error = new Error("simulated timeout");
        error.name = "TimeoutError";
        throw error;
      }
      return new Response(JSON.stringify({ payload: {
        hotels: [
          { hotelId: "renamed-hotel", hotelName: "Renamed Hotel" },
          { hotelId: "hotel-b", hotelName: "Hotel B" },
        ],
        filteredHotelsCount: 4,
        isLoadingCompleted: false,
        nextOffset: 2,
      } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    const error = new Error("simulated timeout");
    error.name = "TimeoutError";
    throw error;
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    if (savedBaseUrl === undefined) delete process.env.TBANK_HOTELS_API_BASE_URL;
    else process.env.TBANK_HOTELS_API_BASE_URL = savedBaseUrl;
  });

  const plan = await callTool("tbank_hotels_plan_stay", {
    destinationId: 17039,
    hotelName: "Original Hotel",
    checkinDate: "2099-12-18",
    checkoutDate: "2099-12-19",
    rooms: [{ adults: 2 }],
  });
  await callTool("tbank_hotels_select_stay_option", {
    journeyId: plan.journeyId,
    optionId: plan.options[0].optionId,
  });
  const continuation = await callTool("tbank_hotels_continue_stay_search", { journeyId: plan.journeyId });
  assert.equal(continuation.selectionReset, true);
  assert.equal(continuation.searchCoverage.continuationAvailable, true);
  assert.equal(continuation.searchCoverage.continuationRecommended, false);
  assert.match(continuation.nextStep, /previously selected hotel is no longer present/);
  const current = await callTool("tbank_hotels_get_stay_options", { journeyId: plan.journeyId });
  assert.equal(current.selectedOptionId, null);
  await assert.rejects(
    callTool("tbank_hotels_get_selected_stay_rates", { journeyId: plan.journeyId }),
    /Select one stay option/,
  );
  assert.deepEqual(offsets, [0, 1, 1, 2]);
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

test("plan_stay resolves a city internally when localized country filtering returns an empty catalog", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  const calls = [];
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async (url, options) => {
    const target = new URL(url);
    calls.push(target);
    if (target.pathname === "/api/v1/seo/locations") {
      const locations = target.searchParams.has("CountryName")
        ? []
        : [{ locationId: 90229, locationName: "Kazan", locationNameRu: "Казань", countryName: "Russia", countryNameRu: "Россия", hotelsCount: 1521 }];
      return new Response(JSON.stringify({ payload: { locations } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    if (target.pathname === "/api/v1/hotels/search") {
      assert.equal(JSON.parse(options.body).destinationId, 90229);
      return new Response(JSON.stringify({ payload: {
        hotels: [{ hotelId: "kazan-1", hotelName: "Kazan Hotel", review: { rating: 9.2, ratingsCount: 500 }, rateForHotelsFeed: { shownPrice: { value: 18000, currency: "RUB" } } }],
        hotelsTotalCount: 1,
        filteredHotelsCount: 1,
        isLoadingCompleted: true,
      } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    throw new Error(`unexpected provider call: ${target}`);
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const result = await callTool("tbank_hotels_plan_stay", {
    destination: "Казань",
    countryName: "Россия",
    checkinDate: "2099-09-15",
    checkoutDate: "2099-09-17",
    rooms: [{ adults: 2 }],
  });
  assert.equal(result.status, "ready");
  assert.equal(result.resolvedDestination.destinationId, 90229);
  assert.equal(result.resolvedDestination.countryCatalogFallbackApplied, true);
  assert.deepEqual(calls.map((target) => target.pathname), [
    "/api/v1/seo/locations",
    "/api/v1/seo/locations",
    "/api/v1/hotels/search",
  ]);
  assert.equal(calls[0].searchParams.get("CountryName"), "Россия");
  assert.equal(calls[1].searchParams.has("CountryName"), false);
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
  const mixedBestValue = await callTool("tbank_hotels_get_stay_options", { journeyId: plan.journeyId, ranking: "best_value" });
  assert.deepEqual(mixedBestValue.options.map((option) => option.hotelName), ["Ruble Hotel", "Dollar Hotel"]);
  assert.equal(mixedBestValue.options.every((option) => option.bestValueScore === null), true);
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
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY", "TBANK_HOTELS_ENABLE_MUTATIONS", "TBANK_HOTELS_MUTATION_EXECUTION_PROFILE"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  const calls = [];
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  process.env.TBANK_HOTELS_ENABLE_MUTATIONS = "true";
  process.env.TBANK_HOTELS_MUTATION_EXECUTION_PROFILE = "non_production_v1_reviewed";
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
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_AUTH_HEADERS_JSON", "TBANK_HOTELS_JWT_PRIVATE_KEY", "TBANK_HOTELS_ENABLE_MUTATIONS", "TBANK_HOTELS_MUTATION_EXECUTION_PROFILE"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  const calls = [];
  let executionMode = "success";
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_HEADERS_JSON = JSON.stringify({ Authorization: "test-customer-auth", "x-real-ip": "192.0.2.1" });
  process.env.TBANK_HOTELS_ENABLE_MUTATIONS = "true";
  process.env.TBANK_HOTELS_MUTATION_EXECUTION_PROFILE = "non_production_v1_reviewed";
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
  assert.deepEqual(blocked.executionReadiness, { available: false, status: "not_available" });
  assert.doesNotMatch(JSON.stringify(blocked), /x-real-ip|missingRequiredHeaders/);
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
    paymentData: { creditCardId: "123e4567-e89b-12d3-a456-426614174000" },
    paymentMeans: "pos",
    isBusinessTrip: false,
  };
  const prepared = await callTool("tbank_hotels_prepare_booking", { payload: bookingPayload });
  assert.match(prepared.payloadPreview.guestContact.email, /REDACTED/);
  assert.match(prepared.payloadPreview.userData.ssoId, /REDACTED/);
  assert.match(prepared.payloadPreview.userIp, /REDACTED/);
  assert.equal(prepared.payloadPreview.paymentMeans, "pos");
  assert.equal(prepared.payloadPreview.isBusinessTrip, false);
  await assert.rejects(
    callTool("tbank_hotels_prepare_booking", { payload: { ...bookingPayload, guestContact: { email: "person@example.test" } } }),
    /guestContact.phone must be a non-empty string/,
  );
  await assert.rejects(
    callTool("tbank_hotels_prepare_booking", { payload: { ...bookingPayload, paymentData: { creditCardId: "not-a-uuid" } } }),
    /creditCardId must be a UUID or null/,
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
      isBusinessTrip: true,
    },
  });
  assert.equal(lsPrepared.action, "lsBooking");
  assert.equal(lsPrepared.payloadPreview.isBusinessTrip, true);
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

test("does not report payment handoff ready before mobile login", async (t) => {
  setAuthBrokerConnectorForTests(() => {
    const connection = new EventEmitter();
    connection.setEncoding = () => {};
    connection.destroy = () => {};
    connection.write = (request) => {
      const parsed = JSON.parse(request.trim());
      assert.equal(parsed.method, "status");
      queueMicrotask(() => connection.emit("data", `${JSON.stringify({
        ok: true,
        result: {
          protocolVersion: 2,
          sessionConfigured: false,
          sessionOwnerOnly: null,
          supportedOperations: ["hotels.create_payment_handoff"],
          verifiedOperations: [],
        },
      })}\n`));
    };
    queueMicrotask(() => connection.emit("connect"));
    return connection;
  });
  t.after(() => setAuthBrokerConnectorForTests());
  const previousSocket = process.env.TBANK_AUTH_BROKER_SOCKET;
  process.env.TBANK_AUTH_BROKER_SOCKET = "/local/test/auth.sock";
  t.after(() => {
    if (previousSocket === undefined) delete process.env.TBANK_AUTH_BROKER_SOCKET;
    else process.env.TBANK_AUTH_BROKER_SOCKET = previousSocket;
  });

  const status = await callTool("tbank_hotels_connection_status");
  assert.equal(status.customerReadiness, "mobile_login_required");
  assert.equal(status.canCreatePaymentHandoff, false);
  assert.equal(status.paymentHandoffPreview.available, false);
  assert.equal(status.paymentHandoffPreview.bookingBindingSupported, false);
  assert.equal(status.paymentHandoffPreview.paymentStatusObservation, "not_available");
});

test("does not promote supported broker operations to verified customer reads", async (t) => {
  setAuthBrokerConnectorForTests(() => {
    const connection = new EventEmitter();
    connection.setEncoding = () => {};
    connection.destroy = () => {};
    connection.write = () => queueMicrotask(() => connection.emit("data", `${JSON.stringify({
      ok: true,
      result: {
        protocolVersion: 1,
        sessionConfigured: true,
        sessionOwnerOnly: true,
        supportedOperations: ["hotels.get_booking_v1", "hotels.get_customer", "hotels.list_bookings"],
      },
    })}\n`));
    queueMicrotask(() => connection.emit("connect"));
    return connection;
  });
  t.after(() => setAuthBrokerConnectorForTests());
  const previousSocket = process.env.TBANK_AUTH_BROKER_SOCKET;
  process.env.TBANK_AUTH_BROKER_SOCKET = "/local/test/legacy-auth.sock";
  t.after(() => {
    if (previousSocket === undefined) delete process.env.TBANK_AUTH_BROKER_SOCKET;
    else process.env.TBANK_AUTH_BROKER_SOCKET = previousSocket;
  });

  const status = await callTool("tbank_hotels_connection_status");
  assert.equal(status.customerReadiness, "partial_read_only_unverified");
  assert.equal(status.canReadCustomer, false);
  assert.equal(status.canListBookings, false);
  assert.equal(status.canReadBookingV1, false);
  assert.equal(status.mobileAuth.verified, false);
  assert.deepEqual(status.mobileAuth.verifiedOperations, []);
});

test("can read verified customer and booking data through the optional local mobile auth broker", async (t) => {
  const requests = [];
  setAuthBrokerConnectorForTests(() => {
    const connection = new EventEmitter();
    connection.setEncoding = () => {};
    connection.destroy = () => {};
    connection.write = (request) => {
      const parsed = JSON.parse(request.trim());
      requests.push(parsed);
      assert.equal(parsed.version, 2);
      assert.equal(parsed.client, "hotels");
      let result;
      if (parsed.method === "status") {
        result = {
          protocolVersion: 2,
          sessionConfigured: true,
          sessionOwnerOnly: true,
          supportedOperations: ["hotels.create_payment_handoff", "hotels.get_booking_v1", "hotels.get_customer", "hotels.list_bookings", "hotels.save_voucher_v1"],
          verifiedOperations: ["hotels.get_booking_v1", "hotels.get_customer", "hotels.list_bookings", "hotels.save_voucher_v1"],
        };
      } else if (parsed.method === "hotels.get_customer") {
        result = { customer: { customer: { firstName: "Ada", lastName: "Lovelace" }, isContactCreationNeeded: false } };
      } else if (parsed.method === "hotels.list_bookings") {
        if (parsed.params.isCancelledRequired) {
          assert.deepEqual(parsed.params, { isActiveRequired: true, isCancelledRequired: true, isCompletedRequired: true });
          result = { bookings: {
            activeList: [{ orderId: "summary-order-1", hotelName: "Sensitive Hotel", city: "Sensitive City" }],
            cancelledList: [{ orderId: "summary-order-2", hotelName: "Sensitive Hotel 2" }],
            completedList: [{ orderId: "summary-order-3" }, { orderId: "summary-order-4" }],
          } };
        } else {
          assert.deepEqual(parsed.params, { isActiveRequired: true, isCancelledRequired: false, isCompletedRequired: true });
          result = { bookings: {
            activeList: [{ orderId: "order-1", hotelName: "Hotel", internalStatus: "confirmed" }],
            cancelledList: [],
            completedList: [],
          } };
        }
      } else if (parsed.method === "hotels.save_voucher_v1") {
        assert.equal(parsed.params.bookingId, "order-1");
        result = {
          voucher: {
            voucherRef: "voucher_0123456789abcdef01234567",
            localPath: "/private/tmp/voucher_0123456789abcdef01234567.pdf",
            contentType: "application/pdf",
            sizeBytes: 512,
            expiresAt: "2099-09-15T12:00:00+00:00",
            ownerOnly: true,
            containsPersonalData: true,
            documentContentIncluded: false,
            credentialsExposed: false,
          },
          handling: "Show the local path only.",
        };
      } else if (parsed.method === "hotels.create_payment_handoff") {
        assert.equal(parsed.params.bookingId, "order-1");
        result = {
          paymentHandoffRef: "payment_handoff_0123456789abcdef01234567",
          bookingBindingVerified: true,
          amountBindingVerified: true,
          amountDecimal: "8663.25",
          currency: "RUB",
          paymentStatusObservation: { rawStatus: "CREATED", interpretation: "not_interpreted" },
          factsObservedAtEpoch: 1_000,
          factsMaxAgeSeconds: 300,
          expiresAtEpoch: 1_300,
          providerRequestsPerformed: true,
        };
      } else {
        assert.equal(parsed.params.bookingId, "order-1");
        result = { booking: {
          orderId: "order-1",
          bookingId: "provider-booking-1",
          providerOrderId: "provider-order-1",
          reservationId: "reservation-1",
          paymentToken: "payment-token-1",
          nested: { confirmationNumber: "confirmation-1", hotelId: "hotel-safe-1" },
          status: "CONFIRMED",
        } };
      }
      queueMicrotask(() => connection.emit("data", `${JSON.stringify({ ok: true, result })}\n`));
    };
    queueMicrotask(() => connection.emit("connect"));
    return connection;
  });
  t.after(() => setAuthBrokerConnectorForTests());
  const names = [
    "TBANK_AUTH_BROKER_SOCKET", "TBANK_AUTH_BROKER_TIMEOUT_MS", "TBANK_HOTELS_API_BASE_URL",
    "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_AUTH_HEADER", "TBANK_HOTELS_AUTH_HEADERS_JSON",
    "TBANK_HOTELS_JWT_PRIVATE_KEY", "TBANK_HOTELS_JWT_ISSUER", "TBANK_HOTELS_JWT_AUDIENCE",
    "TBANK_HOTELS_JWT_AUTH_HEADER", "TBANK_HOTELS_JWT_AUTH_PREFIX", "TBANK_HOTELS_ENABLE_MUTATIONS",
    "TBANK_HOTELS_MUTATION_EXECUTION_PROFILE",
  ];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  for (const name of names) delete process.env[name];
  process.env.TBANK_AUTH_BROKER_SOCKET = "/local/test/auth.sock";
  t.after(() => {
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const status = await callTool("tbank_hotels_connection_status");
  assert.equal(status.mobileAuth.configured, true);
  assert.equal(status.mobileAuth.reachable, true);
  assert.equal(status.mobileAuth.sessionConfigured, true);
  assert.equal(status.mobileAuth.verified, true);
  assert.deepEqual(status.mobileAuth.verifiedOperations, ["hotels.get_booking_v1", "hotels.get_customer", "hotels.list_bookings", "hotels.save_voucher_v1"]);
  assert.equal(status.canReadBookingV1, true);
  assert.equal(status.canReadCustomer, true);
  assert.equal(status.canListBookings, true);
  assert.equal(status.canSaveVoucher, true);
  assert.equal(status.canCreatePaymentHandoff, true);
  assert.equal(status.paymentHandoffPreview.bookingBindingSupported, true);
  assert.equal(status.paymentHandoffPreview.amountBindingVerified, false);
  assert.equal(status.paymentHandoffPreview.paymentStatusObservation, "available_at_handoff");
  assert.equal(status.paymentHandoffPreview.singleUse, true);
  assert.equal(status.customerReadiness, "mobile_read_only_ready");
  assert.equal(status.searchReady, false);
  const customer = await callTool("tbank_hotels_get_customer");
  assert.equal(customer.customer.firstName, "Ada");
  const summary = await callTool("tbank_hotels_summarize_bookings", {});
  assert.deepEqual(summary, {
    status: "ready",
    activeCount: 1,
    cancelledCount: 1,
    completedCount: 2,
    detailsIncluded: false,
    personalTravelFactsIncluded: false,
    bookingReferencesIncluded: false,
  });
  assert.doesNotMatch(JSON.stringify(summary), /Sensitive|summary-order|Hotel|City/);
  const bookings = await callTool("tbank_hotels_list_bookings", {
    isActiveRequired: true,
    isCancelledRequired: false,
    isCompletedRequired: true,
  });
  assert.equal(bookings.activeList.length, 1);
  assert.match(bookings.activeList[0].bookingRef, /^booking_[a-f0-9]{24}$/);
  assert.equal(bookings.activeList[0].orderId, undefined);
  assert.doesNotMatch(JSON.stringify(bookings), /order-1/);
  const booking = await callTool("tbank_hotels_get_booking", { bookingRef: bookings.activeList[0].bookingRef });
  assert.equal(booking.bookingRef, bookings.activeList[0].bookingRef);
  assert.equal(booking.status, "CONFIRMED");
  assert.equal(booking.orderId, undefined);
  assert.equal(booking.bookingId, undefined);
  assert.equal(booking.nested.hotelId, "hotel-safe-1");
  assert.doesNotMatch(JSON.stringify(booking), /order-1|provider-booking-1|provider-order-1|reservation-1|payment-token-1|confirmation-1/);
  const overview = await callTool("tbank_hotels_get_booking_overview", { bookingRef: bookings.activeList[0].bookingRef, includeVoucher: false });
  assert.equal(overview.booking.bookingRef, bookings.activeList[0].bookingRef);
  assert.equal(overview.voucher.requested, false);
  assert.doesNotMatch(JSON.stringify(overview), /order-1|provider-booking-1|payment-token-1/);
  const cancellation = await callTool("tbank_hotels_preview_cancellation", { bookingRef: bookings.activeList[0].bookingRef });
  assert.equal(cancellation.booking.bookingRef, bookings.activeList[0].bookingRef);
  assert.match(cancellation.note, /не вычисляет сумму возврата/);
  assert.doesNotMatch(JSON.stringify(cancellation), /order-1|provider-booking-1|payment-token-1/);
  const handoff = await callTool("tbank_hotels_create_payment_handoff_preview", { bookingRef: bookings.activeList[0].bookingRef });
  assert.equal(handoff.status, "preview_ready");
  assert.equal(handoff.bookingRef, bookings.activeList[0].bookingRef);
  assert.equal(handoff.paymentHandoffRef, "payment_handoff_0123456789abcdef01234567");
  assert.equal(handoff.bookingBindingVerified, true);
  assert.equal(handoff.amountBindingVerified, true);
  assert.equal(handoff.amountDecimal, "8663.25");
  assert.equal(handoff.currency, "RUB");
  assert.equal(handoff.paymentStatusObservation.rawStatus, "CREATED");
  assert.equal(handoff.paymentStatusObservation.interpretation, "not_interpreted");
  assert.equal(handoff.providerRequestsPerformed, true);
  assert.equal(handoff.paymentSetupPerformed, false);
  assert.equal(handoff.paymentExecutionPerformed, false);
  assert.doesNotMatch(JSON.stringify(handoff), /order-1|payment-token-1/);
  const voucher = await callTool("tbank_hotels_save_voucher", { bookingRef: bookings.activeList[0].bookingRef });
  assert.equal(voucher.status, "saved_locally");
  assert.equal(voucher.bookingRef, bookings.activeList[0].bookingRef);
  assert.equal(voucher.voucher.documentContentIncluded, false);
  assert.equal(voucher.voucher.contentType, "application/pdf");
  assert.doesNotMatch(JSON.stringify(voucher), /order-1|%PDF|base64/);
  assert.deepEqual(requests.map(({ method }) => method), [
    "status",
    "hotels.get_customer",
    "hotels.list_bookings",
    "hotels.list_bookings",
    "hotels.get_booking_v1",
    "hotels.get_booking_v1",
    "hotels.get_booking_v1",
    "hotels.create_payment_handoff",
    "hotels.save_voucher_v1",
  ]);
});

test("never fetches or embeds voucher PDF through legacy and overview tools", async (t) => {
  const savedFetch = globalThis.fetch;
  const savedBaseUrl = process.env.TBANK_HOTELS_API_BASE_URL;
  const savedToken = process.env.TBANK_HOTELS_AUTH_TOKEN;
  let calls = 0;
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  globalThis.fetch = async (url) => {
    calls += 1;
    assert.match(String(url), /\/api\/v3\/hotels\/bookings\/order-1$/);
    return new Response(JSON.stringify({ status: "CONFIRMED" }), { status: 200, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    if (savedBaseUrl === undefined) delete process.env.TBANK_HOTELS_API_BASE_URL; else process.env.TBANK_HOTELS_API_BASE_URL = savedBaseUrl;
    if (savedToken === undefined) delete process.env.TBANK_HOTELS_AUTH_TOKEN; else process.env.TBANK_HOTELS_AUTH_TOKEN = savedToken;
  });

  await assert.rejects(callTool("tbank_hotels_get_voucher", { orderId: "order-1" }), /Inline voucher delivery is disabled/);
  assert.equal(calls, 0);
  const overview = await callTool("tbank_hotels_get_booking_overview", { orderId: "order-1", includeVoucher: true });
  assert.equal(calls, 1);
  assert.equal(overview.voucher.documentContentIncluded, false);
  assert.equal(overview.voucher.separateHandoffRequired, true);
  assert.equal(overview.voucher.availableViaTool, null);
  assert.doesNotMatch(JSON.stringify(overview), /%PDF|base64/);
});

test("allows a broker read to exceed the former three-second timeout", async (t) => {
  setAuthBrokerConnectorForTests(() => {
    const connection = new EventEmitter();
    connection.setEncoding = () => {};
    connection.destroy = () => {};
    connection.write = (request) => {
      const parsed = JSON.parse(request.trim());
      const result = parsed.method === "hotels.list_bookings"
        ? { bookings: { activeList: [{ orderId: "slow-order" }], cancelledList: [], completedList: [] } }
        : { booking: { orderId: "slow-order" } };
      const delay = parsed.method === "hotels.get_booking_v1" ? 3_100 : 0;
      setTimeout(() => connection.emit("data", `${JSON.stringify({ ok: true, result })}\n`), delay);
    };
    queueMicrotask(() => connection.emit("connect"));
    return connection;
  });
  t.after(() => setAuthBrokerConnectorForTests());
  const names = ["TBANK_AUTH_BROKER_SOCKET", "TBANK_AUTH_BROKER_TIMEOUT_MS"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  process.env.TBANK_AUTH_BROKER_SOCKET = "/local/test/auth.sock";
  delete process.env.TBANK_AUTH_BROKER_TIMEOUT_MS;
  t.after(() => {
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const bookings = await callTool("tbank_hotels_list_bookings", {
    isActiveRequired: true,
    isCancelledRequired: false,
    isCompletedRequired: false,
  });
  const booking = await callTool("tbank_hotels_get_booking", { bookingRef: bookings.activeList[0].bookingRef });
  assert.equal(booking.bookingRef, bookings.activeList[0].bookingRef);
  assert.equal(booking.orderId, undefined);
});

test("reports an unreachable auth broker and rejects unsupported booking identifiers locally", async (t) => {
  let connectionCount = 0;
  setAuthBrokerConnectorForTests(() => {
    connectionCount += 1;
    const connection = new EventEmitter();
    connection.setEncoding = () => {};
    connection.destroy = () => {};
    queueMicrotask(() => connection.emit("error", new Error("unavailable")));
    return connection;
  });
  t.after(() => setAuthBrokerConnectorForTests());
  const previous = process.env.TBANK_AUTH_BROKER_SOCKET;
  process.env.TBANK_AUTH_BROKER_SOCKET = "/local/test/missing.sock";
  t.after(() => {
    if (previous === undefined) delete process.env.TBANK_AUTH_BROKER_SOCKET;
    else process.env.TBANK_AUTH_BROKER_SOCKET = previous;
  });

  const status = await callTool("tbank_hotels_connection_status");
  assert.equal(status.customerReadiness, "broker_unavailable");
  assert.equal(status.mobileAuth.reachable, false);
  assert.equal(status.canReadBookingV1, false);
  await assert.rejects(callTool("tbank_hotels_get_booking", { bookingRef: "booking_invalid" }), /opaque reference/);
  assert.equal(connectionCount, 1);
});

test("rejects customer autofill locally when no customer auth profile exists", async () => {
  const names = ["TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_AUTH_HEADERS_JSON", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  for (const name of names) delete process.env[name];
  try {
    await assert.rejects(callTool("tbank_hotels_get_customer"), /Customer context is not configured/);
  } finally {
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  }
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

test("allows anonymous read-only search without sending authorization", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = [
    "TBANK_HOTELS_API_BASE_URL",
    "TBANK_HOTELS_AUTH_TOKEN",
    "TBANK_HOTELS_AUTH_HEADER",
    "TBANK_HOTELS_AUTH_HEADERS_JSON",
    "TBANK_HOTELS_JWT_PRIVATE_KEY",
    "TBANK_HOTELS_JWT_PRIVATE_KEY_FILE",
    "TBANK_HOTELS_ENABLE_MUTATIONS",
    "TBANK_HOTELS_MUTATION_EXECUTION_PROFILE",
  ];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  for (const name of names) delete process.env[name];
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels-public.example.test/";
  const calls = [];
  globalThis.fetch = async (_url, options) => {
    calls.push(options);
    return new Response(JSON.stringify({ payload: { hotels: [] } }), {
      status: 200,
      headers: { "content-type": "application/json" },
    });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const status = await callTool("tbank_hotels_connection_status");
  assert.equal(status.searchReady, true);
  assert.equal(status.authentication, "not_required");
  assert.equal(status.authMode, "anonymous");
  assert.equal(status.bookingExecution.available, false);
  await callTool("tbank_hotels_search", {
    payload: {
      destinationId: 1,
      checkinDate: "2099-09-01",
      checkoutDate: "2099-09-02",
      guests: [{ adultsCount: 2 }],
    },
  });
  assert.equal(calls.length, 1);
  assert.equal(Object.keys(calls[0].headers).some((name) => name.toLowerCase() === "authorization"), false);
  await assert.rejects(callTool("tbank_hotels_get_customer"), /Customer context is not configured/);
  assert.equal(calls.length, 1);
});

test("rejects a service JWT key file readable by group or others", async (t) => {
  if (process.platform === "win32") return t.skip("POSIX file modes are not available on Windows");
  const keyDirectory = mkdtempSync(resolve(tmpdir(), "hotels-jwt-permissions-"));
  const keyFile = resolve(keyDirectory, "service-key.pem");
  writeFileSync(keyFile, "fixture-key", { mode: 0o600 });
  chmodSync(keyFile, 0o644);
  const server = startServer({
    TBANK_HOTELS_API_BASE_URL: "https://hotels.example.test",
    TBANK_HOTELS_JWT_PRIVATE_KEY_FILE: keyFile,
    TBANK_HOTELS_JWT_ISSUER: "HOTELSSEARCHAPI",
    TBANK_HOTELS_JWT_AUDIENCE: "HOTELSAPI",
  });
  t.after(() => server.child.kill());
  const result = await server.request({ jsonrpc: "2.0", id: 1, method: "tools/call", params: { name: "tbank_hotels_connection_status", arguments: {} } });
  const status = JSON.parse(result.result.content[0].text);
  assert.equal(status.searchReady, false);
  assert.equal(status.authentication, "invalid_configuration");
  assert.match(status.diagnostics.authentication, /owner-only/);
  assert.doesNotMatch(result.result.content[0].text, /fixture-key|service-key\.pem/);
});

test("creates the configured HotelsApiPrivate RS384 service JWT without exposing its key", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = [
    "TBANK_HOTELS_API_BASE_URL",
    "TBANK_HOTELS_AUTH_TOKEN",
    "TBANK_HOTELS_AUTH_HEADER",
    "TBANK_HOTELS_AUTH_HEADERS_JSON",
    "TBANK_HOTELS_JWT_PRIVATE_KEY",
    "TBANK_HOTELS_JWT_PRIVATE_KEY_FILE",
    "TBANK_HOTELS_JWT_ISSUER",
    "TBANK_HOTELS_JWT_AUDIENCE",
    "TBANK_HOTELS_JWT_AUTH_HEADER",
    "TBANK_HOTELS_JWT_AUTH_PREFIX",
    "TBANK_HOTELS_ENABLE_MUTATIONS",
    "TBANK_HOTELS_MUTATION_EXECUTION_PROFILE",
  ];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  const { privateKey, publicKey } = generateKeyPairSync("rsa", { modulusLength: 2048 });
  const keyDirectory = mkdtempSync(resolve(tmpdir(), "hotels-jwt-key-"));
  const keyFile = resolve(keyDirectory, "service-key.pem");
  writeFileSync(keyFile, privateKey.export({ type: "pkcs1", format: "pem" }), { mode: 0o600 });
  const calls = [];
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels-private.example.test/";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  process.env.TBANK_HOTELS_JWT_PRIVATE_KEY_FILE = keyFile;
  process.env.TBANK_HOTELS_JWT_ISSUER = "HOTELSSEARCHAPI";
  process.env.TBANK_HOTELS_JWT_AUDIENCE = "HOTELSAPI";
  delete process.env.TBANK_HOTELS_AUTH_TOKEN;
  delete process.env.TBANK_HOTELS_AUTH_HEADER;
  delete process.env.TBANK_HOTELS_AUTH_HEADERS_JSON;
  delete process.env.TBANK_HOTELS_JWT_AUTH_HEADER;
  delete process.env.TBANK_HOTELS_JWT_AUTH_PREFIX;
  process.env.TBANK_HOTELS_ENABLE_MUTATIONS = "true";
  process.env.TBANK_HOTELS_MUTATION_EXECUTION_PROFILE = "non_production_v1_reviewed";
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

test("accepts an API-prefix base URL without duplicating the api path", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  const calls = [];
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels.example.test/api";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  globalThis.fetch = async (url) => {
    calls.push(new URL(url));
    return new Response(JSON.stringify({ payload: { hotels: [] } }), { status: 200, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  await callTool("tbank_hotels_search", {
    payload: {
      destinationId: 17039,
      checkinDate: "2099-09-01",
      checkoutDate: "2099-09-02",
      guests: [{ adultsCount: 2, childrenAge: [] }],
      filters: [],
    },
  });

  assert.equal(calls.length, 1);
  assert.equal(calls[0].origin, "https://hotels.example.test");
  assert.equal(calls[0].pathname, "/api/v1/hotels/search");
});

test("prepare is stateless and execute rejects a changed booking payload", async (t) => {
  const server = startServer({
    TBANK_HOTELS_API_BASE_URL: "https://hotels.example.test/",
    TBANK_HOTELS_AUTH_HEADERS_JSON: JSON.stringify({ Authorization: "test-customer-auth", "x-real-ip": "192.0.2.1" }),
    TBANK_HOTELS_ENABLE_MUTATIONS: "true",
    TBANK_HOTELS_MUTATION_EXECUTION_PROFILE: "non_production_v1_reviewed",
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
  assert.deepEqual(preview.executionReadiness, { available: false, status: "not_available" });
  assert.doesNotMatch(JSON.stringify(preview), /x-real-ip|missingRequiredHeaders/);
  assert.doesNotMatch(JSON.stringify(preview), /TBANK_HOTELS_ENABLE_MUTATIONS/);
  const executed = await server.request({ jsonrpc: "2.0", id: 2, method: "tools/call", params: { name: "tbank_hotels_execute_booking", arguments: { payload, preparedRequestHash: "0".repeat(64), confirmation: "not-used", preparedAt: new Date().toISOString(), expiresAt: new Date(Date.now() + 60_000).toISOString() } } });
  assert.equal(executed.result.isError, true);
  assert.match(executed.result.content[0].text, /mutation execution is not available/);
});

test("mutation flag alone cannot activate write execution without a reviewed profile", async (t) => {
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
  assert.equal(preview.status, "preview_only");
  assert.equal(preview.executionAvailable, false);
  assert.equal("requestHash" in preview, false);
  const status = await server.request({ jsonrpc: "2.0", id: 2, method: "tools/call", params: { name: "tbank_hotels_connection_status", arguments: {} } });
  const readiness = JSON.parse(status.result.content[0].text).bookingExecution;
  assert.equal(readiness.available, false);
  assert.equal(readiness.status, "contract_review_required");
});

test("rejects an expired prepared mutation before reaching transport", async (t) => {
  const server = startServer({
    TBANK_HOTELS_API_BASE_URL: "https://hotels.example.test/",
    TBANK_HOTELS_AUTH_HEADERS_JSON: JSON.stringify({ Authorization: "test-customer-auth", "x-real-ip": "192.0.2.1" }),
    TBANK_HOTELS_ENABLE_MUTATIONS: "true",
    TBANK_HOTELS_MUTATION_EXECUTION_PROFILE: "non_production_v1_reviewed",
  });
  t.after(() => server.child.kill());
  const payload = { bookHash: "hash", guestContact: { email: "person@example.test", phone: "+70000000000" }, rooms: [] };
  const expiredWindowStart = Date.now() - 10 * 60 * 1_000;
  const preparedAt = new Date(expiredWindowStart).toISOString();
  const expiresAt = new Date(expiredWindowStart + 5 * 60 * 1_000).toISOString();
  const material = JSON.stringify({ action: "booking", path: "/api/v1/hotels/bookings/tasks/create", payload, preparedAt, expiresAt });
  const requestHash = createHash("sha256").update(material).digest("hex");
  const confirmation = `CONFIRM_TBANK_HOTELS_BOOKING_${requestHash.slice(0, 12)}`;
  const executed = await server.request({ jsonrpc: "2.0", id: 1, method: "tools/call", params: { name: "tbank_hotels_execute_booking", arguments: { payload, preparedRequestHash: requestHash, confirmation, preparedAt, expiresAt } } });
  assert.equal(executed.result.isError, true);
  assert.match(executed.result.content[0].text, /has expired/);
  assert.doesNotMatch(executed.result.content[0].text, /BASE_URL/);
});

test("limits concurrent provider requests inside one MCP process", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_MAX_CONCURRENT_REQUESTS"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels-load-guard.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  process.env.TBANK_HOTELS_MAX_CONCURRENT_REQUESTS = "2";
  let active = 0;
  let maximumActive = 0;
  let requestCount = 0;
  globalThis.fetch = async () => {
    requestCount += 1;
    active += 1;
    maximumActive = Math.max(maximumActive, active);
    await new Promise((resolve) => setTimeout(resolve, 20));
    active -= 1;
    return new Response(JSON.stringify({ payload: { filters: [] } }), {
      status: 200,
      headers: { "content-type": "application/json" },
    });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  await Promise.all(Array.from({ length: 6 }, () => callTool("tbank_hotels_get_search_filters", { apiVersion: "v1" })));

  assert.equal(requestCount, 6);
  assert.equal(maximumActive, 2);
  const status = await callTool("tbank_hotels_connection_status", {});
  assert.equal(status.loadProtection.status, "configured");
  assert.equal(status.loadProtection.maxConcurrentProviderRequests, 2);
  assert.equal(status.loadProtection.activeProviderRequests, 0);
  assert.equal(status.loadProtection.queuedProviderRequests, 0);
});

test("coalesces concurrent identical searches and reuses the short cache", async (t) => {
  const savedFetch = globalThis.fetch;
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_MAX_CONCURRENT_REQUESTS"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels-search-cache.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  process.env.TBANK_HOTELS_MAX_CONCURRENT_REQUESTS = "2";
  let searchRequests = 0;
  globalThis.fetch = async (url) => {
    assert.equal(new URL(url).pathname, "/api/v1/hotels/search");
    searchRequests += 1;
    await new Promise((resolve) => setTimeout(resolve, 20));
    return new Response(JSON.stringify({ payload: {
      hotels: [{ hotelId: "hotel-cache", hotelName: "Cached Hotel", review: { rating: 9.1 } }],
      filteredHotelsCount: 1,
      hotelsTotalCount: 1,
      isLoadingCompleted: true,
      nextOffset: null,
    } }), { status: 200, headers: { "content-type": "application/json" } });
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });
  const args = {
    destinationId: 17039,
    checkinDate: "2099-10-01",
    checkoutDate: "2099-10-02",
    rooms: [{ adults: 2 }],
    maxOptions: 1,
    ranking: "provider_order",
  };

  const concurrent = await Promise.all([
    callTool("tbank_hotels_plan_stay", args),
    callTool("tbank_hotels_plan_stay", args),
  ]);
  const cached = await callTool("tbank_hotels_plan_stay", args);
  const reranked = await callTool("tbank_hotels_plan_stay", { ...args, ranking: "best_value" });

  assert.equal(searchRequests, 1);
  assert.deepEqual(new Set(concurrent.map((plan) => plan.searchCoverage.cacheStatus)), new Set(["miss", "coalesced"]));
  assert.equal(cached.searchCoverage.cacheStatus, "hit");
  assert.equal(reranked.searchCoverage.cacheStatus, "hit");
  assert.equal(reranked.searchCoverage.rankingAppliedLocally, "best_value");
  assert.equal(reranked.preferencesApplied.requested, false);
  assert.equal(reranked.preferencesApplied.applied, false);
  assert.equal(reranked.preferencesApplied.source, "not_requested");
  assert.notEqual(concurrent[0].journeyId, concurrent[1].journeyId);
  assert.notEqual(concurrent[0].options[0].optionId, concurrent[1].options[0].optionId);
});

test("marks search unready for an invalid local concurrency setting", async (t) => {
  const names = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_MAX_CONCURRENT_REQUESTS"];
  const savedEnvironment = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels-invalid-guard.example.test/";
  process.env.TBANK_HOTELS_AUTH_TOKEN = "test-token";
  process.env.TBANK_HOTELS_MAX_CONCURRENT_REQUESTS = "0";
  t.after(() => {
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const status = await callTool("tbank_hotels_connection_status", {});

  assert.equal(status.searchReady, false);
  assert.equal(status.loadProtection.status, "invalid_configuration");
  assert.match(status.diagnostics.loadProtection, /must be an integer from 1 to 8/);
});

test("inspects selected checkout and previews promocode and extra-service changes without writes or identifier leaks", async (t) => {
  const savedFetch = globalThis.fetch;
  const savedNow = Date.now;
  let currentTime = savedNow();
  Date.now = () => currentTime;
  const environmentNames = ["TBANK_HOTELS_API_BASE_URL", "TBANK_HOTELS_AUTH_TOKEN", "TBANK_HOTELS_AUTH_HEADERS_JSON", "TBANK_HOTELS_JWT_PRIVATE_KEY"];
  const savedEnvironment = Object.fromEntries(environmentNames.map((name) => [name, process.env[name]]));
  process.env.TBANK_HOTELS_API_BASE_URL = "https://hotels-checkout.example.test/";
  delete process.env.TBANK_HOTELS_AUTH_TOKEN;
  delete process.env.TBANK_HOTELS_AUTH_HEADERS_JSON;
  delete process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    const request = { url: String(url), method: options.method ?? "GET", body: options.body ? JSON.parse(options.body) : null };
    calls.push(request);
    if (request.url.endsWith("/api/v1/hotels/search")) {
      return new Response(JSON.stringify({ payload: {
        hotels: [{
          hotelId: "101",
          hotelName: "Checkout Hotel",
          starRating: 4,
          areaLocation: { destinationName: "Kazan" },
          hotelLocation: { address: "Safe street" },
          rateForHotelsFeed: { shownPrice: { amount: 12000, currency: "RUB" }, mealName: "Breakfast" },
          review: { rating: 9.2, ratingsCount: 500 },
        }],
        hotelsTotalCount: 1,
        filteredHotelsCount: 1,
        isLoadingCompleted: true,
      } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    if (request.url.endsWith("/api/v3/hotels/101/rates")) {
      return new Response(JSON.stringify({ payload: { rates: [{
        bookHash: "provider-book-secret",
        roomId: "provider-room-secret",
        shownPrice: { amount: 11462.88, currency: "RUB" },
        paymentPrice: { amount: 11462.88, currency: "RUB" },
        mealName: "Breakfast",
        isNonRefundable: false,
      }, {
        bookHash: "provider-second-book-secret",
        roomId: "provider-second-room-secret",
        shownPrice: { amount: 12026.63, currency: "RUB" },
        paymentPrice: { amount: 12026.63, currency: "RUB" },
        mealName: "Breakfast",
        isNonRefundable: false,
      }], rooms: [], otherRates: [] } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    if (request.url.endsWith("/api/v3/rates/provider-book-secret")) {
      return new Response(JSON.stringify({ payload: {
        checkInDate: "2099-09-15",
        checkOutDate: "2099-09-17",
        hotelInfo: { hotelId: "provider-hotel-secret", hotelName: "Checkout Hotel" },
        roomsForBooking: { rooms: [{
          roomId: "provider-checkout-room-secret",
          roomName: "Standard",
          guests: [],
          rates: [{
            bookHash: "provider-decoy-book-secret",
            shownPrice: { amount: 1, currency: "RUB" },
            paymentPrice: { amount: 1, currency: "RUB" },
            cancellationPolicyRules: { policies: [] },
          }, {
            bookHash: "provider-book-secret",
            shownPrice: { amount: 11462.88, currency: "RUB" },
            paymentPrice: { amount: 11462.88, currency: "RUB" },
            cancellationPolicyRules: {
              freeCancellationUntil: "2026-09-14T00:00:00+03:00",
              policies: [{
                startAt: "2026-09-14T00:00:00+03:00",
                endAt: null,
                shownPrice: { amount: 11462.88, currency: "RUB" },
                paymentPrice: { amount: 11462.88, currency: "RUB" },
              }],
            },
            extraServices: {
              earlyCheckIn: [{ id: "provider-early-secret", time: "12:00", price: { amount: 1000, currency: "RUB" } }],
              lateCheckOut: [{ id: "provider-late-secret", time: "16:00", price: { amount: 1500, currency: "RUB" } }],
            },
            discount: { standardRatePrice: { amount: 13000, currency: "RUB" } },
          }],
        }] },
        promocodeInfo: null,
        cashbackInfo: {
          cbServiceName: "Hotels",
          accounts: [{
            accountNumber: "provider-account-secret",
            loyaltyProgram: "Black",
            loyaltyProgramCurrency: "RUB",
            cashbackPercent: 10,
            cashbackAmount: 1146,
            cashbackCorrectionAmount: 0,
            topBorder: null,
          }],
        },
      } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    if (request.url.endsWith("/api/v1/hotels/promocodes/validate")) {
      assert.equal(request.body.bookHash, "provider-book-secret");
      if (request.body.promocode === "BAD") {
        return new Response(JSON.stringify({ code: "promocode_expired", details: "must-not-leak" }), { status: 400, headers: { "content-type": "application/json" } });
      }
      assert.equal(request.body.promocode, "SUMMER");
      return new Response(JSON.stringify({ payload: { promocodeInfo: { value: { amount: 1000, currency: "RUB" } } } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    if (request.url.endsWith("/api/v1/hotels/rates/provider-book-secret/upgrade")) {
      assert.deepEqual(request.body, {
        hotelId: 101,
        checkInDate: "2099-09-15",
        checkOutDate: "2099-09-17",
        guests: [{ adultsCount: 2, childrenAge: [] }],
      });
      return new Response(JSON.stringify({ payload: { rate: {
        bookHash: "provider-upgrade-wrapper-secret",
        rateForUpgrade: {
          bookHash: "provider-upgrade-secret",
          upgradeType: "room",
          additionalCost: { amount: 2500, currency: "RUB" },
          room: {
            roomId: "provider-upgrade-room-secret",
            roomName: "Superior",
            bedName: "King",
            roomSize: 28,
            roomFacilities: [],
            images: [{ url: "https://images.example.test/secret" }],
            facilitiesDifference: { baseFacilityCodes: ["base"], upgradedFacilityCodes: ["view"] },
          },
        },
      } } }), { status: 200, headers: { "content-type": "application/json" } });
    }
    throw new Error(`Unexpected fake provider request: ${request.method} ${request.url}`);
  };
  t.after(() => {
    globalThis.fetch = savedFetch;
    Date.now = savedNow;
    for (const [name, value] of Object.entries(savedEnvironment)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  });

  const plan = await callTool("tbank_hotels_plan_stay", {
    destinationId: 90229,
    checkinDate: "2099-09-15",
    checkoutDate: "2099-09-17",
    rooms: [{ adults: 2 }],
  });
  await callTool("tbank_hotels_select_stay_option", { journeyId: plan.journeyId, optionId: plan.options[0].optionId });
  const rates = await callTool("tbank_hotels_get_selected_stay_rates", { journeyId: plan.journeyId });
  await callTool("tbank_hotels_select_stay_rate", { journeyId: plan.journeyId, rateOptionId: rates.rateOptions[0].rateOptionId });
  const inspection = await callTool("tbank_hotels_inspect_checkout", {
    journeyId: plan.journeyId,
    promocode: "SUMMER",
    includeUpgradeOffer: true,
  });

  assert.equal(inspection.status, "ready");
  assert.match(inspection.checkoutRef, /^checkout_[a-f0-9]{24}$/);
  assert.equal(Date.parse(inspection.expiresAt) - Date.parse(inspection.inspectedAt), 5 * 60 * 1_000);
  assert.equal(inspection.providerRequestCount, 3);
  assert.equal(inspection.checkout.prices.shown.display, "11 462,88 ₽");
  assert.equal(inspection.checkout.prices.standard.display, "13 000 ₽");
  assert.equal(inspection.checkout.cancellation.freeCancellationUntilDisplay, "14.09.2026 00:00 (UTC+03:00)");
  assert.equal(inspection.checkout.cashback.options[0].cashbackAmount, 1146);
  assert.equal(inspection.promocodeValidation.status, "valid");
  assert.equal(inspection.promocodeValidation.discount.display, "1 000 ₽");
  assert.equal(inspection.upgradeOffer.available, true);
  assert.equal(inspection.upgradeOffer.additionalCost.display, "2 500 ₽");
  assert.deepEqual(inspection.upgradeOffer.room, { roomName: "Superior", bedName: "King", roomSize: 28, upgradedFacilityCodes: ["view"] });
  const early = inspection.checkout.extraServices.earlyCheckIn[0];
  const late = inspection.checkout.extraServices.lateCheckOut[0];
  assert.match(early.extraServiceOptionRef, /^checkout_extra_[a-f0-9]{24}$/);
  assert.match(late.extraServiceOptionRef, /^checkout_extra_[a-f0-9]{24}$/);
  const serializedInspection = JSON.stringify(inspection);
  assert.doesNotMatch(serializedInspection, /provider-book-secret|rotated-book-secret|provider-checkout-secret|provider-early-secret|provider-late-secret|provider-account-secret|provider-upgrade|provider-room-secret|images\.example/i);
  assert.equal(inspection.personalDataCollected, false);
  assert.equal(inspection.checkoutModified, false);
  assert.equal(inspection.bookingCreated, false);
  assert.equal(inspection.paymentStarted, false);

  const callsBeforePreview = calls.length;
  const preview = await callTool("tbank_hotels_preview_checkout_changes", {
    journeyId: plan.journeyId,
    promocodeAction: "apply_validated",
    extraServiceOptionRefs: [early.extraServiceOptionRef, late.extraServiceOptionRef],
  });
  assert.equal(calls.length, callsBeforePreview);
  assert.equal(preview.status, "preview_only");
  assert.equal(preview.providerQuoteUpdateRequired, true);
  assert.equal(preview.authoritativeUpdatedPriceAvailable, false);
  assert.equal(preview.requestedChanges.selectedExtraServices.length, 2);
  assert.equal(preview.providerWritePerformed, false);
  assert.equal(preview.checkoutModified, false);
  assert.equal(preview.bookingCreated, false);
  assert.equal(preview.paymentStarted, false);
  assert.doesNotMatch(JSON.stringify(preview), /provider-book-secret|provider-checkout-secret|provider-early-secret|provider-late-secret|provider-account-secret/i);
  assert.equal(calls.some((call) => /\/rates\/[^/]+\/(promocode|extraServices)$/.test(new URL(call.url).pathname)), false);

  await assert.rejects(
    callTool("tbank_hotels_preview_checkout_changes", { journeyId: plan.journeyId, extraServiceOptionRefs: ["checkout_extra_000000000000000000000000"] }),
    /not part of the latest checkout inspection/,
  );
  assert.equal(calls.length, callsBeforePreview);

  const rejectedPromocode = await callTool("tbank_hotels_inspect_checkout", { journeyId: plan.journeyId, promocode: "BAD" });
  assert.equal(rejectedPromocode.status, "ready");
  assert.deepEqual(rejectedPromocode.promocodeValidation, {
    status: "invalid",
    reason: "provider_rejected_promocode",
    providerCode: "promocode_expired",
    providerHttpStatus: 400,
  });
  assert.doesNotMatch(JSON.stringify(rejectedPromocode), /must-not-leak|provider-book-secret|provider-checkout-secret/i);
  const callsBeforeRejectedPreview = calls.length;
  await assert.rejects(
    callTool("tbank_hotels_preview_checkout_changes", { journeyId: plan.journeyId, promocodeAction: "apply_validated" }),
    /successfully validated promocode/,
  );
  assert.equal(calls.length, callsBeforeRejectedPreview);

  currentTime += 5 * 60 * 1_000;
  await assert.rejects(
    callTool("tbank_hotels_preview_checkout_changes", { journeyId: plan.journeyId }),
    /Checkout inspection expired/,
  );
  assert.equal(calls.length, callsBeforeRejectedPreview);

  await callTool("tbank_hotels_select_stay_rate", { journeyId: plan.journeyId, rateOptionId: rates.rateOptions[1].rateOptionId });
  await assert.rejects(
    callTool("tbank_hotels_preview_checkout_changes", { journeyId: plan.journeyId }),
    /Inspect the currently selected checkout/,
  );
  assert.equal(calls.length, callsBeforeRejectedPreview);
});
