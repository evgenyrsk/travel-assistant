import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { extname, join, normalize } from "node:path";
import { fileURLToPath } from "node:url";

const sourceDirectory = fileURLToPath(new URL("./src/", import.meta.url));
const backendUrl = new URL(process.env.BACKEND_URL ?? "http://127.0.0.1:8080");
const port = Number.parseInt(process.env.PORT ?? "4173", 10);
const host = "127.0.0.1";

const contentTypes = new Map([
  [".css", "text/css; charset=utf-8"],
  [".html", "text/html; charset=utf-8"],
  [".js", "text/javascript; charset=utf-8"],
  [".json", "application/json; charset=utf-8"],
]);

const server = createServer(async (request, response) => {
  try {
    if (request.url?.startsWith("/api/v1/")) {
      await proxyToBackend(request, response);
      return;
    }

    await serveFrontend(request, response);
  } catch {
    response.writeHead(500, { "content-type": "text/plain; charset=utf-8" });
    response.end("Frontend server error.");
  }
});

server.listen(port, host, () => {
  console.log(`Travel Assistant frontend: http://${host}:${port}`);
  console.log(`Local backend proxy: ${backendUrl.origin}`);
});

async function proxyToBackend(request, response) {
  const requestUrl = new URL(request.url, backendUrl);
  const method = request.method ?? "GET";
  const headers = new Headers();

  for (const [name, value] of Object.entries(request.headers)) {
    if (value && name !== "host" && name !== "content-length") {
      headers.set(name, Array.isArray(value) ? value.join(", ") : value);
    }
  }

  const backendResponse = await fetch(requestUrl, {
    method,
    headers,
    body: method === "GET" || method === "HEAD" ? undefined : await readRequestBody(request),
  });
  const responseBody = Buffer.from(await backendResponse.arrayBuffer());
  const responseHeaders = {};
  const contentType = backendResponse.headers.get("content-type");

  if (contentType) {
    responseHeaders["content-type"] = contentType;
  }

  response.writeHead(backendResponse.status, responseHeaders);
  response.end(responseBody);
}

async function serveFrontend(request, response) {
  const requestedPath = request.url === "/" ? "index.html" : request.url?.split("?")[0].slice(1);
  const safePath = normalize(requestedPath ?? "index.html");
  const filePath = join(sourceDirectory, safePath);

  if (!filePath.startsWith(sourceDirectory)) {
    response.writeHead(400, { "content-type": "text/plain; charset=utf-8" });
    response.end("Invalid path.");
    return;
  }

  try {
    const content = await readFile(filePath);
    response.writeHead(200, {
      "content-type": contentTypes.get(extname(filePath)) ?? "application/octet-stream",
    });
    response.end(content);
  } catch {
    response.writeHead(404, { "content-type": "text/plain; charset=utf-8" });
    response.end("Not found.");
  }
}

async function readRequestBody(request) {
  const chunks = [];

  for await (const chunk of request) {
    chunks.push(chunk);
  }

  return Buffer.concat(chunks);
}
