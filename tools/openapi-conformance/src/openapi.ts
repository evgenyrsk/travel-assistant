import fs from "node:fs";
import path from "node:path";
import { parse } from "yaml";
import { DEFAULT_OPENAPI_CANDIDATES, joinRoutePath } from "./paths.js";
import type {
  AssistantContractShape,
  HttpMethod,
  OpenApiInventory,
  OpenApiOperation,
  SourceCandidate,
} from "./types.js";

const OPENAPI_METHODS: HttpMethod[] = ["delete", "get", "patch", "post", "put"];

export class OpenApiInputError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "OpenApiInputError";
  }
}

export function loadOpenApiInventory(
  repositoryRoot: string,
  explicitSource?: string,
): OpenApiInventory {
  const candidates = sourceCandidates(repositoryRoot, explicitSource);
  const selected = candidates.find((candidate) => candidate.exists);

  if (!selected) {
    throw new OpenApiInputError("OpenAPI source was not found.");
  }

  const absoluteSource = path.resolve(repositoryRoot, selected.path);
  let documentText: string;
  try {
    documentText = fs.readFileSync(absoluteSource, "utf8");
  } catch (error) {
    throw new OpenApiInputError(
      `OpenAPI source is not readable: ${errorMessage(error)}`,
    );
  }

  let document: unknown;
  try {
    document = parse(documentText);
  } catch (error) {
    throw new OpenApiInputError(
      `OpenAPI source is not parseable YAML: ${errorMessage(error)}`,
    );
  }

  if (!isRecord(document)) {
    throw new OpenApiInputError("OpenAPI source root must be an object.");
  }

  const openApiVersion = stringValue(document.openapi);
  if (!openApiVersion) {
    throw new OpenApiInputError("OpenAPI source must contain an openapi field.");
  }

  if (!openApiVersion.startsWith("3.")) {
    throw new OpenApiInputError(
      `OpenAPI source version must be 3.x, received ${openApiVersion}.`,
    );
  }

  if (!isRecord(document.paths)) {
    throw new OpenApiInputError("OpenAPI source must contain a paths object.");
  }

  const serverBasePath = extractServerBasePath(document);
  const operations = extractOperations(document.paths, serverBasePath);

  return {
    sourcePath: selected.path,
    detectedFromCandidates: candidates,
    openApiVersion,
    serverBasePath,
    operations,
    assistantContractShape: inspectAssistantContractShape(document),
  };
}

function sourceCandidates(
  repositoryRoot: string,
  explicitSource?: string,
): SourceCandidate[] {
  const paths = explicitSource ? [explicitSource] : DEFAULT_OPENAPI_CANDIDATES;

  return paths.map((candidatePath) => ({
    path: candidatePath,
    exists: fs.existsSync(path.resolve(repositoryRoot, candidatePath)),
  }));
}

function extractServerBasePath(document: Record<string, unknown>): string {
  if (!Array.isArray(document.servers)) {
    return "";
  }

  const firstServer = document.servers.find(isRecord);
  if (!firstServer) {
    return "";
  }

  return stringValue(firstServer.url) ?? "";
}

function extractOperations(
  paths: Record<string, unknown>,
  serverBasePath: string,
): OpenApiOperation[] {
  const operations: OpenApiOperation[] = [];

  for (const [apiPath, pathItem] of Object.entries(paths)) {
    if (!isRecord(pathItem)) {
      continue;
    }

    for (const method of OPENAPI_METHODS) {
      const operation = pathItem[method];
      if (!isRecord(operation)) {
        continue;
      }

      operations.push({
        method,
        path: apiPath,
        fullPath: joinRoutePath(serverBasePath, apiPath),
        operationId: stringValue(operation.operationId),
      });
    }
  }

  return operations.sort((left, right) =>
    `${left.path} ${left.method}`.localeCompare(`${right.path} ${right.method}`),
  );
}

