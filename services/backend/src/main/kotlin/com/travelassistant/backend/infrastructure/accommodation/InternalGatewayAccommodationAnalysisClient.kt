package com.travelassistant.backend.infrastructure.accommodation

import com.travelassistant.backend.application.accommodation.AccommodationAnalysisClient
import com.travelassistant.backend.application.accommodation.AccommodationAnalysisRequest
import com.travelassistant.backend.application.accommodation.AccommodationAnalysisResult
import com.travelassistant.backend.domain.hotel.AccommodationEvidenceSource
import com.travelassistant.backend.domain.hotel.AccommodationMatchVerdict
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
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

internal class InternalGatewayAccommodationAnalysisClient(
    private val httpClient: HttpClient,
    private val config: InternalGatewayAccommodationAnalysisConfig,
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
            val response = httpClient.post(config.endpointUrl) {
                expectSuccess = false
                timeout { requestTimeoutMillis = config.timeoutMillis }
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer ${config.accessToken.reveal()}")
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

            val responseDto = RESPONSE_JSON.decodeFromString<AnalysisResponseDto>(response.body())
            responseDto.toApplicationResult(config.deploymentId)
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
            put("schema_version", SCHEMA_VERSION)
            put("deployment_id", config.deploymentId)
            put("concept", request.concept.code)
            putJsonArray("candidates") {
                request.candidates.forEach { candidate ->
                    add(
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
                            putJsonArray("image_urls") {
                                candidate.imageUrls
                                    .mapNotNull(imageUrlPolicy::allowedOrNull)
                                    .distinct()
                                    .take(MAX_IMAGES_PER_CANDIDATE)
                                    .forEach(::add)
                            }
                        },
                    )
                }
            }
        }.toString()

    private fun String.bounded(maxLength: Int): String = trim().take(maxLength)

    private fun String.boundedOrNull(maxLength: Int): String? =
        trim().takeIf(String::isNotEmpty)?.take(maxLength)

    private fun failureForStatus(status: Int): AccommodationAnalysisResult.FailureReason =
        when (status) {
            400, 422 -> AccommodationAnalysisResult.FailureReason.REQUEST_REJECTED
            401, 403 -> AccommodationAnalysisResult.FailureReason.AUTHENTICATION_FAILED
            408, 504 -> AccommodationAnalysisResult.FailureReason.TIMEOUT
            429 -> AccommodationAnalysisResult.FailureReason.RATE_LIMITED
            else -> AccommodationAnalysisResult.FailureReason.UNAVAILABLE
        }

    @Serializable
    private data class AnalysisResponseDto(
        @SerialName("schema_version")
        val schemaVersion: String,
        @SerialName("deployment_id")
        val deploymentId: String,
        val results: List<DecisionDto>,
    ) {
        fun toApplicationResult(expectedDeploymentId: String): AccommodationAnalysisResult.Completed? {
            if (schemaVersion != SCHEMA_VERSION || deploymentId != expectedDeploymentId) {
                return null
            }
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
        val RESPONSE_JSON = Json {
            ignoreUnknownKeys = false
            isLenient = false
            coerceInputValues = false
        }
        const val SCHEMA_VERSION = "1"
        const val MAX_IMAGES_PER_CANDIDATE = 3
        const val MAX_NAME_LENGTH = 200
        const val MAX_DESCRIPTIONS = 8
        const val MAX_TEXT_LENGTH = 2_000
        const val MAX_AMENITIES = 50
        const val MAX_AMENITY_LENGTH = 200
    }
}
