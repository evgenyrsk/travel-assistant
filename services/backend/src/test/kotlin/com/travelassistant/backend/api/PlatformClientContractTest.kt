package com.travelassistant.backend.api

import com.travelassistant.backend.application.llm.LlmClient
import com.travelassistant.backend.application.llm.LlmClientResponse
import com.travelassistant.backend.infrastructure.llm.FakeLlmClient
import com.travelassistant.backend.infrastructure.provider.HotelProviderConfig
import com.travelassistant.backend.infrastructure.provider.HotelProviderMode
import com.travelassistant.backend.infrastructure.provider.HotelsApiConfig
import com.travelassistant.backend.infrastructure.provider.HotelsApiTargetConfig
import com.travelassistant.backend.module
import com.travelassistant.backend.moduleWithAssistantLlm
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
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformClientContractTest {

    @Test
    fun sessionAndMessageEndpointsExposeStablePlatformNeutralShape() = testApplication {
        application { module() }

        val created = client.post("/api/v1/assistant/sessions")
        val createdBody = created.jsonBody()
        val session = createdBody.getValue("session").jsonObject
        val sessionId = session.getValue("sessionId").jsonPrimitive.content

        assertEquals(HttpStatusCode.Created, created.status)
        assertEquals(setOf("session", "assistantMessage", "nextAction"), createdBody.keys)
        assertEquals(setOf("sessionId", "status", "createdAt", "updatedAt"), session.keys)
        assertEquals(
            setOf("role", "content"),
            createdBody.getValue("assistantMessage").jsonObject.keys,
        )
        assertEquals("ask_clarification", createdBody.getValue("nextAction").jsonPrimitive.content)
        assertFalse(created.headers.contains(HttpHeaders.AccessControlAllowOrigin))

        val continued = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            jsonBody(AssistantMessageRequest(message = "Казань"))
        }
        val continuedBody = continued.jsonBody()

        assertEquals(HttpStatusCode.OK, continued.status)
        assertEquals(setOf("session", "assistantMessage", "nextAction"), continuedBody.keys)
        assertEquals("ask_clarification", continuedBody.getValue("nextAction").jsonPrimitive.content)
        assertFalse(continuedBody.containsKey("hotelSearchId"))
        assertFalse(continued.headers.contains(HttpHeaders.AccessControlAllowOrigin))
    }

    @Test
    fun successfulChatHandoffExposesOpaqueSearchIdAndPortableOffers() = testApplication {
        application { module() }

        val created = client.post("/api/v1/assistant/sessions").jsonBody()
        val sessionId = created.getValue("session").jsonObject.getValue("sessionId").jsonPrimitive.content
        val handoff = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            jsonBody(
                AssistantMessageRequest(
                    message =
                        "hotel-search; destination=Rome; check-in=2026-07-01; " +
                            "check-out=2026-07-04; adults=2; rooms=1",
                ),
            )
        }
        val handoffBody = handoff.jsonBody()
        val searchId = handoffBody.getValue("hotelSearchId").jsonPrimitive.content

        assertEquals(HttpStatusCode.OK, handoff.status)
        assertEquals(
            setOf("session", "assistantMessage", "nextAction", "hotelSearchId"),
            handoffBody.keys,
        )
        assertEquals("show_hotel_results", handoffBody.getValue("nextAction").jsonPrimitive.content)
        assertTrue(searchId.isNotBlank())

        val offersResponse = client.get("/api/v1/hotel-searches/$searchId/offers")
        val offersBody = offersResponse.jsonBody()
        val firstOffer = offersBody.getValue("offers").jsonArray.first().jsonObject
        val metadata = offersBody.getValue("metadata").jsonObject

        assertEquals(HttpStatusCode.OK, offersResponse.status)
        assertFalse(offersResponse.headers.contains(HttpHeaders.AccessControlAllowOrigin))
        assertEquals(
            setOf("searchId", "status", "offers", "metadata", "providerFacts"),
            offersBody.keys,
        )
        assertTrue(metadata.getValue("warnings").jsonArray.isEmpty())
        assertTrue(firstOffer.getValue("offerId").jsonPrimitive.content.isNotBlank())
        listOf(
            "providerOfferRef",
            "shownPrice",
            "bookHash",
            "rateForHotelsFeed",
            "destinationId",
        ).forEach { providerField ->
            assertFalse(firstOffer.containsKey(providerField))
        }
    }

    @Test
    fun assistantMessageBoundaryEnforcesStrictJsonAndUnicodeLength() = testApplication {
        application { module() }

        val created = client.post("/api/v1/assistant/sessions").jsonBody()
        val sessionId = created.getValue("session").jsonObject.getValue("sessionId").jsonPrimitive.content
        val path = "/api/v1/assistant/sessions/$sessionId/messages"

        val maximumLength = client.post(path) {
            jsonBody(AssistantMessageRequest(message = "😀".repeat(ASSISTANT_MESSAGE_MAX_CODE_POINTS)))
        }
        assertEquals(HttpStatusCode.OK, maximumLength.status)

        val tooLong = client.post(path) {
            jsonBody(AssistantMessageRequest(message = "😀".repeat(ASSISTANT_MESSAGE_MAX_CODE_POINTS + 1)))
        }
        tooLong.assertValidationField("message")

        val oversizedBody = client.post(path) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("{\"message\":\"${"x".repeat(API_JSON_REQUEST_MAX_BYTES)}\"}")
        }
        oversizedBody.assertValidationField("body")

        val oversizedChunkedBody = client.post(path) {
            setBody(
                object : OutgoingContent.ReadChannelContent() {
                    override val contentType: ContentType = ContentType.Application.Json

                    override fun readFrom(): ByteReadChannel =
                        ByteReadChannel(
                            "{\"message\":\"${"x".repeat(API_JSON_REQUEST_MAX_BYTES)}\"}"
                                .encodeToByteArray(),
                        )
                },
            )
        }
        oversizedChunkedBody.assertValidationField("body")

        val blank = client.post(path) {
            jsonBody(AssistantMessageRequest(message = "   "))
        }
        blank.assertValidationField("message")

        val missingMessage = client.post(path) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("{}")
        }
        missingMessage.assertValidationField("message")

        val missingBody = client.post(path)
        missingBody.assertValidationField("message")

        val malformed = client.post(path) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("{\"message\":")
        }
        malformed.assertValidationField("body")

        val unknownField = client.post(path) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"Казань","unexpected":true}""")
        }
        unknownField.assertValidationField("body")

        val unsupportedContentType = client.post(path) {
            header(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
            setBody("""{"message":"Казань"}""")
        }
        unsupportedContentType.assertValidationField("body")

        val unsupportedJsonBody = client.post(path) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("[]")
        }
        unsupportedJsonBody.assertValidationField("body")

        val malformedCreate = client.post("/api/v1/assistant/sessions") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("{")
        }
        malformedCreate.assertValidationField("body")

        val unknownCreateField = client.post("/api/v1/assistant/sessions") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"Казань","unexpected":true}""")
        }
        unknownCreateField.assertValidationField("body")

        val missingCreateMessage = client.post("/api/v1/assistant/sessions") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("{}")
        }
        missingCreateMessage.assertValidationField("message")
    }

    @Test
    fun validatedMessageIsPassedToApplicationWithoutNormalization() = testApplication {
        var receivedMessage: String? = null
        application {
            moduleWithAssistantLlm(
                llmClient = LlmClient { request ->
                    receivedMessage = request.userMessage
                    LlmClientResponse.Empty
                },
            )
        }

        val created = client.post("/api/v1/assistant/sessions").jsonBody()
        val sessionId = created.getValue("session").jsonObject
            .getValue("sessionId").jsonPrimitive.content
        val originalMessage = "  Найди отель в Казани  "

        val response = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            jsonBody(AssistantMessageRequest(message = originalMessage))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(originalMessage, receivedMessage)
    }

    @Test
    fun unknownIdentifiersReturnTypedSafeErrors() = testApplication {
        application { module() }

        val unknownSession = client.post(
            "/api/v1/assistant/sessions/assistant-session-unknown/messages",
        ) {
            jsonBody(AssistantMessageRequest(message = "Казань"))
        }
        val unknownSessionBody = unknownSession.jsonBody()

        assertEquals(HttpStatusCode.NotFound, unknownSession.status)
        assertEquals(
            "SESSION_NOT_FOUND",
            unknownSessionBody.getValue("code").jsonPrimitive.content,
        )
        assertFalse(unknownSessionBody.containsKey("hotelSearchId"))

        val unknownSearch = client.get("/api/v1/hotel-searches/hotel-search-unknown/offers")
        val unknownSearchBody = unknownSearch.jsonBody()

        assertEquals(HttpStatusCode.NotFound, unknownSearch.status)
        assertEquals(
            "HOTEL_SEARCH_NOT_FOUND",
            unknownSearchBody.getValue("code").jsonPrimitive.content,
        )
        assertFalse(unknownSearchBody.containsKey("hotelSearchId"))
    }

    @Test
    fun diagnosticProviderFailureReturnsSafeInternalError() = testApplication {
        application {
            moduleWithAssistantLlm(
                llmClient = FakeLlmClient(LlmClientResponse.Empty),
                providerConfig = realProviderConfig(),
                realHotelHttpClientFactory = ::unavailableHotelsClient,
            )
        }

        val created = client.post("/api/v1/assistant/sessions").jsonBody()
        val sessionId = created.getValue("session").jsonObject.getValue("sessionId").jsonPrimitive.content
        val response = client.post("/api/v1/hotel-searches") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(validSearchRequest(sessionId))
        }
        val body = response.jsonBody()

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertEquals(setOf("code", "message", "requestId"), body.keys)
        assertEquals(
            response.headers[REQUEST_ID_HEADER],
            body.getValue("requestId").jsonPrimitive.content,
        )
        assertEquals("INTERNAL_ERROR", body.getValue("code").jsonPrimitive.content)
        assertEquals(
            "Hotel search could not be completed.",
            body.getValue("message").jsonPrimitive.content,
        )
        assertFalse(body.toString().contains("provider-sensitive"))
        assertFalse(body.containsKey("hotelSearchId"))
    }

    private fun io.ktor.client.request.HttpRequestBuilder.jsonBody(request: AssistantMessageRequest) {
        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(Json.encodeToString(request))
    }

    private suspend fun io.ktor.client.statement.HttpResponse.jsonBody(): JsonObject =
        Json.parseToJsonElement(bodyAsText()).jsonObject

    private suspend fun io.ktor.client.statement.HttpResponse.assertValidationField(expectedField: String) {
        val body = jsonBody()
        assertEquals(HttpStatusCode.BadRequest, status)
        assertEquals("VALIDATION_ERROR", body.getValue("code").jsonPrimitive.content)
        assertEquals(
            expectedField,
            body.getValue("fields").jsonArray.first().jsonObject.getValue("field").jsonPrimitive.content,
        )
        assertFalse(body.containsKey("hotelSearchId"))
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

    private fun unavailableHotelsClient(): HttpClient =
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

    private fun validSearchRequest(sessionId: String): String =
        """
        {
          "sessionId": "$sessionId",
          "criteria": {
            "destination": "Rome",
            "checkInDate": "2026-08-10",
            "checkOutDate": "2026-08-14",
            "guests": {"adults": 2, "children": 0},
            "rooms": 1
          }
        }
        """.trimIndent()
}
