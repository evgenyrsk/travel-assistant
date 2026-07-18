package com.travelassistant.backend.infrastructure.llm

import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmCandidateRequest
import com.travelassistant.backend.application.llm.LlmClient
import com.travelassistant.backend.application.llm.LlmClientResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
import java.net.URI
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class OpenRouterLlmClient(
    private val httpClient: HttpClient,
    private val config: OpenRouterConfig,
) : LlmClient {

    override suspend fun generateCandidate(request: LlmCandidateRequest): LlmClientResponse =
        try {
            execute(request)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            LlmClientResponse.Failure
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

        if (!response.status.isSuccess() ||
            response.contentType()?.match(ContentType.Application.Json) != true
        ) {
            return LlmClientResponse.Failure
        }

        val completion = OpenRouterJson.wireCodec.decodeFromString<OpenRouterCompletionResponseDto>(
            response.body(),
        )
        val choice = completion.choices.firstOrNull() ?: return LlmClientResponse.Empty
        if (choice.finishReason == ERROR_FINISH_REASON || choice.error != null) {
            return LlmClientResponse.Failure
        }

        val content = choice.message?.content?.takeIf(String::isNotBlank)
            ?: return LlmClientResponse.Empty
        val candidate = OpenRouterJson.candidateCodec
            .decodeFromString<OpenRouterCandidateDto>(content)
            .toDomainCandidate()
            ?: return LlmClientResponse.Failure

        return LlmClientResponse.Candidate(candidate)
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
                    put("outcome", enumSchema(LlmCandidate.Outcome.entries.map(Enum<*>::name)))
                    put("intent", enumSchema(LlmCandidate.Intent.entries.map(Enum<*>::name)))
                    put("extractedConstraints", extractedConstraintsSchema())
                    put("missingRequiredFields", stringArraySchema())
                    put("conflicts", stringArraySchema())
                    put("clarificationQuestion", nullableStringSchema())
                    put("warnings", stringArraySchema())
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
                        put(key, nullableStringSchema())
                    }
                },
            )
            put("required", stringArray(*CANONICAL_CONSTRAINT_KEYS.toTypedArray()))
            put("additionalProperties", false)
        }

    private fun enumSchema(values: List<String>): JsonObject =
        buildJsonObject {
            put("type", "string")
            put(
                "enum",
                buildJsonArray {
                    values.forEach(::add)
                },
            )
        }

    private fun nullableStringSchema(): JsonObject =
        buildJsonObject {
            put("type", stringArray("string", "null"))
        }

    private fun stringArraySchema(): JsonObject =
        buildJsonObject {
            put("type", "array")
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

        const val SYSTEM_PROMPT =
            "You extract hotel-only travel constraints for Travel Assistant. " +
                "Return only JSON matching the required schema. " +
                "Use confirmed constraints as context, never invent missing values, " +
                "and use only the canonical constraint keys supplied by the schema. " +
                "For unsupported non-hotel requests, return UNSUPPORTED intent and outcome."
    }
}

@Serializable
private data class OpenRouterCompletionResponseDto(
    val choices: List<OpenRouterChoiceDto>,
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
            destination?.let { "destination" to it },
            checkIn?.let { "check-in" to it },
            checkOut?.let { "check-out" to it },
            adults?.let { "adults" to it },
            children?.let { "children" to it },
            childrenAges?.let { "children-ages" to it },
            rooms?.let { "rooms" to it },
        ).toMap()
}
