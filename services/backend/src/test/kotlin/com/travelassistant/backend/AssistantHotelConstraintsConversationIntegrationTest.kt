package com.travelassistant.backend

import com.travelassistant.backend.application.assistant.InMemoryAssistantHotelConstraintsStore
import com.travelassistant.backend.application.assistant.InMemoryPendingConfirmationStore
import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmCandidateRequest
import com.travelassistant.backend.application.llm.LlmClient
import com.travelassistant.backend.application.llm.LlmClientResponse
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.ArrayDeque
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AssistantHotelConstraintsConversationIntegrationTest {
    private val now = Instant.parse("2026-07-18T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun accumulatesCityDatesAndGuestsBeforeExplicitlyConfirmedSearch() = testApplication {
        val contextStore = InMemoryAssistantHotelConstraintsStore()
        val pendingStore = InMemoryPendingConfirmationStore()
        val requests = mutableListOf<LlmCandidateRequest>()
        val llmClient = queuedLlmClient(
            requests = requests,
            responses = listOf(
                clarificationCandidate(
                    constraints = mapOf("destination" to "Казань"),
                    missing = listOf("check-in", "check-out", "adults", "rooms"),
                    question = "На какие даты планируется поездка?",
                ),
                clarificationCandidate(
                    constraints = mapOf(
                        "check-in" to "2026-08-10",
                        "check-out" to "2026-08-14",
                    ),
                    missing = listOf("adults", "rooms"),
                    question = "Сколько будет взрослых и номеров?",
                ),
                interpretedCandidate(
                    "adults" to "2",
                    "children" to "0",
                    "rooms" to "1",
                ),
            ),
        )

        application {
            moduleWithAssistantLlm(
                llmClient = llmClient,
                pendingConfirmationStore = pendingStore,
                hotelConstraintsStore = contextStore,
                clock = clock,
            )
        }

        val sessionId = createSession()
        val destinationReply = sendMessage(sessionId, "Ищу отель в Казани")
        val datesReply = sendMessage(sessionId, "С 10 по 14 августа 2026")
        val guestsReply = sendMessage(sessionId, "Двое взрослых, один номер")

        assertEquals("ask_clarification", destinationReply.nextAction())
        assertEquals("ask_clarification", datesReply.nextAction())
        assertEquals("ask_clarification", guestsReply.nextAction())
        assertFalse(guestsReply.containsKey("hotelSearchId"))
        assertNotNull(
            pendingStore.findActiveBySession(
                sessionId = AssistantSessionId(sessionId),
                now = now.plusSeconds(1),
            ),
        )

        assertEquals(emptyMap(), requests[0].confirmedConstraints)
        assertEquals(
            listOf("destination", "check-in", "check-out", "adults", "rooms"),
            requests[0].missingRequiredFields,
        )
        assertEquals(mapOf("destination" to "Казань"), requests[1].confirmedConstraints)
        assertEquals(
            mapOf(
                "destination" to "Казань",
                "check-in" to "2026-08-10",
                "check-out" to "2026-08-14",
            ),
            requests[2].confirmedConstraints,
        )

        val confirmationReply = sendMessage(sessionId, "да")
        val hotelSearchId = confirmationReply["hotelSearchId"]?.jsonPrimitive?.content

        assertEquals("show_hotel_results", confirmationReply.nextAction())
        assertNotNull(hotelSearchId)
        assertEquals(3, requests.size)
        assertEquals(
            "Казань",
            contextStore.findBySession(AssistantSessionId(sessionId))?.destination,
        )

        val offersResponse = client.get("/api/v1/hotel-searches/$hotelSearchId/offers")
        assertEquals(HttpStatusCode.OK, offersResponse.status)
    }

    @Test
    fun appliesPendingCorrectionInSameMessageAndReplacesConfirmation() = testApplication {
        val contextStore = InMemoryAssistantHotelConstraintsStore()
        val pendingStore = InMemoryPendingConfirmationStore()
        val requests = mutableListOf<LlmCandidateRequest>()
        val llmClient = queuedLlmClient(
            requests = requests,
            responses = listOf(
                interpretedCandidate(*completeConstraints(destination = "Rome")),
                interpretedCandidate("destination" to "Paris"),
            ),
        )

        application {
            moduleWithAssistantLlm(
                llmClient = llmClient,
                pendingConfirmationStore = pendingStore,
                hotelConstraintsStore = contextStore,
                clock = clock,
            )
        }

        val sessionId = createSession()
        sendMessage(sessionId, "Найди отель в Риме")

        val correctionReply = sendMessage(sessionId, "лучше Париж")
        val revisedPending = pendingStore.findActiveBySession(
            sessionId = AssistantSessionId(sessionId),
            now = now.plusSeconds(1),
        )

        assertEquals("ask_clarification", correctionReply.nextAction())
        assertFalse(correctionReply.containsKey("hotelSearchId"))
        assertEquals("Paris", revisedPending?.criteria?.destination)
        assertEquals("Paris", contextStore.findBySession(AssistantSessionId(sessionId))?.destination)
        assertEquals("Rome", requests[1].confirmedConstraints["destination"])
        assertTrue(correctionReply.assistantContent().contains("направление: Paris"))

        val offersResponse = client.get("/api/v1/hotel-searches/hotel-search-local-000001/offers")
        assertEquals(HttpStatusCode.NotFound, offersResponse.status)
    }

    @Test
    fun asksForChildAgeAndUsesItInTheNextConfirmation() = testApplication {
        val contextStore = InMemoryAssistantHotelConstraintsStore()
        val pendingStore = InMemoryPendingConfirmationStore()
        val requests = mutableListOf<LlmCandidateRequest>()
        val llmClient = queuedLlmClient(
            requests = requests,
            responses = listOf(
                interpretedCandidate(
                    *(completeConstraints(destination = "Казань") +
                        ("children" to "1")),
                ),
                interpretedCandidate("children-ages" to "7"),
            ),
        )

        application {
            moduleWithAssistantLlm(
                llmClient = llmClient,
                pendingConfirmationStore = pendingStore,
                hotelConstraintsStore = contextStore,
                clock = clock,
            )
        }

        val sessionId = createSession()
        val ageQuestion = sendMessage(sessionId, "Двое взрослых и ребёнок")
        val confirmation = sendMessage(sessionId, "Ребёнку 7 лет")

        assertEquals(
            "Укажите возраст каждого ребёнка (от 0 до 17 лет).",
            ageQuestion.assistantContent(),
        )
        assertEquals("1", requests[1].confirmedConstraints["children"])
        assertFalse(requests[1].confirmedConstraints.containsKey("children-ages"))
        assertEquals(listOf("children-ages"), requests[1].missingRequiredFields)
        assertTrue(confirmation.assistantContent().contains("возраст детей: 7"))
        assertFalse(confirmation.containsKey("hotelSearchId"))
        assertEquals(
            listOf(7),
            pendingStore.findActiveBySession(
                sessionId = AssistantSessionId(sessionId),
                now = now.plusSeconds(1),
            )?.criteria?.guests?.childrenAges,
        )
    }

    @Test
    fun invalidPendingCorrectionClearsOldValueUntilValidReplacement() = testApplication {
        val contextStore = InMemoryAssistantHotelConstraintsStore()
        val pendingStore = InMemoryPendingConfirmationStore()
        val requests = mutableListOf<LlmCandidateRequest>()
        val llmClient = queuedLlmClient(
            requests = requests,
            responses = listOf(
                interpretedCandidate(*completeConstraints(destination = "Rome")),
                interpretedCandidate("adults" to "0"),
                interpretedCandidate("adults" to "2"),
            ),
        )

        application {
            moduleWithAssistantLlm(
                llmClient = llmClient,
                pendingConfirmationStore = pendingStore,
                hotelConstraintsStore = contextStore,
                clock = clock,
            )
        }

        val sessionId = createSession()
        sendMessage(sessionId, "Найди отель в Риме")

        val invalidCorrection = sendMessage(sessionId, "лучше 0 взрослых")

        assertEquals("ask_clarification", invalidCorrection.nextAction())
        assertFalse(invalidCorrection.containsKey("hotelSearchId"))
        assertNull(
            pendingStore.findActiveBySession(
                sessionId = AssistantSessionId(sessionId),
                now = now.plusSeconds(1),
            ),
        )
        assertNull(contextStore.findBySession(AssistantSessionId(sessionId))?.adults)

        val corrected = sendMessage(sessionId, "Двое взрослых")

        assertEquals(listOf("adults"), requests[2].missingRequiredFields)
        assertEquals("ask_clarification", corrected.nextAction())
        assertFalse(corrected.containsKey("hotelSearchId"))
        assertEquals(
            2,
            pendingStore.findActiveBySession(
                sessionId = AssistantSessionId(sessionId),
                now = now.plusSeconds(1),
            )?.criteria?.guests?.adults,
        )
    }

    @Test
    fun keepsContextAfterDeclinedConfirmation() = testApplication {
        val contextStore = InMemoryAssistantHotelConstraintsStore()
        val pendingStore = InMemoryPendingConfirmationStore()
        val llmClient = queuedLlmClient(
            requests = mutableListOf(),
            responses = listOf(interpretedCandidate(*completeConstraints(destination = "Rome"))),
        )

        application {
            moduleWithAssistantLlm(
                llmClient = llmClient,
                pendingConfirmationStore = pendingStore,
                hotelConstraintsStore = contextStore,
                clock = clock,
            )
        }

        val sessionId = createSession()
        sendMessage(sessionId, "Найди отель в Риме")
        val declined = sendMessage(sessionId, "нет")

        assertEquals("ask_clarification", declined.nextAction())
        assertFalse(declined.containsKey("hotelSearchId"))
        assertNull(
            pendingStore.findActiveBySession(
                sessionId = AssistantSessionId(sessionId),
                now = now.plusSeconds(1),
            ),
        )
        assertEquals(
            "Rome",
            contextStore.findBySession(AssistantSessionId(sessionId))?.destination,
        )

        val offersResponse = client.get(
            "/api/v1/hotel-searches/hotel-search-local-000001/offers",
        )
        assertEquals(HttpStatusCode.NotFound, offersResponse.status)
    }

    private fun queuedLlmClient(
        requests: MutableList<LlmCandidateRequest>,
        responses: List<LlmCandidate>,
    ): LlmClient {
        val queue = ArrayDeque(responses.map(LlmClientResponse::Candidate))
        return LlmClient { request ->
            requests += request
            queue.removeFirst()
        }
    }

    private fun clarificationCandidate(
        constraints: Map<String, String>,
        missing: List<String>,
        question: String,
    ): LlmCandidate =
        LlmCandidate(
            outcome = LlmCandidate.Outcome.NEEDS_CLARIFICATION,
            intent = LlmCandidate.Intent.HOTEL_SEARCH,
            extractedConstraints = constraints,
            missingRequiredFields = missing,
            clarificationQuestion = question,
        )

    private fun interpretedCandidate(
        vararg constraints: Pair<String, String>,
    ): LlmCandidate =
        LlmCandidate(
            outcome = LlmCandidate.Outcome.INTERPRETED,
            intent = LlmCandidate.Intent.HOTEL_SEARCH,
            extractedConstraints = mapOf(*constraints),
        )

    private fun completeConstraints(
        destination: String,
    ): Array<Pair<String, String>> =
        arrayOf(
            "destination" to destination,
            "check-in" to "2026-08-10",
            "check-out" to "2026-08-14",
            "adults" to "2",
            "children" to "0",
            "rooms" to "1",
        )

    private suspend fun ApplicationTestBuilder.createSession(): String {
        val response = client.post("/api/v1/assistant/sessions")
        assertEquals(HttpStatusCode.Created, response.status)
        return Json.parseToJsonElement(response.bodyAsText())
            .jsonObject["session"]
            ?.jsonObject
            ?.get("sessionId")
            ?.jsonPrimitive
            ?.content
            .orEmpty()
    }

    private suspend fun ApplicationTestBuilder.sendMessage(
        sessionId: String,
        message: String,
    ): JsonObject {
        val response = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"$message"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        return Json.parseToJsonElement(response.bodyAsText()).jsonObject
    }

    private fun JsonObject.nextAction(): String? =
        get("nextAction")?.jsonPrimitive?.content

    private fun JsonObject.assistantContent(): String =
        get("assistantMessage")
            ?.jsonObject
            ?.get("content")
            ?.jsonPrimitive
            ?.content
            .orEmpty()
}
