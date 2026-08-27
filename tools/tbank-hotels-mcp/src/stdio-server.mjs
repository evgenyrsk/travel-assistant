import { createInterface } from "node:readline";

const text = (value) => ({
  type: "text",
  text: typeof value === "string" ? value : JSON.stringify(value, null, 2),
});

const response = (id, result) => ({ jsonrpc: "2.0", id, result });
const error = (id, code, message) => ({ jsonrpc: "2.0", id, error: { code, message } });
const write = (message) => process.stdout.write(`${JSON.stringify(message)}\n`);

function toolAnnotations(tool) {
  if (tool._annotations) return tool._annotations;
  const mutating = tool._execute === true;
  return {
    readOnlyHint: !mutating,
    destructiveHint: mutating,
    idempotentHint: !mutating,
    openWorldHint: true,
  };
}

function publicTool(tool) {
  const { _action, _execute, _hasPayload, _annotations, ...definition } = tool;
  return {
    ...definition,
    annotations: toolAnnotations({ ...definition, _action, _execute, _hasPayload, _annotations }),
  };
}

export function startStdioServer({ callTool, tools, serverName, serverVersion, protocolVersion }) {
  const handle = async (request) => {
    if (request.id === undefined) return;
    if (request.jsonrpc !== "2.0") return write(error(request.id ?? null, -32600, "Invalid JSON-RPC version."));
    if (request.method === "initialize") {
      return write(response(request.id, {
        protocolVersion,
        capabilities: { tools: { listChanged: false } },
        serverInfo: { name: serverName, version: serverVersion },
        instructions: "API-driven T-Bank Hotels MCP. Configure the base URL and authentication only through environment variables. This server does not use a browser, cookies, local browser state, or stored user sessions. Calls that can create a booking, set up a payment, cancel a booking, apply a promocode, or update extra services are disabled by default and require both an explicit runtime activation and a time-limited prepare/execute confirmation protocol.",
      }));
    }
    if (request.method === "ping") return write(response(request.id, {}));
    if (request.method === "tools/list") return write(response(request.id, { tools: tools.map(publicTool) }));
    if (request.method === "tools/call") {
      try {
        return write(response(request.id, {
          content: [text(await callTool(request.params?.name, request.params?.arguments))],
          isError: false,
        }));
      } catch (toolError) {
        return write(response(request.id, {
          content: [text(toolError.message)],
          isError: true,
        }));
      }
    }
    return write(error(request.id, -32601, "Method not found."));
  };

  const input = createInterface({ input: process.stdin, crlfDelay: Infinity });
  input.on("line", (line) => {
    try {
      const request = JSON.parse(line);
      if (!request || typeof request !== "object" || Array.isArray(request)) {
        return write(error(null, -32600, "JSON-RPC batch requests are not supported."));
      }
      void handle(request);
    } catch {
      write(error(null, -32700, "Parse error."));
    }
  });
}
