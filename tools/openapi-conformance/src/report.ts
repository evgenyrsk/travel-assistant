import { TOOL_VERSION } from "./paths.js";
import { buildEndpointReports } from "./placeholder-policy.js";
import {
  validateSubsetManifestEndpointReferences,
  type SubsetManifestState,
} from "./subset-manifest.js";
import type {
  CheckReport,
  ConformanceReport,
  EndpointClassification,
  EndpointClassificationSummary,
  EndpointReport,
  Finding,
  OpenApiInventory,
  RuntimeRoute,
} from "./types.js";

export function buildReport(
  openApiInventory: OpenApiInventory,
  runtimeRoutes: RuntimeRoute[],
  rawSubsetManifest: SubsetManifestState,
): ConformanceReport {
  const subsetManifest = validateSubsetManifestEndpointReferences(
    rawSubsetManifest,
    openApiInventory.operations,
    runtimeRoutes,
  );
  const endpoints = buildEndpointReports(openApiInventory.operations, runtimeRoutes);
  const platformClientChecks = buildPlatformClientChecks(
    openApiInventory,
    endpoints,
  );
  const endpointClassificationSummary =
    buildEndpointClassificationSummary(endpoints);
  const manifestBlockingFindings = subsetManifest.manifestValidation.findings.filter(
    (finding) => finding.severity === "blocking",
  );
  const manifestAdvisoryFindings = subsetManifest.manifestValidation.findings.filter(
    (finding) => finding.severity === "advisory",
  );
  const advisoryFindings: Finding[] = [
    ...buildSubsetManifestFindings(subsetManifest),
    {
      code: "STATIC_RUNTIME_ROUTE_SCAN",
      severity: "advisory",
      message:
        "Runtime route inventory is collected by conservative static Ktor source scanning; no backend server was started.",
    },
    {
      code: "NON_PRODUCT_ENDPOINTS_EXCLUDED",
      severity: "advisory",
      message:
        "Health is operational, direct hotel search is diagnostic-only, and shortlist/explanation placeholders remain outside the bounded platform-client subset.",
    },
    ...buildEndpointClassificationFindings(endpointClassificationSummary),
    ...platformClientChecks.advisoryFindings,
    ...manifestAdvisoryFindings,
  ];

  return {
    tool: {
      name: "travel-assistant-openapi-conformance",
      version: TOOL_VERSION,
      mode: "classification",
      readOnly: true,
    },
    generatedAt: new Date().toISOString(),
    status: "not_ready",
    readinessClaim: false,
    openApiSource: {
      path: openApiInventory.sourcePath,
      detected: true,
      openApiVersion: openApiInventory.openApiVersion,
      serverBasePath: openApiInventory.serverBasePath,
      operationCount: openApiInventory.operations.length,
      candidates: openApiInventory.detectedFromCandidates,
    },
    subsetManifest: {
      path: subsetManifest.path,
      exists: subsetManifest.exists,
      status: subsetManifest.status,
      requiredForSkeleton: subsetManifest.requiredForSkeleton,
    },
    manifestDetection: subsetManifest.manifestDetection,
    manifestValidation: subsetManifest.manifestValidation,
    inventories: {
      openApi: openApiInventory.operations,
      runtimeRoutes,
    },
    endpointClassificationSummary,
    endpoints,
    checks: buildChecks(
      openApiInventory,
      runtimeRoutes,
      subsetManifest,
      endpointClassificationSummary,
      platformClientChecks.checks,
    ),
    blockingFindings: [
      ...manifestBlockingFindings,
      ...platformClientChecks.blockingFindings,
    ],
    advisoryFindings,
    futureOnlyChecks: buildFutureOnlyChecks(),
  };
}

