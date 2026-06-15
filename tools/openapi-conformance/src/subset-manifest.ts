import fs from "node:fs";
import path from "node:path";
import { parse } from "yaml";
import { DEFAULT_SUBSET_MANIFEST } from "./paths.js";
import type {
  Finding,
  ManifestDetectionReport,
  ManifestValidationReport,
} from "./types.js";

export interface SubsetManifestState {
  path: string;
  exists: boolean;
  status: "missing_not_created" | "present_not_evaluated";
  requiredForSkeleton: false;
  manifestDetection: ManifestDetectionReport;
  manifestValidation: ManifestValidationReport;
}

export function inspectSubsetManifest(
  repositoryRoot: string,
  manifestPath = DEFAULT_SUBSET_MANIFEST,
): SubsetManifestState {
  const absolutePath = path.resolve(repositoryRoot, manifestPath);
  const exists = fs.existsSync(absolutePath);
  const manifestDetection = buildManifestDetection(manifestPath, exists);
  const manifestValidation = exists
    ? validateExistingManifest(absolutePath)
    : missingManifestValidation();

  return {
    path: manifestPath,
    exists,
    status: exists ? "present_not_evaluated" : "missing_not_created",
    requiredForSkeleton: false,
    manifestDetection,
    manifestValidation,
  };
}

function buildManifestDetection(
  manifestPath: string,
  exists: boolean,
): ManifestDetectionReport {
  return {
    manifestPath,
    exists,
    explicitPathProvided: manifestPath !== DEFAULT_SUBSET_MANIFEST,
    status: exists ? "present" : "missing",
    note: exists
      ? "Generated-client-ready subset manifest exists and was inspected read-only."
      : "Generated-client-ready subset manifest is missing/not_created; this is expected for the skeleton and keeps readiness not_ready.",
  };
}

function missingManifestValidation(): ManifestValidationReport {
  return {
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
  };
}

function validateExistingManifest(absolutePath: string): ManifestValidationReport {
  let rawManifest: string;
  try {
    rawManifest = fs.readFileSync(absolutePath, "utf8");
  } catch (error) {
    return failedManifestValidation("schema_violation", `Manifest is not readable: ${errorMessage(error)}`);
  }

  let parsedManifest: unknown;
  try {
    parsedManifest = parse(rawManifest);
  } catch (error) {
    return failedManifestValidation("yaml_parse_error", `Manifest YAML parse failed: ${errorMessage(error)}`);
  }

  const findings = validateManifestSchema(parsedManifest);
  const hasBlockingFindings = findings.some(
    (finding) => finding.severity === "blocking",
  );

  return {
    status: hasBlockingFindings ? "failed" : "advisory_passed",
    schemaValidation: {
      name: "manifest_schema_validation",
      status: hasBlockingFindings ? "failed" : "passed",
      summary: hasBlockingFindings
        ? "Manifest schema validation found skeleton-level issues."
        : "Manifest schema validation passed at skeleton depth without readiness promotion.",
    },
    endpointReferenceValidation: {
      name: "endpoint_reference_validation",
      status: "future_only",
      summary:
        "Endpoint reference validation is future-only in Stage 7.25 skeleton validation.",
    },
    findings: [
      ...findings,
      {
        code: "endpoint_reference_validation_future_only",
        severity: "advisory",
        message:
          "Endpoint reference validation is not enforced by Stage 7.25 skeleton validation.",
      },
      {
        code: "readiness_promotion_blocked",
        severity: "advisory",
        message:
          "Manifest validation does not promote generated-client readiness; status remains not_ready and readinessClaim remains false.",
      },
    ],
  };
}

function failedManifestValidation(
  code: "schema_violation" | "yaml_parse_error",
  message: string,
): ManifestValidationReport {
  return {
    status: "failed",
    reason: code,
    schemaValidation: {
      name: "manifest_schema_validation",
      status: "failed",
      summary: message,
    },
    endpointReferenceValidation: {
      name: "endpoint_reference_validation",
      status: "future_only",
      summary:
        "Endpoint reference validation was not run because manifest schema validation did not pass.",
    },
    findings: [
      {
        code,
        severity: "blocking",
        message,
      },
      {
        code: "readiness_promotion_blocked",
        severity: "advisory",
        message:
          "Manifest validation does not promote generated-client readiness; status remains not_ready and readinessClaim remains false.",
      },
    ],
  };
}

function validateManifestSchema(manifest: unknown): Finding[] {
  const findings: Finding[] = [];

  if (!isRecord(manifest)) {
    return [
      {
        code: "schema_violation",
        severity: "blocking",
        message: "Manifest root must be an object.",
      },
    ];
  }

  requireStringField(manifest, "manifestVersion", findings);
  requireStringField(manifest, "scopeName", findings);
  requireStringField(manifest, "openApiSource", findings);
  requireObjectField(manifest, "validationStatus", findings);
  requireArrayField(manifest, "includedEndpoints", findings);
  requireArrayField(manifest, "excludedEndpoints", findings);
  requireObjectField(manifest, "classificationPolicy", findings);
  requireObjectField(manifest, "readinessCriteria", findings);
  requireArrayField(manifest, "knownLimitations", findings);
  requireArrayField(manifest, "generatedClientTargets", findings);

  const validationStatus = manifest.validationStatus;
  if (isRecord(validationStatus)) {
    const readinessClaim = validationStatus.readinessClaim;
    const status = validationStatus.status;

    if (readinessClaim === true) {
      findings.push({
        code: "readiness_promotion_blocked",
        severity: "blocking",
        message:
          "Manifest validationStatus.readinessClaim must remain false for Stage 7.25.",
      });
    }

    if (status === "ready") {
      findings.push({
        code: "readiness_promotion_blocked",
        severity: "blocking",
        message:
          "Manifest validationStatus.status must not be ready for Stage 7.25.",
      });
    }

    if (typeof status !== "string") {
      findings.push({
        code: "schema_violation",
        severity: "blocking",
        message: "Manifest validationStatus.status must be a string.",
      });
    }
  }

  return findings;
}

function requireStringField(
  value: Record<string, unknown>,
  fieldName: string,
  findings: Finding[],
): void {
  if (typeof value[fieldName] !== "string") {
    findings.push({
      code: "schema_violation",
      severity: "blocking",
      message: `Manifest field ${fieldName} must be a string.`,
    });
  }
}

function requireObjectField(
  value: Record<string, unknown>,
  fieldName: string,
  findings: Finding[],
): void {
  if (!isRecord(value[fieldName])) {
    findings.push({
      code: "schema_violation",
      severity: "blocking",
      message: `Manifest field ${fieldName} must be an object.`,
    });
  }
}

function requireArrayField(
  value: Record<string, unknown>,
  fieldName: string,
  findings: Finding[],
): void {
  if (!Array.isArray(value[fieldName])) {
    findings.push({
      code: "schema_violation",
      severity: "blocking",
      message: `Manifest field ${fieldName} must be an array.`,
    });
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}
