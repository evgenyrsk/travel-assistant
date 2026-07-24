import assert from "node:assert/strict";
import { Readable } from "node:stream";
import test from "node:test";

import {
  MAX_PROXY_REQUEST_BODY_BYTES,
  readBoundedRequestBody,
  RequestBodyTooLargeError,
} from "../request-body.mjs";

test("reads a request body within the proxy limit", async () => {
  const request = Readable.from([Buffer.from('{"message":"Казань"}')]);
  request.headers = {};

  const body = await readBoundedRequestBody(request);

  assert.equal(body.toString("utf8"), '{"message":"Казань"}');
});

test("rejects an oversized declared request before reading the stream", async () => {
  let streamRead = false;
  const request = Readable.from((async function* body() {
    streamRead = true;
    yield Buffer.from("not-read");
  })());
  request.headers = {
    "content-length": String(MAX_PROXY_REQUEST_BODY_BYTES + 1),
  };

  await assert.rejects(
    readBoundedRequestBody(request),
    RequestBodyTooLargeError,
  );
  assert.equal(streamRead, false);
});

test("stops buffering a chunked request when it crosses the proxy limit", async () => {
  const request = Readable.from([
    Buffer.alloc(MAX_PROXY_REQUEST_BODY_BYTES),
    Buffer.from("x"),
  ]);
  request.headers = {};

  await assert.rejects(
    readBoundedRequestBody(request),
    RequestBodyTooLargeError,
  );
});