function buildChecks(
  openApiInventory: OpenApiInventory,
  runtimeRoutes: RuntimeRoute[],
  subsetManifest: SubsetManifestState,
  summary: EndpointClassificationSummary,
  platformChecks: CheckReport[],
): CheckReport[] {
  return [
    {
      name: "openapi_source_detection",
      status: "passed",
      summary: `Detected OpenAPI source at ${openApiInventory.sourcePath}.`,
    },
    {
      name: "openapi_minimal_structure",
      status: "passed",
      summary: `Parsed OpenAPI ${openApiInventory.openApiVersion} with ${openApiInventory.operations.length} operations.`,
    },
    {
      name: "runtime_route_inventory",
      status: runtimeRoutes.length > 0 ? "advisory" : "missing",
      summary:
        runtimeRoutes.length > 0
          ? `Collected ${runtimeRoutes.length} runtime routes by static source scan.`
          : "No runtime routes were detected by static source scan.",
    },
    {
      name: "subset_manifest",
      status: subsetManifest.exists ? "advisory" : "not_created",
      summary: subsetManifest.exists
        ? "Subset manifest exists, remains not_ready, and was validated without readiness promotion."
        : "Subset manifest is missing/not_created and readiness remains not_ready.",
    },
    {
      name: "manifest_detection",
      status: subsetManifest.manifestDetection.exists ? "advisory" : "not_created",
      summary: subsetManifest.manifestDetection.note,
    },
    {
      name: "manifest_validation",
      status:
        subsetManifest.manifestValidation.status === "failed"
          ? "failed"
          : subsetManifest.manifestValidation.status === "advisory_passed"
            ? "advisory"
            : "not_run",
      summary:
        subsetManifest.manifestValidation.reason ??
        subsetManifest.manifestValidation.endpointReferenceValidation.summary,
    },
    {
      name: "endpoint_classification_summary",
      status: "advisory",
      summary:
        `${summary.total} endpoints classified: ` +
        `${summary.byClassification.platform_client_candidate} platform_client_candidate, ` +
        `${summary.byClassification.operational} operational, ` +
        `${summary.byClassification.diagnostic_excluded} diagnostic_excluded, ` +
        `${summary.byClassification.placeholder_excluded} placeholder_excluded, ` +
        `${summary.byClassification.runtime_only} runtime_only, ` +
        `${summary.byClassification.unclassified} unclassified.`,
    },
    ...platformChecks,
    {
      name: "readiness_status",
      status: "not_ready",
      summary:
        "Report status is intentionally not_ready; generated-client/OpenAPI readiness is not claimed.",
    },
  ];
}

interface PlatformClientChecks {
  checks: CheckReport[];
  blockingFindings: Finding[];
  advisoryFindings: Finding[];
}

