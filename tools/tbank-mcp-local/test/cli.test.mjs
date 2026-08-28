import assert from "node:assert/strict";
import { chmodSync, existsSync, linkSync, mkdirSync, mkdtempSync, readFileSync, statSync, symlinkSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { resolve } from "node:path";
import { execFileSync, spawn } from "node:child_process";
import test from "node:test";
import { resolveRuntimeCommand, runtimeEnvironment } from "../src/cli.mjs";
import { captureOwnBookingStructure } from "../src/booking-shape-capture.mjs";
import { clientRegistrationCommands, managedRuntimePaths, publicPackageVersions } from "../src/connect.mjs";

const cli = new URL("../src/cli.mjs", import.meta.url).pathname;
const hotelsPackageVersion = JSON.parse(readFileSync(new URL("../../tbank-hotels-mcp/package.json", import.meta.url), "utf8")).version;

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

test("setup supports anonymous Hotels search without a JWT key", () => {
  const directory = mkdtempSync(resolve(tmpdir(), "tbank-mcp-anonymous-"));
  const configFile = resolve(directory, "config.json");
  execute([
    "setup", "--profile", "hotels", "--config", configFile,
    "--hotels-api-base-url", "https://hotels.example.test/",
  ]);
  const config = JSON.parse(readFileSync(configFile, "utf8"));
  assert.equal(config.hotels.apiBaseUrl, "https://hotels.example.test/");
  assert.equal(config.hotels.jwtPrivateKeyFile, undefined);
  assert.equal(statSync(configFile).mode & 0o077, 0);

  const doctor = JSON.parse(execute(["doctor", "--profile", "hotels"], {
    TBANK_MCP_LOCAL_CONFIG: configFile,
  }));
  assert.equal(doctor.ready, true);
  assert.equal(doctor.checks.hotels.authentication, "not_required");
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
  const output = execute(["doctor", "--profile", "combined"], {
    TBANK_MCP_LOCAL_CONFIG: configFile,
    TBANK_HOTELS_JWT_PRIVATE_KEY: "stale-inline-private-material",
    TBANK_HOTELS_ENABLE_MUTATIONS: "true",
    TBANK_HOTELS_MUTATION_EXECUTION_PROFILE: "non_production_v1_reviewed",
  });
  const report = JSON.parse(output);
  assert.equal(report.providerRequestsPerformed, false);
  assert.equal(report.secretsExposed, false);
  assert.equal(report.checks.hotels.authentication, "ready");
  assert.equal(report.checks.hotels.configurationSource, "local_config");
  assert.equal(report.checks.banking.session, "ready");
  assert.doesNotMatch(output, /private-material|session-material/);
});

test("generates secret-free client registration for all supported local clients", () => {
  for (const client of ["opencode", "codex", "claude"]) {
    const output = execute(["client-config", "--client", client, "--profile", "combined"]);
    assert.match(output, /tbank-hotels/);
    assert.match(output, /tbank-banking/);
    assert.match(output, /run/);
    assert.match(output, /ensure-broker/);
    assert.doesNotMatch(output, /PRIVATE_KEY|AUTH_TOKEN|session\.json/);
  }
});

test("resolves installed runtime commands without a repository checkout", () => {
  const directory = mkdtempSync(resolve(tmpdir(), "tbank-runtime-command-"));
  const installedLogin = resolve(directory, "tbank-banking-login");
  const explicitBanking = resolve(directory, "custom-banking");
  writeFileSync(installedLogin, "#!/bin/sh\nexit 0\n", { mode: 0o700 });
  writeFileSync(explicitBanking, "#!/bin/sh\nexit 0\n", { mode: 0o700 });

  const login = resolveRuntimeCommand("login", { PATH: directory }, { includeDevelopmentFallback: false });
  assert.deepEqual(login, { command: installedLogin, args: [], source: "installed_path" });
  const banking = resolveRuntimeCommand("banking", {
    PATH: "",
    TBANK_MCP_BANKING_EXECUTABLE: explicitBanking,
  });
  assert.deepEqual(banking, { command: explicitBanking, args: [], source: "explicit_environment" });
  assert.throws(
    () => resolveRuntimeCommand("broker", { PATH: "", TBANK_MCP_BROKER_EXECUTABLE: "relative-broker" }),
    /absolute executable file/,
  );

  const managedHotels = resolve(directory, "managed-hotels");
  writeFileSync(managedHotels, "#!/bin/sh\nexit 0\n", { mode: 0o700 });
  assert.deepEqual(
    resolveRuntimeCommand("hotels", { PATH: "" }, {
      includeDevelopmentFallback: false,
      config: { runtimeExecutables: { hotels: managedHotels } },
    }),
    { command: managedHotels, args: [], source: "managed_config" },
  );
});

test("builds explicit OpenCode and Codex registrations for both MCP servers", () => {
  const openCode = clientRegistrationCommands("opencode", "/bin/opencode", "/managed/tbank-mcp-local");
  assert.deepEqual(openCode.map(({ args }) => args), [
    ["mcp", "add", "tbank-hotels", "--", "/managed/tbank-mcp-local", "run", "hotels", "--ensure-broker"],
    ["mcp", "add", "tbank-banking", "--", "/managed/tbank-mcp-local", "run", "banking", "--ensure-broker"],
  ]);
  const codex = clientRegistrationCommands("codex", "/bin/codex", "/managed/tbank-mcp-local");
  assert.deepEqual(codex.map(({ args }) => args.slice(0, 4)), [
    ["mcp", "remove", "tbank-hotels"],
    ["mcp", "add", "tbank-hotels", "--"],
    ["mcp", "remove", "tbank-banking"],
    ["mcp", "add", "tbank-banking", "--"],
  ]);
  assert.deepEqual(
    clientRegistrationCommands("opencode", "/bin/opencode", "/managed/tbank-mcp-local", "hotels")[0].args,
    ["mcp", "add", "tbank-hotels", "--", "/managed/tbank-mcp-local", "run", "hotels"],
  );
  assert.deepEqual(clientRegistrationCommands("cursor", null, "/managed/tbank-mcp-local"), []);
});

test("connect writes a global Cursor stdio config and preserves unrelated servers", () => {
  const directory = mkdtempSync(resolve(tmpdir(), "tbank-connect-cursor-"));
  const runtime = resolve(directory, "runtime");
  const configFile = resolve(directory, "config.json");
  const cursorConfig = resolve(directory, ".cursor/mcp.json");
  const paths = managedRuntimePaths(runtime);
  for (const executable of [paths.toolkit, paths.hotels, paths.banking, paths.broker, paths.login]) {
    mkdirSync(resolve(executable, ".."), { recursive: true, mode: 0o700 });
    writeFileSync(executable, "#!/bin/sh\nexit 0\n", { mode: 0o700 });
  }
  mkdirSync(resolve(cursorConfig, ".."), { recursive: true, mode: 0o700 });
  writeFileSync(cursorConfig, JSON.stringify({
    mcpServers: { existing: { type: "stdio", command: "/existing/server" } },
  }), { mode: 0o600 });

  const output = execute([
    "connect", "cursor", "--profile", "combined",
    "--runtime-dir", runtime, "--config", configFile,
    "--cursor-config", cursorConfig, "--skip-install", "--skip-login",
  ], { HOME: directory, PATH: "" });
  const report = JSON.parse(output);
  const cursor = JSON.parse(readFileSync(cursorConfig, "utf8"));
  assert.equal(report.client, "cursor");
  assert.deepEqual(report.registeredComponents, ["hotels", "banking"]);
  assert.equal(cursor.mcpServers.existing.command, "/existing/server");
  assert.deepEqual(cursor.mcpServers["tbank-hotels"], {
    type: "stdio",
    command: paths.toolkit,
    args: ["run", "hotels", "--ensure-broker"],
  });
  assert.deepEqual(cursor.mcpServers["tbank-banking"], {
    type: "stdio",
    command: paths.toolkit,
    args: ["run", "banking", "--ensure-broker"],
  });
  assert.equal(statSync(cursorConfig).mode & 0o077, 0);
  assert.doesNotMatch(readFileSync(cursorConfig, "utf8") + output, /token|password|private.?key|authorization/i);
});

test("connect registers a secret-free managed combined runtime without network or login", () => {
  const directory = mkdtempSync(resolve(tmpdir(), "tbank-connect-"));
  const runtime = resolve(directory, "runtime");
  const configFile = resolve(directory, "config.json");
  const clientLog = resolve(directory, "client.log");
  const client = resolve(directory, "opencode");
  const paths = managedRuntimePaths(runtime);
  for (const executable of [paths.toolkit, paths.hotels, paths.banking, paths.broker, paths.login]) {
    mkdirSync(resolve(executable, ".."), { recursive: true, mode: 0o700 });
    writeFileSync(executable, "#!/bin/sh\nexit 0\n", { mode: 0o700 });
  }
  writeFileSync(client, "#!/bin/sh\nprintf '%s\\n' \"$*\" >> \"$TEST_CLIENT_LOG\"\n", { mode: 0o700 });
  const output = execute([
    "connect", "--client", "opencode", "--profile", "combined",
    "--runtime-dir", runtime, "--config", configFile,
    "--client-executable", client, "--skip-install", "--skip-login",
  ], { HOME: directory, TEST_CLIENT_LOG: clientLog });
  const report = JSON.parse(output);
  const configText = readFileSync(configFile, "utf8");
  const config = JSON.parse(configText);
  assert.deepEqual(report.registeredComponents, ["hotels", "banking"]);
  assert.equal(report.configContainsCredentials, false);
  assert.equal(report.providerRequestsPerformedByInstaller, false);
  assert.equal(report.mobileLoginCompleted, false);
  assert.equal(config.hotels.apiBaseUrl, "https://hotels.tbank.ru/api");
  assert.equal(config.runtimeExecutables.hotels, paths.hotels);
  assert.equal(config.runtimeExecutables.banking, paths.banking);
  assert.equal(config.runtimeExecutables.broker, paths.broker);
  assert.equal(config.runtimeExecutables.login, paths.login);
  assert.equal(statSync(configFile).mode & 0o077, 0);
  assert.equal(statSync(runtime).mode & 0o077, 0);
  assert.deepEqual(readFileSync(clientLog, "utf8").trim().split("\n"), [
    `mcp add tbank-hotels -- ${paths.toolkit} run hotels --ensure-broker`,
    `mcp add tbank-banking -- ${paths.toolkit} run banking --ensure-broker`,
  ]);
  assert.doesNotMatch(configText + output, /token|password|private.?key|authorization/i);
  assert.deepEqual(publicPackageVersions, { hotels: "0.29.0", banking: "0.17.0", toolkit: "0.15.0" });
});

test("connect binds terminal login to the managed session path", () => {
  const directory = mkdtempSync(resolve(tmpdir(), "tbank-connect-login-"));
  const runtime = resolve(directory, "runtime");
  const configFile = resolve(directory, "config.json");
  const client = resolve(directory, "opencode");
  const loginLog = resolve(directory, "login.log");
  const paths = managedRuntimePaths(runtime);
  for (const executable of [paths.toolkit, paths.hotels, paths.banking, paths.broker]) {
    mkdirSync(resolve(executable, ".."), { recursive: true, mode: 0o700 });
    writeFileSync(executable, "#!/bin/sh\nexit 0\n", { mode: 0o700 });
  }
  writeFileSync(paths.login, "#!/bin/sh\nprintf '%s\\n%s\\n' \"$TBANK_BANKING_SESSION\" \"$TBANK_AUTH_BROKER_SOCKET\" > \"$TEST_LOGIN_LOG\"\n", { mode: 0o700 });
  writeFileSync(client, "#!/bin/sh\nexit 0\n", { mode: 0o700 });
  const output = execute([
    "connect", "--client", "opencode", "--profile", "combined",
    "--runtime-dir", runtime, "--config", configFile,
    "--client-executable", client, "--skip-install",
  ], {
    HOME: directory,
    TEST_LOGIN_LOG: loginLog,
    TBANK_BANKING_SESSION: "/must/not/be/used/session.json",
    TBANK_AUTH_BROKER_SOCKET: "/must/not/be/used/auth.sock",
  });
  const report = JSON.parse(output);
  const config = JSON.parse(readFileSync(configFile, "utf8"));
  assert.equal(report.mobileLoginCompleted, true);
  assert.equal(report.mobileLoginMayContactProvider, true);
  assert.deepEqual(readFileSync(loginLog, "utf8").trim().split("\n"), [
    config.banking.sessionFile,
    config.banking.brokerSocket,
  ]);
  assert.doesNotMatch(readFileSync(loginLog, "utf8"), /must\/not\/be\/used/);
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

test("configured Hotels profile preserves an explicit transport URL but ignores conflicting parent auth and mutation variables", () => {
  const names = [
    "TBANK_HOTELS_API_BASE_URL",
    "TBANK_HOTELS_AUTH_TOKEN",
    "TBANK_HOTELS_AUTH_HEADERS_JSON",
    "TBANK_HOTELS_JWT_PRIVATE_KEY",
    "TBANK_HOTELS_JWT_PRIVATE_KEY_FILE",
    "TBANK_HOTELS_JWT_ISSUER",
    "TBANK_HOTELS_JWT_AUDIENCE",
    "TBANK_HOTELS_ENABLE_MUTATIONS",
    "TBANK_HOTELS_MUTATION_EXECUTION_PROFILE",
  ];
  const previous = Object.fromEntries(names.map((name) => [name, process.env[name]]));
  Object.assign(process.env, {
    TBANK_HOTELS_API_BASE_URL: "https://runtime.example.test/",
    TBANK_HOTELS_AUTH_TOKEN: "stale-token",
    TBANK_HOTELS_AUTH_HEADERS_JSON: "{\"Authorization\":\"stale\"}",
    TBANK_HOTELS_JWT_PRIVATE_KEY: "stale-inline-key",
    TBANK_HOTELS_JWT_PRIVATE_KEY_FILE: "/private/tmp/stale-key.pem",
    TBANK_HOTELS_JWT_ISSUER: "stale-issuer",
    TBANK_HOTELS_JWT_AUDIENCE: "stale-audience",
    TBANK_HOTELS_ENABLE_MUTATIONS: "true",
    TBANK_HOTELS_MUTATION_EXECUTION_PROFILE: "non_production_v1_reviewed",
  });
  try {
    const config = {
      hotels: {
        apiBaseUrl: "https://hotels.example.test/",
        jwtIssuer: "issuer",
        jwtAudience: "audience",
        jwtPrivateKeyFile: "/private/tmp/service-key.pem",
        maxConcurrentRequests: 2,
      },
    };
    const hotels = runtimeEnvironment("hotels", config);
    assert.equal(hotels.TBANK_HOTELS_API_BASE_URL, "https://runtime.example.test/");
    assert.equal(hotels.TBANK_HOTELS_JWT_PRIVATE_KEY_FILE, config.hotels.jwtPrivateKeyFile);
    assert.equal(hotels.TBANK_HOTELS_JWT_ISSUER, config.hotels.jwtIssuer);
    assert.equal(hotels.TBANK_HOTELS_JWT_AUDIENCE, config.hotels.jwtAudience);
    assert.equal(hotels.TBANK_HOTELS_AUTH_TOKEN, undefined);
    assert.equal(hotels.TBANK_HOTELS_AUTH_HEADERS_JSON, undefined);
    assert.equal(hotels.TBANK_HOTELS_JWT_PRIVATE_KEY, undefined);
    assert.equal(hotels.TBANK_HOTELS_ENABLE_MUTATIONS, undefined);
    assert.equal(hotels.TBANK_HOTELS_MUTATION_EXECUTION_PROFILE, undefined);
  } finally {
    for (const [name, value] of Object.entries(previous)) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  }
});

test("Hotels npm artifact contains only runtime files and public README", () => {
  const directory = mkdtempSync(resolve(tmpdir(), "tbank-hotels-pack-"));
  const project = new URL("../../tbank-hotels-mcp", import.meta.url).pathname;
  const output = execFileSync("npm", ["pack", "--dry-run", "--json", "--cache", resolve(directory, "npm-cache")], {
    cwd: project,
    encoding: "utf8",
    env: { PATH: process.env.PATH, HOME: process.env.HOME },
  });
  const report = JSON.parse(output)[0];
  assert.equal(report.version, hotelsPackageVersion);
  const paths = report.files.map(({ path }) => path).sort();
  assert.deepEqual(paths, [
    "README.md",
    "package.json",
    "src/checkout-handoff.mjs",
    "src/config.mjs",
    "src/runtime.mjs",
    "src/server.mjs",
    "src/stdio-server.mjs",
    "src/tool-contracts.mjs",
  ]);
  assert.equal(paths.some((path) => /message|test|\.env|private|session/i.test(path)), false);
});

test("Hotels npm artifact installs and initializes outside the repository checkout", () => {
  const directory = mkdtempSync(resolve(tmpdir(), "tbank-hotels-install-"));
  const project = new URL("../../tbank-hotels-mcp", import.meta.url).pathname;
  const cache = resolve(directory, "npm-cache");
  const packed = JSON.parse(execFileSync("npm", ["pack", "--json", "--pack-destination", directory, "--cache", cache], {
    cwd: project,
    encoding: "utf8",
    env: { PATH: process.env.PATH, HOME: process.env.HOME },
  }))[0];
  const archive = resolve(directory, packed.filename);
  const installation = resolve(directory, "installation");
  execFileSync("npm", ["install", "--ignore-scripts", "--no-audit", "--no-fund", "--cache", cache, "--prefix", installation, archive], {
    cwd: directory,
    encoding: "utf8",
    env: { PATH: process.env.PATH, HOME: process.env.HOME },
  });
  const executable = resolve(installation, "node_modules/.bin/tbank-hotels-mcp");
  const output = execFileSync(executable, [], {
    cwd: directory,
    encoding: "utf8",
    input: `${JSON.stringify({ jsonrpc: "2.0", id: 1, method: "initialize", params: {} })}\n`,
    env: { PATH: process.env.PATH, HOME: process.env.HOME, NODE_ENV: "test" },
  });
  assert.equal(JSON.parse(output.trim()).result.serverInfo.version, hotelsPackageVersion);
});

test("toolkit npm artifact contains only portable runtime and installs outside checkout", () => {
  const directory = mkdtempSync(resolve(tmpdir(), "tbank-toolkit-install-"));
  const project = new URL("..", import.meta.url).pathname;
  const cache = resolve(directory, "npm-cache");
  const packed = JSON.parse(execFileSync("npm", ["pack", "--json", "--pack-destination", directory, "--cache", cache], {
    cwd: project,
    encoding: "utf8",
    env: { PATH: process.env.PATH, HOME: process.env.HOME },
  }))[0];
  const paths = packed.files.map(({ path }) => path).sort();
  assert.deepEqual(paths, [
    "README.md",
    "contracts/banking-tools.json",
    "contracts/hotels-tools.json",
    "package.json",
    "src/booking-shape-capture.mjs",
    "src/cli.mjs",
    "src/connect.mjs",
    "src/fixture-shape.mjs",
    "src/payment-readiness.mjs",
  ]);
  assert.equal(paths.some((path) => /test|qwen|private|session|\.env/i.test(path)), false);

  const installation = resolve(directory, "installation");
  execFileSync("npm", ["install", "--ignore-scripts", "--no-audit", "--no-fund", "--cache", cache, "--prefix", installation, resolve(directory, packed.filename)], {
    cwd: directory,
    encoding: "utf8",
    env: { PATH: process.env.PATH, HOME: process.env.HOME },
  });
  const executable = resolve(installation, "node_modules/.bin/tbank-mcp-local");
  const output = execFileSync(executable, ["client-config", "--client", "opencode", "--profile", "hotels"], {
    cwd: directory,
    encoding: "utf8",
    env: { PATH: process.env.PATH, HOME: process.env.HOME },
  });
  const config = JSON.parse(output);
  assert.equal(config.mcp["tbank-hotels"].type, "local");
  assert.match(config.mcp["tbank-hotels"].command.join(" "), /tbank-mcp-local.*run.*hotels/);
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

test("combined launchers ensure one persistent broker in either lazy-start order", async (t) => {
  const directory = mkdtempSync(resolve(tmpdir(), "tbank-mcp-broker-owner-"));
  const sessionFile = resolve(directory, "session.json");
  const socketFile = resolve(directory, "auth.sock");
  const configFile = resolve(directory, "config.json");
  chmodSync(directory, 0o700);
  writeFileSync(sessionFile, "{}\n", { mode: 0o600 });
  writeFileSync(configFile, JSON.stringify({ version: 1, banking: { sessionFile, brokerSocket: socketFile } }), { mode: 0o600 });
  const launch = (component, brokerArgument = "--ensure-broker") => execFileSync(process.execPath, [cli, "run", component, ...(brokerArgument ? [brokerArgument] : [])], {
    encoding: "utf8",
    input: `${JSON.stringify({ jsonrpc: "2.0", id: 1, method: "initialize", params: { protocolVersion: "2025-03-26", capabilities: {}, clientInfo: { name: "launcher-test", version: "1" } } })}\n`,
    env: { PATH: process.env.PATH, HOME: process.env.HOME, TBANK_MCP_LOCAL_CONFIG: configFile },
  });
  const launchAsync = (component) => new Promise((resolvePromise, rejectPromise) => {
    const child = spawn(process.execPath, [cli, "run", component, "--ensure-broker"], {
      env: { PATH: process.env.PATH, HOME: process.env.HOME, TBANK_MCP_LOCAL_CONFIG: configFile },
      stdio: ["pipe", "pipe", "pipe"],
    });
    let stdout = "";
    let stderr = "";
    child.stdout.setEncoding("utf8");
    child.stderr.setEncoding("utf8");
    child.stdout.on("data", (chunk) => { stdout += chunk; });
    child.stderr.on("data", (chunk) => { stderr += chunk; });
    child.on("error", rejectPromise);
    child.on("exit", (code) => code === 0
      ? resolvePromise(stdout)
      : rejectPromise(new Error(stderr || `${component} launcher exited with code ${code}.`)));
    child.stdin.end(`${JSON.stringify({ jsonrpc: "2.0", id: 1, method: "initialize", params: { protocolVersion: "2025-03-26", capabilities: {}, clientInfo: { name: "launcher-test", version: "1" } } })}\n`);
  });
  const stop = () => execute(["stop-broker"], { TBANK_MCP_LOCAL_CONFIG: configFile });
  try {
    const hotelsFirst = launch("hotels");
    assert.match(hotelsFirst, /tbank-hotels-api-mcp/);
    assert.equal(existsSync(socketFile), true);
    const bankingSecond = launch("banking");
    assert.match(bankingSecond, /tbank-banking-mcp/);
    assert.equal(existsSync(socketFile), true);
    assert.equal(JSON.parse(stop()).status, "stopped");
    assert.equal(existsSync(socketFile), false);

    const bankingFirst = launch("banking");
    assert.match(bankingFirst, /tbank-banking-mcp/);
    assert.equal(existsSync(socketFile), true);
    const hotelsSecond = launch("hotels");
    assert.match(hotelsSecond, /tbank-hotels-api-mcp/);
    assert.equal(existsSync(socketFile), true);
    assert.equal(JSON.parse(stop()).status, "stopped");

    const legacyHotelsConfig = launch("hotels", "");
    assert.match(legacyHotelsConfig, /tbank-hotels-api-mcp/);
    assert.equal(existsSync(socketFile), true);
    assert.equal(JSON.parse(stop()).status, "stopped");

    const legacyWithBroker = launch("banking", "--with-broker");
    assert.match(legacyWithBroker, /tbank-banking-mcp/);
    assert.equal(existsSync(socketFile), true);
    assert.equal(JSON.parse(stop()).status, "stopped");

    const [hotelsConcurrent, bankingConcurrent] = await Promise.all([launchAsync("hotels"), launchAsync("banking")]);
    assert.match(hotelsConcurrent, /tbank-hotels-api-mcp/);
    assert.match(bankingConcurrent, /tbank-banking-mcp/);
    assert.equal(existsSync(socketFile), true);
    assert.equal(JSON.parse(stop()).status, "stopped");
  } catch (error) {
    if (/did not become ready|operation not permitted|not supported/i.test(String(error.stderr ?? error.message))) {
      t.skip("Unix sockets are blocked by the current sandbox");
      return;
    }
    throw error;
  } finally {
    try { stop(); } catch {}
  }
  assert.equal(existsSync(socketFile), false);
});
