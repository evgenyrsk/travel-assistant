package com.travelassistant.backend.infrastructure.accommodation

import com.travelassistant.backend.application.accommodation.AccommodationAnalysisRequest
import com.travelassistant.backend.application.accommodation.AccommodationAnalysisResult
import com.travelassistant.backend.domain.hotel.AccommodationConcept
import com.travelassistant.backend.domain.hotel.AccommodationMatchVerdict
import com.travelassistant.backend.infrastructure.llm.OpenRouterApiKey
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OpenRouterAccommodationAnalysisClientTest {

    @Test
    fun `sends strict private multimodal request and maps bounded result`() = runBlocking {
        var capturedRequest: HttpRequestData? = null
        val httpClient = mockClient { request ->
            capturedRequest = request
            successResponse(
                decisionResponse(
                    candidateId = "candidate-01",
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
                        "https://images.example.test/photo.jpg",
                        "https://evil.example.test/private.jpg",
                        "https://images.example.test/photo.jpg?token=secret",
                    ),
                ),
            ),
        )

        val completed = assertIs<AccommodationAnalysisResult.Completed>(result)
        assertEquals(AccommodationMatchVerdict.MATCH, completed.decisions.single().verdict)
        assertEquals(
            "Bearer synthetic-key",
            capturedRequest?.headers?.get(HttpHeaders.Authorization),
        )
        val bodyText = (capturedRequest?.body as TextContent).text
        val body = Json.parseToJsonElement(bodyText).jsonObject
        val provider = body.getValue("provider").jsonObject
        assertEquals(
            listOf("synthetic/eu"),
            provider.getValue("only").jsonArray.map { value -> value.jsonPrimitive.content },
        )
        assertFalse(provider.getValue("allow_fallbacks").jsonPrimitive.content.toBoolean())
        assertTrue(provider.getValue("require_parameters").jsonPrimitive.content.toBoolean())
        assertEquals("deny", provider.getValue("data_collection").jsonPrimitive.content)
        assertTrue(provider.getValue("zdr").jsonPrimitive.content.toBoolean())
        assertEquals(0, body.getValue("temperature").jsonPrimitive.content.toInt())
        assertTrue(
            body.getValue("response_format").jsonObject
                .getValue("json_schema").jsonObject
                .getValue("strict").jsonPrimitive.content.toBoolean(),
        )
        assertTrue(bodyText.contains("https://images.example.test/photo.jpg"))
        assertFalse(bodyText.contains("evil.example.test"))
        assertFalse(bodyText.contains("token=secret"))
        assertFalse(bodyText.contains("sessionId"))
        assertFalse(bodyText.contains("hotelSearchId"))
        assertFalse(bodyText.contains("offerId"))
        assertFalse(bodyText.contains("providerReference"))
        httpClient.close()
    }

    @Test
    fun `batches at configured size without retry`() = runBlocking {
        var callCount = 0
        val httpClient = mockClient { request ->
            callCount += 1
            val ids = schemaCandidateIds((request.body as TextContent).text)
            successResponse(decisionResponse(ids))
        }
        val result = client(httpClient, batchSize = 2).analyze(
            request(
                candidate("candidate-01"),
                candidate("candidate-02"),
                candidate("candidate-03"),
            ),
        )

        val completed = assertIs<AccommodationAnalysisResult.Completed>(result)
        assertEquals(3, completed.decisions.size)
        assertEquals(2, callCount)
        httpClient.close()
    }

    @Test
    fun `rejects unknown signal and maps provider failure without retry`() = runBlocking {
        var invalidCalls = 0
        val invalidClient = mockClient {
            invalidCalls += 1
            successResponse(
                decisionResponse(
                    candidateId = "candidate-01",
                    verdict = "match",
                    source = "name",
                    signal = "raw_model_reason",
                ),
            )
        }
        assertEquals(
            AccommodationAnalysisResult.Failed(
                AccommodationAnalysisResult.FailureReason.INVALID_RESPONSE,
            ),
            client(invalidClient).analyze(request(candidate("candidate-01"))),
        )
        assertEquals(1, invalidCalls)
        invalidClient.close()

        var unavailableCalls = 0
        val unavailableClient = mockClient {
            unavailableCalls += 1
            respond(
                content = "{}",
                status = HttpStatusCode.TooManyRequests,
                headers = jsonHeaders(),
            )
        }
        assertEquals(
            AccommodationAnalysisResult.Failed(
                AccommodationAnalysisResult.FailureReason.RATE_LIMITED,
            ),
            client(unavailableClient).analyze(request(candidate("candidate-01"))),
        )
        assertEquals(1, unavailableCalls)
        unavailableClient.close()
    }

    private fun client(
        httpClient: HttpClient,
        batchSize: Int = 5,
    ) = OpenRouterAccommodationAnalysisClient(
        httpClient = httpClient,
        config = OpenRouterAccommodationAnalysisConfig(
            apiKey = OpenRouterApiKey.of("synthetic-key"),
            model = "synthetic/vision-model",
            providerEndpoint = "synthetic/eu",
            imageHosts = setOf("images.example.test"),
            baseUrl = "https://router.example.test/api/v1/",
            timeoutMillis = 5_000,
            batchSize = batchSize,
        ),
    )

    private fun request(
        vararg candidates: AccommodationAnalysisRequest.Candidate,
    ) = AccommodationAnalysisRequest(
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
        respond(
            content = buildJsonObject {
                putJsonArray("choices") {
                    add(
                        buildJsonObject {
                            putJsonObject("message") { put("content", content) }
                        },
                    )
                }
            }.toString(),
            status = HttpStatusCode.OK,
            headers = jsonHeaders(),
        )

    private fun decisionResponse(
        candidateId: String,
        verdict: String,
        source: String,
        signal: String,
    ): String = decisionResponse(listOf(candidateId), verdict, source, signal)

    private fun decisionResponse(
        candidateIds: List<String>,
        verdict: String = "unknown",
        source: String? = null,
        signal: String? = null,
    ): String =
        buildJsonObject {
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

    private fun schemaCandidateIds(bodyText: String): List<String> {
        val body = Json.parseToJsonElement(bodyText).jsonObject
        val schema = body.getValue("response_format").jsonObject
            .getValue("json_schema").jsonObject
            .getValue("schema").jsonObject
        val candidateIdSchema = schema.getValue("properties").jsonObject
            .getValue("results").jsonObject
            .getValue("items").jsonObject
            .getValue("properties").jsonObject
            .getValue("candidate_id").jsonObject
        return candidateIdSchema.getValue("enum").jsonArray.map { value ->
            value.jsonPrimitive.content
        }
    }

    private fun jsonHeaders() = headersOf(
        HttpHeaders.ContentType,
        ContentType.Application.Json.toString(),
    )
}