function inspectAssistantContractShape(
  document: Record<string, unknown>,
): AssistantContractShape {
  const paths = recordValue(document.paths);
  const components = recordValue(document.components);
  const schemas = recordValue(components?.schemas);
  const createSession = operationValue(paths, "/assistant/sessions", "post");
  const continueSession = operationValue(
    paths,
    "/assistant/sessions/{sessionId}/messages",
    "post",
  );
  const requestSchema = recordValue(schemas?.AssistantMessageRequest);
  const responseSchema = recordValue(schemas?.AssistantMessageResponse);
  const requestProperties = recordValue(requestSchema?.properties);
  const responseProperties = recordValue(responseSchema?.properties);
  const messageProperty = recordValue(requestProperties?.message);
  const sessionSchema = recordValue(schemas?.AssistantSession);
  const messageResponseSchema = recordValue(schemas?.AssistantMessage);
  const offersSchema = recordValue(schemas?.HotelOffersResponse);
  const detailsSchema = recordValue(schemas?.HotelDetailsResponse);
  const searchSchema = recordValue(schemas?.HotelSearchResponse);
  const metadataSchema = recordValue(schemas?.SearchResultMetadata);
  const hotelOfferSchema = recordValue(schemas?.HotelOffer);
  const appliedPreferencesSchema = recordValue(
    schemas?.AppliedHotelSearchPreferences,
  );
  const refinementSuggestionSchema = recordValue(
    schemas?.HotelSearchRefinementSuggestion,
  );
  const searchProperties = recordValue(searchSchema?.properties);
  const offersProperties = recordValue(offersSchema?.properties);
  const detailsProperties = recordValue(detailsSchema?.properties);
  const hotelOfferProperties = recordValue(hotelOfferSchema?.properties);
  const appliedPreferencesProperties = recordValue(
    appliedPreferencesSchema?.properties,
  );
  const refinementSuggestionProperties = recordValue(
    refinementSuggestionSchema?.properties,
  );
  const nextActionProperty = recordValue(responseProperties?.nextAction);
  const offersOperation = operationValue(
    paths,
    "/hotel-searches/{searchId}/offers",
    "get",
  );
  const detailsOperation = operationValue(
    paths,
    "/hotel-searches/{searchId}/offers/{offerId}/details",
    "get",
  );

  return {
    createSessionRequestBodyOptional:
      recordValue(createSession?.requestBody)?.required === false,
    continueSessionRequestBodyRequired:
      recordValue(continueSession?.requestBody)?.required === true,
    messagePropertyPresent: messageProperty !== undefined,
    messageRequired: stringArray(requestSchema?.required).includes("message"),
    clientContextOptional:
      recordValue(requestProperties?.clientContext) !== undefined &&
      !stringArray(requestSchema?.required).includes("clientContext"),
    nextActionPropertyPresent:
      recordValue(responseProperties?.nextAction) !== undefined,
    nextActionRequired: stringArray(responseSchema?.required).includes("nextAction"),
    sessionNotFoundResponsePresent:
      responseRef(continueSession, "404") ===
      "#/components/responses/SessionNotFound",
    validationErrorResponsesPresent:
      responseRef(createSession, "400") ===
        "#/components/responses/ValidationError" &&
      responseRef(continueSession, "400") ===
        "#/components/responses/ValidationError",
    messageMaxLength: numberValue(messageProperty?.maxLength),
    requestAdditionalPropertiesForbidden:
      requestSchema?.additionalProperties === false,
    responseAdditionalPropertiesForbidden:
      responseSchema?.additionalProperties === false,
    nextActionValues: stringArray(nextActionProperty?.enum),
    hotelSearchIdPropertyPresent:
      recordValue(responseProperties?.hotelSearchId) !== undefined,
    hotelSearchIdConditional: hasHotelSearchIdConditional(responseSchema),
    sessionRequiredFields: stringArray(sessionSchema?.required).sort(),
    messageResponseRequiredFields: stringArray(
      messageResponseSchema?.required,
    ).sort(),
    offersOperationPresent: offersOperation !== undefined,
    offersNotFoundResponsePresent:
      responseRef(offersOperation, "404") ===
      "#/components/responses/HotelSearchNotFound",
    offersRequiredFields: stringArray(offersSchema?.required).sort(),
    offersAdditionalPropertiesForbidden:
      offersSchema?.additionalProperties === false,
    detailsOperationPresent: detailsOperation !== undefined,
    detailsNotFoundResponsePresent:
      responseRef(detailsOperation, "404") ===
      "#/components/responses/HotelDetailsSelectionNotFound",
    detailsInvalidResponsePresent:
      responseRef(detailsOperation, "502") ===
      "#/components/responses/ProviderResponseInvalid",
    detailsUnavailableResponsePresent:
      responseRef(detailsOperation, "503") ===
      "#/components/responses/ProviderUnavailable",
    detailsRequiredFields: stringArray(detailsSchema?.required).sort(),
    detailsFields: Object.keys(detailsProperties ?? {}).sort(),
    detailsAdditionalPropertiesForbidden:
      detailsSchema?.additionalProperties === false,
    searchRequiredFields: stringArray(searchSchema?.required).sort(),
    searchStatusValues: stringArray(
      recordValue(searchProperties?.status)?.enum,
    ).sort(),
    searchAdditionalPropertiesForbidden:
      searchSchema?.additionalProperties === false,
    offersStatusValues: stringArray(
      recordValue(offersProperties?.status)?.enum,
    ).sort(),
    metadataRequiredFields: stringArray(metadataSchema?.required).sort(),
    metadataAdditionalPropertiesForbidden:
      metadataSchema?.additionalProperties === false,
    hotelOfferRequiredFields: stringArray(hotelOfferSchema?.required).sort(),
    hotelOfferAdditionalPropertiesForbidden:
      hotelOfferSchema?.additionalProperties === false,
    ratingOptional:
      recordValue(hotelOfferProperties?.rating) !== undefined &&
      !stringArray(hotelOfferSchema?.required).includes("rating"),
    amenitiesOptional:
      recordValue(hotelOfferProperties?.amenities) !== undefined &&
      !stringArray(hotelOfferSchema?.required).includes("amenities"),
    starRatingOptional:
      recordValue(hotelOfferProperties?.starRating) !== undefined &&
      !stringArray(hotelOfferSchema?.required).includes("starRating"),
    freeCancellationUntilOptional:
      recordValue(hotelOfferProperties?.freeCancellationUntil) !== undefined &&
      !stringArray(hotelOfferSchema?.required).includes("freeCancellationUntil"),
    imageUrlOptional:
      recordValue(hotelOfferProperties?.imageUrl) !== undefined &&
      !stringArray(hotelOfferSchema?.required).includes("imageUrl"),
    appliedPreferencesOptional:
      stringValue(recordValue(offersProperties?.appliedPreferences)?.$ref) ===
        "#/components/schemas/AppliedHotelSearchPreferences" &&
      !stringArray(offersSchema?.required).includes("appliedPreferences"),
    appliedPreferencesFields: Object.keys(
      appliedPreferencesProperties ?? {},
    ).sort(),
    appliedPreferencesAdditionalPropertiesForbidden:
      appliedPreferencesSchema?.additionalProperties === false,
    refinementSuggestionOptional:
      stringValue(recordValue(offersProperties?.refinementSuggestion)?.$ref) ===
        "#/components/schemas/HotelSearchRefinementSuggestion" &&
      !stringArray(offersSchema?.required).includes("refinementSuggestion"),
    refinementSuggestionRequiredFields: stringArray(
      refinementSuggestionSchema?.required,
    ).sort(),
    refinementSuggestionTypeValues: stringArray(
      recordValue(refinementSuggestionProperties?.type)?.enum,
    ).sort(),
    refinementSuggestionPreferenceValues: stringArray(
      recordValue(refinementSuggestionProperties?.preference)?.enum,
    ).sort(),
    refinementSuggestionAdditionalPropertiesForbidden:
      refinementSuggestionSchema?.additionalProperties === false,
  };
}

