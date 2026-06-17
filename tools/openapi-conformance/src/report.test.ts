import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { describe, it } from "node:test";
import { loadOpenApiInventory } from "./openapi.js";
import { buildReport } from "./report.js";
import { collectRuntimeRouteInventory } from "./route-inventory.js";
import type {
  AssistantContractShape,
  ConformanceReport,
  OpenApiInventory,
  RuntimeRoute,
} from "./types.js";
import { inspectSubsetManifest } from "./subset-manifest.js";
import type { SubsetManifestState } from "./subset-manifest.js";

describe("buildReport", () => {
  it("preserves not_ready readiness semantics while reporting endpoint classification counts", () => {
    const report = buildReport(openApiInventory(), runtimeRoutes(), missingSubsetManifest());

    assert.equal(report.status, "not_ready");
    assert.equal(report.readinessClaim, false);
    assert.deepEqual(report.blockingFindings, []);
    assert.deepEqual(report.endpointClassificationSummary.byClassification, {
      foundation_candidate: 1,
      placeholder_excluded: 1,
      runtime_only: 1,
      unclassified: 1,
    });
    assert.equal(report.endpointClassificationSummary.total, 4);
    assert.equal(report.endpointClassificationSummary.openApiOnly, 1);
    assert.equal(report.endpointClassificationSummary.runtimeOnly, 1);
    assert.equal(report.endpointClassificationSummary.inBothInventories, 2);
    assert.ok(
      report.checks.some(
        (check) =>
          check.name === "endpoint_classification_summary" &&
          check.status === "advisory",
      ),
    );
  });

  it("keeps unclassified and runtime-only drift visible as advisory findings", () => {
    const report = buildReport(openApiInventory(), runtimeRoutes(), missingSubsetManifest());

    assert.deepEqual(
      report.advisoryFindings
        .filter((finding) =>
          ["UNCLASSIFIED_ENDPOINTS_VISIBLE", "RUNTIME_ONLY_ENDPOINTS_VISIBLE"].includes(
            finding.code,
          ),
        )
        .map((finding) => finding.severity),
      ["advisory", "advisory"],
    );
    assert.ok(
      report.endpoints.every((endpoint) => endpoint.readiness === "not_ready"),
    );
  });

  it("reports missing manifest detection without readiness promotion", () => {
    const repositoryRoot = makeTempRepositoryRoot();
    const subsetManifest = inspectSubsetManifest(repositoryRoot);
    const report = buildReport(openApiInventory(), runtimeRoutes(), subsetManifest);

    assert.equal(report.manifestDetection.manifestPath, "docs/architecture/stage-7/generated-client-ready-subset.yaml");
    assert.equal(report.manifestDetection.exists, false);
    assert.equal(report.manifestDetection.status, "missing");
    assert.equal(report.manifestValidation.status, "not_run");
    assert.equal(report.manifestValidation.reason, "manifest_missing");
    assert.equal(report.status, "not_ready");
    assert.equal(report.readinessClaim, false);
    assert.deepEqual(report.blockingFindings, []);
    assert.ok(
      report.advisoryFindings.some(
        (finding) => finding.code === "manifest_missing",
      ),
    );
  });

  it("validates a present skeleton manifest without readiness promotion", () => {
    const repositoryRoot = makeTempRepositoryRoot();
    const manifestPath = "tmp-generated-client-ready-subset.yaml";
    fs.writeFileSync(
      path.join(repositoryRoot, manifestPath),
      validSkeletonManifest(),
      "utf8",
    );

    const subsetManifest = inspectSubsetManifest(repositoryRoot, manifestPath);
    const report = buildReport(openApiInventory(), runtimeRoutes(), subsetManifest);

    assert.equal(report.manifestDetection.exists, true);
    assert.equal(report.manifestDetection.status, "present");
    assert.equal(report.manifestValidation.status, "advisory_passed");
    assert.equal(report.manifestValidation.schemaValidation.status, "passed");
    assert.equal(report.manifestValidation.endpointReferenceValidation.status, "future_only");
    assert.equal(report.subsetManifest.status, "present_not_evaluated");
    assert.equal(report.status, "not_ready");
    assert.equal(report.readinessClaim, false);
    assert.deepEqual(report.blockingFindings, []);
    assert.ok(
      report.advisoryFindings.some(
        (finding) => finding.code === "readiness_promotion_blocked",
      ),
    );
  });

  it("blocks top-level readiness promotion fields in manifest candidates", () => {
    const report = reportForManifest(
      validSkeletonManifest()
        .replace('status: "not_ready"', 'status: "ready"')
        .replace("readinessClaim: false", "readinessClaim: true"),
    );

    assertReadinessPromotionBlocked(report);
  });

  it("blocks validationStatus readiness promotion fields in manifest candidates", () => {
    const report = reportForManifest(
      validSkeletonManifest()
        .replace("  readinessClaim: false", "  readinessClaim: true")
        .replace('  status: "not_ready"', '  status: "ready"'),
    );

    assertReadinessPromotionBlocked(report);
  });

  it("blocks endpoint readiness promotion in manifest candidates", () => {
    const report = reportForManifest(
      validSkeletonManifest().replace(
        '    readiness: "not_ready"',
        '    readiness: "ready"',
      ),
    );

    assertReadinessPromotionBlocked(report);
  });

  it("blocks readiness criteria promotion in manifest candidates", () => {
    const report = reportForManifest(
      validSkeletonManifest().replace(
        "  generatedClientCompilePassed: false",
        "  generatedClientCompilePassed: true",
      ),
    );

    assertReadinessPromotionBlocked(report);
  });

  it("validates the repository manifest candidate without readiness promotion", () => {
    const repositoryRoot = path.resolve(process.cwd(), "../..");
    const subsetManifest = inspectSubsetManifest(repositoryRoot);
    const report = buildReport(openApiInventory(), runtimeRoutes(), subsetManifest);

    assert.equal(report.manifestDetection.exists, true);
    assert.equal(report.manifestDetection.status, "present");
    assert.equal(report.manifestValidation.status, "advisory_passed");
    assert.equal(report.status, "not_ready");
    assert.equal(report.readinessClaim, false);
    assert.deepEqual(report.blockingFindings, []);
  });

  it("checks repository Assistant candidates and contract shape without readiness promotion", () => {
    const repositoryRoot = path.resolve(process.cwd(), "../..");
    const openApi = loadOpenApiInventory(repositoryRoot);
    const routes = collectRuntimeRouteInventory(repositoryRoot);
    const subsetManifest = inspectSubsetManifest(repositoryRoot);
    const report = buildReport(openApi, routes, subsetManifest);

    assert.ok(
      report.checks.some(
        (check) =>
          check.name === "assistant_endpoint_candidate_inventory" &&
          check.status === "passed",
      ),
    );
    assert.ok(
      report.checks.some(
        (check) =>
          check.name === "assistant_endpoint_contract_shape" &&
          check.status === "passed",
      ),
    );
    assert.ok(
      report.checks.some(
        (check) =>
          check.name === "assistant_endpoint_runtime_semantics" &&
          check.status === "advisory",
      ),
    );
    assert.ok(
      report.advisoryFindings.some(
        (finding) =>
          finding.code === "ASSISTANT_RUNTIME_SEMANTICS_NOT_CHECKED",
      ),
    );
    assert.equal(report.status, "not_ready");
    assert.equal(report.readinessClaim, false);
    assert.deepEqual(report.blockingFindings, []);
  });

  it("reports Assistant contract shape drift as blocking without readiness promotion", () => {
    const report = buildReport(
      assistantOpenApiInventory({
        nextActionRequired: false,
      }),
      assistantRuntimeRoutes(),
      missingSubsetManifest(),
    );

    assert.ok(
      report.checks.some(
        (check) =>
          check.name === "assistant_endpoint_contract_shape" &&
          check.status === "failed",
      ),
    );
    assert.ok(
      report.blockingFindings.some(
        (finding) =>
          finding.code === "ASSISTANT_ENDPOINT_CONTRACT_SHAPE_MISMATCH",
      ),
    );
    assert.equal(report.status, "not_ready");
    assert.equal(report.readinessClaim, false);
  });

  it("keeps Assistant validation and maxLength runtime semantics advisory-only", () => {
    const report = buildReport(
      assistantOpenApiInventory({
        validationErrorResponsesPresent: false,
        messageMaxLength: undefined,
      }),
      assistantRuntimeRoutes(),
      missingSubsetManifest(),
    );

    assert.deepEqual(report.blockingFindings, []);
    assert.ok(
      report.checks.some(
        (check) =>
          check.name === "assistant_endpoint_runtime_semantics" &&
          check.status === "advisory" &&
          check.summary.includes("message.maxLength is not declared"),
      ),
    );
    assert.equal(report.status, "not_ready");
    assert.equal(report.readinessClaim, false);
  });

  it("reports structured schema errors for an invalid skeleton manifest", () => {
    const repositoryRoot = makeTempRepositoryRoot();
    const manifestPath = "invalid-generated-client-ready-subset.yaml";
    fs.writeFileSync(
      path.join(repositoryRoot, manifestPath),
      [
        'scopeName: "travel-assistant-stage-7-foundation-subset"',
        'openApiSource: "docs/architecture/stage-6/openapi-draft.yaml"',
        "validationStatus:",
        "  readinessClaim: false",
        '  status: "not_ready"',
        "includedEndpoints: []",
        "excludedEndpoints: []",
        "classificationPolicy: {}",
        "readinessCriteria: {}",
        "knownLimitations: []",
        'generatedClientTargets: "not-an-array"',
      ].join("\n"),
      "utf8",
    );

    const subsetManifest = inspectSubsetManifest(repositoryRoot, manifestPath);
    const report = buildReport(openApiInventory(), runtimeRoutes(), subsetManifest);

    assert.equal(report.manifestValidation.status, "failed");
    assert.equal(report.status, "not_ready");
    assert.equal(report.readinessClaim, false);
    assert.ok(
      report.blockingFindings.some(
        (finding) => finding.code === "schema_violation",
      ),
    );
  });

  it("reports YAML parse errors as structured manifest validation findings", () => {
    const repositoryRoot = makeTempRepositoryRoot();
    const manifestPath = "parse-error-generated-client-ready-subset.yaml";
    fs.writeFileSync(path.join(repositoryRoot, manifestPath), "manifestVersion: [", "utf8");

    const subsetManifest = inspectSubsetManifest(repositoryRoot, manifestPath);
    const report = buildReport(openApiInventory(), runtimeRoutes(), subsetManifest);

    assert.equal(report.manifestValidation.status, "failed");
    assert.ok(
      report.blockingFindings.some(
        (finding) => finding.code === "yaml_parse_error",
      ),
    );
    assert.equal(report.status, "not_ready");
    assert.equal(report.readinessClaim, false);
  });

  it("keeps bad argument behavior as exit code 2 with structured not_ready JSON", () => {
    const cliUrl = new URL("./cli.js", import.meta.url);
    const result = spawnSync(process.execPath, [cliUrl.pathname, "--bad-arg"], {
      encoding: "utf8",
    });

    assert.equal(result.status, 2);
    const report = JSON.parse(result.stdout) as { status: string; readinessClaim: boolean };
    assert.equal(report.status, "not_ready");
    assert.equal(report.readinessClaim, false);
  });
});

