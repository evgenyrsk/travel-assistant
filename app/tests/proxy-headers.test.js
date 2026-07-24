import test from "node:test";
import assert from "node:assert/strict";

import {
  buildBackendRequestHeaders,
  buildProxyResponseHeaders,
} from "../proxy-headers.mjs";

test("proxy forwards only a safe inbound request ID", () => {
  const safe = buildBackendRequestHeaders({
    host: "demo.internal",
    "content-type": "application/json",
    "x-request-id": "request_123.test-1",
  });
  const malformed = buildBackendRequestHeaders({
    "x-request-id": "request id with spaces",
  });
  const oversized = buildBackendRequestHeaders({
    "x-request-id": "a".repeat(129),
  });
  const duplicated = buildBackendRequestHeaders({
    "x-request-id": ["request-1", "request-2"],
  });

  assert.equal(safe.get("x-request-id"), "request_123.test-1");
  assert.equal(safe.get("content-type"), "application/json");
  assert.equal(safe.has("host"), false);
  assert.equal(malformed.has("x-request-id"), false);
  assert.equal(oversized.has("x-request-id"), false);
  assert.equal(duplicated.has("x-request-id"), false);
});

test("proxy exposes only safe backend response correlation", () => {
  const safe = buildProxyResponseHeaders(
    new Headers({
      "content-type": "application/json",
      "x-request-id": "request_123.test-1",
      "x-internal-debug": "do-not-forward",
    }),
  );
  const malformed = buildProxyResponseHeaders(
    new Headers({ "x-request-id": "request id with spaces" }),
  );
  const oversized = buildProxyResponseHeaders(
    new Headers({ "x-request-id": "a".repeat(129) }),
  );

  assert.deepEqual(safe, {
    "cache-control": "no-store",
    "content-type": "application/json",
    "x-request-id": "request_123.test-1",
  });
  assert.equal("x-internal-debug" in safe, false);
  assert.deepEqual(malformed, { "cache-control": "no-store" });
  assert.deepEqual(oversized, { "cache-control": "no-store" });
});
