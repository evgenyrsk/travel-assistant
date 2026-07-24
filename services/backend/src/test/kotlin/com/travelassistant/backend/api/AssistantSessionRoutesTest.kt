package com.travelassistant.backend.api

import com.travelassistant.backend.application.assistant.InMemoryPendingConfirmationStore
import com.travelassistant.backend.application.assistant.PendingConfirmationStatus
import com.travelassistant.backend.application.assistant.AssistantLlmDiagnosticEvent
import com.travelassistant.backend.application.assistant.AssistantLlmDiagnosticObserver
import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmCandidateRequest
import com.travelassistant.backend.application.llm.LlmClient
import com.travelassistant.backend.application.llm.LlmClientResponse
import com.travelassistant.backend.application.llm.LlmHotelSearchPreferencesPatch
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.infrastructure.llm.FakeLlmClient
import com.travelassistant.backend.module
import com.travelassistant.backend.moduleWithAssistantLlm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
import io.ktor.server.testing.testApplication
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssistantSessionRoutesTest {
    private val routeNow = Instant.parse("2026-06-27T10:00:00Z")
    private val routeClock = Clock.fixed(routeNow, ZoneOffset.UTC)

    @Test
    fun createAssistantSessionReturnsCreatedSessionMetadata() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/v1/assistant/sessions")
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val session = body["session"]?.jsonObject
        val createdAt = session?.get("createdAt")?.jsonPrimitive?.content.orEmpty()
        val updatedAt = session?.get("updatedAt")?.jsonPrimitive?.content.orEmpty()
        val assistantMessage = body["assistantMessage"]?.jsonObject

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("assistant-session-local-000001", session?.get("sessionId")?.jsonPrimitive?.content)
        assertEquals("collecting_requirements", session?.get("status")?.jsonPrimitive?.content)
        assertEquals("assistant", assistantMessage?.get("role")?.jsonPrimitive?.content)
        assertEquals(
            "Расскажите, куда и когда планируете поездку и кто едет с вами.",
            assistantMessage?.get("content")?.jsonPrimitive?.content,
        )
        assertEquals("ask_clarification", body["nextAction"]?.jsonPrimitive?.content)
        assertEquals(false, body.containsKey("hotelSearchId"))
        assertEquals(false, body.containsKey("hotelSearchRequest"))
        assertEquals(false, body.containsKey("assistantReply"))
        assertEquals(false, body.containsKey("hotelRequirementsState"))
        assertEquals(false, body.containsKey("hotelRequirementsCoveragePlan"))
        assertEquals(false, body.containsKey("slotCoveragePlan"))
        assertEquals(false, body.containsKey("requirementsState"))
        assertEquals(false, body.containsKey("slots"))
        assertEquals(false, session?.containsKey("clarificationState"))
        assertEquals(false, session?.containsKey("hotelRequirementsState"))
        assertEquals(false, session?.containsKey("hotelRequirementsCoveragePlan"))
        assertTrue(createdAt.isNotBlank())
        Instant.parse(createdAt)
        assertTrue(updatedAt.isNotBlank())
        Instant.parse(updatedAt)
    }

    @Test
    fun acceptAssistantMessageReturnsIntakeMetadata() = testApplication {
        application {
            module()
        }

        val createdSession = client.post("/api/v1/assistant/sessions")
        val createdSessionBody = Json.parseToJsonElement(createdSession.bodyAsText()).jsonObject
        val sessionId = createdSessionBody["session"]?.jsonObject?.get("sessionId")?.jsonPrimitive?.content.orEmpty()

        assertEquals(HttpStatusCode.Created, createdSession.status)

        val response = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"I want a hotel in Rome for two adults next weekend"}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val session = body["session"]?.jsonObject
        val updatedAt = session?.get("updatedAt")?.jsonPrimitive?.content.orEmpty()
        val assistantMessage = body["assistantMessage"]?.jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(sessionId, session?.get("sessionId")?.jsonPrimitive?.content)
        assertEquals("collecting_requirements", session?.get("status")?.jsonPrimitive?.content)
        assertEquals("assistant", assistantMessage?.get("role")?.jsonPrimitive?.content)
        assertEquals(
            "Уточните точные даты заезда и выезда.",
            assistantMessage?.get("content")?.jsonPrimitive?.content,
        )
        assertEquals("ask_clarification", body["nextAction"]?.jsonPrimitive?.content)
        assertEquals(false, body.containsKey("hotelSearchId"))
        assertEquals(false, body.containsKey("hotelSearchRequest"))
        assertEquals(false, body.containsKey("assistantReply"))
        assertEquals(false, body.containsKey("hotelRequirementsState"))
        assertEquals(false, body.containsKey("hotelRequirementsCoveragePlan"))
        assertEquals(false, body.containsKey("slotCoveragePlan"))
        assertEquals(false, body.containsKey("requirementsState"))
        assertEquals(false, body.containsKey("slots"))
        assertEquals(false, session?.containsKey("clarificationState"))
        assertEquals(false, session?.containsKey("hotelRequirementsState"))
        assertEquals(false, session?.containsKey("hotelRequirementsCoveragePlan"))
        assertTrue(updatedAt.isNotBlank())
        Instant.parse(updatedAt)
    }

    @Test
    fun completeExplicitAssistantMessageCreatesSearchAndExposesRankedOffers() = testApplication {
        application {
            module()
        }

        val createdSession = client.post("/api/v1/assistant/sessions")
        val createdSessionBody = Json.parseToJsonElement(createdSession.bodyAsText()).jsonObject
        val sessionId = createdSessionBody["session"]?.jsonObject?.get("sessionId")?.jsonPrimitive?.content.orEmpty()

        val assistantResponse = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """
                {
                  "message": "hotel-search; destination=Rome; check-in=2026-07-01; check-out=2026-07-04; adults=2; rooms=1"
                }
                """.trimIndent(),
            )
        }
        val assistantBody = Json.parseToJsonElement(assistantResponse.bodyAsText()).jsonObject
        val hotelSearchId = assistantBody["hotelSearchId"]?.jsonPrimitive?.content.orEmpty()

        assertEquals(HttpStatusCode.OK, assistantResponse.status)
        assertEquals("show_hotel_results", assistantBody["nextAction"]?.jsonPrimitive?.content)
        assertEquals("hotel-search-local-000001", hotelSearchId)
        assertEquals(
            "Поиск завершён. Результат готов.",
            assistantBody["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )

        val offersResponse = client.get("/api/v1/hotel-searches/$hotelSearchId/offers")
        val offersBody = Json.parseToJsonElement(offersResponse.bodyAsText()).jsonObject
        val offers = offersBody["offers"]?.jsonArray.orEmpty()

        assertEquals(HttpStatusCode.OK, offersResponse.status)
        assertEquals(
            "hotel-offer-local-000002",
            offers.first().jsonObject["offerId"]?.jsonPrimitive?.content,
        )
        assertEquals(false, offers.first().jsonObject.containsKey("providerOfferRef"))
        assertEquals(
            "Доступно; выше размещены варианты с лучшим рейтингом, затем — с меньшей общей ценой за проживание.",
            offers.first().jsonObject["matchSummary"]?.jsonPrimitive?.content,
        )
    }

    @Test
    fun createAssistantSessionAcceptsOptionalInitialMessageAsFoundationIntakeOnly() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/v1/assistant/sessions") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"I want a hotel in Rome"}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val session = body["session"]?.jsonObject
        val sessionId = session?.get("sessionId")?.jsonPrimitive?.content.orEmpty()

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("assistant-session-local-000001", sessionId)
        assertEquals("collecting_requirements", session?.get("status")?.jsonPrimitive?.content)
        assertEquals("ask_clarification", body["nextAction"]?.jsonPrimitive?.content)
        assertEquals(false, body.containsKey("hotelSearchRequest"))
        assertEquals(false, body.containsKey("hotelRequirementsState"))
        assertEquals(false, body.containsKey("hotelRequirementsCoveragePlan"))
        assertEquals(false, body.containsKey("slotCoveragePlan"))
        assertEquals(false, body.containsKey("requirementsState"))
        assertEquals(false, body.containsKey("slots"))

        val followUp = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"For two adults"}""")
        }

        assertEquals(HttpStatusCode.OK, followUp.status)
    }

    @Test
    fun llmClarificationPathReturnsExistingAssistantResponseShape() = testApplication {
        val pendingConfirmationStore = InMemoryPendingConfirmationStore()

        application {
            moduleWithAssistantLlm(
                FakeLlmClient(
                    LlmClientResponse.Candidate(
                        LlmCandidate(
                            outcome = LlmCandidate.Outcome.NEEDS_CLARIFICATION,
                            intent = LlmCandidate.Intent.HOTEL_SEARCH,
                            missingRequiredFields = listOf("stay_dates"),
                            clarificationQuestion = "What are your stay dates?",
                        ),
                    ),
                ),
                pendingConfirmationStore = pendingConfirmationStore,
                clock = routeClock,
            )
        }

        val createdSession = client.post("/api/v1/assistant/sessions")
        val createdSessionBody = Json.parseToJsonElement(createdSession.bodyAsText()).jsonObject
        val sessionId = createdSessionBody["session"]?.jsonObject?.get("sessionId")?.jsonPrimitive?.content.orEmpty()

        val response = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"Find me a quiet hotel in Rome"}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ask_clarification", body["nextAction"]?.jsonPrimitive?.content)
        assertEquals(
            "Уточните точные даты заезда и выезда.",
            body["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )
        assertEquals(false, body.containsKey("hotelSearchId"))
        body.assertNoRawLlmFields()
        assertEquals(
            null,
            pendingConfirmationStore.findActiveBySession(
                sessionId = AssistantSessionId(sessionId),
                now = routeNow.plusSeconds(1),
            ),
        )
    }

    @Test
    fun llmFallbackPathReturnsSafePublicOutcomeWithoutRawReason() = testApplication {
        val pendingConfirmationStore = InMemoryPendingConfirmationStore()
        val diagnosticEvents = mutableListOf<AssistantLlmDiagnosticEvent>()

        application {
            moduleWithAssistantLlm(
                FakeLlmClient(LlmClientResponse.Empty),
                pendingConfirmationStore = pendingConfirmationStore,
                clock = routeClock,
                assistantLlmDiagnosticObserver =
                    AssistantLlmDiagnosticObserver(diagnosticEvents::add),
            )
        }

        val createdSession = client.post("/api/v1/assistant/sessions")
        val createdSessionBody = Json.parseToJsonElement(createdSession.bodyAsText()).jsonObject
        val sessionId = createdSessionBody["session"]?.jsonObject?.get("sessionId")?.jsonPrimitive?.content.orEmpty()

        val response = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"Find me something nice"}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("show_boundary_message", body["nextAction"]?.jsonPrimitive?.content)
        assertEquals(
            "Не удалось обработать сообщение из-за временного сбоя. " +
                "Попробуйте отправить его ещё раз.",
            body["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )
        assertEquals(false, body.containsKey("hotelSearchId"))
        assertEquals(false, body.containsKey("fallbackReason"))
        assertEquals(
            listOf(AssistantLlmDiagnosticEvent.CANDIDATE_EMPTY_RESPONSE),
            diagnosticEvents,
        )
        body.assertNoRawLlmFields()
        assertEquals(
            null,
            pendingConfirmationStore.findActiveBySession(
                sessionId = AssistantSessionId(sessionId),
                now = routeNow.plusSeconds(1),
            ),
        )
    }

    @Test
    fun llmProceedCandidateReturnsConfirmationPromptWithoutCreatingHotelSearch() = testApplication {
        val pendingConfirmationStore = InMemoryPendingConfirmationStore()

        application {
            moduleWithAssistantLlm(
                FakeLlmClient(LlmClientResponse.Candidate(interpretedHotelSearchCandidate())),
                pendingConfirmationStore = pendingConfirmationStore,
                clock = routeClock,
            )
        }

        val createdSession = client.post("/api/v1/assistant/sessions")
        val createdSessionBody = Json.parseToJsonElement(createdSession.bodyAsText()).jsonObject
        val sessionId = createdSessionBody["session"]?.jsonObject?.get("sessionId")?.jsonPrimitive?.content.orEmpty()

        val response = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"Find a hotel in Rome for two adults"}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ask_clarification", body["nextAction"]?.jsonPrimitive?.content)
        assertEquals(
            """Проверьте параметры:
