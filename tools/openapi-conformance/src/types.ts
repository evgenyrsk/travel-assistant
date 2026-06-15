export type HttpMethod = "delete" | "get" | "patch" | "post" | "put";

export type CheckStatus =
  | "advisory"
  | "failed"
  | "future_only"
  | "missing"
  | "not_created"
  | "not_run"
  | "not_ready"
  | "passed";

export interface ToolOptions {
  openApiSource?: string;
  subsetManifest?: string;
}

export interface OpenApiOperation {
  method: HttpMethod;
  path: string;
  fullPath: string;
  operationId?: string;
}

export interface OpenApiInventory {
  sourcePath: string;
  detectedFromCandidates: SourceCandidate[];
  openApiVersion?: string;
  serverBasePath: string;
  operations: OpenApiOperation[];
}

export interface RuntimeRoute {
  method: HttpMethod;
  path: string;
  sourceFile: string;
  line: number;
}

export interface SourceCandidate {
  path: string;
  exists: boolean;
}

export interface EndpointReport {
  method: HttpMethod;
  path: string;
  operationId?: string;
  inOpenApi: boolean;
  inRuntime: boolean;
  runtimeSourceFiles: string[];
  classification: EndpointClassification;
  placeholderReason?: string;
  readiness: "not_ready";
}

export type EndpointClassification =
  | "foundation_candidate"
  | "placeholder_excluded"
  | "runtime_only"
  | "unclassified";

export interface EndpointClassificationSummary {
  total: number;
  byClassification: Record<EndpointClassification, number>;
  openApiOnly: number;
  runtimeOnly: number;
  inBothInventories: number;
}

export interface CheckReport {
  name: string;
  status: CheckStatus;
  summary: string;
}

export interface Finding {
  code: string;
  severity: "blocking" | "advisory";
  message: string;
}

export interface ManifestDetectionReport {
  manifestPath: string;
  exists: boolean;
  explicitPathProvided: boolean;
  status: "missing" | "present";
  note: string;
}

export interface ManifestValidationCheck {
  name: string;
  status: "not_run" | "passed" | "failed" | "future_only";
  summary: string;
}

export interface ManifestValidationReport {
  status: "not_run" | "advisory_passed" | "failed";
  reason?: string;
  schemaValidation: ManifestValidationCheck;
  endpointReferenceValidation: ManifestValidationCheck;
  findings: Finding[];
}

export interface ConformanceReport {
  tool: {
    name: string;
    version: string;
    mode: "classification";
    readOnly: true;
  };
  generatedAt: string;
  status: "not_ready";
  readinessClaim: false;
  openApiSource: {
    path: string;
    detected: boolean;
    openApiVersion?: string;
    serverBasePath: string;
    operationCount: number;
    candidates: SourceCandidate[];
  };
  subsetManifest: {
    path: string;
    exists: boolean;
    status: "missing_not_created" | "present_not_evaluated";
    requiredForSkeleton: false;
  };
  manifestDetection: ManifestDetectionReport;
  manifestValidation: ManifestValidationReport;
  inventories: {
    openApi: OpenApiOperation[];
    runtimeRoutes: RuntimeRoute[];
  };
  endpointClassificationSummary: EndpointClassificationSummary;
  endpoints: EndpointReport[];
  checks: CheckReport[];
  blockingFindings: Finding[];
  advisoryFindings: Finding[];
  futureOnlyChecks: CheckReport[];
}
