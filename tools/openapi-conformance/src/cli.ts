import { inspectSubsetManifest } from "./subset-manifest.js";
import { findRepositoryRoot } from "./paths.js";
import { loadOpenApiInventory, OpenApiInputError } from "./openapi.js";
import { collectRuntimeRouteInventory } from "./route-inventory.js";
import { buildReport } from "./report.js";
import type { ConformanceReport, ToolOptions } from "./types.js";

const USAGE = `Usage: ./tools/openapi-conformance/check [--openapi-source <path>] [--subset-manifest <path>]

Emits a read-only JSON report to stdout. Default readiness status is not_ready.`;

async function main(): Promise<number> {
  try {
    const parsed = parseArguments(process.argv.slice(2));
    if (parsed.help) {
      console.log(USAGE);
      return 0;
    }

    const repositoryRoot = findRepositoryRoot(process.cwd());
    const openApiInventory = loadOpenApiInventory(
      repositoryRoot,
      parsed.options.openApiSource,
    );
    const runtimeRoutes = collectRuntimeRouteInventory(repositoryRoot);
    const subsetManifest = inspectSubsetManifest(
      repositoryRoot,
      parsed.options.subsetManifest,
    );

    const report = buildReport(openApiInventory, runtimeRoutes, subsetManifest);
    writeJson(report);
    return 0;
  } catch (error) {
    const executionReport = buildExecutionErrorReport(error);
    writeJson(executionReport);
    return 2;
  }
}

interface ParsedArguments {
  help: boolean;
  options: ToolOptions;
}

function parseArguments(args: string[]): ParsedArguments {
  const options: ToolOptions = {};
  let help = false;

  for (let index = 0; index < args.length; index += 1) {
    const arg = args[index];

    if (arg === "--help" || arg === "-h") {
      help = true;
      continue;
    }

    if (arg === "--openapi-source") {
      options.openApiSource = readValue(args, index, arg);
      index += 1;
      continue;
    }

    if (arg === "--subset-manifest") {
      options.subsetManifest = readValue(args, index, arg);
      index += 1;
      continue;
    }

    throw new Error(`Unknown argument: ${arg}`);
  }

  return { help, options };
}

function readValue(args: string[], index: number, optionName: string): string {
  const value = args[index + 1];
  if (!value || value.startsWith("--")) {
    throw new Error(`Missing value for ${optionName}.`);
  }

  return value;
}

function buildExecutionErrorReport(error: unknown): ConformanceReport {
  const message = error instanceof Error ? error.message : String(error);
  const code =
    error instanceof OpenApiInputError
      ? "OPENAPI_INPUT_ERROR"
      : "TOOL_EXECUTION_ERROR";

  return {
    tool: {
      name: "travel-assistant-openapi-conformance",
      version: "0.1.0",
      mode: "classification",
      readOnly: true,
    },
    generatedAt: new Date().toISOString(),
    status: "not_ready",
    readinessClaim: false,
    openApiSource: {
      path: "",
      detected: false,
      serverBasePath: "",
      operationCount: 0,
      candidates: [],
    },
    subsetManifest: {
      path: "",
      exists: false,
      status: "missing_not_created",
      requiredForSkeleton: false,
    },
    inventories: {
      openApi: [],
      runtimeRoutes: [],
    },
    endpoints: [],
    checks: [
      {
        name: "tool_execution",
        status: "not_ready",
        summary: message,
      },
    ],
    blockingFindings: [
      {
        code,
        severity: "blocking",
        message,
      },
    ],
    advisoryFindings: [],
    futureOnlyChecks: [
      {
        name: "generated_client_compile",
        status: "not_run",
        summary: "Not run because the tool did not complete input inspection.",
      },
      {
        name: "runtime_http_contract_tests",
        status: "not_run",
        summary: "Not run because the tool did not complete input inspection.",
      },
    ],
  };
}

function writeJson(report: ConformanceReport): void {
  process.stdout.write(`${JSON.stringify(report, null, 2)}\n`);
}

main()
  .then((exitCode) => {
    process.exitCode = exitCode;
  })
  .catch((error) => {
    process.stderr.write(`${error instanceof Error ? error.message : String(error)}\n`);
    process.exitCode = 2;
  });