function buildPlatformClientChecks(
  openApiInventory: OpenApiInventory,
  endpoints: EndpointReport[],
): PlatformClientChecks {
  const shape = openApiInventory.assistantContractShape;
  if (!shape) {
    return { checks: [], blockingFindings: [], advisoryFindings: [] };
  }

  const expectedEndpoints = [
    "POST /api/v1/assistant/sessions",
    "POST /api/v1/assistant/sessions/{sessionId}/messages",
    "GET /api/v1/hotel-searches/{searchId}/offers",
  ];
  const endpointIssues = expectedEndpoints.filter((expected) => {
    const [method, path] = expected.split(" ", 2);
    const endpoint = endpoints.find(
      (candidate) =>
        candidate.method === method.toLowerCase() && candidate.path === path,
    );

    return (
      endpoint === undefined ||
      !endpoint.inOpenApi ||
      !endpoint.inRuntime ||
      endpoint.classification !== "platform_client_candidate" ||
      endpoint.readiness !== "not_ready"
    );
  });

  const shapeExpectations: Array<[string, boolean]> = [
    ["create-session requestBody optional", shape.createSessionRequestBodyOptional],
    ["message requestBody required", shape.continueSessionRequestBodyRequired],
    ["AssistantMessageRequest.message present", shape.messagePropertyPresent],
    ["AssistantMessageRequest.message required", shape.messageRequired],
    ["AssistantMessageRequest.clientContext optional", shape.clientContextOptional],
    ["AssistantMessageRequest rejects unknown fields", shape.requestAdditionalPropertiesForbidden],
    ["AssistantMessageResponse.nextAction present", shape.nextActionPropertyPresent],
    ["AssistantMessageResponse.nextAction required", shape.nextActionRequired],
    ["AssistantMessageResponse rejects unknown fields", shape.responseAdditionalPropertiesForbidden],
    ["hotelSearchId property present", shape.hotelSearchIdPropertyPresent],
    ["hotelSearchId conditional enforced", shape.hotelSearchIdConditional],
    ["message endpoint 404 response present", shape.sessionNotFoundResponsePresent],
    ["assistant validation errors present", shape.validationErrorResponsesPresent],
    ["offers operation present", shape.offersOperationPresent],
    ["offers 404 response present", shape.offersNotFoundResponsePresent],
    ["offers response rejects unknown fields", shape.offersAdditionalPropertiesForbidden],
    ["search response rejects unknown fields", shape.searchAdditionalPropertiesForbidden],
    ["metadata rejects unknown fields", shape.metadataAdditionalPropertiesForbidden],
    ["hotel offer rejects unknown fields", shape.hotelOfferAdditionalPropertiesForbidden],
    ["rating remains optional", shape.ratingOptional],
    ["amenities remain optional", shape.amenitiesOptional],
    ["starRating remains optional", shape.starRatingOptional],
    ["freeCancellationUntil remains optional", shape.freeCancellationUntilOptional],
    ["appliedPreferences remains optional", shape.appliedPreferencesOptional],
    [
      "applied preferences reject unknown fields",
      shape.appliedPreferencesAdditionalPropertiesForbidden,
    ],
    ["refinementSuggestion remains optional", shape.refinementSuggestionOptional],
    [
      "refinement suggestion rejects unknown fields",
      shape.refinementSuggestionAdditionalPropertiesForbidden,
    ],
    ["message.maxLength is 4000", shape.messageMaxLength === 4_000],
    [
      "nextAction values match runtime",
      sameValues(shape.nextActionValues, [
        "ask_clarification",
        "show_boundary_message",
        "show_hotel_results",
      ]),
    ],
    [
      "AssistantSession required fields match runtime",
      sameValues(shape.sessionRequiredFields, [
        "createdAt",
        "sessionId",
        "status",
        "updatedAt",
      ]),
    ],
    [
      "AssistantMessage required fields match runtime",
      sameValues(shape.messageResponseRequiredFields, ["content", "role"]),
    ],
    [
      "HotelSearchResponse required fields match runtime",
      sameValues(shape.searchRequiredFields, [
        "criteria",
        "metadata",
        "searchId",
        "sessionId",
        "status",
      ]),
    ],
    [
      "HotelOffersResponse required fields match runtime",
      sameValues(shape.offersRequiredFields, [
        "metadata",
        "offers",
        "providerFacts",
        "searchId",
        "status",
      ]),
    ],
    [
      "terminal search statuses match runtime",
      sameValues(shape.searchStatusValues, [
        "completed_no_offers",
        "completed_with_offers",
      ]) &&
        sameValues(shape.offersStatusValues, [
          "completed_no_offers",
          "completed_with_offers",
        ]),
    ],
    [
      "metadata required fields match runtime",
      sameValues(shape.metadataRequiredFields, [
        "freshness",
        "providerState",
        "resultCompleteness",
        "warnings",
      ]),
    ],
    [
      "hotel offer required fields match runtime",
      sameValues(shape.hotelOfferRequiredFields, [
        "availability",
        "freshness",
        "hotelName",
        "location",
        "matchSummary",
        "offerId",
        "price",
        "providerFacts",
        "source",
      ]),
    ],
    [
      "applied preference fields match runtime",
      sameValues(shape.appliedPreferencesFields, [
        "freeCancellationRequired",
        "maxTotalPrice",
        "minimumGuestRating",
        "stars",
      ]),
    ],
    [
      "refinement suggestion required fields match runtime",
      sameValues(shape.refinementSuggestionRequiredFields, [
        "message",
        "preference",
        "type",
      ]),
    ],
    [
      "refinement suggestion type matches runtime",
      sameValues(shape.refinementSuggestionTypeValues, ["relax_preference"]),
    ],
    [
      "refinement suggestion preferences match runtime",
      sameValues(shape.refinementSuggestionPreferenceValues, [
        "freeCancellationRequired",
        "maxTotalPrice",
        "minimumGuestRating",
        "stars",
      ]),
    ],
  ];
  const shapeIssues = shapeExpectations
    .filter(([, satisfied]) => !satisfied)
    .map(([expectation]) => expectation);

  const blockingFindings: Finding[] = [];
  if (endpointIssues.length > 0) {
    blockingFindings.push({
      code: "PLATFORM_CLIENT_ENDPOINT_INVENTORY_MISMATCH",
      severity: "blocking",
      message:
        `Platform-client endpoint inventory mismatch: ${endpointIssues.join(", ")}. ` +
        "Readiness remains not_ready.",
    });
  }
  if (shapeIssues.length > 0) {
    blockingFindings.push({
      code: "PLATFORM_CLIENT_CONTRACT_SHAPE_MISMATCH",
      severity: "blocking",
      message:
        `Platform-client contract shape mismatch: ${shapeIssues.join(", ")}. ` +
        "This static check does not execute backend behavior.",
    });
  }

  return {
    checks: [
      {
        name: "platform_client_endpoint_inventory",
        status: endpointIssues.length === 0 ? "passed" : "failed",
        summary:
          endpointIssues.length === 0
            ? "All three bounded platform-client endpoints are present in OpenAPI and runtime inventories."
            : `Platform-client endpoint issues: ${endpointIssues.join(", ")}.`,
      },
      {
        name: "platform_client_contract_shape",
        status: shapeIssues.length === 0 ? "passed" : "failed",
        summary:
          shapeIssues.length === 0
            ? "The bounded assistant/search/offers schemas match the current runtime contract."
            : `Platform-client schema issues: ${shapeIssues.join(", ")}.`,
      },
      {
        name: "platform_client_runtime_semantics",
        status: "advisory",
        summary:
          "HTTP behavior is covered by backend PlatformClientContractTest; this read-only tool performs no HTTP calls.",
      },
    ],
    blockingFindings,
    advisoryFindings: [
      {
        code: "RUNTIME_HTTP_NOT_EXECUTED_BY_CONFORMANCE_TOOL",
        severity: "advisory",
        message:
          "The conformance tool validates static OpenAPI/runtime inventories and manifest references only; backend tests remain the runtime evidence.",
      },
    ],
  };
}

