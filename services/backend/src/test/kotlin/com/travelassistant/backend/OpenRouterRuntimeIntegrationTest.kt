package com.travelassistant.backend

import com.travelassistant.backend.infrastructure.llm.LlmProviderConfig
import com.travelassistant.backend.infrastructure.llm.LlmProviderMode
import com.travelassistant.backend.infrastructure.llm.OpenRouterApiKey
import com.travelassistant.backend.infrastructure.llm.OpenRouterConfig
import com.travelassistant.backend.infrastructure.llm.OpenRouterDiagnosticEvent
import com.travelassistant.backend.infrastructure.llm.OpenRouterDiagnosticObserver
import com.travelassistant.backend.infrastructure.provider.HotelProviderConfig
import com.travelassistant.backend.infrastructure.provider.HotelProviderMode
import com.travelassistant.backend.infrastructure.provider.HotelsApiConfig
import com.travelassistant.backend.infrastructure.provider.HotelsApiTargetConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OpenRouterRuntimeIntegrationTest {

    @Test
    fun `default fake mode does not create OpenRouter HTTP client`() = testApplication {
        var openRouterClientCreated = false

        application {
            moduleWithProviderConfigs(
                llmProviderConfig = LlmProviderConfig(),
                openRouterHttpClientFactory = {
                    openRouterClientCreated = true
                    error("OpenRouter HTTP client must not be created in FAKE mode")
                },
            )
        }

        val sessionId = createSession()
        val response = sendAssistantMessage(sessionId, COMPLETE_HOTEL_REQUEST)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ask_clarification", body["nextAction"]?.jsonPrimitive?.content)
        assertFalse(openRouterClientCreated)
    }

    @Test
    fun `OpenRouter mode reaches existing confirmation response through runtime composition`() =
        testApplication {
            var requestCount = 0
            var capturedRequest: HttpRequestData? = null
            val openRouterClient = openRouterClient { request ->
                requestCount += 1
                capturedRequest = request
                respond(
                    content = completionResponse(interpretedCandidate()),
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders(),
                )
            }

            application {
                moduleWithProviderConfigs(
                    llmProviderConfig = openRouterProviderConfig(),
                    openRouterHttpClientFactory = { openRouterClient },
                )
            }

            val sessionId = createSession()
            val response = sendAssistantMessage(sessionId, COMPLETE_HOTEL_REQUEST)
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val assistantMessage = body["assistantMessage"]
                ?.jsonObject
                ?.get("content")
                ?.jsonPrimitive
                ?.content
                .orEmpty()

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(1, requestCount)
            assertEquals(
                "https://openrouter.test/api/v1/chat/completions",
                capturedRequest?.url.toString(),
            )
            assertEquals(
                "Bearer synthetic-openrouter-api-key",
                capturedRequest?.headers?.get(HttpHeaders.Authorization),
            )
            assertEquals("ask_clarification", body["nextAction"]?.jsonPrimitive?.content)
            assertTrue(assistantMessage.contains(CONFIRMATION_QUESTION))
            assertFalse(body.containsKey("hotelSearchId"))

            val requestBody = capturedRequest?.body as io.ktor.http.content.TextContent
            val schema = Json.parseToJsonElement(requestBody.text).jsonObject
                .getValue("response_format").jsonObject
                .getValue("json_schema").jsonObject
                .getValue("schema").jsonObject
            assertTrue(
                "preferencePatch" in schema.getValue("properties").jsonObject,
            )
        }

    @Test
    fun `OpenRouter failure returns safe boundary response without internal data`() =
        testApplication {
            val sensitiveBody = "raw-provider-error synthetic-openrouter-api-key test/model"
            val diagnosticEvents = mutableListOf<OpenRouterDiagnosticEvent>()
            var requestCount = 0
            var hotelRequestCount = 0
            val openRouterClient = openRouterClient {
                requestCount += 1
                respond(
                    content = sensitiveBody,
                    status = HttpStatusCode.TooManyRequests,
                    headers = jsonHeaders(),
                )
            }

            application {
                moduleWithProviderConfigs(
                    llmProviderConfig = openRouterProviderConfig(),
                    providerConfig = realHotelProviderConfig(),
                    openRouterHttpClientFactory = { openRouterClient },
                    realHotelHttpClientFactory = {
                        openRouterClient {
                            hotelRequestCount += 1
                            error("Hotels API must not be called after an OpenRouter failure")
                        }
                    },
                    openRouterDiagnosticObserver =
                        OpenRouterDiagnosticObserver(diagnosticEvents::add),
                )
            }

            val sessionId = createSession()
            val response = sendAssistantMessage(sessionId, COMPLETE_HOTEL_REQUEST)
            val responseText = response.bodyAsText()
            val body = Json.parseToJsonElement(responseText).jsonObject

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("show_boundary_message", body["nextAction"]?.jsonPrimitive?.content)
            assertFalse(body.containsKey("hotelSearchId"))
            assertFalse(responseText.contains(sensitiveBody))
            assertFalse(responseText.contains("synthetic-openrouter-api-key"))
            assertFalse(responseText.contains("test/model"))
            assertEquals(1, requestCount)
            assertEquals(0, hotelRequestCount)
            assertEquals(listOf(OpenRouterDiagnosticEvent.RATE_LIMITED), diagnosticEvents)
        }

    @Test
    fun `OpenRouter retries one unavailable response and reaches confirmation`() =
        testApplication {
            var requestCount = 0
            val diagnosticEvents = mutableListOf<OpenRouterDiagnosticEvent>()
            val openRouterClient = openRouterClient {
                requestCount += 1
                if (requestCount == 1) {
                    respond(
                        content = "temporary-provider-failure",
                        status = HttpStatusCode.ServiceUnavailable,
                        headers = jsonHeaders(),
                    )
                } else {
                    respond(
                        content = completionResponse(interpretedCandidate()),
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders(),
                    )
                }
            }

            application {
                moduleWithProviderConfigs(
                    llmProviderConfig = openRouterProviderConfig(),
                    openRouterHttpClientFactory = { openRouterClient },
                    openRouterDiagnosticObserver =
                        OpenRouterDiagnosticObserver(diagnosticEvents::add),
                )
            }

            val sessionId = createSession()
            val response = sendAssistantMessage(sessionId, COMPLETE_HOTEL_REQUEST)
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val assistantMessage = body["assistantMessage"]
                ?.jsonObject
                ?.get("content")
                ?.jsonPrimitive
                ?.content
                .orEmpty()

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(2, requestCount)
            assertEquals(
                listOf(
                    OpenRouterDiagnosticEvent.PROVIDER_UNAVAILABLE,
                    OpenRouterDiagnosticEvent.CANDIDATE_DECODED,
                ),
                diagnosticEvents,
            )
            assertEquals("ask_clarification", body["nextAction"]?.jsonPrimitive?.content)
            assertTrue(assistantMessage.contains(CONFIRMATION_QUESTION))
            assertFalse(body.containsKey("hotelSearchId"))
        }

    @Test
    fun `OpenRouter authorization is isolated from Hotels API client`() = testApplication {
        val openRouterRequests = mutableListOf<HttpRequestData>()
        val hotelRequests = mutableListOf<HttpRequestData>()
        val openRouterClient = openRouterClient { request ->
            openRouterRequests += request
            respond(
                content = completionResponse(clarificationCandidate()),
                status = HttpStatusCode.OK,
                headers = jsonHeaders(),
            )
        }
        val hotelsClient = openRouterClient { request ->
            hotelRequests += request
            val fixture = when (request.url.encodedPath) {
                "/search-api/search/autocomplete" -> "autocomplete-success.json"
                "/api/v1/hotels/search" -> "search-success.json"
                else -> error("Unexpected Hotels API path: ${request.url.encodedPath}")
            }
            respond(
                content = fixture(fixture),
                status = HttpStatusCode.OK,
                headers = jsonHeaders(),
            )
        }

        application {
            moduleWithProviderConfigs(
                llmProviderConfig = openRouterProviderConfig(),
                providerConfig = realHotelProviderConfig(),
                openRouterHttpClientFactory = { openRouterClient },
                realHotelHttpClientFactory = { hotelsClient },
            )
        }

        val sessionId = createSession()
        sendAssistantMessage(sessionId, "Нужен отель")
        val hotelSearchResponse = client.post("/api/v1/hotel-searches") {
            headers.append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(hotelSearchRequest(sessionId))
        }

        assertEquals(HttpStatusCode.Accepted, hotelSearchResponse.status)
        assertEquals(1, openRouterRequests.size)
        assertEquals(
            "Bearer synthetic-openrouter-api-key",
            openRouterRequests.single().headers[HttpHeaders.Authorization],
        )
        assertEquals(2, hotelRequests.size)
        assertTrue(hotelRequests.all { request ->
            request.headers[HttpHeaders.Authorization] == null
        })
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.createSession(): String {
        val response = client.post("/api/v1/assistant/sessions")
        return Json.parseToJsonElement(response.bodyAsText())
            .jsonObject
            .getValue("session")
            .jsonObject
            .getValue("sessionId")
            .jsonPrimitive
            .content
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.sendAssistantMessage(
        sessionId: String,
        message: String,
    ) = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
        headers.append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(
            buildJsonObject {
                put("message", message)
            }.toString(),
        )
    }

    private fun openRouterProviderConfig(): LlmProviderConfig =
        LlmProviderConfig(
            mode = LlmProviderMode.OPENROUTER,
            openRouter = OpenRouterConfig(
                apiKey = OpenRouterApiKey.of("synthetic-openrouter-api-key"),
                model = "test/model",
                baseUrl = "https://openrouter.test/api/v1/",
                timeoutMillis = 5_000,
            ),
        )

    private fun realHotelProviderConfig(): HotelProviderConfig =
        HotelProviderConfig(
            mode = HotelProviderMode.REAL,
            hotelsApi = HotelsApiConfig(
                publicTarget = HotelsApiTargetConfig.public(
                    baseUrl = "https://hotels.test/",
                    timeoutMillis = 5_000,
                ),
                userLanguage = "RU",
            ),
        )

    private fun openRouterClient(
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(HttpRequestData) ->
            io.ktor.client.request.HttpResponseData,
    ): HttpClient =
        HttpClient(MockEngine(handler)) {
            install(HttpTimeout)
        }

    private fun interpretedCandidate(): String =
        candidate(
            outcome = "INTERPRETED",
            constraints = mapOf(
                "destination" to "Казань",
                "check-in" to "2026-08-10",
                "check-out" to "2026-08-14",
                "adults" to "2",
                "children" to "0",
                "rooms" to "1",
            ),
        )

    private fun clarificationCandidate(): String =
        candidate(
            outcome = "NEEDS_CLARIFICATION",
            constraints = emptyMap(),
            missingRequiredFields = listOf("destination", "stay_dates", "guests"),
            clarificationQuestion = "Уточните направление, даты и гостей.",
        )

    private fun candidate(
        outcome: String,
        constraints: Map<String, String>,
        missingRequiredFields: List<String> = emptyList(),
        clarificationQuestion: String? = null,
    ): String =
        buildJsonObject {
            put("outcome", outcome)
            put("intent", "HOTEL_SEARCH")
            putJsonObject("extractedConstraints") {
                CANONICAL_CONSTRAINT_KEYS.forEach { key ->
                    val value = constraints[key]
                    if (value == null) {
                        put(key, null as String?)
                    } else {
                        put(key, value)
                    }
                }
            }
            putJsonObject("preferencePatch") {
                put("max-total-price", null as String?)
                put("stars", null as String?)
                put("min-guest-rating", null as String?)
                put("free-cancellation", null as String?)
                put("breakfast-included", null as String?)
                putJsonArray("clear") {}
            }
            put("missingRequiredFields", buildJsonArray {
                missingRequiredFields.forEach(::add)
            })
            put("conflicts", buildJsonArray {})
            put("clarificationQuestion", clarificationQuestion)
            put("warnings", buildJsonArray {})
        }.toString()

    private fun completionResponse(candidate: String): String =
        buildJsonObject {
            putJsonArray("choices") {
                addJsonObject {
                    put("finish_reason", "stop")
                    putJsonObject("message") {
                        put("content", candidate)
                    }
                }
            }
        }.toString()

    private fun hotelSearchRequest(sessionId: String): String =
        buildJsonObject {
            put("sessionId", sessionId)
            putJsonObject("criteria") {
                put("destination", "Тестовая локация 1")
                put("checkInDate", "2026-08-10")
                put("checkOutDate", "2026-08-14")
                putJsonObject("guests") {
                    put("adults", 2)
                    put("children", 0)
                }
                put("rooms", 1)
            }
        }.toString()

    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResource("/fixtures/hotels-api/$name")) {
            "Fixture not found: $name"
        }.readText()

    private fun jsonHeaders() =
        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private companion object {
        const val COMPLETE_HOTEL_REQUEST =
            "Найди отель в Казани с 10 по 14 августа 2026 года для двух взрослых без детей, одна комната"
        const val CONFIRMATION_QUESTION = "Найти отели по этим параметрам?"

        val CANONICAL_CONSTRAINT_KEYS = listOf(
            "destination",
            "check-in",
            "check-out",
            "adults",
            "children",
            "children-ages",
            "rooms",
        )
    }
}