Куда: Rome
Даты: 1–4 июля 2026
Гости: 2 взрослых, без детей

Найти отели по этим параметрам?""",
            body["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )
        assertEquals(false, body.containsKey("hotelSearchId"))
        body.assertNoRawLlmFields()

        val pendingConfirmation = pendingConfirmationStore.findActiveBySession(
            sessionId = AssistantSessionId(sessionId),
            now = routeNow.plusSeconds(1),
        )

        assertEquals("Rome", pendingConfirmation?.criteria?.destination)
        assertEquals(LocalDate.parse("2026-07-01"), pendingConfirmation?.criteria?.checkInDate)
        assertEquals(LocalDate.parse("2026-07-04"), pendingConfirmation?.criteria?.checkOutDate)
        assertEquals(2, pendingConfirmation?.criteria?.guests?.adults)
        assertEquals(0, pendingConfirmation?.criteria?.guests?.children)
        assertEquals(1, pendingConfirmation?.criteria?.rooms)
        assertEquals(PendingConfirmationStatus.PENDING, pendingConfirmation?.status)
        assertEquals(routeNow, pendingConfirmation?.createdAt)
        assertEquals(routeNow, pendingConfirmation?.updatedAt)
        assertEquals(routeNow.plusSeconds(900), pendingConfirmation?.expiresAt)
        assertEquals(
            """Проверьте параметры:
