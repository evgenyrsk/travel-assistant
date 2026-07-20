package com.travelassistant.backend.infrastructure.llm

import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmCandidateRequest
import com.travelassistant.backend.application.llm.LlmClient
import com.travelassistant.backend.application.llm.LlmClientResponse
import com.travelassistant.backend.application.llm.LlmClientRetryableFailureReason
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import java.io.IOException
import java.net.URI
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

internal class OpenRouterLlmClient(
    private val httpClient: HttpClient,
    private val config: OpenRouterConfig,
    private val diagnosticObserver: OpenRouterDiagnosticObserver = OpenRouterDiagnosticObserver.NONE,
) : LlmClient {

    override suspend fun generateCandidate(request: LlmCandidateRequest): LlmClientResponse =
        try {
            execute(request)
        } catch (error: CancellationException) {
            throw error
        } catch (_: HttpRequestTimeoutException) {
            failure(OpenRouterDiagnosticEvent.TIMEOUT)
        } catch (_: IOException) {
            failure(OpenRouterDiagnosticEvent.NETWORK_FAILURE)
        } catch (_: SerializationException) {
            failure(OpenRouterDiagnosticEvent.MALFORMED_RESPONSE)
        } catch (_: Exception) {
            failure(OpenRouterDiagnosticEvent.UNKNOWN_FAILURE)
        }

    private suspend fun execute(request: LlmCandidateRequest): LlmClientResponse {
        val response = httpClient.post(chatCompletionsUrl()) {
            expectSuccess = false
            timeout {
                requestTimeoutMillis = config.timeoutMillis
            }
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${config.apiKey.reveal()}")
            setBody(requestBody(request))
        }

        if (!response.status.isSuccess()) {
            return failure(eventForStatus(response.status.value))
        }
        if (response.contentType()?.match(ContentType.Application.Json) != true) {
            return failure(OpenRouterDiagnosticEvent.NON_JSON_RESPONSE)
        }

        val completion = OpenRouterJson.wireCodec.decodeFromString<OpenRouterCompletionResponseDto>(
            response.body(),
        )
        completion.error?.let { error ->
            return failure(eventForProviderError(error))
        }

        val choices = completion.choices
            ?: return failure(OpenRouterDiagnosticEvent.MALFORMED_RESPONSE)
        val choice = choices.firstOrNull()
            ?: return empty(OpenRouterDiagnosticEvent.EMPTY_CHOICES)
        if (choice.finishReason == ERROR_FINISH_REASON || choice.error != null) {
            return failure(
                choice.error?.let(::eventForProviderError)
                    ?: OpenRouterDiagnosticEvent.IN_BAND_PROVIDER_ERROR,
            )
        }

        val content = choice.message?.content?.takeIf(String::isNotBlank)
            ?: return empty(OpenRouterDiagnosticEvent.EMPTY_CONTENT)
        val candidate = try {
            OpenRouterJson.candidateCodec.decodeFromString<OpenRouterCandidateDto>(content)
        } catch (_: SerializationException) {
            return failure(OpenRouterDiagnosticEvent.INVALID_CANDIDATE)
        }
        val domainCandidate = candidate
            .toDomainCandidate()
            ?: return failure(OpenRouterDiagnosticEvent.INVALID_CANDIDATE)

        report(OpenRouterDiagnosticEvent.CANDIDATE_DECODED)
        return LlmClientResponse.Candidate(domainCandidate)
    }

    private fun failure(event: OpenRouterDiagnosticEvent): LlmClientResponse {
        report(event)
        return event.toRetryableFailure() ?: LlmClientResponse.Failure
    }

    private fun empty(event: OpenRouterDiagnosticEvent): LlmClientResponse {
        report(event)
        return LlmClientResponse.RetryableFailure(
            LlmClientRetryableFailureReason.EMPTY_RESPONSE,
        )
    }

    private fun OpenRouterDiagnosticEvent.toRetryableFailure():
        LlmClientResponse.RetryableFailure? =
        when (this) {
            OpenRouterDiagnosticEvent.TIMEOUT,
            OpenRouterDiagnosticEvent.PROVIDER_UNAVAILABLE,
            OpenRouterDiagnosticEvent.IN_BAND_PROVIDER_ERROR,
            OpenRouterDiagnosticEvent.MALFORMED_RESPONSE,
            OpenRouterDiagnosticEvent.NETWORK_FAILURE,
            -> retryableFailure(LlmClientRetryableFailureReason.CLIENT_FAILURE)

            OpenRouterDiagnosticEvent.INVALID_CANDIDATE ->
                retryableFailure(LlmClientRetryableFailureReason.INVALID_CANDIDATE)

            OpenRouterDiagnosticEvent.CANDIDATE_DECODED,
            OpenRouterDiagnosticEvent.REQUEST_REJECTED,
            OpenRouterDiagnosticEvent.AUTHENTICATION_FAILED,
            OpenRouterDiagnosticEvent.INSUFFICIENT_CREDITS,
            OpenRouterDiagnosticEvent.RATE_LIMITED,
            OpenRouterDiagnosticEvent.HTTP_FAILURE,
            OpenRouterDiagnosticEvent.NON_JSON_RESPONSE,
            OpenRouterDiagnosticEvent.EMPTY_CHOICES,
            OpenRouterDiagnosticEvent.EMPTY_CONTENT,
            OpenRouterDiagnosticEvent.UNKNOWN_FAILURE,
            -> null
        }

    private fun retryableFailure(
        reason: LlmClientRetryableFailureReason,
    ): LlmClientResponse.RetryableFailure =
        LlmClientResponse.RetryableFailure(reason)

    private fun report(event: OpenRouterDiagnosticEvent) {
        runCatching { diagnosticObserver.record(event) }
    }

    private fun eventForProviderError(error: JsonObject): OpenRouterDiagnosticEvent {
        val errorType = (error["metadata"] as? JsonObject)
            ?.get("error_type")
            ?.let { value -> value as? JsonPrimitive }
            ?.contentOrNull

        return when (errorType) {
            "authentication" -> OpenRouterDiagnosticEvent.AUTHENTICATION_FAILED
            "rate_limit_exceeded" -> OpenRouterDiagnosticEvent.RATE_LIMITED
            "timeout" -> OpenRouterDiagnosticEvent.TIMEOUT
            "provider_overloaded", "provider_unavailable", "server" ->
                OpenRouterDiagnosticEvent.PROVIDER_UNAVAILABLE

            "invalid_request", "invalid_prompt", "context_length_exceeded" ->
                OpenRouterDiagnosticEvent.REQUEST_REJECTED

            else -> (error["code"] as? JsonPrimitive)
                ?.intOrNull
                ?.let(::eventForStatus)
                ?: OpenRouterDiagnosticEvent.IN_BAND_PROVIDER_ERROR
        }
    }

    private fun eventForStatus(statusCode: Int): OpenRouterDiagnosticEvent =
        when (statusCode) {
            400 -> OpenRouterDiagnosticEvent.REQUEST_REJECTED
            401, 403 -> OpenRouterDiagnosticEvent.AUTHENTICATION_FAILED
            402 -> OpenRouterDiagnosticEvent.INSUFFICIENT_CREDITS
            408 -> OpenRouterDiagnosticEvent.TIMEOUT
            429 -> OpenRouterDiagnosticEvent.RATE_LIMITED
            in 500..599 -> OpenRouterDiagnosticEvent.PROVIDER_UNAVAILABLE
            else -> OpenRouterDiagnosticEvent.HTTP_FAILURE
        }

    private fun chatCompletionsUrl(): String {
        val normalizedBaseUrl = if (config.baseUrl.endsWith('/')) {
            config.baseUrl
        } else {
            "${config.baseUrl}/"
        }

        return URI(normalizedBaseUrl).resolve(CHAT_COMPLETIONS_PATH).toString()
    }

    private fun requestBody(request: LlmCandidateRequest): String =
        buildJsonObject {
            put("model", config.model)
            put(
                "messages",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("role", "system")
                            put("content", SYSTEM_PROMPT)
                        },
                    )
                    add(
                        buildJsonObject {
                            put("role", "user")
                            put("content", promptPayload(request))
                        },
                    )
                },
            )
            put("stream", false)
            put("temperature", 0)
            put(
                "provider",
                buildJsonObject {
                    put("require_parameters", true)
                },
            )
            put(
                "response_format",
                buildJsonObject {
                    put("type", "json_schema")
                    put(
                        "json_schema",
                        buildJsonObject {
                            put("name", CANDIDATE_SCHEMA_NAME)
                            put("strict", true)
                            put("schema", candidateSchema())
                        },
                    )
                },
            )
        }.toString()

    private fun promptPayload(request: LlmCandidateRequest): String =
        buildJsonObject {
            put("userMessage", request.userMessage)
            put(
                "confirmedConstraints",
                buildJsonObject {
                    request.confirmedConstraints.toSortedMap().forEach { (key, value) ->
                        put(key, value)
                    }
                },
            )
            put(
                "missingRequiredFields",
                buildJsonArray {
                    request.missingRequiredFields.forEach(::add)
                },
            )
        }.toString()

    private fun candidateSchema(): JsonObject =
        buildJsonObject {
            put("type", "object")
            put(
                "properties",
                buildJsonObject {
                    put(
                        "outcome",
                        enumSchema(
                            values = LlmCandidate.Outcome.entries.map(Enum<*>::name),
                            description = OUTCOME_DESCRIPTION,
                        ),
                    )
                    put(
                        "intent",
                        enumSchema(
                            values = LlmCandidate.Intent.entries.map(Enum<*>::name),
                            description = INTENT_DESCRIPTION,
                        ),
                    )
                    put("extractedConstraints", extractedConstraintsSchema())
                    put(
                        "missingRequiredFields",
                        stringArraySchema(MISSING_REQUIRED_FIELDS_DESCRIPTION),
                    )
                    put("conflicts", stringArraySchema(CONFLICTS_DESCRIPTION))
                    put(
                        "clarificationQuestion",
                        nullableStringSchema(CLARIFICATION_QUESTION_DESCRIPTION),
                    )
                    put("warnings", stringArraySchema(WARNINGS_DESCRIPTION))
                },
            )
            put(
                "required",
                stringArray(
                    "outcome",
                    "intent",
                    "extractedConstraints",
                    "missingRequiredFields",
                    "conflicts",
                    "clarificationQuestion",
                    "warnings",
                ),
            )
            put("additionalProperties", false)
        }

    private fun extractedConstraintsSchema(): JsonObject =
        buildJsonObject {
            put("type", "object")
            put(
                "properties",
                buildJsonObject {
                    CANONICAL_CONSTRAINT_KEYS.forEach { key ->
                        put(
                            key,
                            nullableStringSchema(CONSTRAINT_DESCRIPTIONS.getValue(key)),
                        )
                    }
                },
            )
            put("required", stringArray(*CANONICAL_CONSTRAINT_KEYS.toTypedArray()))
            put("additionalProperties", false)
        }

    private fun enumSchema(
        values: List<String>,
        description: String,
    ): JsonObject =
        buildJsonObject {
            put("type", "string")
            put("description", description)
            put(
                "enum",
                buildJsonArray {
                    values.forEach(::add)
                },
            )
        }

    private fun nullableStringSchema(description: String): JsonObject =
        buildJsonObject {
            put("type", stringArray("string", "null"))
            put("description", description)
        }

    private fun stringArraySchema(description: String): JsonObject =
        buildJsonObject {
            put("type", "array")
            put("description", description)
            put(
                "items",
                buildJsonObject {
                    put("type", "string")
                },
            )
        }

    private fun stringArray(vararg values: String) =
        buildJsonArray {
            values.forEach(::add)
        }

    private companion object {
        const val CHAT_COMPLETIONS_PATH = "chat/completions"
        const val CANDIDATE_SCHEMA_NAME = "travel_assistant_hotel_candidate"
        const val ERROR_FINISH_REASON = "error"

        val CANONICAL_CONSTRAINT_KEYS = listOf(
            "destination",
            "check-in",
            "check-out",
            "adults",
            "children",
            "children-ages",
            "rooms",
        )

        val CONSTRAINT_DESCRIPTIONS = mapOf(
            "destination" to "Destination name from the user, or null when absent.",
            "check-in" to "Check-in date as YYYY-MM-DD, or null when absent.",
            "check-out" to "Check-out date as YYYY-MM-DD, or null when absent.",
            "adults" to "Adult count as a decimal string, or null when absent.",
            "children" to "Child count as a decimal string, or null when absent.",
            "children-ages" to
                "Comma-separated child ages in user order; null when no children or ages are absent.",
            "rooms" to "Room count as a decimal string, or null when absent.",
        )

        const val OUTCOME_DESCRIPTION =
            "INTERPRETED only for a complete consistent hotel request; " +
                "NEEDS_CLARIFICATION when required values are absent; " +
                "AMBIGUOUS for conflicting meanings; UNSUPPORTED for non-hotel requests."
        const val INTENT_DESCRIPTION =
            "HOTEL_SEARCH for supported hotel requests, UNSUPPORTED for non-hotel requests."
        const val MISSING_REQUIRED_FIELDS_DESCRIPTION =
            "Canonical constraint keys still required from the user; empty for INTERPRETED."
        const val CONFLICTS_DESCRIPTION =
            "Short conflict markers; empty when the request is consistent."
        const val CLARIFICATION_QUESTION_DESCRIPTION =
            "A non-empty user-facing question in Russian only when clarification is required; otherwise null."
        const val WARNINGS_DESCRIPTION =
            "Blocking safety warnings; empty for a safe complete hotel request."

        const val SYSTEM_PROMPT =
            "You extract hotel-only travel constraints for Travel Assistant. " +
                "Return only JSON matching the required schema. " +
                "Use confirmed constraints as context and never invent missing values. " +
                "Use null, never an empty string, for absent constraint values and for an absent " +
                "clarification question. Dates use YYYY-MM-DD. Counts use decimal strings. " +
                "Use children-ages as comma-separated ages only when children is greater than zero. " +
                "For a complete consistent hotel request return INTERPRETED and HOTEL_SEARCH, " +
                "with empty missingRequiredFields, conflicts, and warnings, and null " +
                "clarificationQuestion. For an incomplete request return NEEDS_CLARIFICATION, " +
                "list only missing canonical keys, and ask one non-empty clarification question in Russian. " +
                "Use only the canonical constraint keys supplied by the schema. " +
                "For unsupported non-hotel requests, return UNSUPPORTED intent and outcome."
    }
}

