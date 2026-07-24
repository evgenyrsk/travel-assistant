import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { describe, it } from "node:test";
import { loadOpenApiInventory } from "./openapi.js";
import { buildReport } from "./report.js";
import { collectRuntimeRouteInventory } from "./route-inventory.js";
import { inspectSubsetManifest, type SubsetManifestState } from "./subset-manifest.js";
import type {
  AssistantContractShape,
  ConformanceReport,
  OpenApiInventory,
  RuntimeRoute,
} from "./types.js";

describe("Platform-client conformance", () => {
  it("classifies the repository endpoint inventory without readiness promotion", () => {
    const report = repositoryReport();

    assert.equal(report.status, "not_ready");
    assert.equal(report.readinessClaim, false);
    assert.deepEqual(report.endpointClassificationSummary.byClassification, {
      platform_client_candidate: 4,
      operational: 1,
      diagnostic_excluded: 1,
      placeholder_excluded: 4,
      runtime_only: 0,
      unclassified: 0,
    });
    assert.deepEqual(report.blockingFindings, []);
    assert.ok(
      report.futureOnlyChecks.some(
        (check) =>
          check.name === "generated_client_compile" &&
          check.status === "not_run" &&
          check.summary === "Generated-client compile proof has not run.",
      ),
    );
  });

  it("validates the exact bounded platform-client schema shape", () => {
    const report = repositoryReport();

    assert.ok(
      report.checks.some(
        (check) =>
          check.name === "platform_client_endpoint_inventory" &&
          check.status === "passed",
      ),
    );
    assert.ok(
      report.checks.some(
        (check) =>
          check.name === "platform_client_contract_shape" &&
          check.status === "passed",
      ),
    );
  });

  it("validates every repository manifest endpoint against OpenAPI and runtime inventories", () => {
    const report = repositoryReport();

    assert.equal(report.manifestValidation.status, "advisory_passed");
    assert.equal(
      report.manifestValidation.endpointReferenceValidation.status,
      "passed",
    );
    assert.match(
      report.manifestValidation.endpointReferenceValidation.summary,
      /Validated 10 manifest endpoint references/,
    );
    assert.equal(report.readinessClaim, false);
  });

  it("reports platform-client schema drift as blocking", () => {
    const inventory = syntheticOpenApiInventory({
      nextActionValues: ["ask_clarification", "future_action"],
      hotelSearchIdConditional: false,
      appliedPreferencesOptional: false,
      starRatingOptional: false,
      refinementSuggestionOptional: false,
    });
    const report = buildReport(
      inventory,
      syntheticRuntimeRoutes(),
      missingSubsetManifest(),
    );

    assert.ok(
      report.blockingFindings.some(
        (finding) =>
          finding.code === "PLATFORM_CLIENT_CONTRACT_SHAPE_MISMATCH" &&
          finding.message.includes("nextAction values match runtime") &&
          finding.message.includes("hotelSearchId conditional enforced") &&
          finding.message.includes("appliedPreferences remains optional") &&
          finding.message.includes("starRating remains optional") &&
          finding.message.includes("refinementSuggestion remains optional"),
      ),
    );
    assert.equal(report.status, "not_ready");
  });

  it("reports a missing platform-client runtime endpoint as blocking", () => {
    const report = buildReport(
      syntheticOpenApiInventory(),
      syntheticRuntimeRoutes().slice(0, 2),
      missingSubsetManifest(),
    );

    assert.ok(
      report.blockingFindings.some(
        (finding) =>
          finding.code === "PLATFORM_CLIENT_ENDPOINT_INVENTORY_MISMATCH",
      ),
    );
  });

  it("blocks a manifest endpoint reference absent from runtime inventory", () => {
    const root = makeTempRepositoryRoot();
    const manifestPath = "subset.yaml";
    fs.writeFileSync(
      path.join(root, manifestPath),
      validManifest("/api/v1/missing", "missingOperation"),
      "utf8",
    );
    const report = buildReport(
      syntheticOpenApiInventory(),
      syntheticRuntimeRoutes(),
      inspectSubsetManifest(root, manifestPath),
    );

    assert.equal(report.manifestValidation.status, "failed");
    assert.ok(
      report.blockingFindings.some(
        (finding) => finding.code === "manifest_openapi_reference_missing",
      ),
    );
  });

  it("blocks readiness promotion in a manifest", () => {
    const report = reportForManifest(
      validManifest(
        "/api/v1/assistant/sessions",
        "createAssistantSession",
      ).replace("readinessClaim: false", "readinessClaim: true"),
    );

    assert.equal(report.manifestValidation.status, "failed");
    assert.ok(
      report.blockingFindings.some(
        (finding) => finding.code === "readiness_promotion_blocked",
      ),
    );
  });

  it("reports malformed manifest YAML as a structured blocking finding", () => {
    const root = makeTempRepositoryRoot();
    const manifestPath = "subset.yaml";
    fs.writeFileSync(path.join(root, manifestPath), "manifestVersion: [", "utf8");
    const report = buildReport(
      syntheticOpenApiInventory(),
      syntheticRuntimeRoutes(),
      inspectSubsetManifest(root, manifestPath),
    );

    assert.equal(report.manifestValidation.status, "failed");
    assert.ok(
      report.blockingFindings.some(
        (finding) => finding.code === "yaml_parse_error",
      ),
    );
  });

  it("reports a missing manifest without promoting readiness", () => {
    const report = buildReport(
      syntheticOpenApiInventory(),
      syntheticRuntimeRoutes(),
      missingSubsetManifest(),
    );

    assert.equal(report.manifestValidation.status, "not_run");
    assert.equal(report.readinessClaim, false);
    assert.deepEqual(report.blockingFindings, []);
  });

  it("keeps invalid CLI arguments safe and structured", () => {
    const cliUrl = new URL("./cli.js", import.meta.url);
    const result = spawnSync(process.execPath, [cliUrl.pathname, "--bad-arg"], {
      encoding: "utf8",
    });

    assert.equal(result.status, 2);
    const report = JSON.parse(result.stdout) as {
      status: string;
      readinessClaim: boolean;
    };
    assert.equal(report.status, "not_ready");
    assert.equal(report.readinessClaim, false);
  });
});