function openApiInventory(): OpenApiInventory {
  return {
    sourcePath: "docs/architecture/stage-6/openapi-draft.yaml",
    detectedFromCandidates: [
      {
        path: "docs/architecture/stage-6/openapi-draft.yaml",
        exists: true,
      },
    ],
    openApiVersion: "3.1.0",
    serverBasePath: "/api/v1",
    operations: [
      {
        method: "get",
        path: "/health",
        fullPath: "/api/v1/health",
        operationId: "getHealth",
      },
      {
        method: "post",
        path: "/hotel-searches",
        fullPath: "/api/v1/hotel-searches",
        operationId: "createHotelSearch",
      },
      {
        method: "get",
        path: "/future-report-only",
        fullPath: "/api/v1/future-report-only",
        operationId: "futureReportOnly",
      },
    ],
  };
}

function runtimeRoutes(): RuntimeRoute[] {
  return [
    {
      method: "get",
      path: "/api/v1/health",
      sourceFile: "services/backend/src/main/kotlin/com/travelassistant/backend/api/HealthRoutes.kt",
      line: 8,
    },
    {
      method: "post",
      path: "/api/v1/hotel-searches",
      sourceFile: "services/backend/src/main/kotlin/com/travelassistant/backend/api/HotelSearchPlaceholderRoutes.kt",
      line: 10,
    },
    {
      method: "get",
      path: "/api/v1/runtime-only",
      sourceFile: "services/backend/src/main/kotlin/com/travelassistant/backend/api/RuntimeOnlyRoutes.kt",
      line: 12,
    },
  ];
}

