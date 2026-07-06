package com.travelassistant.backend

import com.travelassistant.backend.application.assistant.InMemoryPendingConfirmationStore
import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmClient
import com.travelassistant.backend.application.llm.LlmClientResponse
import com.travelassistant.backend.infrastructure.llm.FakeLlmClient
import com.travelassistant.backend.infrastructure.provider.HotelProviderConfig
import com.travelassistant.backend.infrastructure.provider.HotelProviderMode
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
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
import kotlin.test.assertTrue

class ProviderSeamIntegrationTest {

    private val routeNow = Instant.parse("2026-06-27T10:00:00Z")
    private val routeClock = Clock.fixed(routeNow, ZoneOffset.UTC)

    @Test
    fun realProviderModeReturnsCompletedNoOffersThroughHotelSearchRoute() = testApplication {
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
                providerConfig = HotelProviderConfig(mode = HotelProviderMode.REAL),
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
        assertEquals("completed_no_offers", searchBody["status"]?.jsonPrimitive?.content)

        val searchId = searchBody["searchId"]?.jsonPrimitive?.content.orEmpty()
        assertTrue(searchId.isNotBlank())

        val offersResponse = client.get("/api/v1/hotel-searches/$searchId/offers")
        val offersBody = Json.parseToJsonElement(offersResponse.bodyAsText()).jsonObject
        val offers = offersBody["offers"]?.jsonArray.orEmpty()

        assertEquals(HttpStatusCode.OK, offersResponse.status)
        assertEquals("completed_no_offers", offersBody["status"]?.jsonPrimitive?.content)
        assertEquals(0, offers.size)
    }

    @Test
    fun realProviderModeConfirmationCycleCreatesSearchWithNoOffers() = testApplication {
        val pendingConfirmationStore = InMemoryPendingConfirmationStore()
        var llmResponse: LlmClientResponse =
            LlmClientResponse.Candidate(interpretedHotelSearchCandidate())
        val testLlmClient = LlmClient { llmResponse }

        application {
            moduleWithAssistantLlm(
                llmClient = testLlmClient,
                providerConfig = HotelProviderConfig(mode = HotelProviderMode.REAL),
                pendingConfirmationStore = pendingConfirmationStore,
                clock = routeClock,
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
        val hotelSearchId = confirmBody["hotelSearchId"]?.jsonPrimitive?.content.orEmpty()

        assertEquals(HttpStatusCode.OK, confirmResponse.status)
        assertEquals("show_hotel_results", confirmBody["nextAction"]?.jsonPrimitive?.content)
        assertTrue(hotelSearchId.isNotBlank())

        val offersResponse = client.get("/api/v1/hotel-searches/$hotelSearchId/offers")
        val offersBody = Json.parseToJsonElement(offersResponse.bodyAsText()).jsonObject
        val offers = offersBody["offers"]?.jsonArray.orEmpty()

        assertEquals(HttpStatusCode.OK, offersResponse.status)
        assertEquals("completed_no_offers", offersBody["status"]?.jsonPrimitive?.content)
        assertEquals(0, offers.size)
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