@Serializable
private data class OpenRouterCompletionResponseDto(
    val choices: List<OpenRouterChoiceDto>? = null,
    val error: JsonObject? = null,
)

@Serializable
private data class OpenRouterChoiceDto(
    @SerialName("finish_reason")
    val finishReason: String? = null,
    val message: OpenRouterMessageDto? = null,
    val error: JsonObject? = null,
)

@Serializable
private data class OpenRouterMessageDto(
    val content: String? = null,
)

@Serializable
private data class OpenRouterCandidateDto(
    val outcome: String,
    val intent: String,
    val extractedConstraints: OpenRouterExtractedConstraintsDto,
    val missingRequiredFields: List<String>,
    val conflicts: List<String>,
    val clarificationQuestion: String?,
    val warnings: List<String>,
) {
    fun toDomainCandidate(): LlmCandidate? {
        val mappedOutcome = runCatching { LlmCandidate.Outcome.valueOf(outcome) }.getOrNull()
            ?: return null
        val mappedIntent = runCatching { LlmCandidate.Intent.valueOf(intent) }.getOrNull()
            ?: return null

        return LlmCandidate(
            outcome = mappedOutcome,
            intent = mappedIntent,
            extractedConstraints = extractedConstraints.toDomainMap(),
            missingRequiredFields = missingRequiredFields,
            conflicts = conflicts,
            clarificationQuestion = clarificationQuestion,
            warnings = warnings,
        )
    }
}

@Serializable
private data class OpenRouterExtractedConstraintsDto(
    val destination: String?,
    @SerialName("check-in")
    val checkIn: String?,
    @SerialName("check-out")
    val checkOut: String?,
    val adults: String?,
    val children: String?,
    @SerialName("children-ages")
    val childrenAges: String?,
    val rooms: String?,
) {
    fun toDomainMap(): Map<String, String> =
        listOfNotNull(
            destination.toConstraint("destination"),
            checkIn.toConstraint("check-in"),
            checkOut.toConstraint("check-out"),
            adults.toConstraint("adults"),
            children.toConstraint("children"),
            childrenAges.toConstraint("children-ages"),
            rooms.toConstraint("rooms"),
        ).toMap()

    private fun String?.toConstraint(key: String): Pair<String, String>? =
        this?.takeUnless(String::isBlank)?.let { value -> key to value }
}
