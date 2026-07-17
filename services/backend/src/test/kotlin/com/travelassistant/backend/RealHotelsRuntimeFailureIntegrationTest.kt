package com.travelassistant.backend

import com.travelassistant.backend.application.llm.LlmClientResponse
import com.travelassistant.backend.infrastructure.llm.FakeLlmClient
import com.travelassistant.backend.infrastructure.provider.HotelProviderConfig
import com.travelassistant.backend.infrastructure.provider.HotelProviderMode
import com.travelassistant.backend.infrastructure.provider.HotelsApiConfig
import com.travelassistant.backend.infrastructure.provider.HotelsApiTargetConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.server.testing.testApplication
import java.net.SocketTimeoutException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RealHotelsRuntimeFailureIntegrationTest {

    @Test
    fun `real runtime maps absent and ambiguous locations to safe validation errors`() {
        assertFailure(
            clientFactory = { autocompleteClient(locations = emptyList()) },
            expectedStatus = HttpStatusCode.BadRequest,
            expectedCode = "VALIDATION_ERROR",
            expectedField = "criteria.destination",
        )
        assertFailure(
            clientFactory = {
                autocompleteClient(
                    locations = listOf(
                        locationJson(1001, "Казань"),
                        locationJson(1002, "Казань, аэропорт"),
                    ),
                )
            },
            expectedStatus = HttpStatusCode.BadRequest,
            expectedCode = "VALIDATION_ERROR",
            expectedField = "criteria.destination",
        )
    }

    @Test
    fun `real runtime maps malformed provider response to safe internal error`() {
        assertFailure(
            clientFactory = { responseClient("provider-sensitive-invalid-json") },
            expectedStatus = HttpStatusCode.InternalServerError,
            expectedCode = "INTERNAL_ERROR",
        )
    }

    @Test
    fun `real runtime maps timeout and unavailable without internal data`() {
        assertFailure(
            clientFactory = {
                HttpClient(
                    MockEngine { throw SocketTimeoutException("provider-sensitive-timeout") },
                ) {
                    install(HttpTimeout)
                }
            },
            expectedStatus = HttpStatusCode.InternalServerError,
            expectedCode = "INTERNAL_ERROR",
        )
        assertFailure(
            clientFactory = {
                HttpClient(
                    MockEngine {
                        respond(
                            content = "provider-sensitive-unavailable-body",
                            status = HttpStatusCode.ServiceUnavailable,
                        )
                    },
                ) {
                    install(HttpTimeout)
                }
            },
            expectedStatus = HttpStatusCode.InternalServerError,
            expectedCode = "INTERNAL_ERROR",
        )
    }

    private fun assertFailure(
        clientFactory: () -> HttpClient,
        expectedStatus: HttpStatusCode,
        expectedCode: String,
        expectedField: String? = null,
    ) = testApplication {
        application {
            moduleWithAssistantLlm(
                llmClient = FakeLlmClient(LlmClientResponse.Empty),
                providerConfig = realProviderConfig(),
                realHotelHttpClientFactory = clientFactory,
            )
        }

        val sessionResponse = client.post("/api/v1/assistant/sessions")
        val sessionBody = Json.parseToJsonElement(sessionResponse.bodyAsText()).jsonObject
        val sessionId = sessionBody["session"]
            ?.jsonObject
            ?.get("sessionId")
            ?.jsonPrimitive
            ?.content
            .orEmpty()
        val response = client.post("/api/v1/hotel-searches") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(searchBody(sessionId))
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(expectedStatus, response.status)
        assertEquals(expectedCode, body["code"]?.jsonPrimitive?.content)
        if (expectedField != null) {
            assertEquals(
                expectedField,
                body["fields"]?.jsonArray?.first()?.jsonObject
                    ?.get("field")?.jsonPrimitive?.content,
            )
        }
        assertFalse(body.containsKey("searchId"))
        assertFalse(body.containsKey("hotelSearchId"))
        listOf(
            "provider-sensitive",
            "INVALID_PAYLOAD",
            "TIMEOUT",
            "UNAVAILABLE",
            "destinationId",
        ).forEach { forbidden ->
            assertFalse(body.toString().contains(forbidden))
        }
    }

    private fun realProviderConfig(): HotelProviderConfig =
        HotelProviderConfig(
            mode = HotelProviderMode.REAL,
            hotelsApi = HotelsApiConfig(
                publicTarget = HotelsApiTargetConfig.public(
                    baseUrl = "https://hotels.test/",
                    timeoutMillis = 5_000,
                ),
            ),
        )

    private fun autocompleteClient(locations: List<String>): HttpClient =
        responseClient(
            """
            {
              "payload": {
                "locations": [${locations.joinToString(",")}],
                "hotels": []
              }
            }
            """.trimIndent(),
        )

    private fun responseClient(body: String): HttpClient =
        HttpClient(
            MockEngine {
                respond(
                    content = body,
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString(),
                    ),
                )
            },
        ) {
            install(HttpTimeout)
        }

    private fun locationJson(id: Int, name: String): String =
        """
        {
          "id": $id,
          "name": "$name",
          "signature": "$name, Россия",
          "type": {"name": "Город", "code": "city"}
        }
        """.trimIndent()

    private fun searchBody(sessionId: String): String =
        """
        {
          "sessionId": "$sessionId",
          "criteria": {
            "destination": "Казань",
            "checkInDate": "2026-08-10",
            "checkOutDate": "2026-08-14",
            "guests": {"adults": 2, "children": 0},
            "rooms": 1
          }
        }
        """.trimIndent()
}