function repositoryReport(): ConformanceReport {
  const repositoryRoot = path.resolve(process.cwd(), "../..");
  return buildReport(
    loadOpenApiInventory(repositoryRoot),
    collectRuntimeRouteInventory(repositoryRoot),
    inspectSubsetManifest(repositoryRoot),
  );
}

function syntheticOpenApiInventory(
  overrides: Partial<AssistantContractShape> = {},
): OpenApiInventory {
  return {
    sourcePath: "docs/architecture/stage-6/openapi-draft.yaml",
    detectedFromCandidates: [],
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
      {
        method: "get",
        path: "/hotel-searches/{searchId}/offers",
        fullPath: "/api/v1/hotel-searches/{searchId}/offers",
        operationId: "getHotelOffers",
      },
      {
        method: "get",
        path: "/hotel-searches/{searchId}/offers/{offerId}/details",
        fullPath:
          "/api/v1/hotel-searches/{searchId}/offers/{offerId}/details",
        operationId: "getHotelOfferDetails",
      },
    ],
    assistantContractShape: {
      createSessionRequestBodyOptional: true,
      continueSessionRequestBodyRequired: true,
      messagePropertyPresent: true,
      messageRequired: true,
      clientContextOptional: true,
      nextActionPropertyPresent: true,
      nextActionRequired: true,
      sessionNotFoundResponsePresent: true,
      validationErrorResponsesPresent: true,
      messageMaxLength: 4_000,
      requestAdditionalPropertiesForbidden: true,
      responseAdditionalPropertiesForbidden: true,
      nextActionValues: [
        "ask_clarification",
        "show_boundary_message",
        "show_hotel_results",
      ],
      hotelSearchIdPropertyPresent: true,
      hotelSearchIdConditional: true,
      sessionRequiredFields: ["createdAt", "sessionId", "status", "updatedAt"],
      messageResponseRequiredFields: ["content", "role"],
      offersOperationPresent: true,
      offersNotFoundResponsePresent: true,
      offersRequiredFields: [
        "metadata",
        "offers",
        "providerFacts",
        "searchId",
        "status",
      ],
      offersAdditionalPropertiesForbidden: true,
      detailsOperationPresent: true,
      detailsNotFoundResponsePresent: true,
      detailsInvalidResponsePresent: true,
      detailsUnavailableResponsePresent: true,
      detailsRequiredFields: ["hotelName", "metadata"],
      detailsFields: [
        "amenityGroups",
        "checkInTime",
        "checkOutTime",
        "descriptionSections",
        "hotelChain",
        "hotelName",
        "imageUrls",
        "location",
        "metadata",
        "paymentMethods",
        "starRating",
      ],
      detailsAdditionalPropertiesForbidden: true,
      searchRequiredFields: [
        "criteria",
        "metadata",
        "searchId",
        "sessionId",
        "status",
      ],
      searchStatusValues: ["completed_no_offers", "completed_with_offers"],
      searchAdditionalPropertiesForbidden: true,
      offersStatusValues: ["completed_no_offers", "completed_with_offers"],
      metadataRequiredFields: [
        "freshness",
        "providerState",
        "resultCompleteness",
        "warnings",
      ],
      metadataAdditionalPropertiesForbidden: true,
      hotelOfferRequiredFields: [
        "availability",
        "freshness",
        "hotelName",
        "location",
        "matchSummary",
        "offerId",
        "price",
        "providerFacts",
        "source",
      ],
      hotelOfferAdditionalPropertiesForbidden: true,
      ratingOptional: true,
      amenitiesOptional: true,
      starRatingOptional: true,
      freeCancellationUntilOptional: true,
      imageUrlOptional: true,
      breakfastIncludedOptional: true,
      appliedPreferencesOptional: true,
      appliedPreferencesFields: [
        "breakfastIncludedRequired",
        "freeCancellationRequired",
        "maxTotalPrice",
        "minimumGuestRating",
        "stars",
      ],
      appliedPreferencesAdditionalPropertiesForbidden: true,
      refinementSuggestionOptional: true,
      refinementSuggestionRequiredFields: ["message", "preference", "type"],
      refinementSuggestionTypeValues: ["relax_preference"],
      refinementSuggestionPreferenceValues: [
        "breakfastIncludedRequired",
        "freeCancellationRequired",
        "maxTotalPrice",
        "minimumGuestRating",
        "stars",
      ],
      refinementSuggestionAdditionalPropertiesForbidden: true,
      ...overrides,
    },
  };
}

