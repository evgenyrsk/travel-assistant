package com.travelassistant.backend.api

import com.travelassistant.backend.application.assistant.InMemoryPendingConfirmationStore
import com.travelassistant.backend.application.assistant.PendingConfirmationStatus
import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmClient
import com.travelassistant.backend.application.llm.LlmClientResponse
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
            "I received your hotel request. Please share destination, dates, guests, and budget so I can continue.",
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
            "I received your hotel request. Please share destination, dates, guests, and budget so I can continue.",
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
            "Hotel search created. Ranked offers are ready.",
            assistantBody["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )

        val offersResponse = client.get("/api/v1/hotel-searches/$hotelSearchId/offers")
        val offersBody = Json.parseToJsonElement(offersResponse.bodyAsText()).jsonObject
        val offers = offersBody["offers"]?.jsonArray.orEmpty()

        assertEquals(HttpStatusCode.OK, offersResponse.status)
        assertEquals("fake-offer-rome-001", offers.first().jsonObject["offerId"]?.jsonPrimitive?.content)
        assertEquals(
            "Available; ranked by rating, total stay price, then offer ID.",
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
            "What are your stay dates?",
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

        application {
            moduleWithAssistantLlm(
                FakeLlmClient(LlmClientResponse.Empty),
                pendingConfirmationStore = pendingConfirmationStore,
                clock = routeClock,
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
        assertEquals(false, body.containsKey("hotelSearchId"))
        assertEquals(false, body.containsKey("fallbackReason"))
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
            "Параметры hotel search: направление: Rome; заезд: 2026-07-01; " +
                "выезд: 2026-07-04; взрослые: 2; дети: 0; номера: 1. " +
                "Проверить отели по этим параметрам?",
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
            "Параметры hotel search: направление: Rome; заезд: 2026-07-01; " +
                "выезд: 2026-07-04; взрослые: 2; дети: 0; номера: 1.",
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
            "Please confirm the destination, dates, guests, and rooms before I prepare a hotel search confirmation.",
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

        application {
            moduleWithAssistantLlm(
                FakeLlmClient(LlmClientResponse.Candidate(warningInterpretedHotelSearchCandidate())),
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
        assertEquals("show_boundary_message", body["nextAction"]?.jsonPrimitive?.content)
        assertEquals(false, body.containsKey("hotelSearchId"))
        assertEquals(false, body.containsKey("fallbackReason"))
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
    fun positiveConfirmationReplyConsumesPendingStateWithoutCreatingHotelSearch() = testApplication {
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

        val confirmationPromptResponse = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
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

        assertEquals(HttpStatusCode.OK, confirmationPromptResponse.status)
        assertEquals(HttpStatusCode.OK, replyResponse.status)
        assertEquals("ask_clarification", replyBody["nextAction"]?.jsonPrimitive?.content)
        assertEquals(
            "Confirmation received. I will not start a hotel search automatically yet.",
            replyBody["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )
        assertEquals(false, replyBody.containsKey("hotelSearchId"))
        assertEquals(false, replyBody.containsKey("hotelSearchRequest"))
        replyBody.assertNoRawLlmFields()
        assertEquals(PendingConfirmationStatus.PENDING, activePendingBeforeReply?.status)
        assertEquals(null, activePendingAfterReply)

        val offersResponse = client.get("/api/v1/hotel-searches/hotel-search-local-000001/offers")
        assertEquals(HttpStatusCode.NotFound, offersResponse.status)
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
            "What are your stay dates?",
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
            "Please confirm clearly, cancel, or share corrected hotel search criteria.",
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
            "Okay, I will not start a hotel search. You can share new hotel criteria when ready.",
            replyBody["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )
        assertEquals(false, replyBody.containsKey("hotelSearchId"))
        replyBody.assertNoRawLlmFields()
        assertEquals(null, activePendingAfterReply)

        val offersResponse = client.get("/api/v1/hotel-searches/hotel-search-local-000001/offers")
        assertEquals(HttpStatusCode.NotFound, offersResponse.status)
    }

    @Test
    fun correctionConfirmationReplyConsumesPendingStateWithoutCreatingHotelSearch() = testApplication {
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
            "Please share the corrected destination, dates, guests, and rooms before I continue.",
            replyBody["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )
        assertEquals(false, replyBody.containsKey("hotelSearchId"))
        replyBody.assertNoRawLlmFields()
        assertEquals(null, activePendingAfterReply)

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
            "I could not match that reply to the pending confirmation. Please confirm, cancel, or share corrected criteria.",
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
    fun createAssistantSessionAcceptsOptionalClientContextAsBehaviorNeutralInput() = testApplication {
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
    fun acceptAssistantMessageAcceptsOptionalClientContextAndKeepsNextActionRequired() = testApplication {
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
    fun stage8CompatibilityFullConfirmationCycleDoesNotCreateHotelSearch() = testApplication {
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

        assertEquals("ask_clarification", promptBody["nextAction"]?.jsonPrimitive?.content)
        assertEquals(false, promptBody.containsKey("hotelSearchId"))
        assertEquals("ask_clarification", confirmBody["nextAction"]?.jsonPrimitive?.content)
        assertEquals(
            "Confirmation received. I will not start a hotel search automatically yet.",
            confirmBody["assistantMessage"]?.jsonObject?.get("content")?.jsonPrimitive?.content,
        )
        assertEquals(false, confirmBody.containsKey("hotelSearchId"))
        assertEquals(false, confirmBody.containsKey("hotelSearchRequest"))
        confirmBody.assertNoRawLlmFields()

        val offersResponse = client.get("/api/v1/hotel-searches/hotel-search-local-000001/offers")
        assertEquals(HttpStatusCode.NotFound, offersResponse.status)

        val secondOffersResponse = client.get("/api/v1/hotel-searches/hotel-search-local-000002/offers")
        assertEquals(HttpStatusCode.NotFound, secondOffersResponse.status)

        assertEquals(
            null,
            pendingConfirmationStore.findActiveBySession(
                sessionId = AssistantSessionId(sessionId),
                now = routeNow.plusSeconds(1),
            ),
        )
    }

    @Test
    fun stage8CompatibilityStrictHandoffRemainsOnlySearchCreationPath() = testApplication {
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

        val strictHandoffResponse = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """
                {
                  "message": "hotel-search; destination=Rome; check-in=2026-07-01; check-out=2026-07-04; adults=2; rooms=1"
                }
                """.trimIndent(),
            )
        }
        val strictHandoffBody = Json.parseToJsonElement(strictHandoffResponse.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, strictHandoffResponse.status)
        assertEquals("show_hotel_results", strictHandoffBody["nextAction"]?.jsonPrimitive?.content)
        assertEquals("hotel-search-local-000001", strictHandoffBody["hotelSearchId"]?.jsonPrimitive?.content)

        val offersResponse = client.get("/api/v1/hotel-searches/hotel-search-local-000001/offers")
        assertEquals(HttpStatusCode.OK, offersResponse.status)
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
