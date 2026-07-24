const SAFE_REQUEST_ID = /^[A-Za-z0-9._-]{1,128}$/;

export function buildBackendRequestHeaders(requestHeaders) {
  const headers = new Headers();

  for (const [name, value] of Object.entries(requestHeaders)) {
    if (!value || name === "host" || name === "content-length") {
      continue;
    }

    if (name === "x-request-id") {
      if (typeof value === "string" && SAFE_REQUEST_ID.test(value)) {
        headers.set(name, value);
      }
      continue;
    }

    headers.set(name, Array.isArray(value) ? value.join(", ") : value);
  }

  return headers;
}

export function buildProxyResponseHeaders(backendHeaders) {
  const responseHeaders = { "cache-control": "no-store" };
  const contentType = backendHeaders.get("content-type");
  const requestId = backendHeaders.get("x-request-id");

  if (contentType) {
    responseHeaders["content-type"] = contentType;
  }
  if (requestId && SAFE_REQUEST_ID.test(requestId)) {
    responseHeaders["x-request-id"] = requestId;
  }

  return responseHeaders;
}
