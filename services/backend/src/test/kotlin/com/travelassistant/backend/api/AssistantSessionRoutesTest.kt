package com.travelassistant.backend.api

import com.travelassistant.backend.module
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
import io.ktor.server.testing.testApplication
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssistantSessionRoutesTest {

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
}
