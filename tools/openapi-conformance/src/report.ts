import { TOOL_VERSION } from "./paths.js";
import { buildEndpointReports } from "./placeholder-policy.js";
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
import type { SubsetManifestState } from "./subset-manifest.js";

export function buildReport(
  openApiInventory: OpenApiInventory,
  runtimeRoutes: RuntimeRoute[],
  subsetManifest: SubsetManifestState,
): ConformanceReport {
  const endpoints = buildEndpointReports(openApiInventory.operations, runtimeRoutes);
  const assistantCandidateChecks = buildAssistantCandidateChecks(
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
      code: "PLACEHOLDER_ENDPOINTS_EXCLUDED",
      severity: "advisory",
      message:
        "Hotel search, offers, shortlist, and explanation placeholder endpoints remain excluded and are not generated-client-ready.",
    },
    ...buildEndpointClassificationFindings(endpointClassificationSummary),
    ...assistantCandidateChecks.advisoryFindings,
    ...manifestAdvisoryFindings,
  ];

  const futureOnlyChecks = buildFutureOnlyChecks();

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
      assistantCandidateChecks.checks,
    ),
    blockingFindings: [
      ...manifestBlockingFindings,
      ...assistantCandidateChecks.blockingFindings,
    ],
    advisoryFindings,
    futureOnlyChecks,
  };
}

function buildChecks(
  openApiInventory: OpenApiInventory,
  runtimeRoutes: RuntimeRoute[],
  subsetManifest: SubsetManifestState,
  endpointClassificationSummary: EndpointClassificationSummary,
  assistantChecks: CheckReport[],
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
        ? "Subset manifest exists but is not enforced by the skeleton."
        : "Subset manifest is missing/not_created and is optional for this skeleton.",
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
        subsetManifest.manifestValidation.schemaValidation.summary,
    },
    {
      name: "endpoint_classification_summary",
      status: "advisory",
      summary:
        `${endpointClassificationSummary.total} endpoints classified: ` +
        `${endpointClassificationSummary.byClassification.foundation_candidate} foundation_candidate, ` +
        `${endpointClassificationSummary.byClassification.placeholder_excluded} placeholder_excluded, ` +
        `${endpointClassificationSummary.byClassification.runtime_only} runtime_only, ` +
        `${endpointClassificationSummary.byClassification.unclassified} unclassified.`,
    },
    ...assistantChecks,
    {
      name: "readiness_status",
      status: "not_ready",
      summary:
        "Report status is intentionally not_ready; generated-client/OpenAPI readiness is not claimed.",
    },
  ];
}

interface AssistantCandidateChecks {
  checks: CheckReport[];
  blockingFindings: Finding[];
  advisoryFindings: Finding[];
}

