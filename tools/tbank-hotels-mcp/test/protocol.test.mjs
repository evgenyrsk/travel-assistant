import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import test from "node:test";

const serverPath = new URL("../src/server.mjs", import.meta.url).pathname;

function startServer(env = {}) {
  const child = spawn(process.execPath, [serverPath], { stdio: ["pipe", "pipe", "pipe"], env: { ...process.env, ...env } });
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
  return { child, request };
}

test("reports API MCP metadata and no browser tools", async (t) => {
  const server = startServer();
  t.after(() => server.child.kill());
  const initialized = await server.request({ jsonrpc: "2.0", id: 1, method: "initialize", params: { protocolVersion: "2025-03-26" } });
  assert.equal(initialized.result.serverInfo.name, "tbank-hotels-api-mcp");
  const listed = await server.request({ jsonrpc: "2.0", id: 2, method: "tools/list" });
  const names = listed.result.tools.map((tool) => tool.name);
  assert.ok(names.includes("tbank_hotels_search"));
  assert.ok(names.includes("tbank_hotels_execute_booking"));
  assert.ok(!names.some((name) => /browser|snapshot|cookie|open_/.test(name)));
});

test("does not expose configured auth secrets", async (t) => {
  const server = startServer({ TBANK_HOTELS_API_BASE_URL: "https://hotels.example.test", TBANK_HOTELS_AUTH_TOKEN: "super-secret" });
  t.after(() => server.child.kill());
  const result = await server.request({ jsonrpc: "2.0", id: 1, method: "tools/call", params: { name: "tbank_hotels_connection_status", arguments: {} } });
  assert.equal(result.result.isError, false);
  assert.match(result.result.content[0].text, /configured/);
  assert.doesNotMatch(result.result.content[0].text, /super-secret|Authorization/);
});

test("does not attempt a provider request until API base URL is configured", async (t) => {
  const server = startServer();
  t.after(() => server.child.kill());
  const result = await server.request({ jsonrpc: "2.0", id: 1, method: "tools/call", params: { name: "tbank_hotels_search", arguments: { language: "ru-RU", payload: { destinationId: 1 } } } });
  assert.equal(result.result.isError, true);
  assert.match(result.result.content[0].text, /TBANK_HOTELS_API_BASE_URL is required/);
});

test("prepare is stateless and execute rejects a changed booking payload", async (t) => {
  const server = startServer();
  t.after(() => server.child.kill());
  const payload = { bookHash: "hash", guestContact: { email: "person@example.test" }, rooms: [] };
  const prepared = await server.request({ jsonrpc: "2.0", id: 1, method: "tools/call", params: { name: "tbank_hotels_prepare_booking", arguments: { payload } } });
  assert.equal(prepared.result.isError, false);
  const preview = JSON.parse(prepared.result.content[0].text);
  assert.match(prepared.result.content[0].text, /REDACTED/);
  const executed = await server.request({ jsonrpc: "2.0", id: 2, method: "tools/call", params: { name: "tbank_hotels_execute_booking", arguments: { payload: { ...payload, bookHash: "changed" }, preparedRequestHash: preview.requestHash, confirmation: preview.confirmation } } });
  assert.equal(executed.result.isError, true);
  assert.match(executed.result.content[0].text, /does not match/);
});

test("matching prepared confirmation reaches the configured transport gate", async (t) => {
  const server = startServer();
  t.after(() => server.child.kill());
  const payload = { bookHash: "hash", guestContact: {}, rooms: [] };
  const prepared = await server.request({ jsonrpc: "2.0", id: 1, method: "tools/call", params: { name: "tbank_hotels_prepare_booking", arguments: { payload } } });
  const preview = JSON.parse(prepared.result.content[0].text);
  const executed = await server.request({ jsonrpc: "2.0", id: 2, method: "tools/call", params: { name: "tbank_hotels_execute_booking", arguments: { payload, preparedRequestHash: preview.requestHash, confirmation: preview.confirmation } } });
  assert.equal(executed.result.isError, true);
  assert.match(executed.result.content[0].text, /TBANK_HOTELS_API_BASE_URL is required/);
});
