import fs from "node:fs";
import path from "node:path";
import {
  BACKEND_API_ROUTE_DIR,
  joinRoutePath,
  normalizeRoutePath,
  toPosixPath,
} from "./paths.js";
import type { HttpMethod, RuntimeRoute } from "./types.js";

const API_BASE_PATH = "/api/v1";
const ROUTE_DECLARATION = /\broute\s*\(\s*"([^"]+)"\s*\)\s*\{/;
const VERB_DECLARATION =
  /\b(get|post|put|delete|patch)\s*(?:\(\s*"([^"]*)"\s*\))?\s*\{/;

interface RouteContext {
  path: string;
  depth: number;
}

export function collectRuntimeRouteInventory(repositoryRoot: string): RuntimeRoute[] {
  const apiRouteDirectory = path.resolve(repositoryRoot, BACKEND_API_ROUTE_DIR);
  if (!fs.existsSync(apiRouteDirectory)) {
    return [];
  }

  const files = fs
    .readdirSync(apiRouteDirectory)
    .filter((fileName) => fileName.endsWith(".kt"))
    .map((fileName) => path.join(apiRouteDirectory, fileName))
    .sort();

  return files.flatMap((filePath) => extractRoutesFromFile(repositoryRoot, filePath));
}

function extractRoutesFromFile(
  repositoryRoot: string,
  filePath: string,
): RuntimeRoute[] {
  const lines = fs.readFileSync(filePath, "utf8").split(/\r?\n/);
  const contexts: RouteContext[] = [];
  const routes: RuntimeRoute[] = [];
  let braceDepth = 0;

  lines.forEach((line, index) => {
    while (contexts.length > 0 && braceDepth < contexts[contexts.length - 1].depth) {
      contexts.pop();
    }

    const currentPrefix = contexts.map((context) => context.path).join("");
    const routeMatch = ROUTE_DECLARATION.exec(line);
    if (routeMatch) {
      contexts.push({
        path: normalizeRoutePath(routeMatch[1]),
        depth: braceDepth + 1,
      });
    }

    const verbMatch = VERB_DECLARATION.exec(line);
    if (verbMatch) {
      const method = verbMatch[1] as HttpMethod;
      const declaredPath = verbMatch[2] ?? "";
      const routePath = joinRoutePath(
        API_BASE_PATH,
        joinRoutePath(currentPrefix, declaredPath),
      );

      routes.push({
        method,
        path: routePath,
        sourceFile: toPosixPath(path.relative(repositoryRoot, filePath)),
        line: index + 1,
      });
    }

    braceDepth += countBracesOutsideStrings(line);
  });

  return routes.sort((left, right) =>
    `${left.path} ${left.method}`.localeCompare(`${right.path} ${right.method}`),
  );
}

function countBracesOutsideStrings(line: string): number {
  let inString = false;
  let escaped = false;
  let total = 0;

  for (const char of line) {
    if (escaped) {
      escaped = false;
      continue;
    }

    if (char === "\\") {
      escaped = true;
      continue;
    }

    if (char === "\"") {
      inString = !inString;
      continue;
    }

    if (!inString && char === "{") {
      total += 1;
    }

    if (!inString && char === "}") {
      total -= 1;
    }
  }

  return total;
}
