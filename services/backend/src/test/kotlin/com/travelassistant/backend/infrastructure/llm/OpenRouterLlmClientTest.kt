package com.travelassistant.backend.infrastructure.llm

import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmCandidateRequest
import com.travelassistant.backend.application.llm.LlmClientResponse
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
        val client = client(httpClient, apiKey = apiKey)
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
        val extractedConstraints = schema.getValue("properties").jsonObject
            .getValue("extractedConstraints").jsonObject
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
            extractedConstraints.getValue("properties").jsonObject.keys,
        )

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
                LlmClientResponse.Empty,
                client(httpClient).generateCandidate(safeRequest()),
            )
            httpClient.close()
        }
    }

    @Test
    fun `maps malformed or provider-error responses to safe failure`() = runBlocking {
        val invalidCandidate = candidateContent(outcome = "NOT_A_REAL_OUTCOME")
        val responseBodies = listOf(
            "{}",
            completionResponse(content = "not-json"),
            completionResponse(content = invalidCandidate),
            """{"choices":[{"finish_reason":"error","message":{"content":null}}]}""",
            """{"choices":[{"error":{"code":500},"message":{"content":null}}]}""",
        )

        responseBodies.forEach { responseBody ->
            val httpClient = mockClient { successfulResponseBody(responseBody) }

            assertEquals(
                LlmClientResponse.Failure,
                client(httpClient).generateCandidate(safeRequest()),
            )
            httpClient.close()
        }
    }

    @Test
    fun `maps unsuccessful status and non-JSON success to safe failure`() = runBlocking {
        val cases = listOf(
            HttpStatusCode.BadRequest to ContentType.Application.Json,
            HttpStatusCode.Unauthorized to ContentType.Application.Json,
            HttpStatusCode.TooManyRequests to ContentType.Application.Json,
            HttpStatusCode.InternalServerError to ContentType.Application.Json,
            HttpStatusCode.OK to ContentType.Text.Plain,
        )

        cases.forEach { (status, contentType) ->
            val httpClient = mockClient {
                respond(
                    content = "sensitive-provider-error-body",
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, contentType.toString()),
                )
            }

            assertEquals(
                LlmClientResponse.Failure,
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
            LlmClientResponse.Failure,
            client(timeoutClient, timeoutMillis = 10).generateCandidate(safeRequest()),
        )
        assertEquals(
            LlmClientResponse.Failure,
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
    ): OpenRouterLlmClient =
        OpenRouterLlmClient(
            httpClient = httpClient,
            config = OpenRouterConfig(
                apiKey = OpenRouterApiKey.of(apiKey),
                model = "provider/model-under-test",
                baseUrl = "https://openrouter.test/api/v1",
                timeoutMillis = timeoutMillis,
            ),
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
                    put("children-ages", JsonNull)
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
}
