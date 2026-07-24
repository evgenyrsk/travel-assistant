import fs from "node:fs";
import path from "node:path";
import { parse } from "yaml";
import { DEFAULT_SUBSET_MANIFEST } from "./paths.js";
import type {
  Finding,
  HttpMethod,
  ManifestDetectionReport,
  ManifestValidationReport,
  OpenApiOperation,
  RuntimeRoute,
} from "./types.js";

export interface ManifestEndpointReference {
  method: HttpMethod;
  path: string;
  operationId?: string;
}

export interface SubsetManifestState {
  path: string;
  exists: boolean;
  status: "missing_not_created" | "present_candidate" | "present_validated";
  requiredForSkeleton: false;
  manifestDetection: ManifestDetectionReport;
  manifestValidation: ManifestValidationReport;
  endpointReferences: ManifestEndpointReference[];
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
    status: exists ? "present_candidate" : "missing_not_created",
    requiredForSkeleton: false,
    manifestDetection,
    manifestValidation,
    endpointReferences: exists
      ? extractEndpointReferences(absolutePath)
      : [],
  };
}

export function validateSubsetManifestEndpointReferences(
  state: SubsetManifestState,
  openApiOperations: OpenApiOperation[],
  runtimeRoutes: RuntimeRoute[],
): SubsetManifestState {
  if (!state.exists || state.manifestValidation.status === "failed") {
    return state;
  }

  const findings: Finding[] = [];
  const seen = new Set<string>();

  for (const reference of state.endpointReferences) {
    const key = `${reference.method.toUpperCase()} ${reference.path}`;
    if (seen.has(key)) {
      findings.push({
        code: "duplicate_manifest_endpoint_reference",
        severity: "blocking",
        message: `Manifest endpoint reference is duplicated: ${key}.`,
      });
      continue;
    }
    seen.add(key);

    const operation = openApiOperations.find(
      (candidate) =>
        candidate.method === reference.method &&
        candidate.fullPath === reference.path,
    );
    if (!operation) {
      findings.push({
        code: "manifest_openapi_reference_missing",
        severity: "blocking",
        message: `Manifest endpoint is absent from OpenAPI inventory: ${key}.`,
      });
      continue;
    }
    if (
      reference.operationId !== undefined &&
      operation.operationId !== reference.operationId
    ) {
      findings.push({
        code: "manifest_operation_id_mismatch",
        severity: "blocking",
        message:
          `Manifest operationId mismatch for ${key}: expected ` +
          `${reference.operationId}, received ${operation.operationId ?? "missing"}.`,
      });
    }

    const runtimeRoute = runtimeRoutes.find(
      (candidate) =>
        candidate.method === reference.method &&
        candidate.path === reference.path,
    );
    if (!runtimeRoute) {
      findings.push({
        code: "manifest_runtime_reference_missing",
        severity: "blocking",
        message: `Manifest endpoint is absent from runtime inventory: ${key}.`,
      });
    }
  }

  const retainedFindings = state.manifestValidation.findings.filter(
    (finding) => finding.code !== "endpoint_reference_validation_future_only",
  );
  const hasBlockingFindings = findings.some(
    (finding) => finding.severity === "blocking",
  );

  return {
    ...state,
    status: hasBlockingFindings ? "present_candidate" : "present_validated",
    manifestValidation: {
      ...state.manifestValidation,
      status: hasBlockingFindings ? "failed" : "advisory_passed",
      reason: hasBlockingFindings
        ? "endpoint_reference_validation_failed"
        : undefined,
      endpointReferenceValidation: {
        name: "endpoint_reference_validation",
        status: hasBlockingFindings ? "failed" : "passed",
        summary: hasBlockingFindings
          ? "Manifest endpoint references do not match OpenAPI/runtime inventories."
          : `Validated ${state.endpointReferences.length} manifest endpoint references against OpenAPI and runtime inventories.`,
      },
      findings: [...retainedFindings, ...findings],
    },
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
      ? "Platform-client subset manifest exists and was inspected read-only."
      : "Platform-client subset manifest is missing/not_created; readiness remains not_ready.",
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
          "Platform-client subset manifest is missing/not_created; readiness remains not_ready.",
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
        ? "Manifest schema validation found bounded contract issues."
        : "Manifest schema validation passed without readiness promotion.",
    },
    endpointReferenceValidation: {
      name: "endpoint_reference_validation",
      status: "future_only",
      summary:
        "Endpoint reference validation runs after manifest schema validation.",
    },
    findings: [
      ...findings,
      {
        code: "endpoint_reference_validation_future_only",
        severity: "advisory",
        message:
          "Endpoint reference validation has not run yet.",
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

function extractEndpointReferences(
  absolutePath: string,
): ManifestEndpointReference[] {
  try {
    const parsed = parse(fs.readFileSync(absolutePath, "utf8"));
    if (!isRecord(parsed)) {
      return [];
    }

    return [parsed.includedEndpoints, parsed.excludedEndpoints]
      .flatMap((entries) => (Array.isArray(entries) ? entries : []))
      .flatMap((entry) => {
        if (!isRecord(entry)) {
          return [];
        }
        const method = httpMethodValue(entry.method);
        const endpointPath = stringValue(entry.path);
        if (!method || !endpointPath) {
          return [];
        }

        return [{
          method,
          path: endpointPath,
          operationId: stringValue(entry.operationId),
        }];
      });
  } catch {
    return [];
  }
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

  validateTopLevelReadinessState(manifest, findings);

  const validationStatus = manifest.validationStatus;
  if (isRecord(validationStatus)) {
    validateValidationStatus(validationStatus, findings);
  }

  validateEndpointEntries(manifest.includedEndpoints, "includedEndpoints", findings);
  validateEndpointEntries(manifest.excludedEndpoints, "excludedEndpoints", findings);
  validateReadinessCriteria(manifest.readinessCriteria, findings);

  return findings;
}

function validateTopLevelReadinessState(
  manifest: Record<string, unknown>,
  findings: Finding[],
): void {
  const readinessClaim = manifest.readinessClaim;
  const status = manifest.status;

  if (readinessClaim === true) {
    findings.push(
      readinessPromotionBlocked(
        "Manifest readinessClaim must remain false for the current candidate subset.",
      ),
    );
  }

  if (readinessClaim !== undefined && typeof readinessClaim !== "boolean") {
    findings.push({
      code: "schema_violation",
      severity: "blocking",
      message: "Manifest readinessClaim must be a boolean when present.",
    });
  }

  if (status === "ready") {
    findings.push(
      readinessPromotionBlocked(
        "Manifest status must not be ready for the current candidate subset.",
      ),
    );
  }

  if (status !== undefined && typeof status !== "string") {
    findings.push({
      code: "schema_violation",
      severity: "blocking",
      message: "Manifest status must be a string when present.",
    });
  }
}

function validateValidationStatus(
  validationStatus: Record<string, unknown>,
  findings: Finding[],
): void {
  const readinessClaim = validationStatus.readinessClaim;
  const status = validationStatus.status;

  if (readinessClaim === true) {
    findings.push(
      readinessPromotionBlocked(
        "Manifest validationStatus.readinessClaim must remain false for the current candidate subset.",
      ),
    );
  }

  if (readinessClaim !== undefined && typeof readinessClaim !== "boolean") {
    findings.push({
      code: "schema_violation",
      severity: "blocking",
      message: "Manifest validationStatus.readinessClaim must be a boolean when present.",
    });
  }

  if (status === "ready") {
    findings.push(
      readinessPromotionBlocked(
        "Manifest validationStatus.status must not be ready for the current candidate subset.",
      ),
    );
  }

  if (typeof status !== "string") {
    findings.push({
      code: "schema_violation",
      severity: "blocking",
      message: "Manifest validationStatus.status must be a string.",
    });
  }
}

function validateEndpointEntries(
  value: unknown,
  fieldName: "includedEndpoints" | "excludedEndpoints",
  findings: Finding[],
): void {
  if (!Array.isArray(value)) {
    return;
  }

  value.forEach((entry, index) => {
    if (!isRecord(entry)) {
      findings.push({
        code: "schema_violation",
        severity: "blocking",
        message: `Manifest ${fieldName}[${index}] must be an object.`,
      });
      return;
    }

    const readiness = entry.readiness;
    if (readiness === "ready") {
      findings.push(
        readinessPromotionBlocked(
          `Manifest ${fieldName}[${index}].readiness must not be ready for the current candidate subset.`,
        ),
      );
    } else if (readiness !== undefined && readiness !== "not_ready") {
      findings.push({
        code: "schema_violation",
        severity: "blocking",
        message:
          `Manifest ${fieldName}[${index}].readiness must be not_ready when present.`,
      });
    }

    const classification = entry.classification;
    if (classification === "generated_client_ready" || classification === "ready") {
      findings.push(
        readinessPromotionBlocked(
          `Manifest ${fieldName}[${index}].classification must not imply generated-client readiness.`,
        ),
      );
    }
  });
}

function validateReadinessCriteria(
  value: unknown,
  findings: Finding[],
): void {
  if (!isRecord(value)) {
    return;
  }

  for (const [fieldName, fieldValue] of Object.entries(value)) {
    if (fieldValue === true) {
      findings.push(
        readinessPromotionBlocked(
          `Manifest readinessCriteria.${fieldName} must remain false until a separate readiness stage runs actual checks.`,
        ),
      );
    }
  }
}

function readinessPromotionBlocked(message: string): Finding {
  return {
    code: "readiness_promotion_blocked",
    severity: "blocking",
    message,
  };
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

function stringValue(value: unknown): string | undefined {
  return typeof value === "string" ? value : undefined;
}

function httpMethodValue(value: unknown): HttpMethod | undefined {
  if (typeof value !== "string") {
    return undefined;
  }

  const normalized = value.toLowerCase();
  return ["delete", "get", "patch", "post", "put"].includes(normalized)
    ? normalized as HttpMethod
    : undefined;
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}
