package com.travelassistant.backend

import com.travelassistant.backend.application.assistant.InMemoryPendingConfirmationStore
import com.travelassistant.backend.application.assistant.PendingConfirmationStatus
import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmClient
import com.travelassistant.backend.application.llm.LlmClientResponse
import com.travelassistant.backend.domain.assistant.AssistantSessionId
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
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.server.testing.testApplication
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class ProviderSeamIntegrationTest {

    private val routeNow = Instant.parse("2026-06-27T10:00:00Z")
    private val routeClock = Clock.fixed(routeNow, ZoneOffset.UTC)

    private fun completeRealProviderConfig(): HotelProviderConfig =
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

    @Test
    fun realProviderModeReturnsSafeInternalErrorWithoutCreatingSearch() = testApplication {
        application {
            moduleWithAssistantLlm(
                llmClient = FakeLlmClient(
                    LlmClientResponse.Candidate(
                        LlmCandidate(
                            outcome = LlmCandidate.Outcome.NEEDS_CLARIFICATION,
                            intent = LlmCandidate.Intent.HOTEL_SEARCH,
                            missingRequiredFields = listOf("destination", "stay_dates", "guests"),
                            clarificationQuestion = "Please share your hotel details.",
                        ),
                    ),
                ),
                providerConfig = completeRealProviderConfig(),
                clock = routeClock,
                realHotelHttpClientFactory = ::unavailableHttpClient,
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

        val searchResponse = client.post("/api/v1/hotel-searches") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """
                {
                  "sessionId": "$sessionId",
                  "criteria": {
                    "destination": "Rome",
                    "checkInDate": "2026-07-01",
                    "checkOutDate": "2026-07-04",
                    "guests": {"adults": 2, "children": 0},
                    "rooms": 1
                  }
                }
                """.trimIndent(),
            )
        }
        val searchBody = Json.parseToJsonElement(searchResponse.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.InternalServerError, searchResponse.status)
        assertEquals("INTERNAL_ERROR", searchBody["code"]?.jsonPrimitive?.content)
        assertEquals(
            "Hotel search could not be completed.",
            searchBody["message"]?.jsonPrimitive?.content,
        )
        assertFalse(searchBody.containsKey("searchId"))
        assertFalse(searchBody.containsKey("hotelSearchId"))
        assertFalse(searchBody.containsKey("status"))
        assertFalse(searchBody.toString().contains("UNAVAILABLE"))

        val offersResponse = client.get(
            "/api/v1/hotel-searches/hotel-search-local-000001/offers",
        )
        assertEquals(HttpStatusCode.NotFound, offersResponse.status)
    }

    @Test
    fun realProviderModeConfirmationCycleReturnsClarificationWithoutConsumingPending() = testApplication {
        val pendingConfirmationStore = InMemoryPendingConfirmationStore()
        var llmResponse: LlmClientResponse =
            LlmClientResponse.Candidate(interpretedHotelSearchCandidate())
        val testLlmClient = LlmClient { llmResponse }

        application {
            moduleWithAssistantLlm(
                llmClient = testLlmClient,
                providerConfig = completeRealProviderConfig(),
                pendingConfirmationStore = pendingConfirmationStore,
                clock = routeClock,
                realHotelHttpClientFactory = ::unavailableHttpClient,
            )
        }

        val createdSession = client.post("/api/v1/assistant/sessions")
        val createdSessionBody = Json.parseToJsonElement(createdSession.bodyAsText()).jsonObject
        val sessionId = createdSessionBody["session"]?.jsonObject?.get("sessionId")?.jsonPrimitive?.content.orEmpty()

        client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"Find a hotel in Rome for two adults"}""")
        }

        llmResponse = LlmClientResponse.Empty

        val confirmResponse = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"да"}""")
        }
        val confirmBody = Json.parseToJsonElement(confirmResponse.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, confirmResponse.status)
        assertEquals("ask_clarification", confirmBody["nextAction"]?.jsonPrimitive?.content)
        assertEquals(
            "I could not complete the hotel search right now. Please try again.",
            confirmBody["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )
        assertFalse(confirmBody.containsKey("hotelSearchId"))
        assertFalse(confirmBody.toString().contains("UNAVAILABLE"))

        val pendingConfirmation = pendingConfirmationStore.findActiveBySession(
            sessionId = AssistantSessionId(sessionId),
            now = routeNow.plusSeconds(1),
        )
        assertNotNull(pendingConfirmation)
        assertEquals(PendingConfirmationStatus.PENDING, pendingConfirmation.status)

        val offersResponse = client.get(
            "/api/v1/hotel-searches/hotel-search-local-000001/offers",
        )
        assertEquals(HttpStatusCode.NotFound, offersResponse.status)
    }

    @Test
    fun realProviderModeUsesPublicAutocompleteAndSearchComposition() = testApplication {
        val requestedPaths = mutableListOf<String>()

        application {
            moduleWithAssistantLlm(
                llmClient = FakeLlmClient(LlmClientResponse.Empty),
                providerConfig = completeRealProviderConfig(),
                clock = routeClock,
                realHotelHttpClientFactory = { successfulHttpClient(requestedPaths) },
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

        val searchResponse = client.post("/api/v1/hotel-searches") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """
                {
                  "sessionId": "$sessionId",
                  "criteria": {
                    "destination": "Казань",
                    "checkInDate": "2026-07-18",
                    "checkOutDate": "2026-07-19",
                    "guests": {"adults": 2, "children": 0},
                    "rooms": 1
                  }
                }
                """.trimIndent(),
            )
        }
        val searchBody = Json.parseToJsonElement(searchResponse.bodyAsText()).jsonObject
        val searchId = searchBody["searchId"]?.jsonPrimitive?.content.orEmpty()

        assertEquals(HttpStatusCode.Accepted, searchResponse.status)
        assertEquals("completed_with_offers", searchBody["status"]?.jsonPrimitive?.content)
        assertEquals(
            listOf(
                "/search-api/search/autocomplete",
                "/api/v1/hotels/search",
            ),
            requestedPaths,
        )

        val offersResponse = client.get("/api/v1/hotel-searches/$searchId/offers")
        val offersBody = Json.parseToJsonElement(offersResponse.bodyAsText()).jsonObject
        val offers = offersBody["offers"]?.jsonArray.orEmpty()

        assertEquals(HttpStatusCode.OK, offersResponse.status)
        assertEquals(1, offers.size)
        assertEquals(
            "hotel-runtime-1",
            offers.single().jsonObject["providerOfferRef"]?.jsonPrimitive?.content,
        )
        assertEquals(
            "tbank_hotels_api",
            offers.single().jsonObject["source"]?.jsonPrimitive?.content,
        )
    }

    @Test
    fun fakeProviderModeStillReturnsDeterministicOffersByDefault() = testApplication {
        application {
            moduleWithAssistantLlm(
                llmClient = FakeLlmClient(
                    LlmClientResponse.Candidate(
                        LlmCandidate(
                            outcome = LlmCandidate.Outcome.NEEDS_CLARIFICATION,
                            intent = LlmCandidate.Intent.HOTEL_SEARCH,
                            missingRequiredFields = listOf("destination", "stay_dates", "guests"),
                            clarificationQuestion = "Please share your hotel details.",
                        ),
                    ),
                ),
                clock = routeClock,
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

        val searchResponse = client.post("/api/v1/hotel-searches") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """
                {
                  "sessionId": "$sessionId",
                  "criteria": {
                    "destination": "Rome",
                    "checkInDate": "2026-07-01",
                    "checkOutDate": "2026-07-04",
                    "guests": {"adults": 2, "children": 0},
                    "rooms": 1
                  }
                }
                """.trimIndent(),
            )
        }
        val searchBody = Json.parseToJsonElement(searchResponse.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.Accepted, searchResponse.status)
        assertEquals("completed_with_offers", searchBody["status"]?.jsonPrimitive?.content)

        val searchId = searchBody["searchId"]?.jsonPrimitive?.content.orEmpty()
        val offersResponse = client.get("/api/v1/hotel-searches/$searchId/offers")
        val offersBody = Json.parseToJsonElement(offersResponse.bodyAsText()).jsonObject
        val offers = offersBody["offers"]?.jsonArray.orEmpty()

        assertEquals(2, offers.size)
        assertEquals("local_fake_provider", offers.first().jsonObject["source"]?.jsonPrimitive?.content)
    }

    private fun unavailableHttpClient(): HttpClient =
        HttpClient(
            MockEngine {
                respond(
                    content = "provider-sensitive-unavailable-body",
                    status = HttpStatusCode.ServiceUnavailable,
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString(),
                    ),
                )
            },
        ) {
            install(HttpTimeout)
        }

    private fun successfulHttpClient(requestedPaths: MutableList<String>): HttpClient =
        HttpClient(
            MockEngine { request ->
                val path = request.url.encodedPath
                requestedPaths += path
                val responseBody = when (path) {
                    "/search-api/search/autocomplete" -> autocompleteResponse()
                    "/api/v1/hotels/search" -> searchResponse()
                    else -> error("Unexpected provider path: $path")
                }
                respond(
                    content = responseBody,
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

    private fun autocompleteResponse(): String =
        """
        {
          "payload": {
            "locations": [
              {
                "id": 1001,
                "name": "Казань",
                "signature": "Казань, Россия",
                "type": {"name": "Город", "code": "city"}
              }
            ],
            "hotels": []
          }
        }
        """.trimIndent()

    private fun searchResponse(): String =
        """
        {
          "payload": {
            "filteredHotelsCount": 1,
            "hotelsTotalCount": 1,
            "isLoadingCompleted": true,
            "hotels": [
              {
                "hotelId": "hotel-runtime-1",
                "hotelName": "Тестовый отель",
                "starRating": 4,
                "areaLocation": {
                  "countryName": "Россия",
                  "destinationId": 1001,
                  "destinationName": "Казань",
                  "signature": "Казань, Россия"
                },
                "hotelLocation": {"address": "Тестовая улица"},
                "rateForHotelsFeed": {
                  "availableRoomsCount": 1,
                  "isCreditCardDataRequired": false,
                  "paymentPlace": "online",
                  "shownPrice": {"amount": 12000.0, "currency": "RUB"}
                },
                "review": {"rating": 8.7, "ratingsCount": 42}
              }
            ]
          }
        }
        """.trimIndent()

    private fun interpretedHotelSearchCandidate(): LlmCandidate =
        LlmCandidate(
            outcome = LlmCandidate.Outcome.INTERPRETED,
            intent = LlmCandidate.Intent.HOTEL_SEARCH,
            extractedConstraints = mapOf(
                "destination" to "Rome",
                "check-in" to "2026-07-01",
                "check-out" to "2026-07-04",
                "adults" to "2",
                "children" to "0",
                "rooms" to "1",
            ),
        )
}