function assistantOpenApiInventory(
  overrides: Partial<AssistantContractShape> = {},
): OpenApiInventory {
  return {
    sourcePath: "docs/architecture/stage-6/openapi-draft.yaml",
    detectedFromCandidates: [
      {
        path: "docs/architecture/stage-6/openapi-draft.yaml",
        exists: true,
      },
    ],
    openApiVersion: "3.1.0",
    serverBasePath: "/api/v1",
    operations: [
      {
        method: "post",
        path: "/assistant/sessions",
        fullPath: "/api/v1/assistant/sessions",
        operationId: "createAssistantSession",
      },
      {
        method: "post",
        path: "/assistant/sessions/{sessionId}/messages",
        fullPath: "/api/v1/assistant/sessions/{sessionId}/messages",
        operationId: "continueAssistantSession",
      },
    ],
    assistantContractShape: {
      createSessionRequestBodyOptional: true,
      continueSessionRequestBodyRequired: true,
      messageRequired: true,
      clientContextOptional: true,
      nextActionRequired: true,
      sessionNotFoundResponsePresent: true,
      validationErrorResponsesPresent: true,
      messageMaxLength: 4000,
      ...overrides,
    },
  };
}

function assistantRuntimeRoutes(): RuntimeRoute[] {
  return [
    {
      method: "post",
      path: "/api/v1/assistant/sessions",
      sourceFile: "services/backend/src/main/kotlin/com/travelassistant/backend/api/AssistantPlaceholderRoutes.kt",
      line: 26,
    },
    {
      method: "post",
      path: "/api/v1/assistant/sessions/{sessionId}/messages",
      sourceFile: "services/backend/src/main/kotlin/com/travelassistant/backend/api/AssistantPlaceholderRoutes.kt",
      line: 59,
    },
  ];
}

