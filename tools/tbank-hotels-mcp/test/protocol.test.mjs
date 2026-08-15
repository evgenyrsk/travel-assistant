import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import test from "node:test";

const serverPath = new URL("../src/server.mjs", import.meta.url).pathname;

function startServer() {
  const child = spawn(process.execPath, [serverPath], { stdio: ["pipe", "pipe", "pipe"] });
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

test("reports MCP metadata and the hotel flow tools", async (t) => {
  const server = startServer();
  t.after(() => server.child.kill());
  const initialized = await server.request({
    jsonrpc: "2.0",
    id: 1,
    method: "initialize",
    params: { protocolVersion: "2025-03-26" },
  });
  assert.equal(initialized.result.serverInfo.name, "tbank-hotels-browser-mcp");
  const listed = await server.request({ jsonrpc: "2.0", id: 2, method: "tools/list" });
  const names = listed.result.tools.map((tool) => tool.name);
  assert.deepEqual(names, [
    "tbank_hotels_open",
    "tbank_hotels_start_login",
    "tbank_hotels_import_auth_cookies",
    "tbank_hotels_open_favorites",
    "tbank_hotels_open_orders",
    "tbank_hotels_open_order",
    "tbank_hotels_open_city",
    "tbank_hotels_open_search_results",
    "tbank_hotels_open_hotel",
    "tbank_hotels_open_booking_preview",
    "tbank_hotels_fill_destination",
    "tbank_hotels_arm_user_action",
    "tbank_hotels_execute_armed_action",
    "tbank_hotels_snapshot",
    "tbank_hotels_click",
    "tbank_hotels_press",
    "tbank_hotels_current_url",
    "tbank_hotels_close",
  ]);
});

test("validates an order identifier before browser automation starts", async (t) => {
  const server = startServer();
  t.after(() => server.child.kill());
  const result = await server.request({
    jsonrpc: "2.0",
    id: 1,
    method: "tools/call",
    params: {
      name: "tbank_hotels_open_order",
      arguments: { orderId: "not-an-order" },
    },
  });
  assert.equal(result.result.isError, true);
  assert.match(result.result.content[0].text, /digits only/);
});

test("validates a booking-preview date range before browser automation starts", async (t) => {
  const server = startServer();
  t.after(() => server.child.kill());
  const result = await server.request({
    jsonrpc: "2.0",
    id: 1,
    method: "tools/call",
    params: {
      name: "tbank_hotels_open_booking_preview",
      arguments: {
        hotelId: "1441391",
        checkIn: "2026-08-18",
        checkOut: "2026-08-18",
        guests: 2,
      },
    },
  });
  assert.equal(result.result.isError, true);
  assert.match(result.result.content[0].text, /later/);
});

test("rejects an invalid cookie file before browser automation starts", async (t) => {
  const server = startServer();
  t.after(() => server.child.kill());
  const result = await server.request({
    jsonrpc: "2.0",
    id: 1,
    method: "tools/call",
    params: {
      name: "tbank_hotels_import_auth_cookies",
      arguments: { cookieFile: "not-an-absolute-path" },
    },
  });
  assert.equal(result.result.isError, true);
  assert.match(result.result.content[0].text, /absolute path/);
});

test("requires a fresh snapshot before any page click", async (t) => {
  const server = startServer();
  t.after(() => server.child.kill());
  const result = await server.request({
    jsonrpc: "2.0",
    id: 1,
    method: "tools/call",
    params: {
      name: "tbank_hotels_click",
      arguments: { ref: "@e42" },
    },
  });
  assert.equal(result.result.isError, true);
  assert.match(result.result.content[0].text, /snapshot/);
});

test("does not prepare a mutating action without a fresh snapshot", async (t) => {
  const server = startServer();
  t.after(() => server.child.kill());
  const result = await server.request({
    jsonrpc: "2.0",
    id: 1,
    method: "tools/call",
    params: {
      name: "tbank_hotels_arm_user_action",
      arguments: { ref: "@e42" },
    },
  });
  assert.equal(result.result.isError, true);
  assert.match(result.result.content[0].text, /snapshot/);
});
