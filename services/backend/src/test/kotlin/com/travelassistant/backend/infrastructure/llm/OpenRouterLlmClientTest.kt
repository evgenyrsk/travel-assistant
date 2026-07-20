package com.travelassistant.backend.infrastructure.llm

import com.travelassistant.backend.application.llm.GenerateLlmCandidateUseCase
import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmCandidateRequest
import com.travelassistant.backend.application.llm.LlmCandidateValidationResult
import com.travelassistant.backend.application.llm.LlmClientResponse
import com.travelassistant.backend.application.llm.LlmClientRetryableFailureReason
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
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OpenRouterLlmClientTest {

    @Test
    fun `posts strict structured request and maps candidate without runtime wiring`() = runBlocking {
        val apiKey = "synthetic-openrouter-api-key"
        var capturedRequest: HttpRequestData? = null
        val httpClient = mockClient { request ->
            capturedRequest = request
            successfulResponse(candidateContent())
        }
        val diagnosticEvents = mutableListOf<OpenRouterDiagnosticEvent>()
        val client = client(
            httpClient = httpClient,
            apiKey = apiKey,
            diagnosticObserver = OpenRouterDiagnosticObserver(diagnosticEvents::add),
        )
        val request = LlmCandidateRequest(
            userMessage = "Найди отель в Казани",
            confirmedConstraints = mapOf("adults" to "2", "destination" to "Казань"),
            missingRequiredFields = listOf("check-in", "check-out", "rooms"),
        )

        val result = client.generateCandidate(request)

        assertEquals(
            LlmClientResponse.Candidate(
                LlmCandidate(
                    outcome = LlmCandidate.Outcome.NEEDS_CLARIFICATION,
                    intent = LlmCandidate.Intent.HOTEL_SEARCH,
                    extractedConstraints = mapOf(
                        "destination" to "Казань",
                        "adults" to "2",
                    ),
                    missingRequiredFields = listOf("check-in", "check-out", "rooms"),
                    clarificationQuestion = "Укажите даты и количество номеров.",
                ),
            ),
            result,
        )
        assertEquals(listOf(OpenRouterDiagnosticEvent.CANDIDATE_DECODED), diagnosticEvents)
        assertEquals(
            "https://openrouter.test/api/v1/chat/completions",
            capturedRequest?.url.toString(),
        )
        assertEquals("Bearer $apiKey", capturedRequest?.headers?.get(HttpHeaders.Authorization))
        assertEquals(
            ContentType.Application.Json.toString(),
            capturedRequest?.headers?.get(HttpHeaders.Accept),
        )

        val content = assertIs<TextContent>(capturedRequest?.body)
        assertEquals(ContentType.Application.Json, content.contentType)
        val body = Json.parseToJsonElement(content.text).jsonObject
        assertEquals("provider/model-under-test", body.getValue("model").jsonPrimitive.content)
        assertFalse(body.getValue("stream").jsonPrimitive.boolean)
        assertEquals(0.0, body.getValue("temperature").jsonPrimitive.double)
        assertTrue(
            body.getValue("provider").jsonObject
                .getValue("require_parameters").jsonPrimitive.boolean,
        )
        assertFalse(body.containsKey("plugins"))
        assertFalse(body.containsKey("tools"))
        assertFalse(body.containsKey("tool_choice"))

        val messages = body.getValue("messages").jsonArray
        assertEquals(listOf("system", "user"), messages.map { message ->
            message.jsonObject.getValue("role").jsonPrimitive.content
        })
        val systemPrompt = messages[0].jsonObject.getValue("content").jsonPrimitive.content
        assertTrue(systemPrompt.contains("Use null, never an empty string"))
        assertTrue(systemPrompt.contains("For a complete consistent hotel request"))
        assertTrue(systemPrompt.contains("clarification question in Russian"))
        val promptPayload = Json.parseToJsonElement(
            messages[1].jsonObject.getValue("content").jsonPrimitive.content,
        ).jsonObject
        assertEquals(request.userMessage, promptPayload.getValue("userMessage").jsonPrimitive.content)
        assertEquals(
            request.confirmedConstraints,
            promptPayload.getValue("confirmedConstraints").jsonObject
                .mapValues { (_, value) -> value.jsonPrimitive.content },
        )
        assertEquals(
            request.missingRequiredFields,
            promptPayload.getValue("missingRequiredFields").jsonArray
                .map { value -> value.jsonPrimitive.content },
        )

        val responseFormat = body.getValue("response_format").jsonObject
        assertEquals("json_schema", responseFormat.getValue("type").jsonPrimitive.content)
        val jsonSchema = responseFormat.getValue("json_schema").jsonObject
        assertTrue(jsonSchema.getValue("strict").jsonPrimitive.boolean)
        val schema = jsonSchema.getValue("schema").jsonObject
        assertFalse(schema.getValue("additionalProperties").jsonPrimitive.boolean)
        val properties = schema.getValue("properties").jsonObject
        val extractedConstraints = properties
            .getValue("extractedConstraints").jsonObject
        val constraintProperties = extractedConstraints.getValue("properties").jsonObject
        assertEquals(
            setOf(
                "destination",
                "check-in",
                "check-out",
                "adults",
                "children",
                "children-ages",
                "rooms",
            ),
            constraintProperties.keys,
        )
        assertTrue(constraintProperties.values.all { property ->
            property.jsonObject.getValue("description").jsonPrimitive.content.isNotBlank()
        })
        assertTrue(
            properties.getValue("outcome").jsonObject
                .getValue("description").jsonPrimitive.content.isNotBlank(),
        )
        assertTrue(
            properties.getValue("clarificationQuestion").jsonObject
                .getValue("description").jsonPrimitive.content.contains("in Russian"),
        )

        httpClient.close()
    }

    @Test
    fun `maps blank nullable wire constraint as absent`() = runBlocking {
        val httpClient = mockClient {
            successfulResponse(candidateContent(childrenAges = "   "))
        }

        val response = assertIs<LlmClientResponse.Candidate>(
            client(httpClient).generateCandidate(safeRequest()),
        )

        assertFalse(response.value.extractedConstraints.containsKey("children-ages"))
        httpClient.close()
    }

    @Test
    fun `accepts complete candidate with blank optional child ages`() = runBlocking {
        val httpClient = mockClient {
            successfulResponse(completeCandidateContent(childrenAges = "   "))
        }

        val result = GenerateLlmCandidateUseCase(client(httpClient))(safeRequest())
        val accepted = assertIs<LlmCandidateValidationResult.Accepted>(result)

        assertFalse(accepted.candidate.extractedConstraints.containsKey("children-ages"))
        httpClient.close()
    }

    @Test
    fun `maps absent choices or content to empty response`() = runBlocking {
        val responseBodies = listOf(
            """{"choices":[]}""",
            completionResponse(content = null),
            completionResponse(content = " "),
        )

        responseBodies.forEach { responseBody ->
            val httpClient = mockClient { successfulResponseBody(responseBody) }

            assertEquals(
                retryableFailure(LlmClientRetryableFailureReason.EMPTY_RESPONSE),
                client(httpClient).generateCandidate(safeRequest()),
            )
            httpClient.close()
        }
    }

    @Test
    fun `maps malformed or provider-error responses to safe failure`() = runBlocking {
        val invalidCandidate = candidateContent(outcome = "NOT_A_REAL_OUTCOME")
        val cases = listOf(
            "{}" to retryableFailure(LlmClientRetryableFailureReason.CLIENT_FAILURE),
            completionResponse(content = "not-json") to
                retryableFailure(LlmClientRetryableFailureReason.INVALID_CANDIDATE),
            completionResponse(content = invalidCandidate) to
                retryableFailure(LlmClientRetryableFailureReason.INVALID_CANDIDATE),
            """{"choices":[{"finish_reason":"error","message":{"content":null}}]}""" to
                retryableFailure(LlmClientRetryableFailureReason.CLIENT_FAILURE),
            """{"choices":[{"error":{"code":500},"message":{"content":null}}]}""" to
                retryableFailure(LlmClientRetryableFailureReason.CLIENT_FAILURE),
        )

        cases.forEach { (responseBody, expectedResponse) ->
            val httpClient = mockClient { successfulResponseBody(responseBody) }

            assertEquals(
                expectedResponse,
                client(httpClient).generateCandidate(safeRequest()),
            )
            httpClient.close()
        }
    }

    @Test
    fun `reports only safe diagnostic categories for unsuccessful outcomes`() = runBlocking {
        val cases = listOf(
            DiagnosticCase(
                responseBody = "sensitive-invalid-request",
                status = HttpStatusCode.BadRequest,
                expectedEvent = OpenRouterDiagnosticEvent.REQUEST_REJECTED,
            ),
            DiagnosticCase(
                responseBody = "sensitive-authentication-error",
                status = HttpStatusCode.Unauthorized,
                expectedEvent = OpenRouterDiagnosticEvent.AUTHENTICATION_FAILED,
            ),
            DiagnosticCase(
                responseBody = "sensitive-credit-error",
                status = HttpStatusCode.PaymentRequired,
                expectedEvent = OpenRouterDiagnosticEvent.INSUFFICIENT_CREDITS,
            ),
            DiagnosticCase(
                responseBody = "sensitive-rate-limit-error",
                status = HttpStatusCode.TooManyRequests,
                expectedEvent = OpenRouterDiagnosticEvent.RATE_LIMITED,
            ),
            DiagnosticCase(
                responseBody = "sensitive-unavailable-error",
                status = HttpStatusCode.ServiceUnavailable,
                expectedEvent = OpenRouterDiagnosticEvent.PROVIDER_UNAVAILABLE,
                expectedResponse = retryableFailure(
                    LlmClientRetryableFailureReason.CLIENT_FAILURE,
                ),
            ),
            DiagnosticCase(
                responseBody =
                    """{"error":{"code":504,"message":"sensitive-timeout","metadata":{"error_type":"timeout"}}}""",
                expectedEvent = OpenRouterDiagnosticEvent.TIMEOUT,
                expectedResponse = retryableFailure(
                    LlmClientRetryableFailureReason.CLIENT_FAILURE,
                ),
            ),
            DiagnosticCase(
                responseBody = "sensitive-non-json-body",
                contentType = ContentType.Text.Plain,
                expectedEvent = OpenRouterDiagnosticEvent.NON_JSON_RESPONSE,
            ),
            DiagnosticCase(
                responseBody = "not-json",
                expectedEvent = OpenRouterDiagnosticEvent.MALFORMED_RESPONSE,
                expectedResponse = retryableFailure(
                    LlmClientRetryableFailureReason.CLIENT_FAILURE,
                ),
            ),
            DiagnosticCase(
                responseBody = """{"choices":[]}""",
                expectedEvent = OpenRouterDiagnosticEvent.EMPTY_CHOICES,
                expectedResponse = retryableFailure(
                    LlmClientRetryableFailureReason.EMPTY_RESPONSE,
                ),
            ),
            DiagnosticCase(
                responseBody = completionResponse(content = null),
                expectedEvent = OpenRouterDiagnosticEvent.EMPTY_CONTENT,
                expectedResponse = retryableFailure(
                    LlmClientRetryableFailureReason.EMPTY_RESPONSE,
                ),
            ),
            DiagnosticCase(
                responseBody = completionResponse(content = "not-candidate-json"),
                expectedEvent = OpenRouterDiagnosticEvent.INVALID_CANDIDATE,
                expectedResponse = retryableFailure(
                    LlmClientRetryableFailureReason.INVALID_CANDIDATE,
                ),
            ),
        )

        cases.forEach { case ->
            val events = mutableListOf<OpenRouterDiagnosticEvent>()
            val httpClient = mockClient {
                respond(
                    content = case.responseBody,
                    status = case.status,
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        case.contentType.toString(),
                    ),
                )
            }

            val response = client(
                httpClient = httpClient,
                diagnosticObserver = OpenRouterDiagnosticObserver(events::add),
            ).generateCandidate(safeRequest())

            assertEquals(case.expectedResponse, response)
            assertEquals(listOf(case.expectedEvent), events)
            httpClient.close()
        }
    }

    @Test
    fun `reports network failure without exposing exception details`() = runBlocking {
        val events = mutableListOf<OpenRouterDiagnosticEvent>()
        val httpClient = mockClient {
            throw IOException("sensitive-network-details")
        }

        assertEquals(
            retryableFailure(LlmClientRetryableFailureReason.CLIENT_FAILURE),
            client(
                httpClient = httpClient,
                diagnosticObserver = OpenRouterDiagnosticObserver(events::add),
            ).generateCandidate(safeRequest()),
        )
        assertEquals(listOf(OpenRouterDiagnosticEvent.NETWORK_FAILURE), events)
        httpClient.close()
    }

    @Test
    fun `diagnostic observer failure does not change candidate result`() = runBlocking {
        val httpClient = mockClient { successfulResponse(candidateContent()) }

        val response = client(
            httpClient = httpClient,
            diagnosticObserver = OpenRouterDiagnosticObserver {
                error("sensitive-observer-error")
            },
        ).generateCandidate(safeRequest())

        assertIs<LlmClientResponse.Candidate>(response)
        httpClient.close()
    }

    @Test
    fun `maps unsuccessful status and non-JSON success to safe failure`() = runBlocking {
        val cases = listOf(
            Triple(
                HttpStatusCode.BadRequest,
                ContentType.Application.Json,
                LlmClientResponse.Failure,
            ),
            Triple(
                HttpStatusCode.Unauthorized,
                ContentType.Application.Json,
                LlmClientResponse.Failure,
            ),
            Triple(
                HttpStatusCode.TooManyRequests,
                ContentType.Application.Json,
                LlmClientResponse.Failure,
            ),
            Triple(
                HttpStatusCode.InternalServerError,
                ContentType.Application.Json,
                retryableFailure(LlmClientRetryableFailureReason.CLIENT_FAILURE),
            ),
            Triple(
                HttpStatusCode.Conflict,
                ContentType.Application.Json,
                LlmClientResponse.Failure,
            ),
            Triple(
                HttpStatusCode.OK,
                ContentType.Text.Plain,
                LlmClientResponse.Failure,
            ),
        )

        cases.forEach { (status, contentType, expectedResponse) ->
            val httpClient = mockClient {
                respond(
                    content = "sensitive-provider-error-body",
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, contentType.toString()),
                )
            }

            assertEquals(
                expectedResponse,
                client(httpClient).generateCandidate(safeRequest()),
            )
            httpClient.close()
        }
    }

    @Test
    fun `maps timeout and network failure without exposing causes`() = runBlocking {
        val timeoutClient = mockClient {
            delay(100)
            successfulResponse(candidateContent())
        }
        val networkClient = mockClient {
            throw IOException("sensitive-network-details")
        }

        assertEquals(
            retryableFailure(LlmClientRetryableFailureReason.CLIENT_FAILURE),
            client(timeoutClient, timeoutMillis = 10).generateCandidate(safeRequest()),
        )
        assertEquals(
            retryableFailure(LlmClientRetryableFailureReason.CLIENT_FAILURE),
            client(networkClient).generateCandidate(safeRequest()),
        )
        timeoutClient.close()
        networkClient.close()
    }

    @Test
    fun `propagates coroutine cancellation`() = runBlocking {
        val httpClient = mockClient {
            throw CancellationException("OpenRouter request cancelled")
        }

        assertFailsWith<CancellationException> {
            client(httpClient).generateCandidate(safeRequest())
        }
        httpClient.close()
    }

    @Test
    fun `does not add optional attribution or session headers`() = runBlocking {
        var capturedRequest: HttpRequestData? = null
        val httpClient = mockClient { request ->
            capturedRequest = request
            successfulResponse(candidateContent())
        }

        client(httpClient).generateCandidate(safeRequest())

        assertNull(capturedRequest?.headers?.get("HTTP-Referer"))
        assertNull(capturedRequest?.headers?.get("X-OpenRouter-Title"))
        assertNull(capturedRequest?.headers?.get("Cookie"))
        httpClient.close()
    }

    private fun client(
        httpClient: HttpClient,
        apiKey: String = "synthetic-api-key",
        timeoutMillis: Long = 5_000,
        diagnosticObserver: OpenRouterDiagnosticObserver = OpenRouterDiagnosticObserver.NONE,
    ): OpenRouterLlmClient =
        OpenRouterLlmClient(
            httpClient = httpClient,
            config = OpenRouterConfig(
                apiKey = OpenRouterApiKey.of(apiKey),
                model = "provider/model-under-test",
                baseUrl = "https://openrouter.test/api/v1",
                timeoutMillis = timeoutMillis,
            ),
            diagnosticObserver = diagnosticObserver,
        )

    private fun safeRequest(): LlmCandidateRequest =
        LlmCandidateRequest(
            userMessage = "Найди отель в Казани",
            confirmedConstraints = mapOf("destination" to "Казань"),
            missingRequiredFields = listOf("check-in", "check-out", "adults", "rooms"),
        )

    private fun mockClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): HttpClient =
        HttpClient(MockEngine(handler)) {
            install(HttpTimeout)
        }

    private fun MockRequestHandleScope.successfulResponse(candidate: String): HttpResponseData =
        respond(
            content = completionResponse(candidate),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )

    private fun MockRequestHandleScope.successfulResponseBody(body: String): HttpResponseData =
        respond(
            content = body,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )

    private fun completionResponse(content: String?): String =
        buildJsonObject {
            put(
                "choices",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("finish_reason", "stop")
                            put(
                                "message",
                                buildJsonObject {
                                    if (content == null) {
                                        put("content", JsonNull)
                                    } else {
                                        put("content", content)
                                    }
                                },
                            )
                        },
                    )
                },
            )
        }.toString()

    private fun candidateContent(
        outcome: String = "NEEDS_CLARIFICATION",
        childrenAges: String? = null,
    ): String =
        buildJsonObject {
            put("outcome", outcome)
            put("intent", "HOTEL_SEARCH")
            put(
                "extractedConstraints",
                buildJsonObject {
                    put("destination", "Казань")
                    put("check-in", JsonNull)
                    put("check-out", JsonNull)
                    put("adults", "2")
                    put("children", JsonNull)
                    if (childrenAges == null) {
                        put("children-ages", JsonNull)
                    } else {
                        put("children-ages", childrenAges)
                    }
                    put("rooms", JsonNull)
                },
            )
            put(
                "missingRequiredFields",
                buildJsonArray {
                    add("check-in")
                    add("check-out")
                    add("rooms")
                },
            )
            put("conflicts", buildJsonArray {})
            put("clarificationQuestion", "Укажите даты и количество номеров.")
            put("warnings", buildJsonArray {})
        }.toString()

    private fun completeCandidateContent(childrenAges: String?): String =
        buildJsonObject {
            put("outcome", "INTERPRETED")
            put("intent", "HOTEL_SEARCH")
            put(
                "extractedConstraints",
                buildJsonObject {
                    put("destination", "Казань")
                    put("check-in", "2026-08-10")
                    put("check-out", "2026-08-14")
                    put("adults", "2")
                    put("children", "0")
                    if (childrenAges == null) {
                        put("children-ages", JsonNull)
                    } else {
                        put("children-ages", childrenAges)
                    }
                    put("rooms", "1")
                },
            )
            put("missingRequiredFields", buildJsonArray {})
            put("conflicts", buildJsonArray {})
            put("clarificationQuestion", JsonNull)
            put("warnings", buildJsonArray {})
        }.toString()

    private data class DiagnosticCase(
        val responseBody: String,
        val status: HttpStatusCode = HttpStatusCode.OK,
        val contentType: ContentType = ContentType.Application.Json,
        val expectedEvent: OpenRouterDiagnosticEvent,
        val expectedResponse: LlmClientResponse = LlmClientResponse.Failure,
    )

    private fun retryableFailure(
        reason: LlmClientRetryableFailureReason,
    ): LlmClientResponse.RetryableFailure =
        LlmClientResponse.RetryableFailure(reason)
}