function missingSubsetManifest(): SubsetManifestState {
  return {
    path: "docs/architecture/stage-7/generated-client-ready-subset.yaml",
    exists: false,
    status: "missing_not_created",
    requiredForSkeleton: false,
    manifestDetection: {
      manifestPath: "docs/architecture/stage-7/generated-client-ready-subset.yaml",
      exists: false,
      explicitPathProvided: false,
      status: "missing",
      note: "Generated-client-ready subset manifest is missing/not_created; this is expected for the skeleton and keeps readiness not_ready.",
    },
    manifestValidation: {
      status: "not_run",
      reason: "manifest_missing",
      schemaValidation: {
        name: "manifest_schema_validation",
        status: "not_run",
        summary: "Manifest schema validation was not run because the manifest is missing/not_created.",
      },
      endpointReferenceValidation: {
        name: "endpoint_reference_validation",
        status: "future_only",
        summary: "Endpoint reference validation is future-only until a manifest exists.",
      },
      findings: [
        {
          code: "manifest_missing",
          severity: "advisory",
          message:
            "Generated-client-ready subset manifest is missing/not_created; readiness remains not_ready.",
        },
      ],
    },
  };
}

function makeTempRepositoryRoot(): string {
  return fs.mkdtempSync(path.join(os.tmpdir(), "openapi-conformance-manifest-"));
}

