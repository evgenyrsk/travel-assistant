#!/usr/bin/env node

import { chmodSync, existsSync, mkdirSync, readFileSync, statSync, writeFileSync } from "node:fs";
import { homedir } from "node:os";
import { dirname, isAbsolute, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { spawn } from "node:child_process";
import { createConnection } from "node:net";
import { inspectBookingFixture } from "./fixture-shape.mjs";
import { writeStructureOnlyReport } from "./fixture-shape.mjs";
import { captureOwnBookingStructure } from "./booking-shape-capture.mjs";
import { paymentReadinessReport } from "./payment-readiness.mjs";

const localRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const repositoryRoot = resolve(localRoot, "../..");
const hotelsServer = resolve(repositoryRoot, "tools/tbank-hotels-mcp/src/server.mjs");
const bankingRoot = resolve(repositoryRoot, "tools/tbank-banking-mcp");
const bankingExecutable = resolve(bankingRoot, ".venv/bin/tbank-banking-mcp");
const brokerExecutable = resolve(bankingRoot, ".venv/bin/tbank-auth-broker");
const contractsDirectory = resolve(localRoot, "contracts");

function defaultConfigPath() {
  return process.env.TBANK_MCP_LOCAL_CONFIG || resolve(homedir(), ".config/tbank-mcp/config.json");
}

function argumentValue(args, name) {
  const index = args.indexOf(name);
  return index < 0 ? undefined : args[index + 1];
}

function profileComponents(profile) {
  if (profile === "hotels") return ["hotels"];
  if (profile === "banking") return ["banking"];
  if (profile === "combined") return ["hotels", "banking"];
  throw new Error("profile must be hotels, banking, or combined.");
}

function readConfig(path = defaultConfigPath()) {
  if (!existsSync(path)) return {};
  let parsed;
  try { parsed = JSON.parse(readFileSync(path, "utf8")); } catch { throw new Error("Local MCP config must contain valid JSON."); }
  if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) throw new Error("Local MCP config must contain a JSON object.");
  return parsed;
}

function absoluteExistingFile(path, name) {
  if (!path || !isAbsolute(path)) throw new Error(`${name} must be an absolute path.`);
  if (!existsSync(path) || !statSync(path).isFile()) throw new Error(`${name} does not point to a readable file.`);
  return path;
}

