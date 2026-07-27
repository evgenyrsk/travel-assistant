package com.travelassistant.backend

import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmClient
import com.travelassistant.backend.application.llm.LlmClientResponse
import com.travelassistant.backend.application.llm.LlmHotelSearchPreferencesPatch
import com.travelassistant.backend.application.observability.OperationalComponent
import com.travelassistant.backend.application.observability.OperationalDependency
import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventName
import com.travelassistant.backend.application.observability.OperationalEventSink
import com.travelassistant.backend.application.observability.OperationalLevel
import com.travelassistant.backend.application.observability.OperationalOperation
import com.travelassistant.backend.application.observability.OperationalOutcome
import com.travelassistant.backend.domain.hotel.AccommodationConcept
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
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SemanticRuntimeSafetyIntegrationTest {
    private val clock = Clock.fixed(
        Instant.parse("2026-07-22T10:00:00Z"),
        ZoneOffset.UTC,
    )

    @Test
    fun realHotelsWithFakeSemanticAnalysisFailsClosedWithoutDependencyCalls() =
        testApplication {
            val events = mutableListOf<OperationalEvent>()
            var hotelRequestCount = 0

            application {
                moduleWithAssistantLlm(
                    llmClient = semanticSearchLlmClient(),
                    providerConfig = realProviderConfig(),
                    clock = clock,
                    realHotelHttpClientFactory = {
                        countingHttpClient { hotelRequestCount += 1 }
                    },
                    eventSink = OperationalEventSink(events::add),
                )
            }

            val sessionId = createSession()
            val confirmation = sendMessage(sessionId, "Хочу глемпинг в Казани")
            assertEquals("ask_clarification", confirmation.nextAction())

            val confirmed = sendMessage(sessionId, "Да")
            val searchId = confirmed.getValue("hotelSearchId").jsonPrimitive.content

            assertEquals("show_hotel_results", confirmed.nextAction())
            assertEquals(
                "Сейчас не удалось завершить поиск отелей. Попробуйте ещё раз.",
                confirmed.assistantContent(),
            )

            val offers = offersBody(searchId)
            val analysis = offers
                .getValue("metadata")
                .jsonObject
                .getValue("analysis")
                .jsonObject

            assertEquals("failed", offers.getValue("status").jsonPrimitive.content)
            assertTrue(offers.getValue("offers").jsonArray.isEmpty())
            assertEquals("failed", analysis.getValue("status").jsonPrimitive.content)
            assertEquals(0, analysis.getValue("analyzedCount").jsonPrimitive.content.toInt())
            assertEquals(0, analysis.getValue("deepAnalyzedCount").jsonPrimitive.content.toInt())
            assertEquals(0, analysis.getValue("matchCount").jsonPrimitive.content.toInt())
            assertEquals(0, analysis.getValue("probableCount").jsonPrimitive.content.toInt())
            assertEquals(0, hotelRequestCount)

            val terminalEvent = events.single { event ->
                event.name == OperationalEventName.HOTEL_SEARCH_COMPLETED &&
                    event.operation == OperationalOperation.CREATE_HOTEL_SEARCH
            }
            assertEquals(OperationalComponent.HOTEL_SEARCH, terminalEvent.component)
            assertEquals(OperationalOutcome.FAILED, terminalEvent.outcome)
            assertEquals(OperationalLevel.ERROR, terminalEvent.level)
            assertEquals(0, terminalEvent.offerCount)
            assertTrue(
                events.none { event ->
                    event.dependency == OperationalDependency.HOTEL_PROVIDER ||
                        event.dependency == OperationalDependency.ACCOMMODATION_ANALYZER
                },
            )
            assertFalse(events.toString().contains("Казань"))
            assertFalse(events.toString().contains("Хочу глемпинг"))
        }

    @Test
    fun fakeHotelsWithFakeSemanticAnalysisRemainsDeterministicAndNetworkFree() =
        testApplication {
            var hotelClientCreated = false
            var analysisClientCreated = false

            application {
                moduleWithAssistantLlm(
                    llmClient = semanticSearchLlmClient(),
                    clock = clock,
                    realHotelHttpClientFactory = {
                        hotelClientCreated = true
                        countingHttpClient {}
                    },
                    accommodationAnalysisHttpClientFactory = {
                        analysisClientCreated = true
                        countingHttpClient {}
                    },
                )
            }

            val sessionId = createSession()
            sendMessage(sessionId, "Хочу глемпинг в Казани")
            val confirmed = sendMessage(sessionId, "Да")
            val searchId = confirmed.getValue("hotelSearchId").jsonPrimitive.content
            val offers = awaitTerminalOffers(searchId)

            assertEquals("completed_with_offers", offers.getValue("status").jsonPrimitive.content)
            assertEquals(1, offers.getValue("offers").jsonArray.size)
            assertEquals(
                "probable",
                offers.getValue("offers")
                    .jsonArray
                    .single()
                    .jsonObject
                    .getValue("semanticMatch")
                    .jsonObject
                    .getValue("verdict")
                    .jsonPrimitive
                    .content,
            )
            assertFalse(hotelClientCreated)
            assertFalse(analysisClientCreated)
        }

    private fun semanticSearchLlmClient(): LlmClient =
        LlmClient {
            LlmClientResponse.Candidate(
                LlmCandidate(
                    outcome = LlmCandidate.Outcome.INTERPRETED,
                    intent = LlmCandidate.Intent.HOTEL_SEARCH,
                    extractedConstraints = mapOf(
                        "destination" to "Казань",
                        "check-in" to "2026-08-10",
                        "check-out" to "2026-08-14",
                        "adults" to "2",
                        "children" to "0",
                        "rooms" to "1",
                    ),
                    preferencePatch = LlmHotelSearchPreferencesPatch(
                        accommodationConcept = AccommodationConcept.GLAMPING,
                    ),
                ),
            )
        }

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

    private fun countingHttpClient(onRequest: () -> Unit): HttpClient =
        HttpClient(
            MockEngine {
                onRequest()
                respond(
                    content = "{}",
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

    private suspend fun ApplicationTestBuilder.createSession(): String {
        val response = client.post("/api/v1/assistant/sessions")
        assertEquals(HttpStatusCode.Created, response.status)
        return Json.parseToJsonElement(response.bodyAsText())
            .jsonObject
            .getValue("session")
            .jsonObject
            .getValue("sessionId")
            .jsonPrimitive
            .content
    }

    private suspend fun ApplicationTestBuilder.sendMessage(
        sessionId: String,
        message: String,
    ): JsonObject {
        val response = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                buildJsonObject {
                    put("message", message)
                }.toString(),
            )
        }
        assertEquals(HttpStatusCode.OK, response.status)
        return Json.parseToJsonElement(response.bodyAsText()).jsonObject
    }

    private suspend fun ApplicationTestBuilder.offersBody(searchId: String): JsonObject {
        val response = client.get("/api/v1/hotel-searches/$searchId/offers")
        assertEquals(HttpStatusCode.OK, response.status)
        return Json.parseToJsonElement(response.bodyAsText()).jsonObject
    }

    private suspend fun ApplicationTestBuilder.awaitTerminalOffers(searchId: String): JsonObject {
        repeat(MAX_POLL_ATTEMPTS) {
            val offers = offersBody(searchId)
            if (offers.getValue("status").jsonPrimitive.content != "searching") {
                return offers
            }
            delay(POLL_DELAY_MILLIS)
        }
        error("Semantic hotel search did not reach a terminal state")
    }

    private fun JsonObject.nextAction(): String? =
        get("nextAction")?.jsonPrimitive?.content

    private fun JsonObject.assistantContent(): String =
        getValue("assistantMessage")
            .jsonObject
            .getValue("content")
            .jsonPrimitive
            .content

    private companion object {
        const val MAX_POLL_ATTEMPTS = 100
        const val POLL_DELAY_MILLIS = 10L
    }
}
