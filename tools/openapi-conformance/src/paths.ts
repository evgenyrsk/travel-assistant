import path from "node:path";

export const TOOL_VERSION = "0.1.0";
export const DEFAULT_OPENAPI_CANDIDATES = [
  "docs/architecture/stage-6/openapi-draft.yaml",
  "openapi-draft.yaml",
];
export const DEFAULT_SUBSET_MANIFEST =
  "docs/architecture/stage-7/generated-client-ready-subset.yaml";
export const BACKEND_API_ROUTE_DIR =
  "services/backend/src/main/kotlin/com/travelassistant/backend/api";

export function findRepositoryRoot(startDirectory: string): string {
  let current = startDirectory;

  while (true) {
    const parent = path.dirname(current);
    if (parent === current) {
      return startDirectory;
    }

    if (path.basename(current) === "openapi-conformance") {
      return path.resolve(current, "../..");
    }

    current = parent;
  }
}

export function toPosixPath(filePath: string): string {
  return filePath.split(path.sep).join("/");
}

export function normalizeRoutePath(value: string): string {
  const collapsed = value.replace(/\/+/g, "/");
  if (collapsed === "") {
    return "/";
  }

  return collapsed.startsWith("/") ? collapsed : `/${collapsed}`;
}

export function joinRoutePath(prefix: string, suffix: string): string {
  const normalizedPrefix = normalizeRoutePath(prefix);
  const normalizedSuffix = normalizeRoutePath(suffix);

  if (normalizedPrefix === "/") {
    return normalizedSuffix;
  }

  if (normalizedSuffix === "/") {
    return normalizedPrefix;
  }

  return normalizeRoutePath(`${normalizedPrefix}/${normalizedSuffix}`);
}
