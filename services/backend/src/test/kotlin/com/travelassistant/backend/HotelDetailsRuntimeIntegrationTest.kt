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
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class HotelDetailsRuntimeIntegrationTest {

    @Test
    fun `REAL runtime shares one Hotels API client between search and selected details`() =
        testApplication {
            var httpClientCreations = 0
            val requests = mutableListOf<Pair<HttpMethod, String>>()
            application {
                moduleWithAssistantLlm(
                    llmClient = FakeLlmClient(LlmClientResponse.Empty),
                    providerConfig = realProviderConfig(),
                    realHotelHttpClientFactory = {
                        httpClientCreations++
                        fixtureHttpClient(requests)
                    },
                )
            }

            val sessionResponse = client.post("/api/v1/assistant/sessions")
            val sessionId = Json.parseToJsonElement(sessionResponse.bodyAsText())
                .jsonObject.getValue("session").jsonObject
                .getValue("sessionId").jsonPrimitive.content
            val searchResponse = client.post("/api/v1/hotel-searches") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(searchBody(sessionId))
            }
            val searchId = Json.parseToJsonElement(searchResponse.bodyAsText())
                .jsonObject.getValue("searchId").jsonPrimitive.content
            val offersResponse = client.get("/api/v1/hotel-searches/$searchId/offers")
            val offers = Json.parseToJsonElement(offersResponse.bodyAsText())
                .jsonObject.getValue("offers").jsonArray
            val selectedOffer = offers
                .map { it.jsonObject }
                .single { offer ->
                    offer.getValue("hotelName").jsonPrimitive.content == "Тестовый отель 1"
                }
            val offerId = selectedOffer.getValue("offerId").jsonPrimitive.content

            val detailsResponse = client.get(
                "/api/v1/hotel-searches/$searchId/offers/$offerId/details",
            )
            val detailsBody = detailsResponse.bodyAsText()
            val details = Json.parseToJsonElement(detailsBody).jsonObject

            assertEquals(HttpStatusCode.Accepted, searchResponse.status)
            assertEquals(HttpStatusCode.OK, offersResponse.status)
            assertEquals(HttpStatusCode.OK, detailsResponse.status)
            assertEquals("Отель Пример", details.getValue("hotelName").jsonPrimitive.content)
            assertEquals(
                "https://images.example.test/hotel-1/image-1.jpg",
                selectedOffer.getValue("imageUrl").jsonPrimitive.content,
            )
            assertNotEquals("hotel-fixture-1", offerId)
            assertEquals(1, httpClientCreations)
            assertEquals(
                listOf(
                    HttpMethod.Post to "/search-api/search/autocomplete",
                    HttpMethod.Post to "/api/v1/hotels/search",
                    HttpMethod.Get to "/api/v1/hotels/hotel-fixture-1",
                ),
                requests,
            )
            assertFalse(detailsBody.contains("hotel-fixture-1"))
            assertFalse(detailsBody.contains("providerReference"))
            listOf("ИНН", "ОГРН", "КПП", "registry.example", "owner data").forEach { forbidden ->
                assertFalse(detailsBody.contains(forbidden, ignoreCase = true))
            }
        }

    private fun fixtureHttpClient(
        requests: MutableList<Pair<HttpMethod, String>>,
    ): HttpClient =
        HttpClient(
            MockEngine { request ->
                requests += request.method to request.url.encodedPath
                assertNull(request.headers[HttpHeaders.Authorization])
                assertNull(request.headers[HttpHeaders.Cookie])

                val body = when (request.url.encodedPath) {
                    "/search-api/search/autocomplete" -> fixture("autocomplete-success.json")
                    "/api/v1/hotels/search" -> fixture("search-success.json")
                    "/api/v1/hotels/hotel-fixture-1" ->
                        fixture("stage-13-1/hotel-details-success.json")
                            .replace("hotel-example-001", "hotel-fixture-1")
                            .replace(
                                "\"description\": [",
                                """"description": [
                                  {
                                    "title": "Сертификация",
                                    "paragraphs": [
                                      "ИНН 1234567890, ОГРН 1234567890123, КПП 123456789",
                                      "Owner data: https://registry.example.invalid/hotel"
                                    ]
                                  },""",
                            )
                    else -> error("Unexpected Hotels API path")
                }
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

    private fun fixture(path: String): String =
        checkNotNull(javaClass.classLoader.getResource("fixtures/hotels-api/$path"))
            .readText()

    private fun realProviderConfig(): HotelProviderConfig =
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

    private fun searchBody(sessionId: String): String =
        """
        {
          "sessionId": "$sessionId",
          "criteria": {
            "destination": "Тестовая локация 1",
            "checkInDate": "2026-08-10",
            "checkOutDate": "2026-08-14",
            "guests": {"adults": 2, "children": 0},
            "rooms": 1
          }
        }
        """.trimIndent()
}
