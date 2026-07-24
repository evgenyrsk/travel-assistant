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
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.ArrayDeque
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
    fun serializesConcurrentMessagesBeforeUpdatingOneSessionContext() = testApplication {
        val contextStore = InMemoryAssistantHotelConstraintsStore()
        val firstGenerationStarted = CompletableDeferred<Unit>()
        val releaseFirstGeneration = CompletableDeferred<Unit>()
        val generationCalls = AtomicInteger(0)
        val requests = Collections.synchronizedList(mutableListOf<LlmCandidateRequest>())
        val llmClient = LlmClient { request ->
            requests += request
            when (generationCalls.getAndIncrement()) {
                0 -> {
                    firstGenerationStarted.complete(Unit)
                    releaseFirstGeneration.await()
                    LlmClientResponse.Candidate(
                        clarificationCandidate(
                            constraints = mapOf("destination" to "Казань"),
                            missing = listOf("check-in", "check-out", "adults"),
                            question = "На какие даты планируется поездка?",
                        ),
                    )
                }

                else -> LlmClientResponse.Candidate(
                    clarificationCandidate(
                        constraints = mapOf(
                            "check-in" to "2026-08-10",
                            "check-out" to "2026-08-14",
                        ),
                        missing = listOf("adults"),
                        question = "Сколько будет взрослых?",
                    ),
                )
            }
        }

        application {
            moduleWithAssistantLlm(
                llmClient = llmClient,
                hotelConstraintsStore = contextStore,
                clock = clock,
            )
        }

        val sessionId = createSession()
        coroutineScope {
            val destinationMessage = async {
                sendMessage(sessionId, "Ищу отель в Казани")
            }
            firstGenerationStarted.await()

            val datesMessage = async {
                sendMessage(sessionId, "С 10 по 14 августа 2026")
            }
            delay(50)

            assertEquals(1, generationCalls.get())
            releaseFirstGeneration.complete(Unit)
            destinationMessage.await()
            datesMessage.await()
        }

        val stored = contextStore.findBySession(AssistantSessionId(sessionId))
        assertEquals("Казань", stored?.destination)
        assertEquals(LocalDate.parse("2026-08-10"), stored?.checkInDate)
        assertEquals(LocalDate.parse("2026-08-14"), stored?.checkOutDate)
        assertEquals("Казань", requests[1].confirmedConstraints["destination"])
    }

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
                    missing = listOf("check-in", "check-out", "adults"),
                    question = "На какие даты планируется поездка?",
                ),
                clarificationCandidate(
                    constraints = mapOf(
                        "check-in" to "2026-08-10",
                        "check-out" to "2026-08-14",
                    ),
                    missing = listOf("adults"),
                    question = "Сколько будет взрослых?",
                ),
                interpretedCandidate(
                    "adults" to "2",
                    "children" to "0",
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
        val guestsReply = sendMessage(sessionId, "Двое взрослых")

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

        assertEquals(mapOf("rooms" to "1"), requests[0].confirmedConstraints)
        assertEquals(
            listOf("destination", "check-in", "check-out", "adults"),
            requests[0].missingRequiredFields,
        )
        assertEquals(
            mapOf("destination" to "Казань", "rooms" to "1"),
            requests[1].confirmedConstraints,
        )
        assertEquals(
            mapOf(
                "destination" to "Казань",
                "check-in" to "2026-08-10",
                "check-out" to "2026-08-14",
                "rooms" to "1",
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
        assertTrue(correctionReply.assistantContent().contains("Куда: Paris"))

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
        assertTrue(confirmation.assistantContent().contains("1 ребёнок (7 лет)"))
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
    fun carriesStayLengthAcrossTurnsAndIgnoresInventedChildQuestion() = testApplication {
        val contextStore = InMemoryAssistantHotelConstraintsStore()
        val pendingStore = InMemoryPendingConfirmationStore()
        val requests = mutableListOf<LlmCandidateRequest>()
        val llmClient = queuedLlmClient(
            requests = requests,
            responses = listOf(
                clarificationCandidate(
                    constraints = mapOf(
                        "adults" to "2",
                    ),
                    missing = listOf("check-in", "check-out"),
                    question = "Уточните дату заезда и город.",
                ),
                clarificationCandidate(
                    constraints = mapOf("check-in" to "2026-08-01"),
                    missing = listOf("check-out", "children", "children-ages"),
                    question = "Уточните дату выезда, количество детей и их возраст.",
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
        val dateQuestion = sendMessage(
            sessionId,
            "Хочу вместе с супругой в Cosmos ВДНХ в начале августа на 7 ночей с завтраками",
        )
        val confirmation = sendMessage(sessionId, "1 августа 2026 в Cosmos ВДНХ")

        assertEquals(
            "Уточните даты поездки, указав день, месяц и год.",
            dateQuestion.assistantContent(),
        )
        assertEquals("ask_clarification", confirmation.nextAction())
        assertTrue(
            confirmation.assistantContent().contains("Даты: 1–8 августа 2026"),
            "${confirmation.assistantContent()} stored=" +
                contextStore.findBySession(AssistantSessionId(sessionId)),
        )
        assertTrue(confirmation.assistantContent().contains("Гости: 2 взрослых, без детей"))
        assertFalse(confirmation.assistantContent().contains("Уточните дату выезда"))
        assertFalse(confirmation.assistantContent().contains("количество детей"))
        assertFalse(confirmation.containsKey("hotelSearchId"))

        val stored = contextStore.findBySession(AssistantSessionId(sessionId))
        assertEquals(7, stored?.stayLengthNights)
        assertEquals(LocalDate.parse("2026-08-08"), stored?.checkOutDate)
        assertEquals("Cosmos ВДНХ", requests[1].confirmedConstraints["destination"])
        assertEquals("7", requests[1].confirmedConstraints["stay-length-nights"])
        assertEquals(listOf("check-in"), requests[1].missingRequiredFields)
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
    fun blocksMultipleRoomsBeforeConfirmationAndAcceptsSingleRoomCorrection() = testApplication {
        val contextStore = InMemoryAssistantHotelConstraintsStore()
        val pendingStore = InMemoryPendingConfirmationStore()
        val requests = mutableListOf<LlmCandidateRequest>()
        val llmClient = queuedLlmClient(
            requests = requests,
            responses = listOf(
                interpretedCandidate(
                    *(completeConstraints(destination = "Cosmos ВДНХ") +
                        ("adults" to "3")),
                ),
                interpretedCandidate("rooms" to "2"),
                interpretedCandidate("rooms" to "1"),
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
        sendMessage(sessionId, "Найди Cosmos ВДНХ для троих")
        val initialSearch = sendMessage(sessionId, "да")
        assertEquals("show_hotel_results", initialSearch.nextAction())

        val unsupportedRooms = sendMessage(
            sessionId,
            "Давай два номера: в одном двое, во втором один",
        )

        assertEquals("ask_clarification", unsupportedRooms.nextAction())
        assertEquals(
            "Сейчас я могу искать только один номер за раз. " +
                "Укажите состав гостей для одного номера или выполните отдельный поиск для второго номера.",
            unsupportedRooms.assistantContent(),
        )
        assertFalse(unsupportedRooms.containsKey("hotelSearchId"))
        assertNull(contextStore.findBySession(AssistantSessionId(sessionId))?.rooms)
        assertNull(
            pendingStore.findActiveBySession(
                sessionId = AssistantSessionId(sessionId),
                now = now.plusSeconds(1),
            ),
        )

        val corrected = sendMessage(sessionId, "Тогда один номер на троих")

        assertEquals("ask_clarification", corrected.nextAction())
        assertTrue(corrected.assistantContent().contains("Гости: 3 взрослых, без детей"))
        assertFalse(corrected.assistantContent().contains("Номера:"))
        assertFalse(corrected.containsKey("hotelSearchId"))
        assertEquals(
            1,
            pendingStore.findActiveBySession(
                sessionId = AssistantSessionId(sessionId),
                now = now.plusSeconds(1),
            )?.criteria?.rooms,
        )
        assertEquals(listOf("rooms"), requests[2].missingRequiredFields)
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