function hasHotelSearchIdConditional(
  responseSchema: Record<string, unknown> | undefined,
): boolean {
  if (!Array.isArray(responseSchema?.allOf)) {
    return false;
  }

  return responseSchema.allOf.some((entry) => {
    const conditional = recordValue(entry);
    const ifSchema = recordValue(conditional?.if);
    const ifProperties = recordValue(ifSchema?.properties);
    const ifNextAction = recordValue(ifProperties?.nextAction);
    const thenSchema = recordValue(conditional?.then);
    const elseSchema = recordValue(conditional?.else);
    const elseNotSchema = recordValue(elseSchema?.not);

    return (
      ifNextAction?.const === "show_hotel_results" &&
      stringArray(thenSchema?.required).includes("hotelSearchId") &&
      stringArray(elseNotSchema?.required).includes("hotelSearchId")
    );
  });
}

function operationValue(
  paths: Record<string, unknown> | undefined,
  apiPath: string,
  method: HttpMethod,
): Record<string, unknown> | undefined {
  return recordValue(recordValue(paths?.[apiPath])?.[method]);
}

function responseRef(
  operation: Record<string, unknown> | undefined,
  statusCode: string,
): string | undefined {
  const responses = recordValue(operation?.responses);
  return stringValue(recordValue(responses?.[statusCode])?.$ref);
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function recordValue(
  value: unknown,
): Record<string, unknown> | undefined {
  return isRecord(value) ? value : undefined;
}

function stringArray(value: unknown): string[] {
  return Array.isArray(value)
    ? value.filter((item): item is string => typeof item === "string")
    : [];
}

function numberValue(value: unknown): number | undefined {
  return typeof value === "number" ? value : undefined;
}

function stringValue(value: unknown): string | undefined {
  return typeof value === "string" ? value : undefined;
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}