Куда: Rome
Даты: 1–4 июля 2026
Гости: 2 взрослых, без детей""",
            pendingConfirmation?.proposal?.summary,
        )
        listOf(
            "LlmCandidate",
            "raw candidate",
            "candidatePayload",
            "modelResponse",
            "validationIssues",
            "hotelSearchId",
            "show_hotel_results",
        ).forEach { forbidden ->
            assertEquals(false, pendingConfirmation.toString().contains(forbidden))
        }

        val offersResponse = client.get("/api/v1/hotel-searches/hotel-search-local-000001/offers")
        assertEquals(HttpStatusCode.NotFound, offersResponse.status)
    }

    @Test
    fun partialProceedCandidateReturnsClarificationWithoutCreatingHotelSearch() = testApplication {
        val pendingConfirmationStore = InMemoryPendingConfirmationStore()

        application {
            moduleWithAssistantLlm(
                FakeLlmClient(LlmClientResponse.Candidate(partialInterpretedHotelSearchCandidate())),
                pendingConfirmationStore = pendingConfirmationStore,
                clock = routeClock,
            )
        }

        val createdSession = client.post("/api/v1/assistant/sessions")
        val createdSessionBody = Json.parseToJsonElement(createdSession.bodyAsText()).jsonObject
        val sessionId = createdSessionBody["session"]?.jsonObject?.get("sessionId")?.jsonPrimitive?.content.orEmpty()

        val response = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"Find a hotel in Rome"}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ask_clarification", body["nextAction"]?.jsonPrimitive?.content)
        assertEquals(
            "Уточните точные даты заезда и выезда.",
            body["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )
        assertEquals(false, body.containsKey("hotelSearchId"))
        body.assertNoRawLlmFields()
        assertEquals(
            null,
            pendingConfirmationStore.findActiveBySession(
                sessionId = AssistantSessionId(sessionId),
                now = routeNow.plusSeconds(1),
            ),
        )

        val offersResponse = client.get("/api/v1/hotel-searches/hotel-search-local-000001/offers")
        assertEquals(HttpStatusCode.NotFound, offersResponse.status)
    }

    @Test
    fun unsafeProceedCandidateReturnsSafeFallbackWithoutRawReason() = testApplication {
        val pendingConfirmationStore = InMemoryPendingConfirmationStore()
        val diagnosticEvents = mutableListOf<AssistantLlmDiagnosticEvent>()

        application {
            moduleWithAssistantLlm(
                FakeLlmClient(LlmClientResponse.Candidate(warningInterpretedHotelSearchCandidate())),
                pendingConfirmationStore = pendingConfirmationStore,
                clock = routeClock,
                assistantLlmDiagnosticObserver =
                    AssistantLlmDiagnosticObserver(diagnosticEvents::add),
            )
        }

        val createdSession = client.post("/api/v1/assistant/sessions")
        val createdSessionBody = Json.parseToJsonElement(createdSession.bodyAsText()).jsonObject
        val sessionId = createdSessionBody["session"]?.jsonObject?.get("sessionId")?.jsonPrimitive?.content.orEmpty()

        val response = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"Find a hotel in Rome for two adults"}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("show_boundary_message", body["nextAction"]?.jsonPrimitive?.content)
        assertEquals(
            "В параметрах поездки осталось противоречие. " +
                "Переформулируйте запрос или уточните спорное условие.",
            body["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )
        assertEquals(false, body.containsKey("hotelSearchId"))
        assertEquals(false, body.containsKey("fallbackReason"))
        assertEquals(
            listOf(AssistantLlmDiagnosticEvent.CONFIRMATION_CONFLICTS_OR_WARNINGS),
            diagnosticEvents,
        )
        body.assertNoRawLlmFields()
        assertEquals(
            false,
            body["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content.orEmpty()
                .contains("CONFLICTS_OR_WARNINGS"),
        )
        assertEquals(
            null,
            pendingConfirmationStore.findActiveBySession(
                sessionId = AssistantSessionId(sessionId),
                now = routeNow.plusSeconds(1),
            ),
        )

        val offersResponse = client.get("/api/v1/hotel-searches/hotel-search-local-000001/offers")
        assertEquals(HttpStatusCode.NotFound, offersResponse.status)
    }

    @Test
    fun explicitHotelSearchHandoffStillCreatesSearchWhenLlmWouldProceed() = testApplication {
        val pendingConfirmationStore = InMemoryPendingConfirmationStore()

        application {
            moduleWithAssistantLlm(
                FakeLlmClient(LlmClientResponse.Candidate(interpretedHotelSearchCandidate())),
                pendingConfirmationStore = pendingConfirmationStore,
                clock = routeClock,
            )
        }

        val createdSession = client.post("/api/v1/assistant/sessions")
        val createdSessionBody = Json.parseToJsonElement(createdSession.bodyAsText()).jsonObject
        val sessionId = createdSessionBody["session"]?.jsonObject?.get("sessionId")?.jsonPrimitive?.content.orEmpty()

        val assistantResponse = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """
                {
                  "message": "hotel-search; destination=Rome; check-in=2026-07-01; check-out=2026-07-04; adults=2; rooms=1"
                }
                """.trimIndent(),
            )
        }
        val assistantBody = Json.parseToJsonElement(assistantResponse.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, assistantResponse.status)
        assertEquals("show_hotel_results", assistantBody["nextAction"]?.jsonPrimitive?.content)
        assertEquals("hotel-search-local-000001", assistantBody["hotelSearchId"]?.jsonPrimitive?.content)
        assertEquals(
            null,
            pendingConfirmationStore.findActiveBySession(
                sessionId = AssistantSessionId(sessionId),
                now = routeNow.plusSeconds(1),
            ),
        )

        val offersResponse = client.get("/api/v1/hotel-searches/hotel-search-local-000001/offers")
        assertEquals(HttpStatusCode.OK, offersResponse.status)
    }

    @Test
    fun positiveConfirmationReplyConsumesPendingAfterSuccessfulSearchCreation() = testApplication {
        val pendingConfirmationStore = InMemoryPendingConfirmationStore()
        var llmResponse: LlmClientResponse =
            LlmClientResponse.Candidate(interpretedHotelSearchCandidate())
        val testLlmClient = LlmClient { llmResponse }

        application {
            moduleWithAssistantLlm(
                testLlmClient,
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
        val activePendingBeforeReply = pendingConfirmationStore.findActiveBySession(
            sessionId = AssistantSessionId(sessionId),
            now = routeNow.plusSeconds(1),
        )

        llmResponse = LlmClientResponse.Empty

        val replyResponse = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"да"}""")
        }
        val replyBody = Json.parseToJsonElement(replyResponse.bodyAsText()).jsonObject
        val activePendingAfterReply = pendingConfirmationStore.findActiveBySession(
            sessionId = AssistantSessionId(sessionId),
            now = routeNow.plusSeconds(1),
        )
        val hotelSearchId = replyBody["hotelSearchId"]?.jsonPrimitive?.content.orEmpty()

        assertEquals(HttpStatusCode.OK, replyResponse.status)
        assertEquals("show_hotel_results", replyBody["nextAction"]?.jsonPrimitive?.content)
        assertEquals(
            "Поиск завершён. Результат готов.",
            replyBody["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )
        assertTrue(hotelSearchId.isNotBlank())
        replyBody.assertNoRawLlmFields()
        assertEquals(PendingConfirmationStatus.PENDING, activePendingBeforeReply?.status)
        assertEquals(null, activePendingAfterReply)

        val offersResponse = client.get("/api/v1/hotel-searches/$hotelSearchId/offers")
        assertEquals(HttpStatusCode.OK, offersResponse.status)
    }

    @Test
    fun positiveReplyWithoutActivePendingStateUsesExistingLlmPathWithoutCreatingHotelSearch() = testApplication {
        application {
            moduleWithAssistantLlm(
                FakeLlmClient(
                    LlmClientResponse.Candidate(
                        LlmCandidate(
                            outcome = LlmCandidate.Outcome.NEEDS_CLARIFICATION,
                            intent = LlmCandidate.Intent.HOTEL_SEARCH,
                            missingRequiredFields = listOf("stay_dates"),
                            clarificationQuestion = "What are your stay dates?",
                        ),
                    ),
                ),
                pendingConfirmationStore = InMemoryPendingConfirmationStore(),
                clock = routeClock,
            )
        }

        val createdSession = client.post("/api/v1/assistant/sessions")
        val createdSessionBody = Json.parseToJsonElement(createdSession.bodyAsText()).jsonObject
        val sessionId = createdSessionBody["session"]?.jsonObject?.get("sessionId")?.jsonPrimitive?.content.orEmpty()

        val replyResponse = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"да"}""")
        }
        val replyBody = Json.parseToJsonElement(replyResponse.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, replyResponse.status)
        assertEquals("ask_clarification", replyBody["nextAction"]?.jsonPrimitive?.content)
        assertEquals(
            "Уточните точные даты заезда и выезда.",
            replyBody["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )
        assertEquals(false, replyBody.containsKey("hotelSearchId"))
        replyBody.assertNoRawLlmFields()

        val offersResponse = client.get("/api/v1/hotel-searches/hotel-search-local-000001/offers")
        assertEquals(HttpStatusCode.NotFound, offersResponse.status)
    }

    @Test
    fun ambiguousConfirmationReplyKeepsPendingStateActiveWithoutCreatingHotelSearch() = testApplication {
        val pendingConfirmationStore = InMemoryPendingConfirmationStore()
        var llmResponse: LlmClientResponse =
            LlmClientResponse.Candidate(interpretedHotelSearchCandidate())
        val testLlmClient = LlmClient { llmResponse }

        application {
            moduleWithAssistantLlm(
                testLlmClient,
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
        val activePendingBeforeReply = pendingConfirmationStore.findActiveBySession(
            sessionId = AssistantSessionId(sessionId),
            now = routeNow.plusSeconds(1),
        )
        llmResponse = LlmClientResponse.Empty

        val replyResponse = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"ок"}""")
        }
        val replyBody = Json.parseToJsonElement(replyResponse.bodyAsText()).jsonObject
        val activePendingAfterReply = pendingConfirmationStore.findActiveBySession(
            sessionId = AssistantSessionId(sessionId),
            now = routeNow.plusSeconds(1),
        )

        assertEquals(HttpStatusCode.OK, replyResponse.status)
        assertEquals("ask_clarification", replyBody["nextAction"]?.jsonPrimitive?.content)
        assertEquals(
            "Подтвердите параметры, отмените поиск или пришлите исправленные условия.",
            replyBody["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )
        assertEquals(false, replyBody.containsKey("hotelSearchId"))
        replyBody.assertNoRawLlmFields()
        assertEquals(PendingConfirmationStatus.PENDING, activePendingAfterReply?.status)
        assertEquals(activePendingBeforeReply, activePendingAfterReply)

        val offersResponse = client.get("/api/v1/hotel-searches/hotel-search-local-000001/offers")
        assertEquals(HttpStatusCode.NotFound, offersResponse.status)
    }

    @Test
    fun negativeConfirmationReplyConsumesPendingStateWithoutCreatingHotelSearch() = testApplication {
        val pendingConfirmationStore = InMemoryPendingConfirmationStore()
        var llmResponse: LlmClientResponse =
            LlmClientResponse.Candidate(interpretedHotelSearchCandidate())
        val testLlmClient = LlmClient { llmResponse }

        application {
            moduleWithAssistantLlm(
                testLlmClient,
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

        val replyResponse = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"не надо"}""")
        }
        val replyBody = Json.parseToJsonElement(replyResponse.bodyAsText()).jsonObject
        val activePendingAfterReply = pendingConfirmationStore.findActiveBySession(
            sessionId = AssistantSessionId(sessionId),
            now = routeNow.plusSeconds(1),
        )

        assertEquals(HttpStatusCode.OK, replyResponse.status)
        assertEquals("ask_clarification", replyBody["nextAction"]?.jsonPrimitive?.content)
        assertEquals(
            "Хорошо, поиск отелей не запущен. Когда будете готовы, сообщите новые параметры.",
            replyBody["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )
        assertEquals(false, replyBody.containsKey("hotelSearchId"))
        replyBody.assertNoRawLlmFields()
        assertEquals(null, activePendingAfterReply)

        val offersResponse = client.get("/api/v1/hotel-searches/hotel-search-local-000001/offers")
        assertEquals(HttpStatusCode.NotFound, offersResponse.status)
    }

    @Test
    fun correctionConfirmationReplyReplacesPendingStateWithoutCreatingHotelSearch() = testApplication {
        val pendingConfirmationStore = InMemoryPendingConfirmationStore()
        var llmResponse: LlmClientResponse =
            LlmClientResponse.Candidate(interpretedHotelSearchCandidate())
        val testLlmClient = LlmClient { llmResponse }

        application {
            moduleWithAssistantLlm(
                testLlmClient,
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
        llmResponse = LlmClientResponse.Candidate(
            interpretedHotelSearchCandidate().copy(
                extractedConstraints = mapOf("destination" to "Paris"),
            ),
        )

        val replyResponse = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"лучше Париж"}""")
        }
        val replyBody = Json.parseToJsonElement(replyResponse.bodyAsText()).jsonObject
        val activePendingAfterReply = pendingConfirmationStore.findActiveBySession(
            sessionId = AssistantSessionId(sessionId),
            now = routeNow.plusSeconds(1),
        )

        assertEquals(HttpStatusCode.OK, replyResponse.status)
        assertEquals("ask_clarification", replyBody["nextAction"]?.jsonPrimitive?.content)
        assertEquals(
            """Проверьте параметры:
Куда: Paris
Даты: 1–4 июля 2026
Гости: 2 взрослых, без детей

Найти отели по этим параметрам?""",
            replyBody["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )
        assertEquals(false, replyBody.containsKey("hotelSearchId"))
        replyBody.assertNoRawLlmFields()
        assertEquals("Paris", activePendingAfterReply?.criteria?.destination)

        val offersResponse = client.get("/api/v1/hotel-searches/hotel-search-local-000001/offers")
        assertEquals(HttpStatusCode.NotFound, offersResponse.status)
    }

    @Test
    fun unknownConfirmationReplyKeepsPendingStateActiveWithoutCreatingHotelSearch() = testApplication {
        val pendingConfirmationStore = InMemoryPendingConfirmationStore()
        var llmResponse: LlmClientResponse =
            LlmClientResponse.Candidate(interpretedHotelSearchCandidate())
        val testLlmClient = LlmClient { llmResponse }

        application {
            moduleWithAssistantLlm(
                testLlmClient,
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
        val activePendingBeforeReply = pendingConfirmationStore.findActiveBySession(
            sessionId = AssistantSessionId(sessionId),
            now = routeNow.plusSeconds(1),
        )
        llmResponse = LlmClientResponse.Empty

        val replyResponse = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"расскажи про музеи рядом"}""")
        }
        val replyBody = Json.parseToJsonElement(replyResponse.bodyAsText()).jsonObject
        val activePendingAfterReply = pendingConfirmationStore.findActiveBySession(
            sessionId = AssistantSessionId(sessionId),
            now = routeNow.plusSeconds(1),
        )

        assertEquals(HttpStatusCode.OK, replyResponse.status)
        assertEquals("ask_clarification", replyBody["nextAction"]?.jsonPrimitive?.content)
        assertEquals(
            "Не удалось распознать ответ на подтверждение. Подтвердите параметры, отмените поиск или пришлите исправленные условия.",
            replyBody["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )
        assertEquals(false, replyBody.containsKey("hotelSearchId"))
        replyBody.assertNoRawLlmFields()
        assertEquals(PendingConfirmationStatus.PENDING, activePendingAfterReply?.status)
        assertEquals(activePendingBeforeReply, activePendingAfterReply)

        val offersResponse = client.get("/api/v1/hotel-searches/hotel-search-local-000001/offers")
        assertEquals(HttpStatusCode.NotFound, offersResponse.status)
    }

    @Test
    fun createAssistantSessionAcceptsOptionalClientContextWithoutEchoingIt() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/v1/assistant/sessions") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """
                {
                  "message": "I want a hotel in Rome",
                  "clientContext": {
                    "locale": "en-US",
                    "timezone": "Europe/Rome"
                  }
                }
                """.trimIndent(),
            )
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val session = body["session"]?.jsonObject

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("assistant-session-local-000001", session?.get("sessionId")?.jsonPrimitive?.content)
        assertEquals("collecting_requirements", session?.get("status")?.jsonPrimitive?.content)
        assertEquals("ask_clarification", body["nextAction"]?.jsonPrimitive?.content)
        assertEquals(false, body.containsKey("clientContext"))
        assertEquals(false, body.containsKey("hotelSearchRequest"))
        assertEquals(false, body.containsKey("searchIntentSummary"))
    }

    @Test
    fun acceptAssistantMessageAcceptsOptionalClientContextWithoutEchoingIt() = testApplication {
        application {
            module()
        }

        val createdSession = client.post("/api/v1/assistant/sessions")
        val createdSessionBody = Json.parseToJsonElement(createdSession.bodyAsText()).jsonObject
        val sessionId = createdSessionBody["session"]?.jsonObject?.get("sessionId")?.jsonPrimitive?.content.orEmpty()

        assertEquals(HttpStatusCode.Created, createdSession.status)

        val response = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """
                {
                  "message": "For two adults",
                  "clientContext": {
                    "locale": "en-US",
                    "timezone": "Europe/Rome"
                  }
                }
                """.trimIndent(),
            )
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val session = body["session"]?.jsonObject
        val assistantMessage = body["assistantMessage"]?.jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(sessionId, session?.get("sessionId")?.jsonPrimitive?.content)
        assertEquals("collecting_requirements", session?.get("status")?.jsonPrimitive?.content)
        assertEquals("assistant", assistantMessage?.get("role")?.jsonPrimitive?.content)
        assertEquals("ask_clarification", body["nextAction"]?.jsonPrimitive?.content)
        assertEquals(false, body.containsKey("clientContext"))
        assertEquals(false, body.containsKey("hotelSearchRequest"))
        assertEquals(false, body.containsKey("searchIntentSummary"))
    }

    @Test
    fun clientTimezoneProvidesCurrentReferenceDateAndOmittedRoomsDefaultToOne() = testApplication {
        val clientNow = Instant.parse("2026-07-22T22:30:00Z")
        var capturedRequest: LlmCandidateRequest? = null
        val testLlmClient = LlmClient { request ->
            capturedRequest = request
            LlmClientResponse.Candidate(
                LlmCandidate(
                    outcome = LlmCandidate.Outcome.INTERPRETED,
                    intent = LlmCandidate.Intent.HOTEL_SEARCH,
                    extractedConstraints = mapOf(
                        "destination" to "Москва",
                        "check-in" to "2026-07-23",
                        "check-out" to "2026-07-24",
                        "adults" to "2",
                        "children" to "0",
                    ),
                ),
            )
        }

        application {
            moduleWithAssistantLlm(
                testLlmClient,
                clock = Clock.fixed(clientNow, ZoneOffset.UTC),
            )
        }

        val response = client.post("/api/v1/assistant/sessions") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """
                {
                  "message": "Хочу в Москву сегодня до завтра с супругой вдвоём",
                  "clientContext": {
                    "locale": "ru-RU",
                    "timezone": "Europe/Moscow"
                  }
                }
                """.trimIndent(),
            )
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(LocalDate.parse("2026-07-23"), capturedRequest?.referenceDate)
        assertEquals("1", capturedRequest?.confirmedConstraints?.get("rooms"))
        assertEquals(false, capturedRequest?.missingRequiredFields?.contains("rooms"))
        assertEquals("ask_clarification", body["nextAction"]?.jsonPrimitive?.content)
        assertTrue(
            body["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content
                ?.contains("Даты: 23–24 июля 2026") == true,
        )
        assertTrue(
            body["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content
                ?.contains("Гости: 2 взрослых, без детей") == true,
        )
        assertEquals(
            false,
            body["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content
                ?.contains("Номера:") == true,
        )
        assertEquals(false, body.containsKey("hotelSearchId"))
    }

    @Test
    fun spouseTomorrowOneNightAndBreakfastReachReadableConfirmation() = testApplication {
        val clientNow = Instant.parse("2026-07-23T08:00:00Z")
        var capturedRequest: LlmCandidateRequest? = null
        val testLlmClient = LlmClient { request ->
            capturedRequest = request
            LlmClientResponse.Candidate(
                LlmCandidate(
                    outcome = LlmCandidate.Outcome.INTERPRETED,
                    intent = LlmCandidate.Intent.HOTEL_SEARCH,
                    extractedConstraints = mapOf(
                        "destination" to "Москва",
                        "check-in" to "2026-07-24",
                        "check-out" to "2026-07-25",
                        "adults" to "2",
                        "children" to "0",
                        "rooms" to "1",
                    ),
                    preferencePatch = LlmHotelSearchPreferencesPatch(
                        breakfastIncludedRequired = true,
                    ),
                ),
            )
        }

        application {
            moduleWithAssistantLlm(
                testLlmClient,
                clock = Clock.fixed(clientNow, ZoneOffset.UTC),
            )
        }

        val response = client.post("/api/v1/assistant/sessions") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """
                {
                  "message": "Хочу в Москву с супругой на завтра на одну ночь в отель с завтраками",
                  "clientContext": {
                    "locale": "ru-RU",
                    "timezone": "Europe/Moscow"
                  }
                }
                """.trimIndent(),
            )
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val reply = body["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(LocalDate.parse("2026-07-23"), capturedRequest?.referenceDate)
        assertEquals("1", capturedRequest?.confirmedConstraints?.get("rooms"))
        assertEquals("ask_clarification", body["nextAction"]?.jsonPrimitive?.content)
        assertEquals(
            """Проверьте параметры:
Куда: Москва
Даты: 24–25 июля 2026
Гости: 2 взрослых, без детей
Условия: завтрак включён

Найти отели по этим параметрам?""",
            reply,
        )
        assertEquals(false, body.containsKey("hotelSearchId"))
    }

    @Test
    fun relativeDatesWithoutClientTimezoneRequireAbsoluteDateClarification() = testApplication {
        val testLlmClient = LlmClient {
            LlmClientResponse.Candidate(
                LlmCandidate(
                    outcome = LlmCandidate.Outcome.INTERPRETED,
                    intent = LlmCandidate.Intent.HOTEL_SEARCH,
                    extractedConstraints = mapOf(
                        "destination" to "Москва",
                        "check-in" to "2026-07-23",
                        "check-out" to "2026-07-24",
                        "adults" to "2",
                    ),
                ),
            )
        }

        application {
            moduleWithAssistantLlm(testLlmClient, clock = routeClock)
        }

        val response = client.post("/api/v1/assistant/sessions") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"Хочу в Москву сегодня до завтра с супругой вдвоём"}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("ask_clarification", body["nextAction"]?.jsonPrimitive?.content)
        assertEquals(
            "Уточните даты поездки, указав день, месяц и год.",
            body["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )
        assertEquals(false, body.containsKey("hotelSearchId"))
    }

    @Test
    fun relativeDatesWithInvalidClientTimezoneRequireAbsoluteDateClarification() = testApplication {
        val testLlmClient = LlmClient {
            LlmClientResponse.Candidate(
                LlmCandidate(
                    outcome = LlmCandidate.Outcome.INTERPRETED,
                    intent = LlmCandidate.Intent.HOTEL_SEARCH,
                    extractedConstraints = mapOf(
                        "destination" to "Москва",
                        "check-in" to "2026-07-23",
                        "check-out" to "2026-07-24",
                        "adults" to "2",
                    ),
                ),
            )
        }

        application {
            moduleWithAssistantLlm(testLlmClient, clock = routeClock)
        }

        val response = client.post("/api/v1/assistant/sessions") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """
                {
                  "message": "Хочу в Москву сегодня до завтра с супругой вдвоём",
                  "clientContext": {"timezone": "not-a-timezone"}
                }
                """.trimIndent(),
            )
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("ask_clarification", body["nextAction"]?.jsonPrimitive?.content)
        assertEquals(
            "Уточните даты поездки, указав день, месяц и год.",
            body["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )
        assertEquals(false, body.containsKey("hotelSearchId"))
    }

    @Test
    fun pastYearFromLlmIsRejectedBeforeConfirmation() = testApplication {
        val testLlmClient = LlmClient {
            LlmClientResponse.Candidate(
                LlmCandidate(
                    outcome = LlmCandidate.Outcome.INTERPRETED,
                    intent = LlmCandidate.Intent.HOTEL_SEARCH,
                    extractedConstraints = mapOf(
                        "destination" to "Москва",
                        "check-in" to "2025-08-10",
                        "check-out" to "2025-08-14",
                        "adults" to "2",
                    ),
                ),
            )
        }

        application {
            moduleWithAssistantLlm(
                testLlmClient,
                clock = Clock.fixed(Instant.parse("2026-07-23T10:00:00Z"), ZoneOffset.UTC),
            )
        }

        val response = client.post("/api/v1/assistant/sessions") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """
                {
                  "message": "Москва с 10 по 14 августа 2025 года для двоих",
                  "clientContext": {"timezone": "Europe/Moscow"}
                }
                """.trimIndent(),
            )
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("ask_clarification", body["nextAction"]?.jsonPrimitive?.content)
        assertEquals(
            "Уточните даты поездки, указав день, месяц и год.",
            body["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )
        assertEquals(false, body.containsKey("hotelSearchId"))
    }

    @Test
    fun unknownAssistantSessionReturnsStructuredNotFoundError() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/v1/assistant/sessions/assistant-session-local-unknown/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"I want a hotel in Rome"}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("SESSION_NOT_FOUND", body["code"]?.jsonPrimitive?.content)
        assertEquals("Assistant session was not found.", body["message"]?.jsonPrimitive?.content)
        assertEquals(
            "assistant-session-local-unknown",
            body["details"]?.jsonObject?.get("sessionId")?.jsonPrimitive?.content,
        )
    }

    @Test
    fun blankAssistantMessageReturnsValidationError() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/v1/assistant/sessions/assistant-session-local-000001/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"   "}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("VALIDATION_ERROR", body["code"]?.jsonPrimitive?.content)
        assertEquals("Request validation failed.", body["message"]?.jsonPrimitive?.content)
        assertEquals("message", body["fields"]?.jsonArray?.get(0)?.jsonObject?.get("field")?.jsonPrimitive?.content)
    }

    @Test
    fun missingAssistantMessageReturnsValidationError() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/v1/assistant/sessions/assistant-session-local-000001/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("VALIDATION_ERROR", body["code"]?.jsonPrimitive?.content)
        assertEquals("Request validation failed.", body["message"]?.jsonPrimitive?.content)
        assertEquals("message", body["fields"]?.jsonArray?.get(0)?.jsonObject?.get("field")?.jsonPrimitive?.content)
    }

    @Test
    fun missingAssistantMessageBodyReturnsValidationError() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/v1/assistant/sessions/assistant-session-local-000001/messages")
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("VALIDATION_ERROR", body["code"]?.jsonPrimitive?.content)
        assertEquals("Request validation failed.", body["message"]?.jsonPrimitive?.content)
        assertEquals("message", body["fields"]?.jsonArray?.get(0)?.jsonObject?.get("field")?.jsonPrimitive?.content)
    }

    @Test
    fun blankInitialAssistantMessageReturnsValidationError() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/v1/assistant/sessions") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"   "}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("VALIDATION_ERROR", body["code"]?.jsonPrimitive?.content)
        assertEquals("Request validation failed.", body["message"]?.jsonPrimitive?.content)
        assertEquals("message", body["fields"]?.jsonArray?.get(0)?.jsonObject?.get("field")?.jsonPrimitive?.content)
    }

    @Test
    fun missingInitialAssistantMessageReturnsValidationErrorWhenRequestBodyIsPresent() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/v1/assistant/sessions") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("VALIDATION_ERROR", body["code"]?.jsonPrimitive?.content)
        assertEquals("Request validation failed.", body["message"]?.jsonPrimitive?.content)
        assertEquals("message", body["fields"]?.jsonArray?.get(0)?.jsonObject?.get("field")?.jsonPrimitive?.content)
    }

    @Test
    fun stage8CompatibilityFullConfirmationCycleCreatesHotelSearchWithResults() = testApplication {
        val pendingConfirmationStore = InMemoryPendingConfirmationStore()
        var llmResponse: LlmClientResponse =
            LlmClientResponse.Candidate(interpretedHotelSearchCandidate())
        val testLlmClient = LlmClient { llmResponse }

        application {
            moduleWithAssistantLlm(
                testLlmClient,
                pendingConfirmationStore = pendingConfirmationStore,
                clock = routeClock,
            )
        }

        val createdSession = client.post("/api/v1/assistant/sessions")
        val createdSessionBody = Json.parseToJsonElement(createdSession.bodyAsText()).jsonObject
        val sessionId = createdSessionBody["session"]?.jsonObject?.get("sessionId")?.jsonPrimitive?.content.orEmpty()

        val promptResponse = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"Find a hotel in Rome for two adults"}""")
        }
        val promptBody = Json.parseToJsonElement(promptResponse.bodyAsText()).jsonObject

        llmResponse = LlmClientResponse.Empty

        val confirmResponse = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"да"}""")
        }
        val confirmBody = Json.parseToJsonElement(confirmResponse.bodyAsText()).jsonObject
        val hotelSearchId = confirmBody["hotelSearchId"]?.jsonPrimitive?.content.orEmpty()

        assertEquals("ask_clarification", promptBody["nextAction"]?.jsonPrimitive?.content)
        assertEquals(false, promptBody.containsKey("hotelSearchId"))
        assertEquals("show_hotel_results", confirmBody["nextAction"]?.jsonPrimitive?.content)
        assertEquals(
            "Поиск завершён. Результат готов.",
            confirmBody["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )
        assertTrue(hotelSearchId.isNotBlank())
        confirmBody.assertNoRawLlmFields()

        val offersResponse = client.get("/api/v1/hotel-searches/$hotelSearchId/offers")
        assertEquals(HttpStatusCode.OK, offersResponse.status)

        val activePendingAfterConfirmation = pendingConfirmationStore.findActiveBySession(
            sessionId = AssistantSessionId(sessionId),
            now = routeNow.plusSeconds(1),
        )
        assertEquals(null, activePendingAfterConfirmation)
    }

    @Test
    fun stage8CompatibilityStrictHandoffAfterConfirmationStillCreatesSearch() = testApplication {
        val pendingConfirmationStore = InMemoryPendingConfirmationStore()
        var llmResponse: LlmClientResponse =
            LlmClientResponse.Candidate(interpretedHotelSearchCandidate())
        val testLlmClient = LlmClient { llmResponse }

        application {
            moduleWithAssistantLlm(
                testLlmClient,
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
        val confirmationSearchId = confirmBody["hotelSearchId"]?.jsonPrimitive?.content.orEmpty()

        val strictHandoffResponse = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """
                {
                  "message": "hotel-search; destination=Paris; check-in=2026-08-01; check-out=2026-08-05; adults=1; rooms=1"
                }
                """.trimIndent(),
            )
        }
        val strictHandoffBody = Json.parseToJsonElement(strictHandoffResponse.bodyAsText()).jsonObject
        val strictSearchId = strictHandoffBody["hotelSearchId"]?.jsonPrimitive?.content.orEmpty()

        assertEquals(HttpStatusCode.OK, strictHandoffResponse.status)
        assertEquals("show_hotel_results", strictHandoffBody["nextAction"]?.jsonPrimitive?.content)
        assertTrue(strictSearchId.isNotBlank())
        assertTrue(confirmationSearchId.isNotBlank())
        assertTrue(confirmationSearchId != strictSearchId)
    }

    @Test
    fun repeatedConfirmationAfterConsumedSuccessGoesThroughLlmPath() = testApplication {
        val pendingConfirmationStore = InMemoryPendingConfirmationStore()
        var llmResponse: LlmClientResponse =
            LlmClientResponse.Candidate(interpretedHotelSearchCandidate())
        val testLlmClient = LlmClient { llmResponse }

        application {
            moduleWithAssistantLlm(
                testLlmClient,
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

        val firstReplyResponse = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"да"}""")
        }
        val firstReplyBody = Json.parseToJsonElement(firstReplyResponse.bodyAsText()).jsonObject
        val firstSearchId = firstReplyBody["hotelSearchId"]?.jsonPrimitive?.content.orEmpty()

        assertEquals("show_hotel_results", firstReplyBody["nextAction"]?.jsonPrimitive?.content)
        assertTrue(firstSearchId.isNotBlank())

        val secondReplyResponse = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"да"}""")
        }
        val secondReplyBody = Json.parseToJsonElement(secondReplyResponse.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, secondReplyResponse.status)
        assertEquals("show_boundary_message", secondReplyBody["nextAction"]?.jsonPrimitive?.content)
        assertEquals(false, secondReplyBody.containsKey("hotelSearchId"))

        val activePendingAfterReplies = pendingConfirmationStore.findActiveBySession(
            sessionId = AssistantSessionId(sessionId),
            now = routeNow.plusSeconds(1),
        )
        assertEquals(null, activePendingAfterReplies)
    }

    @Test
    fun stage8WiringStrictHandoffAfterConfirmedReplyStillCreatesSearch() = testApplication {
        val pendingConfirmationStore = InMemoryPendingConfirmationStore()
        var llmResponse: LlmClientResponse =
            LlmClientResponse.Candidate(interpretedHotelSearchCandidate())
        val testLlmClient = LlmClient { llmResponse }

        application {
            moduleWithAssistantLlm(
                testLlmClient,
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

        client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"да"}""")
        }

        val strictResponse = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """
                {
                  "message": "hotel-search; destination=Paris; check-in=2026-08-01; check-out=2026-08-05; adults=1; rooms=1"
                }
                """.trimIndent(),
            )
        }
        val strictBody = Json.parseToJsonElement(strictResponse.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, strictResponse.status)
        assertEquals("show_hotel_results", strictBody["nextAction"]?.jsonPrimitive?.content)
        assertTrue(strictBody.containsKey("hotelSearchId"))
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

    private fun partialInterpretedHotelSearchCandidate(): LlmCandidate =
        LlmCandidate(
            outcome = LlmCandidate.Outcome.INTERPRETED,
            intent = LlmCandidate.Intent.HOTEL_SEARCH,
            extractedConstraints = mapOf("destination" to "Rome"),
        )

    private fun warningInterpretedHotelSearchCandidate(): LlmCandidate =
        LlmCandidate(
            outcome = LlmCandidate.Outcome.INTERPRETED,
            intent = LlmCandidate.Intent.HOTEL_SEARCH,
            extractedConstraints = mapOf(
                "destination" to "Rome",
                "check-in" to "2026-07-01",
                "check-out" to "2026-07-04",
                "adults" to "2",
                "rooms" to "1",
            ),
            warnings = listOf("Destination may be ambiguous."),
        )

    private fun JsonObject.assertNoRawLlmFields() {
        listOf(
            "candidate",
            "llmCandidate",
            "candidatePayload",
            "modelResponse",
            "extractedConstraints",
            "missingRequiredFields",
            "conflicts",
            "warnings",
            "displayFields",
            "confirmationQuestion",
            "confirmationProposal",
            "validationIssues",
            "fallbackReason",
        ).forEach { field ->
            assertEquals(false, containsKey(field))
        }
    }
}
