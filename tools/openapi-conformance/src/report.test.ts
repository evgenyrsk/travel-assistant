import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { describe, it } from "node:test";
import { buildReport } from "./report.js";
import type { OpenApiInventory, RuntimeRoute } from "./types.js";
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
    assert.equal(report.status, "not_ready");
    assert.equal(report.readinessClaim, false);
    assert.deepEqual(report.blockingFindings, []);
    assert.ok(
      report.advisoryFindings.some(
        (finding) => finding.code === "readiness_promotion_blocked",
      ),
    );
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

function validSkeletonManifest(): string {
  return [
    'manifestVersion: "stage-7-generated-client-ready-subset-v1"',
    'scopeName: "travel-assistant-stage-7-foundation-subset"',
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
    "includedEndpoints: []",
    "excludedEndpoints: []",
    "classificationPolicy:",
    '  placeholderEndpoints: "exclude_until_contract_aligned"',
    '  foundationCandidates: "candidate_only_not_ready"',
    '  runtimeOnlyRoutes: "must_be_classified_before_readiness"',
    '  unclassifiedEndpoints: "block_readiness"',
    "readinessCriteria: {}",
    "knownLimitations: []",
    "generatedClientTargets: []",
  ].join("\n");
}