function buildAssistantCandidateChecks(
  openApiInventory: OpenApiInventory,
  endpoints: EndpointReport[],
): AssistantCandidateChecks {
  const shape = openApiInventory.assistantContractShape;
  if (!shape) {
    return {
      checks: [],
      blockingFindings: [],
      advisoryFindings: [],
    };
  }

  const expectedEndpoints = [
    "POST /api/v1/assistant/sessions",
    "POST /api/v1/assistant/sessions/{sessionId}/messages",
  ];
  const endpointIssues = expectedEndpoints.filter((expected) => {
    const [method, path] = expected.split(" ", 2);
    const endpoint = endpoints.find(
      (candidate) =>
        candidate.method === method.toLowerCase() &&
        candidate.path === path,
    );

    return (
      endpoint === undefined ||
      !endpoint.inOpenApi ||
      !endpoint.inRuntime ||
      endpoint.classification !== "foundation_candidate" ||
      endpoint.readiness !== "not_ready"
    );
  });

  const shapeExpectations: Array<[string, boolean]> = [
    ["create-session requestBody optional", shape.createSessionRequestBodyOptional],
    ["message requestBody required", shape.continueSessionRequestBodyRequired],
    ["AssistantMessageRequest.message property present", shape.messagePropertyPresent],
    ["AssistantMessageRequest.message required", shape.messageRequired],
    ["AssistantMessageRequest.clientContext optional", shape.clientContextOptional],
    [
      "AssistantMessageResponse.nextAction property present",
      shape.nextActionPropertyPresent,
    ],
    ["AssistantMessageResponse.nextAction required", shape.nextActionRequired],
    ["message endpoint 404 response present", shape.sessionNotFoundResponsePresent],
  ];
  const shapeIssues = shapeExpectations
    .filter(([, satisfied]) => !satisfied)
    .map(([expectation]) => expectation);

  const blockingFindings: Finding[] = [];
  if (endpointIssues.length > 0) {
    blockingFindings.push({
      code: "ASSISTANT_ENDPOINT_CANDIDATE_INVENTORY_MISMATCH",
      severity: "blocking",
      message:
        `Assistant endpoint candidate inventory mismatch: ${endpointIssues.join(", ")}. ` +
        "Generated-client readiness remains not_ready.",
    });
  }
  if (shapeIssues.length > 0) {
    blockingFindings.push({
      code: "ASSISTANT_ENDPOINT_CONTRACT_SHAPE_MISMATCH",
      severity: "blocking",
      message:
        `Assistant endpoint contract shape mismatch: ${shapeIssues.join(", ")}. ` +
        "This static check does not validate runtime behavior.",
    });
  }

  const validationSummary = shape.validationErrorResponsesPresent
    ? "Both Assistant operations expose a 400 validation-error response."
    : "One or more Assistant operations do not expose a 400 validation-error response.";
  const maxLengthSummary =
    shape.messageMaxLength === undefined
      ? "message.maxLength is not declared."
      : `message.maxLength is declared as ${shape.messageMaxLength}.`;

  return {
    checks: [
      {
        name: "assistant_endpoint_candidate_inventory",
        status: endpointIssues.length === 0 ? "passed" : "failed",
        summary:
          endpointIssues.length === 0
            ? "Assistant foundation candidates are present in OpenAPI and static runtime inventories with not_ready readiness."
            : `Assistant endpoint candidate inventory issues: ${endpointIssues.join(", ")}.`,
      },
      {
        name: "assistant_endpoint_contract_shape",
        status: shapeIssues.length === 0 ? "passed" : "failed",
        summary:
          shapeIssues.length === 0
            ? "Assistant request/response contract shape matches the bounded Stage 7.39 candidate expectations."
            : `Assistant contract shape issues: ${shapeIssues.join(", ")}.`,
      },
      {
        name: "assistant_endpoint_runtime_semantics",
        status: "advisory",
        summary:
          `${validationSummary} ${maxLengthSummary} ` +
          "clientContext behavior, empty-object validation, malformed/unknown JSON, and maxLength enforcement are not checked by this static tool.",
      },
    ],
    blockingFindings,
    advisoryFindings: [
      {
        code: "ASSISTANT_RUNTIME_SEMANTICS_NOT_CHECKED",
        severity: "advisory",
        message:
          "Assistant runtime semantics remain covered by backend tests or future-only decisions; this tool performs no HTTP calls and makes no runtime or readiness claim.",
      },
    ],
  };
}

function buildEndpointClassificationSummary(
  endpoints: EndpointReport[],
): EndpointClassificationSummary {
  const byClassification: Record<EndpointClassification, number> = {
    foundation_candidate: 0,
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
      continue;
    }

    if (endpoint.inOpenApi) {
      openApiOnly += 1;
    }

    if (endpoint.inRuntime) {
      runtimeOnly += 1;
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
        `${summary.byClassification.unclassified} endpoints are unclassified in the skeleton report; ` +
        "they remain not_ready and require a future explicit subset/classification decision.",
    });
  }

  if (summary.byClassification.runtime_only > 0) {
    findings.push({
      code: "RUNTIME_ONLY_ENDPOINTS_VISIBLE",
      severity: "advisory",
      message:
        `${summary.byClassification.runtime_only} runtime-only endpoints are visible in the skeleton report; ` +
        "static inventory drift is advisory until a future conformance mode is activated.",
    });
  }

  return findings;
}

function buildSubsetManifestFindings(
  subsetManifest: SubsetManifestState,
): Finding[] {
  if (subsetManifest.exists) {
    return [];
  }

  return [
    {
      code: "GENERATED_CLIENT_READY_SUBSET_MISSING",
      severity: "advisory",
      message:
        "Generated-client-ready subset manifest is not created; this is expected for the skeleton and keeps readiness not_ready.",
    },
  ];
}

function buildFutureOnlyChecks(): CheckReport[] {
  return [
    {
      name: "generated_client_generation",
      status: "future_only",
      summary: "Generated clients are not generated by this skeleton.",
    },
    {
      name: "generated_client_compile",
      status: "not_run",
      summary:
        "Generated-client compile check is future-only and was not run.",
    },
    {
      name: "runtime_http_contract_tests",
      status: "not_run",
      summary:
        "Runtime HTTP contract tests are future-only and were not run.",
    },
    {
      name: "full_openapi_finalization_gate",
      status: "future_only",
      summary:
        "Full OpenAPI finalization remains blocked by missing real hotel search/resource behavior.",
    },
  ];
}