function reportForManifest(manifestText: string): ConformanceReport {
  const repositoryRoot = makeTempRepositoryRoot();
  const manifestPath = "tmp-generated-client-ready-subset.yaml";
  fs.writeFileSync(path.join(repositoryRoot, manifestPath), manifestText, "utf8");
  const subsetManifest = inspectSubsetManifest(repositoryRoot, manifestPath);
  return buildReport(openApiInventory(), runtimeRoutes(), subsetManifest);
}

function assertReadinessPromotionBlocked(report: ConformanceReport): void {
  assert.equal(report.manifestValidation.status, "failed");
  assert.equal(report.status, "not_ready");
  assert.equal(report.readinessClaim, false);
  assert.ok(
    report.blockingFindings.some(
      (finding) => finding.code === "readiness_promotion_blocked",
    ),
  );
}

function validSkeletonManifest(): string {
  return [
    'manifestVersion: "stage-7-generated-client-ready-subset-v1"',
    'scopeName: "travel-assistant-stage-7-foundation-subset"',
    'status: "not_ready"',
    "readinessClaim: false",
    'openApiSource: "docs/architecture/stage-6/openapi-draft.yaml"',
    "validationStatus:",
    "  readinessClaim: false",
    '  status: "not_ready"',
    '  schemaValidation: "not_run"',
    '  endpointReferenceValidation: "not_run"',
    '  generatedClientCompile: "not_run"',
    '  runtimeContractValidation: "not_run"',
    "  lastValidatedBy: null",
    "  lastValidatedAt: null",
    "includedEndpoints:",
    '  - method: "GET"',
    '    path: "/api/v1/health"',
    '    operationId: "getHealth"',
    '    classification: "foundation_candidate"',
    '    readiness: "not_ready"',
    '    inclusionReason: "candidate_for_future_low_risk_foundation_subset_validation"',
    "    requiredChecks:",
    '      - "openapi_source_identified"',
    '      - "runtime_route_present"',
    '      - "response_schema_validated"',
    '      - "generated_client_compile_passed"',
    '      - "runtime_contract_checks_passed"',
    "    unresolvedBlockers:",
    '      - "response_schema_not_validated_by_runtime_contract_check"',
    '      - "generated_client_compile_not_run"',
    '      - "runtime_contract_checks_not_run"',
    "excludedEndpoints:",
    '  - method: "POST"',
    '    path: "/api/v1/hotel-searches"',
    '    operationId: "createHotelSearch"',
    '    classification: "placeholder_excluded"',
    '    readiness: "not_ready"',
    '    exclusionReason: "placeholder_501_not_implemented_hotel_search"',
    "    requiredBeforeInclusion:",
    '      - "real_hotel_search_behavior"',
    '      - "runtime_contract_checks"',
    '      - "generated_client_compile_check"',
    "classificationPolicy:",
    '  placeholderEndpoints: "exclude_until_contract_aligned"',
    '  foundationCandidates: "candidate_only_not_ready"',
    '  runtimeOnlyRoutes: "must_be_classified_before_readiness"',
    '  unclassifiedEndpoints: "block_readiness"',
    "readinessCriteria:",
    "  openApiSourceValidated: false",
    "  manifestSchemaValidated: false",
    "  allIncludedEndpointsInOpenApi: false",
    "  allIncludedEndpointsInRuntimeInventory: false",
    "  noPlaceholderEndpointsIncluded: false",
    "  allRuntimeOnlyRoutesClassified: false",
    "  allUnclassifiedEndpointsResolved: false",
    "  includedEndpointSuccessSchemasValidated: false",
    "  includedEndpointErrorTaxonomyValidated: false",
    "  generatedClientTargetDeclared: false",
    "  generatedClientGenerationConfigured: false",
    "  generatedClientCompilePassed: false",
    "  runtimeContractChecksPassed: false",
    "knownLimitations:",
    '  - code: "runtime_contract_checks_not_run"',
    '    severity: "blocking_before_readiness"',
    '    description: "Runtime HTTP contract checks are future-only and have not run."',
    "    blocksReadiness: true",
    "generatedClientTargets: []",
    "notes:",
    '  - "This manifest is a non-readiness candidate baseline, not a readiness certificate."',
  ].join("\n");
}
