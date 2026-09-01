#!/usr/bin/env node

import { realpathSync } from "node:fs";
import { fileURLToPath } from "node:url";

import { MCP_PROTOCOL_VERSION, SERVER_NAME, SERVER_VERSION } from "./config.mjs";
import { callTool } from "./runtime.mjs";
import { startStdioServer } from "./stdio-server.mjs";
import { tools } from "./tool-contracts.mjs";

export { callTool, setAuthBrokerConnectorForTests } from "./runtime.mjs";

if (process.argv[1] && realpathSync(process.argv[1]) === fileURLToPath(import.meta.url)) {
  startStdioServer({
    callTool,
    tools,
    serverName: SERVER_NAME,
    serverVersion: SERVER_VERSION,
    protocolVersion: MCP_PROTOCOL_VERSION,
  });
}