function sameValues(actual: string[], expected: string[]): boolean {
  return JSON.stringify([...actual].sort()) === JSON.stringify([...expected].sort());
}

function buildEndpointClassificationSummary(
  endpoints: EndpointReport[],
): EndpointClassificationSummary {
  const byClassification: Record<EndpointClassification, number> = {
    platform_client_candidate: 0,
    operational: 0,
    diagnostic_excluded: 0,
    placeholder_excluded: 0,
    runtime_only: 0,
    unclassified: 0,
  };
  let openApiOnly = 0;
  let runtimeOnly = 0;
  let inBothInventories = 0;

  for (const endpoint of endpoints) {
    byClassification[endpoint.classification] += 1;
    if (endpoint.inOpenApi && endpoint.inRuntime) {
      inBothInventories += 1;
    } else {
      if (endpoint.inOpenApi) openApiOnly += 1;
      if (endpoint.inRuntime) runtimeOnly += 1;
    }
  }

  return {
    total: endpoints.length,
    byClassification,
    openApiOnly,
    runtimeOnly,
    inBothInventories,
  };
}

function buildEndpointClassificationFindings(
  summary: EndpointClassificationSummary,
): Finding[] {
  const findings: Finding[] = [];
  if (summary.byClassification.unclassified > 0) {
    findings.push({
      code: "UNCLASSIFIED_ENDPOINTS_VISIBLE",
      severity: "advisory",
      message:
        `${summary.byClassification.unclassified} endpoints remain unclassified and not_ready.`,
    });
  }
  if (summary.byClassification.runtime_only > 0) {
    findings.push({
      code: "RUNTIME_ONLY_ENDPOINTS_VISIBLE",
      severity: "advisory",
      message:
        `${summary.byClassification.runtime_only} runtime-only endpoints remain outside the OpenAPI candidate subset.`,
    });
  }
  return findings;
}

function buildSubsetManifestFindings(
  subsetManifest: SubsetManifestState,
): Finding[] {
  if (subsetManifest.exists) return [];

  return [
    {
      code: "GENERATED_CLIENT_READY_SUBSET_MISSING",
      severity: "advisory",
      message:
        "Platform-client subset manifest is missing; readiness remains not_ready.",
    },
  ];
}

function buildFutureOnlyChecks(): CheckReport[] {
  return [
    {
      name: "generated_client_generation",
      status: "future_only",
      summary:
        "Generated-client generation is outside this read-only conformance run and was not executed.",
    },
    {
      name: "generated_client_compile",
      status: "not_run",
      summary: "Generated-client compile proof has not run.",
    },
    {
      name: "full_openapi_finalization_gate",
      status: "future_only",
      summary:
        "The whole OpenAPI document remains not_ready because diagnostic and placeholder endpoints are outside the bounded subset.",
    },
  ];
}
