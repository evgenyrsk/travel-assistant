import type {
  EndpointClassification,
  EndpointReport,
  HttpMethod,
  OpenApiOperation,
  RuntimeRoute,
} from "./types.js";

interface ExplicitClassification {
  classification: EndpointClassification;
  reason: string;
}

const EXPLICIT_CLASSIFICATIONS = new Map<string, ExplicitClassification>([
  [
    endpointKey("post", "/api/v1/assistant/sessions"),
    {
      classification: "platform_client_candidate",
      reason: "bounded_chat_first_session_creation",
    },
  ],
  [
    endpointKey("post", "/api/v1/assistant/sessions/{sessionId}/messages"),
    {
      classification: "platform_client_candidate",
      reason: "bounded_chat_first_session_continuation",
    },
  ],
  [
    endpointKey("get", "/api/v1/hotel-searches/{searchId}/offers"),
    {
      classification: "platform_client_candidate",
      reason: "bounded_chat_first_offer_loading",
    },
  ],
  [
    endpointKey(
      "get",
      "/api/v1/hotel-searches/{searchId}/offers/{offerId}/details",
    ),
    {
      classification: "platform_client_candidate",
      reason: "bounded_selected_hotel_details_loading",
    },
  ],
  [
    endpointKey("get", "/api/v1/health"),
    {
      classification: "operational",
      reason: "operational_health_check_not_product_client_flow",
    },
  ],
  [
    endpointKey("get", "/health/live"),
    {
      classification: "operational",
      reason: "root_liveness_probe_not_product_client_flow",
    },
  ],
  [
    endpointKey("get", "/health/ready"),
    {
      classification: "operational",
      reason: "root_readiness_probe_not_product_client_flow",
    },
  ],
  [
    endpointKey("get", "/metrics"),
    {
      classification: "operational",
      reason: "root_metrics_scrape_not_product_client_flow",
    },
  ],
  [
    endpointKey("post", "/api/v1/hotel-searches"),
    {
      classification: "diagnostic_excluded",
      reason: "direct_hotel_search_diagnostic_not_chat_first_contract",
    },
  ],
  [
    endpointKey("get", "/api/v1/assistant/sessions/{sessionId}/shortlist"),
    {
      classification: "placeholder_excluded",
      reason: "placeholder_501_not_implemented_shortlist_read",
    },
  ],
  [
    endpointKey("put", "/api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}"),
    {
      classification: "placeholder_excluded",
      reason: "placeholder_501_not_implemented_shortlist_upsert",
    },
  ],
  [
    endpointKey("delete", "/api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}"),
    {
      classification: "placeholder_excluded",
      reason: "placeholder_501_not_implemented_shortlist_delete",
    },
  ],
  [
    endpointKey("post", "/api/v1/assistant/sessions/{sessionId}/explanations"),
    {
      classification: "placeholder_excluded",
      reason: "placeholder_501_not_implemented_explanation",
    },
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
      const explicit = EXPLICIT_CLASSIFICATIONS.get(key);

      return {
        method,
        path,
        operationId: operation?.operationId,
        inOpenApi: operation !== undefined,
        inRuntime: matchingRoutes.length > 0,
        runtimeSourceFiles: Array.from(
          new Set(matchingRoutes.map((route) => route.sourceFile)),
        ),
        classification:
          explicit?.classification ??
          (matchingRoutes.length > 0 ? "runtime_only" : "unclassified"),
        classificationReason: explicit?.reason,
        readiness: "not_ready",
      };
    });
}

function endpointKey(method: HttpMethod, path: string): string {
  return `${method.toUpperCase()} ${path}`;
}

function splitEndpointKey(key: string): [HttpMethod, string] {
  const [method, ...pathParts] = key.split(" ");
  return [method.toLowerCase() as HttpMethod, pathParts.join(" ")];
}
