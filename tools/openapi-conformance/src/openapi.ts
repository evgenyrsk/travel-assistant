import fs from "node:fs";
import path from "node:path";
import { parse } from "yaml";
import { DEFAULT_OPENAPI_CANDIDATES, joinRoutePath } from "./paths.js";
import type {
  HttpMethod,
  OpenApiInventory,
  OpenApiOperation,
  SourceCandidate,
} from "./types.js";

const OPENAPI_METHODS: HttpMethod[] = ["delete", "get", "patch", "post", "put"];

export class OpenApiInputError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "OpenApiInputError";
  }
}

export function loadOpenApiInventory(
  repositoryRoot: string,
  explicitSource?: string,
): OpenApiInventory {
  const candidates = sourceCandidates(repositoryRoot, explicitSource);
  const selected = candidates.find((candidate) => candidate.exists);

  if (!selected) {
    throw new OpenApiInputError("OpenAPI source was not found.");
  }

  const absoluteSource = path.resolve(repositoryRoot, selected.path);
  let documentText: string;
  try {
    documentText = fs.readFileSync(absoluteSource, "utf8");
  } catch (error) {
    throw new OpenApiInputError(
      `OpenAPI source is not readable: ${errorMessage(error)}`,
    );
  }

  let document: unknown;
  try {
    document = parse(documentText);
  } catch (error) {
    throw new OpenApiInputError(
      `OpenAPI source is not parseable YAML: ${errorMessage(error)}`,
    );
  }

  if (!isRecord(document)) {
    throw new OpenApiInputError("OpenAPI source root must be an object.");
  }

  const openApiVersion = stringValue(document.openapi);
  if (!openApiVersion) {
    throw new OpenApiInputError("OpenAPI source must contain an openapi field.");
  }

  if (!openApiVersion.startsWith("3.")) {
    throw new OpenApiInputError(
      `OpenAPI source version must be 3.x, received ${openApiVersion}.`,
    );
  }

  if (!isRecord(document.paths)) {
    throw new OpenApiInputError("OpenAPI source must contain a paths object.");
  }

  const serverBasePath = extractServerBasePath(document);
  const operations = extractOperations(document.paths, serverBasePath);

  return {
    sourcePath: selected.path,
    detectedFromCandidates: candidates,
    openApiVersion,
    serverBasePath,
    operations,
  };
}

function sourceCandidates(
  repositoryRoot: string,
  explicitSource?: string,
): SourceCandidate[] {
  const paths = explicitSource ? [explicitSource] : DEFAULT_OPENAPI_CANDIDATES;

  return paths.map((candidatePath) => ({
    path: candidatePath,
    exists: fs.existsSync(path.resolve(repositoryRoot, candidatePath)),
  }));
}

function extractServerBasePath(document: Record<string, unknown>): string {
  if (!Array.isArray(document.servers)) {
    return "";
  }

  const firstServer = document.servers.find(isRecord);
  if (!firstServer) {
    return "";
  }

  return stringValue(firstServer.url) ?? "";
}

function extractOperations(
  paths: Record<string, unknown>,
  serverBasePath: string,
): OpenApiOperation[] {
  const operations: OpenApiOperation[] = [];

  for (const [apiPath, pathItem] of Object.entries(paths)) {
    if (!isRecord(pathItem)) {
      continue;
    }

    for (const method of OPENAPI_METHODS) {
      const operation = pathItem[method];
      if (!isRecord(operation)) {
        continue;
      }

      operations.push({
        method,
        path: apiPath,
        fullPath: joinRoutePath(serverBasePath, apiPath),
        operationId: stringValue(operation.operationId),
      });
    }
  }

  return operations.sort((left, right) =>
    `${left.path} ${left.method}`.localeCompare(`${right.path} ${right.method}`),
  );
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function stringValue(value: unknown): string | undefined {
  return typeof value === "string" ? value : undefined;
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}
