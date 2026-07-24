export const MAX_PROXY_REQUEST_BODY_BYTES = 32 * 1024;

export class RequestBodyTooLargeError extends Error {
  constructor() {
    super("Request body is too large.");
    this.name = "RequestBodyTooLargeError";
  }
}

export async function readBoundedRequestBody(
  request,
  maxBytes = MAX_PROXY_REQUEST_BODY_BYTES,
) {
  const declaredLength = Number(request.headers?.["content-length"] ?? 0);
  if (Number.isFinite(declaredLength) && declaredLength > maxBytes) {
    throw new RequestBodyTooLargeError();
  }

  const chunks = [];
  let receivedBytes = 0;

  for await (const chunk of request) {
    const buffer = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);
    receivedBytes += buffer.length;
    if (receivedBytes > maxBytes) {
      throw new RequestBodyTooLargeError();
    }
    chunks.push(buffer);
  }

  return Buffer.concat(chunks, receivedBytes);
}