function setup(args) {
  const profile = argumentValue(args, "--profile") ?? "combined";
  const components = profileComponents(profile);
  const configPath = resolve(argumentValue(args, "--config") ?? defaultConfigPath());
  const config = { version: 1 };
  if (components.includes("hotels")) {
    const apiBaseUrl = argumentValue(args, "--hotels-api-base-url");
    if (!apiBaseUrl || !/^https:\/\//.test(apiBaseUrl)) throw new Error("--hotels-api-base-url must be an approved HTTPS URL.");
    const jwtPrivateKeyFile = absoluteExistingFile(argumentValue(args, "--hotels-jwt-key-file"), "--hotels-jwt-key-file");
    config.hotels = {
      apiBaseUrl,
      jwtIssuer: argumentValue(args, "--hotels-jwt-issuer") ?? "HOTELSSEARCHAPI",
      jwtAudience: argumentValue(args, "--hotels-jwt-audience") ?? "HOTELSAPI",
      jwtPrivateKeyFile,
      maxConcurrentRequests: 2,
    };
  }
  if (components.includes("banking")) {
    config.banking = {
      sessionFile: resolve(argumentValue(args, "--banking-session") ?? resolve(homedir(), ".local/share/tbank-banking-mcp/session.json")),
      brokerSocket: resolve(argumentValue(args, "--broker-socket") ?? resolve(homedir(), ".local/share/tbank-auth-broker/auth.sock")),
    };
  }
  mkdirSync(dirname(configPath), { recursive: true, mode: 0o700 });
  writeFileSync(configPath, `${JSON.stringify(config, null, 2)}\n`, { mode: 0o600 });
  chmodSync(configPath, 0o600);
  process.stdout.write(`Local MCP config created at ${configPath}. It contains paths and non-secret JWT metadata, not private key material or mobile tokens.\n`);
}

export function runtimeEnvironment(component, config) {
  const env = Object.fromEntries(Object.entries(process.env).filter(([name]) => !name.startsWith("TBANK_")));
  const inheritedPrefixes = component === "hotels"
    ? ["TBANK_HOTELS_", "TBANK_AUTH_BROKER_"]
    : component === "broker"
      ? ["TBANK_BANKING_SESSION", "TBANK_AUTH_BROKER_", "TBANK_HOTELS_VOUCHER_"]
      : ["TBANK_BANKING_", "TBANK_AUTH_BROKER_"];
  for (const [name, value] of Object.entries(process.env)) {
    if (inheritedPrefixes.some((prefix) => name.startsWith(prefix))) env[name] = value;
  }
  if (component === "hotels" && config.hotels) {
    env.TBANK_HOTELS_API_BASE_URL ??= config.hotels.apiBaseUrl;
    env.TBANK_HOTELS_JWT_ISSUER ??= config.hotels.jwtIssuer;
    env.TBANK_HOTELS_JWT_AUDIENCE ??= config.hotels.jwtAudience;
    env.TBANK_HOTELS_JWT_PRIVATE_KEY_FILE ??= config.hotels.jwtPrivateKeyFile;
    env.TBANK_HOTELS_MAX_CONCURRENT_REQUESTS ??= String(config.hotels.maxConcurrentRequests ?? 2);
  }
  if (config.banking && component !== "hotels") {
    env.TBANK_BANKING_SESSION ??= config.banking.sessionFile;
  }
  if (config.banking) {
    env.TBANK_AUTH_BROKER_SOCKET ??= config.banking.brokerSocket;
  }
  return env;
}

async function runInteractiveLogin(logout = false) {
  const config = readConfig();
  if (logout) await stopBroker(config);
  const python = resolve(bankingRoot, ".venv/bin/python");
  const loginScript = resolve(bankingRoot, "login_cli.py");
  if (!existsSync(python) || !existsSync(loginScript)) {
    throw new Error("Banking local environment is missing. Run the documented installation first.");
  }
  const args = logout ? [loginScript, "--logout"] : [loginScript];
  const child = spawn(python, args, {
    cwd: bankingRoot,
    stdio: "inherit",
    env: runtimeEnvironment("banking", config),
  });
  const [code, signal] = await new Promise((resolvePromise) => child.on("exit", (...result) => resolvePromise(result)));
  if (signal) process.kill(process.pid, signal);
  process.exitCode = code ?? 1;
}

function brokerRequest(socketPath, client, method, params = {}, timeoutMs = 300) {
  return new Promise((resolvePromise) => {
    if (!socketPath) return resolvePromise(null);
    const connection = createConnection(socketPath);
    let settled = false;
    let response = "";
    const finish = (result) => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      connection.destroy();
      resolvePromise(result);
    };
    const timer = setTimeout(() => finish(null), timeoutMs);
    connection.setEncoding("utf8");
    connection.on("connect", () => connection.write(`${JSON.stringify({ version: 2, client, method, params })}\n`));
    connection.on("data", (chunk) => {
      response += chunk;
      if (!response.includes("\n")) return;
      try {
        const parsed = JSON.parse(response.split("\n", 1)[0]);
        finish(parsed.ok === true ? parsed.result : null);
      } catch { finish(null); }
    });
    connection.on("error", () => finish(null));
    connection.on("end", () => finish(null));
  });
}

async function brokerStatus(socketPath, timeoutMs = 300) {
  return (await brokerRequest(socketPath, "lifecycle", "status", {}, timeoutMs)) !== null;
}

