import assert from "node:assert/strict";
import { chmodSync, existsSync, linkSync, mkdtempSync, readFileSync, statSync, symlinkSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { resolve } from "node:path";
import { execFileSync } from "node:child_process";
import test from "node:test";
import { runtimeEnvironment } from "../src/cli.mjs";
import { captureOwnBookingStructure } from "../src/booking-shape-capture.mjs";

const cli = new URL("../src/cli.mjs", import.meta.url).pathname;

function execute(args, env = {}) {
  return execFileSync(process.execPath, [cli, ...args], {
    encoding: "utf8",
    env: { PATH: process.env.PATH, HOME: process.env.HOME, ...env },
  });
}

test("setup stores only a private-key path in an owner-only config", () => {
  const directory = mkdtempSync(resolve(tmpdir(), "tbank-mcp-local-"));
  const keyFile = resolve(directory, "service-key.pem");
  const configFile = resolve(directory, "config.json");
  writeFileSync(keyFile, "-----BEGIN RSA PRIVATE KEY-----\nfixture-only\n-----END RSA PRIVATE KEY-----\n", { mode: 0o600 });
  execute([
    "setup", "--profile", "hotels", "--config", configFile,
    "--hotels-api-base-url", "https://hotels.example.test/",
    "--hotels-jwt-key-file", keyFile,
  ]);
  const configText = readFileSync(configFile, "utf8");
  const config = JSON.parse(configText);
  assert.equal(config.hotels.jwtPrivateKeyFile, keyFile);
  assert.doesNotMatch(configText, /fixture-only/);
  assert.equal(statSync(configFile).mode & 0o077, 0);
});

test("doctor is offline and reports readiness without secret values", () => {
  const directory = mkdtempSync(resolve(tmpdir(), "tbank-mcp-doctor-"));
  const keyFile = resolve(directory, "service-key.pem");
  const sessionFile = resolve(directory, "session.json");
  const configFile = resolve(directory, "config.json");
  writeFileSync(keyFile, "private-material-not-printed", { mode: 0o600 });
  writeFileSync(sessionFile, "session-material-not-printed", { mode: 0o600 });
  chmodSync(directory, 0o700);
  writeFileSync(configFile, JSON.stringify({
    version: 1,
    hotels: { apiBaseUrl: "https://hotels.example.test/", jwtIssuer: "issuer", jwtAudience: "audience", jwtPrivateKeyFile: keyFile },
    banking: { sessionFile, brokerSocket: resolve(directory, "missing.sock") },
  }), { mode: 0o600 });
  const output = execute(["doctor", "--profile", "combined"], { TBANK_MCP_LOCAL_CONFIG: configFile });
  const report = JSON.parse(output);
  assert.equal(report.providerRequestsPerformed, false);
  assert.equal(report.secretsExposed, false);
  assert.equal(report.checks.hotels.authentication, "ready");
  assert.equal(report.checks.banking.session, "ready");
  assert.doesNotMatch(output, /private-material|session-material/);
});

test("generates secret-free client registration for all supported local clients", () => {
  for (const client of ["opencode", "codex", "claude"]) {
    const output = execute(["client-config", "--client", client, "--profile", "combined"]);
    assert.match(output, /tbank-hotels/);
    assert.match(output, /tbank-banking/);
    assert.match(output, /run/);
    assert.match(output, /with-broker/);
    assert.doesNotMatch(output, /PRIVATE_KEY|AUTH_TOKEN|session\.json/);
  }
});

test("combined profile does not inject Banking session into Hotels", () => {
  const previousSession = process.env.TBANK_BANKING_SESSION;
  process.env.TBANK_BANKING_SESSION = "/private/tmp/parent-session.json";
  try {
    const config = {
      hotels: {
        apiBaseUrl: "https://hotels.example.test/",
        jwtIssuer: "issuer",
        jwtAudience: "audience",
        jwtPrivateKeyFile: "/private/tmp/service-key.pem",
      },
      banking: {
        sessionFile: "/private/tmp/config-session.json",
        brokerSocket: "/private/tmp/auth.sock",
      },
    };
    const hotels = runtimeEnvironment("hotels", config);
    const banking = runtimeEnvironment("banking", config);
    assert.equal(hotels.TBANK_BANKING_SESSION, undefined);
    assert.equal(hotels.TBANK_AUTH_BROKER_SOCKET, config.banking.brokerSocket);
    assert.equal(banking.TBANK_BANKING_SESSION, "/private/tmp/parent-session.json");
  } finally {
    if (previousSession === undefined) delete process.env.TBANK_BANKING_SESSION;
    else process.env.TBANK_BANKING_SESSION = previousSession;
  }
});

test("booking fixture inspection exposes structure without source values", () => {
  const directory = mkdtempSync(resolve(tmpdir(), "tbank-booking-fixture-"));
  const fixtureFile = resolve(directory, "booking.json");
  const reportFile = resolve(directory, "shape.json");
  const secrets = {
    order: "443021782873",
    email: "person@example.test",
    token: "payment-token-must-not-leak",
    hotel: "Private Hotel Name",
  };
  writeFileSync(fixtureFile, JSON.stringify({
    orderId: secrets.order,
    guest: { email: secrets.email, firstName: "PrivateName" },
    payment: { token: secrets.token, amount: 12345.67, paid: false },
    hotel: { name: secrets.hotel },
    items: [{ price: 10, label: "first" }, { price: null, cancellation: true }],
    byId: { "550e8400-e29b-41d4-a716-446655440000": { status: "confirmed" } },
    byShortId: { "abc1234def5678": { status: "confirmed" } },
  }));
  const output = execute([
    "inspect-booking-fixture", "--input", fixtureFile, "--output", reportFile,
  ]);
  const report = JSON.parse(readFileSync(reportFile, "utf8"));
  assert.equal(JSON.parse(output).sourceValuesIncluded, false);
  assert.equal(report.providerRequestsPerformed, false);
  assert.equal(report.sourceIdentifiersIncluded, false);
  assert.equal(report.shape.properties.orderId.type, "string");
  assert.equal(report.shape.properties.payment.properties.amount.type, "number");
  assert.deepEqual(report.shape.properties.items.items.observedInEveryObject, ["price"]);
  assert.ok(report.shape.properties.byId.properties["<dynamic-key>"]);
  assert.ok(report.shape.properties.byShortId.properties["<dynamic-key>"]);
  assert.equal(report.limitations.dynamicKeyMaskingIsHeuristic, true);
  const serialized = JSON.stringify(report);
  for (const value of Object.values(secrets)) assert.doesNotMatch(serialized, new RegExp(value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")));
  assert.equal(statSync(reportFile).mode & 0o077, 0);
});

test("booking fixture inspection rejects unsafe input and output", () => {
  const directory = mkdtempSync(resolve(tmpdir(), "tbank-booking-fixture-invalid-"));
  const fixtureFile = resolve(directory, "booking.json");
  writeFileSync(fixtureFile, "{}\n");
  assert.throws(
    () => execute(["inspect-booking-fixture", "--input", "relative.json"]),
    /--input must be an absolute path/,
  );
  assert.throws(
    () => execute(["inspect-booking-fixture", "--input", fixtureFile, "--output", fixtureFile]),
    /must not overwrite the source fixture/,
  );
  const hardLink = resolve(directory, "hard-link.json");
  linkSync(fixtureFile, hardLink);
  assert.throws(
    () => execute(["inspect-booking-fixture", "--input", fixtureFile, "--output", hardLink]),
    /must not overwrite the source fixture/,
  );
  const symbolicLink = resolve(directory, "symbolic-link.json");
  symlinkSync(resolve(directory, "report.json"), symbolicLink);
  assert.throws(
    () => execute(["inspect-booking-fixture", "--input", fixtureFile, "--output", symbolicLink]),
    /must not be a symbolic link/,
  );
});

test("own booking capture keeps provider values out of the structure-only report", async () => {
  const calls = [];
  const secrets = ["443021782873", "Private Hotel", "person@example.test", "secret-payment-token", "19999.25"];
  const report = await captureOwnBookingStructure({
    category: "active",
    brokerCall: async (method, params) => {
      calls.push({ method, params });
      if (method === "hotels.list_bookings") return {
        bookings: {
          activeList: [{ orderId: secrets[0], hotelName: secrets[1], totalAmount: 19999.25 }],
          cancelledList: [],
          completedList: [],
        },
      };
      return {
        booking: {
          orderId: secrets[0],
          contact: { email: secrets[2] },
          payment: { token: secrets[3], amount: 19999.25, currency: "RUB", status: "created" },
        },
      };
    },
  });
  assert.deepEqual(calls.map(({ method }) => method), ["hotels.list_bookings", "hotels.get_booking_v1"]);
  assert.equal(calls[1].params.bookingId, secrets[0]);
  assert.equal(report.providerRequestsPerformed, true);
  assert.equal(report.rawPayloadPersisted, false);
  assert.equal(report.shapes.bookingDetails.properties.payment.properties.amount.type, "number");
  const serialized = JSON.stringify(report);
  for (const value of secrets) assert.doesNotMatch(serialized, new RegExp(value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")));
});

test("own booking capture validates category and requires an available booking", async () => {
  await assert.rejects(
    captureOwnBookingStructure({ category: "unknown", brokerCall: async () => ({}) }),
    /--category must be active, completed, or cancelled/,
  );
  await assert.rejects(
    captureOwnBookingStructure({
      category: "completed",
      brokerCall: async () => ({ bookings: { activeList: [], cancelledList: [], completedList: [] } }),
    }),
    /No own completed booking is available/,
  );
});

test("live booking capture requires explicit acknowledgement before broker access", () => {
  assert.throws(
    () => execute(["capture-booking-shape", "--output", resolve(tmpdir(), "shape.json")]),
    /requires --acknowledge-read-own-data/,
  );
});

test("payment readiness is offline, explicit and fail-closed", () => {
  const output = execute(["payment-readiness"]);
  const report = JSON.parse(output);
  assert.equal(report.providerRequestsPerformed, false);
  assert.equal(report.readyForPaymentSetup, false);
  assert.equal(report.readyForPaymentExecution, false);
  assert.equal(report.unknownOutcomePolicy, "do_not_retry_automatically");
  assert.equal(report.reconciliationStatus, "not_configured");
  assert.ok(report.completedGates.includes("decimal_safe_amount"));
  assert.ok(report.blockers.some(({ id }) => id === "provider_idempotency"));
  assert.ok(report.forbiddenUntilReady.includes("banking_v1_pay_as_hotel_payment"));
  assert.doesNotMatch(output, /token|private.?key|authorization/i);
});

test("combined launcher owns broker lifecycle without provider requests", async (t) => {
  const directory = mkdtempSync(resolve(tmpdir(), "tbank-mcp-broker-owner-"));
  const sessionFile = resolve(directory, "session.json");
  const socketFile = resolve(directory, "auth.sock");
  const configFile = resolve(directory, "config.json");
  chmodSync(directory, 0o700);
  writeFileSync(sessionFile, "{}\n", { mode: 0o600 });
  writeFileSync(configFile, JSON.stringify({ version: 1, banking: { sessionFile, brokerSocket: socketFile } }), { mode: 0o600 });
  let output;
  try {
    output = execFileSync(process.execPath, [cli, "run", "banking", "--with-broker"], {
      encoding: "utf8",
      input: `${JSON.stringify({ jsonrpc: "2.0", id: 1, method: "initialize", params: { protocolVersion: "2025-03-26", capabilities: {}, clientInfo: { name: "launcher-test", version: "1" } } })}\n`,
      env: { PATH: process.env.PATH, HOME: process.env.HOME, TBANK_MCP_LOCAL_CONFIG: configFile },
    });
  } catch (error) {
    if (/did not become ready|operation not permitted|not supported/i.test(String(error.stderr ?? error.message))) {
      t.skip("Unix sockets are blocked by the current sandbox");
      return;
    }
    throw error;
  }
  assert.match(output, /tbank-banking-mcp/);
  for (let attempt = 0; attempt < 20 && existsSync(socketFile); attempt += 1) {
    await new Promise((resolvePromise) => setTimeout(resolvePromise, 25));
  }
  assert.equal(existsSync(socketFile), false);
});
