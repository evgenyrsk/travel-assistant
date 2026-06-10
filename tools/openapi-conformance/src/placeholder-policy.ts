import type { EndpointReport, HttpMethod, OpenApiOperation, RuntimeRoute } from "./types.js";

const FOUNDATION_CANDIDATES = new Set([
  endpointKey("get", "/api/v1/health"),
  endpointKey("post", "/api/v1/assistant/sessions"),
  endpointKey("post", "/api/v1/assistant/sessions/{sessionId}/messages"),
]);

const PLACEHOLDER_EXCLUSIONS = new Map<string, string>([
  [
    endpointKey("post", "/api/v1/hotel-searches"),
    "placeholder_501_not_implemented_hotel_search",
  ],
  [
    endpointKey("get", "/api/v1/hotel-searches/{searchId}/offers"),
    "placeholder_501_not_implemented_hotel_offers",
  ],
  [
    endpointKey("get", "/api/v1/assistant/sessions/{sessionId}/shortlist"),
    "placeholder_501_not_implemented_shortlist_read",
  ],
  [
    endpointKey("put", "/api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}"),
    "placeholder_501_not_implemented_shortlist_upsert",
  ],
  [
    endpointKey("delete", "/api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}"),
    "placeholder_501_not_implemented_shortlist_delete",
  ],
  [
    endpointKey("post", "/api/v1/assistant/sessions/{sessionId}/explanations"),
    "placeholder_501_not_implemented_explanation",
  ],
]);

export function buildEndpointReports(
  openApiOperations: OpenApiOperation[],
  runtimeRoutes: RuntimeRoute[],
): EndpointReport[] {
  const endpointKeys = new Set<string>();
  for (const operation of openApiOperations) {
    endpointKeys.add(endpointKey(operation.method, operation.fullPath));
  }
  for (const route of runtimeRoutes) {
    endpointKeys.add(endpointKey(route.method, route.path));
  }

  return Array.from(endpointKeys)
    .sort()
    .map((key) => {
      const [method, path] = splitEndpointKey(key);
      const operation = openApiOperations.find(
        (candidate) =>
          candidate.method === method && candidate.fullPath === path,
      );
      const matchingRoutes = runtimeRoutes.filter(
        (candidate) => candidate.method === method && candidate.path === path,
      );
      const placeholderReason = PLACEHOLDER_EXCLUSIONS.get(key);

      return {
        method,
        path,
        operationId: operation?.operationId,
        inOpenApi: operation !== undefined,
        inRuntime: matchingRoutes.length > 0,
        runtimeSourceFiles: Array.from(
          new Set(matchingRoutes.map((route) => route.sourceFile)),
        ),
        classification: classifyEndpoint(key, placeholderReason, matchingRoutes),
        placeholderReason,
        readiness: "not_ready",
      };
    });
}

function classifyEndpoint(
  key: string,
  placeholderReason: string | undefined,
  matchingRoutes: RuntimeRoute[],
): EndpointReport["classification"] {
  if (placeholderReason) {
    return "placeholder_excluded";
  }

  if (FOUNDATION_CANDIDATES.has(key)) {
    return "foundation_candidate";
  }

  if (matchingRoutes.length > 0) {
    return "runtime_only";
  }

  return "unclassified";
}

function endpointKey(method: HttpMethod, path: string): string {
  return `${method.toUpperCase()} ${path}`;
}

function splitEndpointKey(key: string): [HttpMethod, string] {
  const [method, ...pathParts] = key.split(" ");
  return [method.toLowerCase() as HttpMethod, pathParts.join(" ")];
}