function syntheticRuntimeRoutes(): RuntimeRoute[] {
  return [
    {
      method: "post",
      path: "/api/v1/assistant/sessions",
      sourceFile: "AssistantPlaceholderRoutes.kt",
      line: 1,
    },
    {
      method: "post",
      path: "/api/v1/assistant/sessions/{sessionId}/messages",
      sourceFile: "AssistantPlaceholderRoutes.kt",
      line: 2,
    },
    {
      method: "get",
      path: "/api/v1/hotel-searches/{searchId}/offers",
      sourceFile: "HotelSearchRoutes.kt",
      line: 3,
    },
    {
      method: "get",
      path: "/api/v1/hotel-searches/{searchId}/offers/{offerId}/details",
      sourceFile: "HotelDetailsRoutes.kt",
      line: 4,
    },
  ];
}

function missingSubsetManifest(): SubsetManifestState {
  return {
    path: "docs/architecture/stage-7/generated-client-ready-subset.yaml",
    exists: false,
    status: "missing_not_created",
    requiredForSkeleton: false,
    endpointReferences: [],
    manifestDetection: {
      manifestPath: "docs/architecture/stage-7/generated-client-ready-subset.yaml",
      exists: false,
      explicitPathProvided: false,
      status: "missing",
      note: "Manifest is missing.",
    },
    manifestValidation: {
      status: "not_run",
      reason: "manifest_missing",
      schemaValidation: {
        name: "manifest_schema_validation",
        status: "not_run",
        summary: "Manifest is missing.",
      },
      endpointReferenceValidation: {
        name: "endpoint_reference_validation",
        status: "future_only",
        summary: "Manifest is missing.",
      },
      findings: [],
    },
  };
}

function makeTempRepositoryRoot(): string {
  return fs.mkdtempSync(path.join(os.tmpdir(), "openapi-conformance-"));
}

function reportForManifest(manifest: string): ConformanceReport {
  const root = makeTempRepositoryRoot();
  const manifestPath = "subset.yaml";
  fs.writeFileSync(path.join(root, manifestPath), manifest, "utf8");
  return buildReport(
    syntheticOpenApiInventory(),
    syntheticRuntimeRoutes(),
    inspectSubsetManifest(root, manifestPath),
  );
}

function validManifest(endpointPath: string, operationId: string): string {
  return [
    'manifestVersion: "stage-10-platform-client-contract-subset-v1"',
    'scopeName: "test-subset"',
    'status: "not_ready"',
    "readinessClaim: false",
    'openApiSource: "docs/architecture/stage-6/openapi-draft.yaml"',
    "validationStatus:",
    "  readinessClaim: false",
    '  status: "not_ready"',
    '  schemaValidation: "not_run"',
    '  endpointReferenceValidation: "not_run"',
    "includedEndpoints:",
    '  - method: "POST"',
    `    path: "${endpointPath}"`,
    `    operationId: "${operationId}"`,
    '    classification: "platform_client_candidate"',
    '    readiness: "not_ready"',
    "excludedEndpoints: []",
    "classificationPolicy: {}",
    "readinessCriteria:",
    "  generatedClientCompilePassed: false",
    "knownLimitations: []",
    "generatedClientTargets: []",
  ].join("\n");
}
