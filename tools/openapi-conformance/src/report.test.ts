import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { buildReport } from "./report.js";
import type { OpenApiInventory, RuntimeRoute } from "./types.js";
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
  };
}