function scopedBrokerRequest(socketPath, method, params = {}, timeoutMs = 45_000) {
  return new Promise((resolvePromise, rejectPromise) => {
    if (!socketPath) return rejectPromise(new Error("Combined mobile auth is not configured."));
    const connection = createConnection(socketPath);
    let settled = false;
    let response = "";
    const finish = (callback, value) => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      connection.destroy();
      callback(value);
    };
    const timer = setTimeout(() => finish(rejectPromise, new Error("Auth broker operation timed out.")), timeoutMs);
    connection.setEncoding("utf8");
    connection.on("connect", () => connection.write(`${JSON.stringify({ version: 2, client: "hotels", method, params })}\n`));
    connection.on("data", (chunk) => {
      response += chunk;
      if (Buffer.byteLength(response) > 2 * 1024 * 1024) {
        finish(rejectPromise, new Error("Auth broker response exceeded the local safety limit."));
        return;
      }
      const newline = response.indexOf("\n");
      if (newline < 0) return;
      try {
        const parsed = JSON.parse(response.slice(0, newline));
        if (!parsed || parsed.ok !== true || !parsed.result || typeof parsed.result !== "object") {
          throw new Error(typeof parsed?.error === "string" ? parsed.error : "Auth broker request failed.");
        }
        finish(resolvePromise, parsed.result);
      } catch (error) {
        finish(rejectPromise, new Error(String(error.message || "Auth broker returned an invalid response.").slice(0, 240)));
      }
    });
    connection.on("error", () => finish(rejectPromise, new Error("Auth broker is unavailable.")));
    connection.on("end", () => {
      if (!settled) finish(rejectPromise, new Error("Auth broker closed without a response."));
    });
  });
}

async function captureBookingShape(args) {
  if (!args.includes("--acknowledge-read-own-data")) {
    throw new Error("capture-booking-shape requires --acknowledge-read-own-data.");
  }
  const outputPath = argumentValue(args, "--output");
  if (!outputPath) throw new Error("capture-booking-shape requires --output with an absolute path.");
  const category = argumentValue(args, "--category") ?? "active";
  const config = readConfig();
  const socketPath = process.env.TBANK_AUTH_BROKER_SOCKET ?? config.banking?.brokerSocket;
  await ensureBroker(config);
  const report = await captureOwnBookingStructure({
    category,
    brokerCall: (method, params) => scopedBrokerRequest(socketPath, method, params),
  });
  process.stdout.write(writeStructureOnlyReport(report, outputPath));
}

async function ensureBroker(config) {
  const socketPath = process.env.TBANK_AUTH_BROKER_SOCKET ?? config.banking?.brokerSocket;
  if (!socketPath) throw new Error("Combined mobile auth is not configured. Run setup with the combined profile first.");
  if (await brokerStatus(socketPath)) return { started: false };
  if (!existsSync(brokerExecutable)) throw new Error("Auth broker executable is missing. Run the documented local installation first.");
  const broker = spawn(brokerExecutable, [], {
    stdio: "ignore",
    env: runtimeEnvironment("broker", config),
    detached: true,
  });
  broker.unref();
  for (let attempt = 0; attempt < 40; attempt += 1) {
    if (await brokerStatus(socketPath)) return { started: broker.exitCode === null };
    // Another concurrently launched MCP may have won the socket race. Keep
    // polling for that shared broker even when this child has already exited.
    await new Promise((resolvePromise) => setTimeout(resolvePromise, 50));
  }
  if (broker.exitCode === null) broker.kill("SIGTERM");
  throw new Error("Auth broker did not become ready for the MCP session.");
}

async function stopBroker(config) {
  const socketPath = process.env.TBANK_AUTH_BROKER_SOCKET ?? config.banking?.brokerSocket;
  if (!socketPath || !(await brokerStatus(socketPath))) return { stopped: false, status: "not_running" };
  const result = await brokerRequest(socketPath, "lifecycle", "shutdown", {}, 1_000);
  if (!result?.shutdownRequested) throw new Error("Auth broker rejected the lifecycle shutdown request.");
  for (let attempt = 0; attempt < 40; attempt += 1) {
    if (!(await brokerStatus(socketPath, 100))) return { stopped: true, status: "stopped" };
    await new Promise((resolvePromise) => setTimeout(resolvePromise, 50));
  }
  throw new Error("Auth broker did not stop within the local lifecycle timeout.");
}

