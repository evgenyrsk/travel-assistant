package com.travelassistant.backend.infrastructure.accommodation

import com.travelassistant.backend.application.accommodation.AccommodationAnalysisClient
import com.travelassistant.backend.application.accommodation.AccommodationAnalysisRequest
import com.travelassistant.backend.application.accommodation.AccommodationAnalysisResult
import com.travelassistant.backend.domain.hotel.AccommodationEvidenceSource
import com.travelassistant.backend.domain.hotel.AccommodationMatchVerdict
import com.travelassistant.backend.infrastructure.llm.OpenRouterJson
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
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

internal class OpenRouterAccommodationAnalysisClient(
    private val httpClient: HttpClient,
    private val config: OpenRouterAccommodationAnalysisConfig,
) : AccommodationAnalysisClient {
    private val imageUrlPolicy = AccommodationAnalysisImageUrlPolicy(config.imageHosts)

    override suspend fun analyze(request: AccommodationAnalysisRequest): AccommodationAnalysisResult {
        if (request.candidates.isEmpty()) {
            return AccommodationAnalysisResult.Completed(emptyList())
        }
        val decisions = mutableListOf<AccommodationAnalysisResult.Decision>()
        request.candidates.chunked(config.batchSize).forEach { batch ->
            when (val batchResult = executeBatch(request.copy(candidates = batch))) {
                is AccommodationAnalysisResult.Completed -> decisions += batchResult.decisions
                is AccommodationAnalysisResult.Failed -> return batchResult
            }
        }
        return AccommodationAnalysisResult.Completed(decisions)
    }

    private suspend fun executeBatch(
        request: AccommodationAnalysisRequest,
    ): AccommodationAnalysisResult {
        return try {
            val response = httpClient.post(chatCompletionsUrl()) {
                expectSuccess = false
                timeout { requestTimeoutMillis = config.timeoutMillis }
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer ${config.apiKey.reveal()}")
                setBody(requestBody(request))
            }
            if (!response.status.isSuccess()) {
                return AccommodationAnalysisResult.Failed(failureForStatus(response.status.value))
            }
            if (response.contentType()?.match(ContentType.Application.Json) != true) {
                return AccommodationAnalysisResult.Failed(
                    AccommodationAnalysisResult.FailureReason.INVALID_RESPONSE,
                )
            }

            val completion = OpenRouterJson.wireCodec.decodeFromString<CompletionDto>(
                response.body(),
            )
            if (completion.error != null) {
                return AccommodationAnalysisResult.Failed(
                    AccommodationAnalysisResult.FailureReason.UNAVAILABLE,
                )
            }
            val content = completion.choices
                ?.singleOrNull()
                ?.message
                ?.content
                ?.takeIf(String::isNotBlank)
                ?: return AccommodationAnalysisResult.Failed(
                    AccommodationAnalysisResult.FailureReason.INVALID_RESPONSE,
                )
            val responseDto = OpenRouterJson.candidateCodec.decodeFromString<AnalysisResponseDto>(
                content,
            )
            responseDto.toApplicationResult()
                ?: AccommodationAnalysisResult.Failed(
                    AccommodationAnalysisResult.FailureReason.INVALID_RESPONSE,
                )
        } catch (error: CancellationException) {
            throw error
        } catch (_: HttpRequestTimeoutException) {
            AccommodationAnalysisResult.Failed(AccommodationAnalysisResult.FailureReason.TIMEOUT)
        } catch (_: IOException) {
            AccommodationAnalysisResult.Failed(
                AccommodationAnalysisResult.FailureReason.UNAVAILABLE,
            )
        } catch (_: SerializationException) {
            AccommodationAnalysisResult.Failed(
                AccommodationAnalysisResult.FailureReason.INVALID_RESPONSE,
            )
        } catch (_: Exception) {
            AccommodationAnalysisResult.Failed(
                AccommodationAnalysisResult.FailureReason.INVALID_RESPONSE,
            )
        }
    }

    private fun requestBody(request: AccommodationAnalysisRequest): String =
        buildJsonObject {
            put("model", config.model)
            putJsonArray("messages") {
                add(
                    buildJsonObject {
                        put("role", "system")
                        put("content", SYSTEM_PROMPT)
                    },
                )
                add(
                    buildJsonObject {
                        put("role", "user")
                        put("content", request.multimodalContent())
                    },
                )
            }
            put("stream", false)
            put("temperature", 0)
            putJsonObject("provider") {
                putJsonArray("only") {
                    add(config.providerEndpoint)
                }
                put("allow_fallbacks", false)
                put("require_parameters", true)
                put("data_collection", "deny")
                put("zdr", true)
            }
            putJsonObject("response_format") {
                put("type", "json_schema")
                putJsonObject("json_schema") {
                    put("name", SCHEMA_NAME)
                    put("strict", true)
                    put("schema", responseSchema(request))
                }
            }
        }.toString()

    private fun AccommodationAnalysisRequest.multimodalContent() =
        buildJsonArray {
            candidates.forEach { candidate ->
                val imageUrls = candidate.imageUrls
                    .mapNotNull(imageUrlPolicy::allowedOrNull)
                    .distinct()
                    .take(MAX_IMAGES_PER_CANDIDATE)
                add(
                    buildJsonObject {
                        put("type", "text")
                        put(
                            "text",
                            buildJsonObject {
                                put("candidate_id", candidate.ephemeralCandidateId)
                                put("hotel_name", candidate.hotelName.bounded(MAX_NAME_LENGTH))
                                putJsonArray("descriptions") {
                                    candidate.descriptions
                                        .mapNotNull { value -> value.boundedOrNull(MAX_TEXT_LENGTH) }
                                        .take(MAX_DESCRIPTIONS)
                                        .forEach(::add)
                                }
                                putJsonArray("amenities") {
                                    candidate.amenities
                                        .mapNotNull { value -> value.boundedOrNull(MAX_AMENITY_LENGTH) }
                                        .take(MAX_AMENITIES)
                                        .forEach(::add)
                                }
                                put("following_image_count", imageUrls.size)
                            }.toString(),
                        )
                    },
                )
                imageUrls.forEach { imageUrl ->
                    add(
                        buildJsonObject {
                            put("type", "image_url")
                            putJsonObject("image_url") {
                                put("url", imageUrl)
                            }
                        },
                    )
                }
            }
        }

    private fun responseSchema(request: AccommodationAnalysisRequest): JsonObject =
        buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("results") {
                    put("type", "array")
                    put("minItems", request.candidates.size)
                    put("maxItems", request.candidates.size)
                    putJsonObject("items") {
                        put("type", "object")
                        putJsonObject("properties") {
                            putJsonObject("candidate_id") {
                                put("type", "string")
                                putJsonArray("enum") {
                                    request.candidates.forEach { candidate ->
                                        add(candidate.ephemeralCandidateId)
                                    }
                                }
                            }
                            enumSchema(
                                key = "verdict",
                                values = AccommodationMatchVerdict.entries.map { it.apiValue },
                            )
                            putJsonObject("evidence") {
                                put("type", "array")
                                put("uniqueItems", true)
                                putJsonObject("items") {
                                    put("type", "object")
                                    putJsonObject("properties") {
                                        enumSchema(
                                            key = "source",
                                            values = AccommodationEvidenceSource.entries.map {
                                                it.apiValue
                                            },
                                        )
                                        enumSchema(
                                            key = "signal",
                                            values = AccommodationAnalysisResult.Signal.entries.map {
                                                it.name.lowercase(Locale.ROOT)
                                            },
                                        )
                                    }
                                    put("required", stringArray("source", "signal"))
                                    put("additionalProperties", false)
                                }
                            }
                        }
                        put("required", stringArray("candidate_id", "verdict", "evidence"))
                        put("additionalProperties", false)
                    }
                }
            }
            put("required", stringArray("results"))
            put("additionalProperties", false)
        }

    private fun kotlinx.serialization.json.JsonObjectBuilder.enumSchema(
        key: String,
        values: List<String>,
    ) {
        putJsonObject(key) {
            put("type", "string")
            putJsonArray("enum") { values.forEach(::add) }
        }
    }

    private fun stringArray(vararg values: String) =
        buildJsonArray { values.forEach(::add) }

    private fun String.bounded(maxLength: Int): String = trim().take(maxLength)

    private fun String.boundedOrNull(maxLength: Int): String? =
        trim().takeIf(String::isNotEmpty)?.take(maxLength)

    private fun chatCompletionsUrl(): String {
        val baseUrl = config.baseUrl.let { value ->
            if (value.endsWith('/')) value else "$value/"
        }
        return URI(baseUrl).resolve(CHAT_COMPLETIONS_PATH).toString()
    }

    private fun failureForStatus(status: Int): AccommodationAnalysisResult.FailureReason =
        when (status) {
            400 -> AccommodationAnalysisResult.FailureReason.REQUEST_REJECTED
            401, 403 -> AccommodationAnalysisResult.FailureReason.AUTHENTICATION_FAILED
            408 -> AccommodationAnalysisResult.FailureReason.TIMEOUT
            429 -> AccommodationAnalysisResult.FailureReason.RATE_LIMITED
            else -> AccommodationAnalysisResult.FailureReason.UNAVAILABLE
        }

    @Serializable
    private data class CompletionDto(
        val choices: List<ChoiceDto>? = null,
        val error: JsonObject? = null,
    )

    @Serializable
    private data class ChoiceDto(
        val message: MessageDto? = null,
    )

    @Serializable
    private data class MessageDto(
        val content: String? = null,
    )

    @Serializable
    private data class AnalysisResponseDto(
        val results: List<DecisionDto>,
    ) {
        fun toApplicationResult(): AccommodationAnalysisResult.Completed? {
            val decisions = results.map { decision ->
                decision.toApplicationDecision() ?: return null
            }
            return AccommodationAnalysisResult.Completed(decisions)
        }
    }

    @Serializable
    private data class DecisionDto(
        @SerialName("candidate_id")
        val candidateId: String,
        val verdict: String,
        val evidence: List<EvidenceDto>,
    ) {
        fun toApplicationDecision(): AccommodationAnalysisResult.Decision? {
            val mappedVerdict = AccommodationMatchVerdict.entries.firstOrNull { candidate ->
                candidate.apiValue == verdict
            } ?: return null
            val mappedEvidence = evidence.map { item ->
                item.toApplicationEvidence() ?: return null
            }.toSet()
            return AccommodationAnalysisResult.Decision(candidateId, mappedVerdict, mappedEvidence)
        }
    }

    @Serializable
    private data class EvidenceDto(
        val source: String,
        val signal: String,
    ) {
        fun toApplicationEvidence(): AccommodationAnalysisResult.Evidence? {
            val mappedSource = AccommodationEvidenceSource.entries.firstOrNull { candidate ->
                candidate.apiValue == source
            } ?: return null
            val mappedSignal = AccommodationAnalysisResult.Signal.entries.firstOrNull { candidate ->
                candidate.name.lowercase(Locale.ROOT) == signal
            } ?: return null
            return AccommodationAnalysisResult.Evidence(mappedSource, mappedSignal)
        }
    }

    private companion object {
        const val CHAT_COMPLETIONS_PATH = "chat/completions"
        const val SCHEMA_NAME = "accommodation_analysis"
        const val MAX_IMAGES_PER_CANDIDATE = 3
        const val MAX_NAME_LENGTH = 200
        const val MAX_DESCRIPTIONS = 8
        const val MAX_TEXT_LENGTH = 2_000
        const val MAX_AMENITIES = 50
        const val MAX_AMENITY_LENGTH = 200
        const val SYSTEM_PROMPT =
            "Classify only the managed GLAMPING concept. GLAMPING broadly includes equipped " +
                "tents, domes, yurts, safari tents, tiny houses, and detached cabins in a " +
                "natural setting. Exclude ordinary hotel rooms, apartment blocks, empty camping " +
                "pitches, and standard cottages without glamping signals. Return only the strict " +
                "schema. Never add rationale or invent facts. MATCH requires an explicit glamping " +
                "self-description or at least two independent positive signals; one signal is " +
                "at most PROBABLE. Use only supplied candidate IDs and bounded evidence codes."
    }
}
