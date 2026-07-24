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
  assistantContractShape?: AssistantContractShape;
}

export interface AssistantContractShape {
  requestIdHeaderPatternPresent: boolean;
  productResponseRequestIdHeadersPresent: boolean;
  errorResponseRequestIdRequired: boolean;
  validationErrorResponseRequestIdRequired: boolean;
  createSessionRequestBodyOptional: boolean;
  continueSessionRequestBodyRequired: boolean;
  messagePropertyPresent: boolean;
  messageRequired: boolean;
  clientContextOptional: boolean;
  nextActionPropertyPresent: boolean;
  nextActionRequired: boolean;
  sessionNotFoundResponsePresent: boolean;
  validationErrorResponsesPresent: boolean;
  messageMaxLength?: number;
  requestAdditionalPropertiesForbidden: boolean;
  responseAdditionalPropertiesForbidden: boolean;
  nextActionValues: string[];
  hotelSearchIdPropertyPresent: boolean;
  hotelSearchIdConditional: boolean;
  sessionRequiredFields: string[];
  messageResponseRequiredFields: string[];
  offersOperationPresent: boolean;
  offersNotFoundResponsePresent: boolean;
  offersRequiredFields: string[];
  offersAdditionalPropertiesForbidden: boolean;
  detailsOperationPresent: boolean;
  detailsNotFoundResponsePresent: boolean;
  detailsInvalidResponsePresent: boolean;
  detailsUnavailableResponsePresent: boolean;
  detailsRequiredFields: string[];
  detailsFields: string[];
  detailsAdditionalPropertiesForbidden: boolean;
  searchRequiredFields: string[];
  searchStatusValues: string[];
  searchAdditionalPropertiesForbidden: boolean;
  offersStatusValues: string[];
  metadataRequiredFields: string[];
  metadataAdditionalPropertiesForbidden: boolean;
  hotelOfferRequiredFields: string[];
  hotelOfferAdditionalPropertiesForbidden: boolean;
  ratingOptional: boolean;
  amenitiesOptional: boolean;
  starRatingOptional: boolean;
  freeCancellationUntilOptional: boolean;
  imageUrlOptional: boolean;
  breakfastIncludedOptional: boolean;
  appliedPreferencesOptional: boolean;
  appliedPreferencesFields: string[];
  appliedPreferencesAdditionalPropertiesForbidden: boolean;
  refinementSuggestionOptional: boolean;
  refinementSuggestionRequiredFields: string[];
  refinementSuggestionTypeValues: string[];
  refinementSuggestionPreferenceValues: string[];
  refinementSuggestionAdditionalPropertiesForbidden: boolean;
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
  classificationReason?: string;
  readiness: "not_ready";
}

export type EndpointClassification =
  | "platform_client_candidate"
  | "operational"
  | "diagnostic_excluded"
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
    status: "missing_not_created" | "present_candidate" | "present_validated";
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