async function runComponent(component, args = []) {
  const config = readConfig();
  const commands = {
    hotels: [process.execPath, [hotelsServer]],
    banking: [bankingExecutable, []],
    broker: [brokerExecutable, []],
  };
  const selected = commands[component];
  if (!selected) throw new Error("component must be hotels, banking, or broker.");
  if (!existsSync(selected[0])) throw new Error(`${component} executable is missing. Run the documented local installation first.`);
  const sharedMobileSessionConfigured = Boolean(config.banking?.sessionFile && config.banking?.brokerSocket);
  const brokerRequired = args.includes("--ensure-broker") || args.includes("--with-broker")
    || ((component === "hotels" || component === "banking") && sharedMobileSessionConfigured);
  if (brokerRequired) await ensureBroker(config);
  const child = spawn(selected[0], selected[1], { stdio: "inherit", env: runtimeEnvironment(component, config) });
  for (const signal of ["SIGINT", "SIGTERM"]) process.on(signal, () => {
    child.kill(signal);
  });
  const [code, signal] = await new Promise((resolvePromise) => child.on("exit", (...result) => resolvePromise(result)));
  if (signal) process.kill(process.pid, signal);
  process.exitCode = code ?? 1;
}

function secureFileCheck(path, required) {
  if (!path || !existsSync(path)) return { status: required ? "missing" : "not_configured" };
  const metadata = statSync(path);
  if (!metadata.isFile()) return { status: "invalid" };
  return { status: (metadata.mode & 0o077) === 0 ? "ready" : "permissions_too_open" };
}

function doctor(args) {
  const profile = argumentValue(args, "--profile") ?? "combined";
  const components = profileComponents(profile);
  const config = readConfig();
  const checks = {};
  if (components.includes("hotels")) {
    const hotels = config.hotels ?? {};
    const keyPath = process.env.TBANK_HOTELS_JWT_PRIVATE_KEY_FILE ?? hotels.jwtPrivateKeyFile;
    const inlineAuth = Boolean(process.env.TBANK_HOTELS_JWT_PRIVATE_KEY || process.env.TBANK_HOTELS_AUTH_TOKEN || process.env.TBANK_HOTELS_AUTH_HEADERS_JSON);
    checks.hotels = {
      runtime: Number(process.versions.node.split(".")[0]) >= 20 ? "ready" : "unsupported",
      server: existsSync(hotelsServer) ? "ready" : "missing",
      transport: process.env.TBANK_HOTELS_API_BASE_URL || hotels.apiBaseUrl ? "configured" : "missing",
      authentication: inlineAuth ? "configured_from_environment" : secureFileCheck(keyPath, true).status,
    };
  }
  if (components.includes("banking")) {
    const banking = config.banking ?? {};
    const sessionPath = process.env.TBANK_BANKING_SESSION ?? banking.sessionFile;
    const socketPath = process.env.TBANK_AUTH_BROKER_SOCKET ?? banking.brokerSocket;
    checks.banking = {
      executable: existsSync(bankingExecutable) ? "ready" : "missing",
      session: secureFileCheck(sessionPath, true).status,
      brokerSocket: socketPath && existsSync(socketPath) ? "available" : "not_running",
    };
  }
  const blocking = JSON.stringify(checks).includes('"missing"') || JSON.stringify(checks).includes('"invalid"') || JSON.stringify(checks).includes('"unsupported"') || JSON.stringify(checks).includes('"permissions_too_open"');
  const report = { doctorVersion: "1.0", profile, providerRequestsPerformed: false, secretsExposed: false, ready: !blocking, checks };
  process.stdout.write(`${JSON.stringify(report, null, 2)}\n`);
  if (blocking) process.exitCode = 1;
}

function shellQuote(value) {
  return `'${String(value).replaceAll("'", "'\\''")}'`;
}

