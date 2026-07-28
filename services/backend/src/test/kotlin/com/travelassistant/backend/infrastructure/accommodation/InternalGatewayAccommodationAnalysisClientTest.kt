package com.travelassistant.backend.infrastructure.accommodation

import com.travelassistant.backend.application.accommodation.AccommodationAnalysisRequest
import com.travelassistant.backend.application.accommodation.AccommodationAnalysisResult
import com.travelassistant.backend.domain.hotel.AccommodationConcept
import com.travelassistant.backend.domain.hotel.AccommodationMatchVerdict
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class InternalGatewayAccommodationAnalysisClientTest {

    @Test
    fun `sends versioned provider neutral request and maps typed result`() = runBlocking {
        var capturedRequest: HttpRequestData? = null
        val httpClient = mockClient { request ->
            capturedRequest = request
            successResponse(
                response(
                    candidateIds = listOf("candidate-01"),
                    verdict = "match",
                    source = "name",
                    signal = "explicit_glamping_label",
                ),
            )
        }

        val result = client(httpClient).analyze(
            request(
                AccommodationAnalysisRequest.Candidate(
                    ephemeralCandidateId = "candidate-01",
                    hotelName = "Synthetic Glamping",
                    descriptions = listOf("Synthetic description"),
                    amenities = listOf("Synthetic amenity"),
                    imageUrls = listOf(
                        "https://images.internal.test/one.jpg",
                        "https://images.internal.test/two.jpg",
                        "https://images.internal.test/three.jpg",
                        "https://images.internal.test/four.jpg",
                        "https://outside.test/private.jpg",
                    ),
                ),
            ),
        )

        val completed = assertIs<AccommodationAnalysisResult.Completed>(result)
        assertEquals(AccommodationMatchVerdict.MATCH, completed.decisions.single().verdict)
        assertEquals(
            "https://semantic.internal.test/v1/accommodation-analysis",
            capturedRequest?.url.toString(),
        )
        assertEquals(
            "Bearer synthetic-token",
            capturedRequest?.headers?.get(HttpHeaders.Authorization),
        )
        val bodyText = (capturedRequest?.body as TextContent).text
        val body = Json.parseToJsonElement(bodyText).jsonObject
        assertEquals("1", body.getValue("schema_version").jsonPrimitive.content)
        assertEquals("vision-balanced-v1", body.getValue("deployment_id").jsonPrimitive.content)
        assertEquals("glamping", body.getValue("concept").jsonPrimitive.content)
        val candidate = body.getValue("candidates").jsonArray.single().jsonObject
        assertEquals(3, candidate.getValue("image_urls").jsonArray.size)
        assertFalse(bodyText.contains("outside.test"))
        assertFalse(bodyText.contains("provider.only"))
        assertFalse(bodyText.contains("sessionId"))
        assertFalse(bodyText.contains("hotelSearchId"))
        assertFalse(bodyText.contains("offerId"))
        httpClient.close()
    }

    @Test
    fun `batches without retry and rejects deployment drift`() = runBlocking {
        var batchCalls = 0
        val batchingClient = mockClient { request ->
            batchCalls += 1
            val ids = requestCandidateIds((request.body as TextContent).text)
            successResponse(response(ids))
        }
        val completed = assertIs<AccommodationAnalysisResult.Completed>(
            client(batchingClient, batchSize = 2).analyze(
                request(
                    candidate("candidate-01"),
                    candidate("candidate-02"),
                    candidate("candidate-03"),
                ),
            ),
        )
        assertEquals(3, completed.decisions.size)
        assertEquals(2, batchCalls)
        batchingClient.close()

        var driftCalls = 0
        val driftClient = mockClient {
            driftCalls += 1
            successResponse(response(listOf("candidate-01"), deploymentId = "unexpected-v2"))
        }
        assertEquals(
            AccommodationAnalysisResult.Failed(
                AccommodationAnalysisResult.FailureReason.INVALID_RESPONSE,
            ),
            client(driftClient).analyze(request(candidate("candidate-01"))),
        )
        assertEquals(1, driftCalls)
        driftClient.close()

        val schemaDriftClient = mockClient {
            successResponse(response(listOf("candidate-01"), schemaVersion = "2"))
        }
        assertEquals(
            AccommodationAnalysisResult.Failed(
                AccommodationAnalysisResult.FailureReason.INVALID_RESPONSE,
            ),
            client(schemaDriftClient).analyze(request(candidate("candidate-01"))),
        )
        schemaDriftClient.close()
    }

    @Test
    fun `maps gateway failures without retry`() = runBlocking {
        var calls = 0
        val httpClient = mockClient {
            calls += 1
            respond(
                content = "{}",
                status = HttpStatusCode.Unauthorized,
                headers = jsonHeaders(),
            )
        }

        assertEquals(
            AccommodationAnalysisResult.Failed(
                AccommodationAnalysisResult.FailureReason.AUTHENTICATION_FAILED,
            ),
            client(httpClient).analyze(request(candidate("candidate-01"))),
        )
        assertEquals(1, calls)
        httpClient.close()
    }

    private fun client(httpClient: HttpClient, batchSize: Int = 6) =
        InternalGatewayAccommodationAnalysisClient(
            httpClient = httpClient,
            config = InternalGatewayAccommodationAnalysisConfig(
                endpointUrl = "https://semantic.internal.test/v1/accommodation-analysis",
                deploymentId = "vision-balanced-v1",
                accessToken = InternalGatewayAccessToken.of("synthetic-token"),
                imageHosts = setOf("images.internal.test"),
                timeoutMillis = 5_000,
                batchSize = batchSize,
            ),
        )

    private fun request(vararg candidates: AccommodationAnalysisRequest.Candidate) =
        AccommodationAnalysisRequest(
            concept = AccommodationConcept.GLAMPING,
            candidates = candidates.toList(),
        )

    private fun candidate(id: String) =
        AccommodationAnalysisRequest.Candidate(id, "Synthetic candidate")

    private fun mockClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): HttpClient = HttpClient(MockEngine(handler)) {
        install(HttpTimeout)
    }

    private fun MockRequestHandleScope.successResponse(content: String): HttpResponseData =
        respond(content, HttpStatusCode.OK, jsonHeaders())

    private fun response(
        candidateIds: List<String>,
        verdict: String = "unknown",
        source: String? = null,
        signal: String? = null,
        deploymentId: String = "vision-balanced-v1",
        schemaVersion: String = "1",
    ): String =
        buildJsonObject {
            put("schema_version", schemaVersion)
            put("deployment_id", deploymentId)
            putJsonArray("results") {
                candidateIds.forEach { candidateId ->
                    add(
                        buildJsonObject {
                            put("candidate_id", candidateId)
                            put("verdict", verdict)
                            putJsonArray("evidence") {
                                if (source != null && signal != null) {
                                    add(
                                        buildJsonObject {
                                            put("source", source)
                                            put("signal", signal)
                                        },
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }.toString()

    private fun requestCandidateIds(bodyText: String): List<String> =
        Json.parseToJsonElement(bodyText).jsonObject
            .getValue("candidates").jsonArray
            .map { candidate -> candidate.jsonObject.getValue("candidate_id").jsonPrimitive.content }

    private fun jsonHeaders() = headersOf(
        HttpHeaders.ContentType,
        ContentType.Application.Json.toString(),
    )
}