function clientConfig(args) {
  const client = argumentValue(args, "--client");
  const profile = argumentValue(args, "--profile") ?? "combined";
  const components = profileComponents(profile);
  const brokerComponents = profile === "combined" ? new Set(["hotels", "banking"]) : new Set(components.includes("banking") ? ["banking"] : []);
  const entries = Object.fromEntries(components.map((component) => [
    `tbank-${component}`,
    { type: "local", command: [process.execPath, fileURLToPath(import.meta.url), "run", component, ...(brokerComponents.has(component) ? ["--ensure-broker"] : [])], enabled: true },
  ]));
  if (client === "opencode") {
    process.stdout.write(`${JSON.stringify({ mcp: entries }, null, 2)}\n`);
    return;
  }
  const lines = components.map((component) => {
    const command = [process.execPath, fileURLToPath(import.meta.url), "run", component, ...(brokerComponents.has(component) ? ["--ensure-broker"] : [])].map(shellQuote).join(" ");
    if (client === "codex") return `codex mcp add tbank-${component} -- ${command}`;
    if (client === "claude") return `claude mcp add --scope user tbank-${component} -- ${command}`;
    throw new Error("client must be opencode, codex, or claude.");
  });
  process.stdout.write(`${lines.join("\n")}\n`);
}

function cleanChildEnvironment() {
  return Object.fromEntries(Object.entries(process.env).filter(([name]) => !name.startsWith("TBANK_")).concat([["NODE_ENV", "test"]]));
}

async function mcpManifest(component) {
  const command = component === "hotels" ? process.execPath : bankingExecutable;
  const commandArgs = component === "hotels" ? [hotelsServer] : [];
  if (!existsSync(command)) throw new Error(`${component} executable is missing.`);
  const child = spawn(command, commandArgs, { stdio: ["pipe", "pipe", "pipe"], env: cleanChildEnvironment() });
  let buffer = "";
  let stderr = "";
  const responses = new Map();
  child.stdout.setEncoding("utf8");
  child.stdout.on("data", (chunk) => {
    buffer += chunk;
    const lines = buffer.split("\n");
    buffer = lines.pop() ?? "";
    for (const line of lines) if (line) {
      const message = JSON.parse(line);
      responses.set(message.id, message);
    }
  });
  child.stderr.setEncoding("utf8");
  child.stderr.on("data", (chunk) => { stderr += chunk; });
  const waitFor = (id) => new Promise((resolvePromise, reject) => {
    const started = Date.now();
    const timer = setInterval(() => {
      if (responses.has(id)) {
        clearInterval(timer);
        resolvePromise(responses.get(id));
      } else if (Date.now() - started > 5_000) {
        clearInterval(timer);
        reject(new Error(`${component} MCP manifest request timed out.`));
      }
    }, 10);
  });
  child.stdin.write(`${JSON.stringify({ jsonrpc: "2.0", id: 1, method: "initialize", params: { protocolVersion: "2025-03-26", capabilities: {}, clientInfo: { name: "contract-exporter", version: "1" } } })}\n`);
  const initialized = await waitFor(1);
  child.stdin.write(`${JSON.stringify({ jsonrpc: "2.0", id: 2, method: "tools/list", params: {} })}\n`);
  const listed = await waitFor(2);
  child.stdin.write(`${JSON.stringify({ jsonrpc: "2.0", id: 3, method: "ping", params: {} })}\n`);
  const ping = await waitFor(3);
  child.stdin.end();
  const exitCode = await new Promise((resolvePromise, reject) => {
    const timer = setTimeout(() => {
      child.kill();
      reject(new Error(`${component} MCP did not shut down after stdin EOF.`));
    }, 2_000);
    child.on("exit", (code) => {
      clearTimeout(timer);
      resolvePromise(code);
    });
  });
  if (initialized.error || listed.error || ping.error || JSON.stringify(ping.result) !== "{}") throw new Error(`${component} MCP rejected conformance requests.`);
  if (exitCode !== 0) throw new Error(`${component} MCP exited with code ${exitCode} after stdin EOF.`);
  if (stderr.trim()) throw new Error(`${component} MCP wrote unexpected diagnostics during offline conformance.`);
  return {
    manifestVersion: 1,
    component,
    protocolVersion: initialized.result.protocolVersion,
    serverInfo: initialized.result.serverInfo,
    tools: [...listed.result.tools].sort((left, right) => left.name.localeCompare(right.name)).map((tool) => ({
      name: tool.name,
      description: tool.description,
      inputSchema: tool.inputSchema,
      outputSchema: tool.outputSchema ?? null,
      annotations: tool.annotations ?? null,
    })),
  };
}

async function conformance() {
  const report = { conformanceVersion: "1.0", providerRequestsPerformed: false, components: {} };
  for (const component of ["hotels", "banking"]) {
    const first = await mcpManifest(component);
    const second = await mcpManifest(component);
    if (JSON.stringify(first) !== JSON.stringify(second)) throw new Error(`${component} MCP contract changed across a clean restart.`);
    report.components[component] = {
      initialize: "passed",
      toolsList: "passed",
      ping: "passed",
      newlineFraming: "passed",
      eofShutdown: "passed",
      cleanRestart: "passed",
      unexpectedStderr: false,
    };
  }
  process.stdout.write(`${JSON.stringify(report, null, 2)}\n`);
}

async function runChecked(command, args, cwd) {
  const child = spawn(command, args, { cwd, stdio: "inherit", env: cleanChildEnvironment() });
  const [code, signal] = await new Promise((resolvePromise) => child.on("exit", (...result) => resolvePromise(result)));
  if (signal) throw new Error(`Offline verification was interrupted by ${signal}.`);
  if (code !== 0) throw new Error(`Offline verification failed in ${cwd}.`);
}

async function verify() {
  const bankingPython = resolve(bankingRoot, ".venv/bin/python");
  if (!existsSync(bankingPython)) throw new Error("Banking local environment is missing. Run the documented installation first.");
  await runChecked(process.execPath, ["--test", resolve(localRoot, "test/cli.test.mjs")], localRoot);
  await runChecked(process.execPath, ["--test", resolve(repositoryRoot, "tools/tbank-hotels-mcp/test/protocol.test.mjs")], resolve(repositoryRoot, "tools/tbank-hotels-mcp"));
  await runChecked(bankingPython, ["-m", "unittest", "discover", "-s", "test", "-v"], bankingRoot);
  await contracts("check");
  await conformance();
  process.stdout.write("All local MCP release checks passed; no provider requests were performed.\n");
}

async function contracts(action) {
  if (!['update', 'check'].includes(action)) throw new Error("contracts action must be update or check.");
  mkdirSync(contractsDirectory, { recursive: true });
  for (const component of ["hotels", "banking"]) {
    const manifest = await mcpManifest(component);
    const content = `${JSON.stringify(manifest, null, 2)}\n`;
    const target = resolve(contractsDirectory, `${component}-tools.json`);
    if (action === "update") writeFileSync(target, content);
    else if (!existsSync(target) || readFileSync(target, "utf8") !== content) throw new Error(`${component} tool manifest is out of date. Run contracts update and review the diff.`);
  }
  process.stdout.write(`Contract manifests ${action === "update" ? "updated" : "match"}; no provider requests were performed.\n`);
}

export async function main(argv = process.argv.slice(2)) {
  const [command, subcommand] = argv;
  if (command === "setup") return setup(argv.slice(1));
  if (command === "doctor") return doctor(argv.slice(1));
  if (command === "login") return runInteractiveLogin(false);
  if (command === "logout") return runInteractiveLogin(true);
  if (command === "run") return runComponent(subcommand, argv.slice(2));
  if (command === "client-config") return clientConfig(argv.slice(1));
  if (command === "inspect-booking-fixture") {
    process.stdout.write(inspectBookingFixture({
      inputPath: argumentValue(argv.slice(1), "--input"),
      outputPath: argumentValue(argv.slice(1), "--output"),
    }));
    return;
  }
  if (command === "capture-booking-shape") return captureBookingShape(argv.slice(1));
  if (command === "stop-broker") {
    process.stdout.write(`${JSON.stringify(await stopBroker(readConfig()), null, 2)}\n`);
    return;
  }
  if (command === "payment-readiness") {
    process.stdout.write(`${JSON.stringify(paymentReadinessReport(), null, 2)}\n`);
    return;
  }
  if (command === "contracts") return contracts(subcommand);
  if (command === "conformance") return conformance();
  if (command === "verify") return verify();
  throw new Error("Usage: tbank-mcp-local setup|doctor|login|logout|run|client-config|inspect-booking-fixture|capture-booking-shape|payment-readiness|stop-broker|contracts|conformance|verify");
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  main().catch((error) => {
    process.stderr.write(`${error.message}\n`);
    process.exitCode = 1;
  });
}
