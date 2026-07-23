package com.travelassistant.backend

import com.travelassistant.backend.application.assistant.InMemoryAssistantHotelConstraintsStore
import com.travelassistant.backend.application.assistant.InMemoryPendingConfirmationStore
import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmCandidateRequest
import com.travelassistant.backend.application.llm.LlmClient
import com.travelassistant.backend.application.llm.LlmClientResponse
import com.travelassistant.backend.application.llm.LlmHotelSearchPreferencesPatch
import com.travelassistant.backend.domain.assistant.AssistantSessionId
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
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.ArrayDeque
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
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AssistantHotelRefinementIntegrationTest {
    private val now = Instant.parse("2026-07-22T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun appliesMultiplePreferencesThenClearsOneThroughSeparateConfirmations() = testApplication {
        val constraintsStore = InMemoryAssistantHotelConstraintsStore()
        val pendingStore = InMemoryPendingConfirmationStore()
        val requests = mutableListOf<LlmCandidateRequest>()
        val llmClient = queuedCandidateClient(
            requests = requests,
            responses = listOf(
                candidate(constraints = completeConstraints()),
                candidate(
                    preferencePatch = LlmHotelSearchPreferencesPatch(
                        maxTotalPrice = LlmHotelSearchPreferencesPatch.MaxTotalPrice("80000"),
                        stars = linkedSetOf(4, 5),
                        minimumGuestRating = 8,
                        freeCancellationRequired = true,
                    ),
                ),
                candidate(
                    preferencePatch = LlmHotelSearchPreferencesPatch(
                        clear = setOf(
                            LlmHotelSearchPreferencesPatch.Field.MINIMUM_GUEST_RATING,
                        ),
                    ),
                ),
            ),
        )

        application {
            moduleWithAssistantLlm(
                llmClient = llmClient,
                pendingConfirmationStore = pendingStore,
                hotelConstraintsStore = constraintsStore,
                clock = clock,
            )
        }

        val sessionId = createSession()
        val initialConfirmation = sendMessage(sessionId, "Найди отель в Казани")
        assertEquals("ask_clarification", initialConfirmation.nextAction())
        assertFalse(initialConfirmation.containsKey("hotelSearchId"))

        val firstSearchId = confirmedSearchId(sessionId)
        assertEquals(HttpStatusCode.OK, offersStatus(firstSearchId))

        val refinedConfirmation = sendMessage(
            sessionId,
            "До 80 тысяч, 4–5 звёзд, рейтинг от 8 и бесплатная отмена",
        )

        assertEquals("ask_clarification", refinedConfirmation.nextAction())
        assertFalse(refinedConfirmation.containsKey("hotelSearchId"))
        assertTrue(refinedConfirmation.assistantContent().contains("до 80 000 ₽"))
        assertTrue(refinedConfirmation.assistantContent().contains("4–5 звёзд"))
        assertTrue(refinedConfirmation.assistantContent().contains("рейтинг от 8"))
        assertTrue(refinedConfirmation.assistantContent().contains("бесплатная отмена"))
        assertEquals(1, requests.count { request -> request.userMessage.contains("80 тысяч") })

        val secondSearchId = confirmedSearchId(sessionId)
        assertNotEquals(firstSearchId, secondSearchId)
        assertEquals(HttpStatusCode.OK, offersStatus(firstSearchId))
        assertEquals(HttpStatusCode.OK, offersStatus(secondSearchId))

        val clearConfirmation = sendMessage(sessionId, "Убери ограничение по рейтингу")

        assertEquals("ask_clarification", clearConfirmation.nextAction())
        assertFalse(clearConfirmation.containsKey("hotelSearchId"))
        assertFalse(clearConfirmation.assistantContent().contains("рейтинг от"))
        assertEquals("80000 RUB", requests[2].confirmedConstraints["max-total-price"])
        assertEquals("4,5", requests[2].confirmedConstraints["stars"])
        assertEquals("8", requests[2].confirmedConstraints["min-guest-rating"])
        assertEquals("true", requests[2].confirmedConstraints["free-cancellation"])

        val thirdSearchId = confirmedSearchId(sessionId)
        assertNotEquals(secondSearchId, thirdSearchId)
        assertEquals(HttpStatusCode.OK, offersStatus(firstSearchId))
        assertEquals(HttpStatusCode.OK, offersStatus(secondSearchId))
        assertEquals(HttpStatusCode.OK, offersStatus(thirdSearchId))

        val storedPreferences = constraintsStore
            .findBySession(AssistantSessionId(sessionId))
            ?.preferences
        assertNotNull(storedPreferences)
        assertEquals("80000", storedPreferences.maxTotalPrice?.amount?.toPlainString())
        assertEquals(setOf(4, 5), storedPreferences.stars)
        assertNull(storedPreferences.minimumGuestRating)
        assertTrue(storedPreferences.freeCancellationRequired)
        assertEquals(3, requests.size)
    }

    @Test
    fun declineDoesNotCreateAnotherSearchAndKeepsAppliedPreferences() = testApplication {
        val constraintsStore = InMemoryAssistantHotelConstraintsStore()
        val pendingStore = InMemoryPendingConfirmationStore()
        val llmClient = queuedCandidateClient(
            requests = mutableListOf(),
            responses = listOf(
                candidate(constraints = completeConstraints()),
                candidate(
                    preferencePatch = LlmHotelSearchPreferencesPatch(stars = setOf(5)),
                ),
            ),
        )

        application {
            moduleWithAssistantLlm(
                llmClient = llmClient,
                pendingConfirmationStore = pendingStore,
                hotelConstraintsStore = constraintsStore,
                clock = clock,
            )
        }

        val sessionId = createSession()
        sendMessage(sessionId, "Найди отель в Казани")
        val firstSearchId = confirmedSearchId(sessionId)
        sendMessage(sessionId, "Только пять звёзд")

        val declined = sendMessage(sessionId, "Нет")

        assertEquals("ask_clarification", declined.nextAction())
        assertFalse(declined.containsKey("hotelSearchId"))
        assertEquals(HttpStatusCode.OK, offersStatus(firstSearchId))
        assertEquals(HttpStatusCode.NotFound, offersStatus("hotel-search-local-000002"))
        assertEquals(
            setOf(5),
            constraintsStore.findBySession(AssistantSessionId(sessionId))?.preferences?.stars,
        )
        assertNull(
            pendingStore.findActiveBySession(
                sessionId = AssistantSessionId(sessionId),
                now = now.plusSeconds(1),
            ),
        )
    }

    @Test
    fun llmFailureDoesNotCreateSearchOrDropExistingPreferences() = testApplication {
        val constraintsStore = InMemoryAssistantHotelConstraintsStore()
        val llmClient = queuedResponseClient(
            requests = mutableListOf(),
            responses = listOf(
                LlmClientResponse.Candidate(
                    candidate(
                        constraints = completeConstraints(),
                        preferencePatch = LlmHotelSearchPreferencesPatch(stars = setOf(4, 5)),
                    ),
                ),
                LlmClientResponse.Failure,
            ),
        )

        application {
            moduleWithAssistantLlm(
                llmClient = llmClient,
                hotelConstraintsStore = constraintsStore,
                clock = clock,
            )
        }

        val sessionId = createSession()
        sendMessage(sessionId, "Найди отель 4–5 звёзд в Казани")
        val firstSearchId = confirmedSearchId(sessionId)

        val failure = sendMessage(sessionId, "Теперь только с бесплатной отменой")

        assertEquals("show_boundary_message", failure.nextAction())
        assertFalse(failure.containsKey("hotelSearchId"))
        assertEquals(HttpStatusCode.OK, offersStatus(firstSearchId))
        assertEquals(HttpStatusCode.NotFound, offersStatus("hotel-search-local-000002"))
        assertEquals(
            setOf(4, 5),
            constraintsStore.findBySession(AssistantSessionId(sessionId))?.preferences?.stars,
        )
    }

    @Test
    fun realProviderReceivesFiltersOnlyAfterRefinementConfirmation() = testApplication {
        val searchRequestBodies = mutableListOf<String>()
        val requestedPaths = mutableListOf<String>()
        val hotelsHttpClient = HttpClient(
            MockEngine { request ->
                val path = request.url.encodedPath
                requestedPaths += path
                val body = when (path) {
                    "/search-api/search/autocomplete" -> autocompleteResponse()
                    "/api/v1/hotels/search" -> {
                        searchRequestBodies += (request.body as TextContent).text
                        fixture("search-success.json")
                    }

                    else -> error("Unexpected Hotels API path: $path")
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
        val llmClient = queuedCandidateClient(
            requests = mutableListOf(),
            responses = listOf(
                candidate(constraints = completeConstraints()),
                candidate(
                    preferencePatch = LlmHotelSearchPreferencesPatch(
                        maxTotalPrice = LlmHotelSearchPreferencesPatch.MaxTotalPrice("80000"),
                        stars = setOf(4, 5),
                        minimumGuestRating = 8,
                        freeCancellationRequired = true,
                    ),
                ),
            ),
        )

        application {
            moduleWithAssistantLlm(
                llmClient = llmClient,
                providerConfig = realProviderConfig(),
                realHotelHttpClientFactory = { hotelsHttpClient },
                clock = clock,
            )
        }

        val sessionId = createSession()
        sendMessage(sessionId, "Найди отель в Казани")
        assertEquals(emptyList(), requestedPaths)

        val firstSearchId = confirmedSearchId(sessionId)
        assertEquals(1, searchRequestBodies.size)
        assertFalse(
            Json.parseToJsonElement(searchRequestBodies.single()).jsonObject
                .containsKey("filters"),
        )

        sendMessage(sessionId, "До 80 тысяч, 4–5 звёзд, рейтинг от 8 и бесплатная отмена")
        assertEquals(1, searchRequestBodies.size)

        val secondSearchId = confirmedSearchId(sessionId)
        assertEquals(2, searchRequestBodies.size)
        val refinedRequest = Json.parseToJsonElement(searchRequestBodies[1]).jsonObject
        val filters = refinedRequest.getValue("filters").jsonArray
        assertFalse(offersBody(firstSearchId).containsKey("appliedPreferences"))
        val appliedPreferences = offersBody(secondSearchId)
            .getValue("appliedPreferences")
            .jsonObject

        assertNotEquals(firstSearchId, secondSearchId)
        assertEquals(
            "80000",
            appliedPreferences
                .getValue("maxTotalPrice")
                .jsonObject
                .getValue("amount")
                .jsonPrimitive
                .content,
        )
        assertEquals(
            listOf(4, 5),
            appliedPreferences.getValue("stars").jsonArray.map { star ->
                star.jsonPrimitive.content.toInt()
            },
        )
        assertEquals(
            8,
            appliedPreferences.getValue("minimumGuestRating").jsonPrimitive.content.toInt(),
        )
        assertTrue(
            appliedPreferences.getValue("freeCancellationRequired").jsonPrimitive.content.toBoolean(),
        )
        assertEquals(
            listOf(
                "price",
                "stars",
                "review_rating",
                "free_cancellation_allowed",
            ),
            filters.map { filter ->
                filter.jsonObject.getValue("filterId").jsonPrimitive.content
            },
        )
        assertFalse(refinedRequest.containsKey("sort"))
        assertEquals(
            listOf(
                "/search-api/search/autocomplete",
                "/api/v1/hotels/search",
                "/search-api/search/autocomplete",
                "/api/v1/hotels/search",
            ),
            requestedPaths,
        )
    }

    @Test
    fun completedEmptyRefinementReturnsOneSuggestionWithoutAutomaticRetry() =
        testApplication {
            val constraintsStore = InMemoryAssistantHotelConstraintsStore()
            val requestedPaths = mutableListOf<String>()
            val hotelsHttpClient = queuedHotelsHttpClient(
                requestedPaths = requestedPaths,
                searchResponses = listOf(
                    HttpStatusCode.OK to fixture("search-success.json"),
                    HttpStatusCode.OK to emptySearchResponse(),
                ),
            )
            val llmClient = queuedCandidateClient(
                requests = mutableListOf(),
                responses = listOf(
                    candidate(constraints = completeConstraints()),
                    candidate(
                        preferencePatch = LlmHotelSearchPreferencesPatch(
                            minimumGuestRating = 9,
                        ),
                    ),
                ),
            )

            application {
                moduleWithAssistantLlm(
                    llmClient = llmClient,
                    providerConfig = realProviderConfig(),
                    realHotelHttpClientFactory = { hotelsHttpClient },
                    hotelConstraintsStore = constraintsStore,
                    clock = clock,
                )
            }

            val sessionId = createSession()
            sendMessage(sessionId, "Найди отель в Казани")
            val firstSearchId = confirmedSearchId(sessionId)

            val refinement = sendMessage(sessionId, "Рейтинг не ниже 9")
            assertEquals("ask_clarification", refinement.nextAction())
            assertFalse(refinement.containsKey("hotelSearchId"))

            val emptyResult = sendMessage(sessionId, "Да")
            val secondSearchId = emptyResult["hotelSearchId"]?.jsonPrimitive?.content.orEmpty()
            assertEquals("show_hotel_results", emptyResult.nextAction())
            assertTrue(secondSearchId.isNotBlank())
            assertNotEquals(firstSearchId, secondSearchId)

            val offers = offersBody(secondSearchId)
            val suggestion = offers.getValue("refinementSuggestion").jsonObject
            assertEquals("completed_no_offers", offers.getValue("status").jsonPrimitive.content)
            assertTrue(offers.getValue("offers").jsonArray.isEmpty())
            assertEquals(
                9,
                offers.getValue("appliedPreferences")
                    .jsonObject
                    .getValue("minimumGuestRating")
                    .jsonPrimitive
                    .content
                    .toInt(),
            )
            assertEquals("relax_preference", suggestion.getValue("type").jsonPrimitive.content)
            assertEquals(
                "minimumGuestRating",
                suggestion.getValue("preference").jsonPrimitive.content,
            )
            assertEquals(HttpStatusCode.OK, offersStatus(firstSearchId))
            assertEquals(
                listOf(
                    "/search-api/search/autocomplete",
                    "/api/v1/hotels/search",
                    "/search-api/search/autocomplete",
                    "/api/v1/hotels/search",
                ),
                requestedPaths,
            )
        }

    @Test
    fun providerFailureAfterRefinementKeepsPreviousSearchAndCreatesNoNewId() =
        testApplication {
            val constraintsStore = InMemoryAssistantHotelConstraintsStore()
            val requestedPaths = mutableListOf<String>()
            val hotelsHttpClient = queuedHotelsHttpClient(
                requestedPaths = requestedPaths,
                searchResponses = listOf(
                    HttpStatusCode.OK to fixture("search-success.json"),
                    HttpStatusCode.ServiceUnavailable to providerUnavailableResponse(),
                ),
            )
            val llmClient = queuedCandidateClient(
                requests = mutableListOf(),
                responses = listOf(
                    candidate(constraints = completeConstraints()),
                    candidate(
                        preferencePatch = LlmHotelSearchPreferencesPatch(
                            freeCancellationRequired = true,
                        ),
                    ),
                ),
            )

            application {
                moduleWithAssistantLlm(
                    llmClient = llmClient,
                    providerConfig = realProviderConfig(),
                    realHotelHttpClientFactory = { hotelsHttpClient },
                    hotelConstraintsStore = constraintsStore,
                    clock = clock,
                )
            }

            val sessionId = createSession()
            sendMessage(sessionId, "Найди отель в Казани")
            val firstSearchId = confirmedSearchId(sessionId)
            sendMessage(sessionId, "Только с бесплатной отменой")

            val failure = sendMessage(sessionId, "Да")

            assertEquals("ask_clarification", failure.nextAction())
            assertFalse(failure.containsKey("hotelSearchId"))
            assertEquals(HttpStatusCode.OK, offersStatus(firstSearchId))
            assertEquals(HttpStatusCode.NotFound, offersStatus("hotel-search-local-000002"))
            assertTrue(
                constraintsStore.findBySession(AssistantSessionId(sessionId))
                    ?.preferences
                    ?.freeCancellationRequired == true,
            )
            assertEquals(
                listOf(
                    "/search-api/search/autocomplete",
                    "/api/v1/hotels/search",
                    "/search-api/search/autocomplete",
                    "/api/v1/hotels/search",
                ),
                requestedPaths,
            )
        }

    private fun queuedCandidateClient(
        requests: MutableList<LlmCandidateRequest>,
        responses: List<LlmCandidate>,
    ): LlmClient =
        queuedResponseClient(
            requests = requests,
            responses = responses.map(LlmClientResponse::Candidate),
        )

    private fun queuedResponseClient(
        requests: MutableList<LlmCandidateRequest>,
        responses: List<LlmClientResponse>,
    ): LlmClient {
        val queue = ArrayDeque(responses)
        return LlmClient { request ->
            requests += request
            queue.removeFirst()
        }
    }

    private fun candidate(
        constraints: Map<String, String> = emptyMap(),
        preferencePatch: LlmHotelSearchPreferencesPatch = LlmHotelSearchPreferencesPatch(),
    ): LlmCandidate =
        LlmCandidate(
            outcome = LlmCandidate.Outcome.INTERPRETED,
            intent = LlmCandidate.Intent.HOTEL_SEARCH,
            extractedConstraints = constraints,
            preferencePatch = preferencePatch,
        )

    private fun completeConstraints(): Map<String, String> =
        mapOf(
            "destination" to "Казань",
            "check-in" to "2026-08-10",
            "check-out" to "2026-08-14",
            "adults" to "2",
            "children" to "0",
            "rooms" to "1",
        )

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

    private fun queuedHotelsHttpClient(
        requestedPaths: MutableList<String>,
        searchResponses: List<Pair<HttpStatusCode, String>>,
    ): HttpClient {
        val responses = ArrayDeque(searchResponses)

        return HttpClient(
            MockEngine { request ->
                val path = request.url.encodedPath
                requestedPaths += path
                val response = when (path) {
                    "/search-api/search/autocomplete" ->
                        HttpStatusCode.OK to autocompleteResponse()

                    "/api/v1/hotels/search" -> responses.removeFirst()
                    else -> error("Unexpected Hotels API path: $path")
                }
                respond(
                    content = response.second,
                    status = response.first,
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString(),
                    ),
                )
            },
        ) {
            install(HttpTimeout)
        }
    }

    private fun emptySearchResponse(): String =
        """
        {
          "payload": {
            "filteredHotelsCount": 0,
            "hotels": [],
            "hotelsTotalCount": 0,
            "isLoadingCompleted": true,
            "nextOffset": null
          }
        }
        """.trimIndent()

    private fun providerUnavailableResponse(): String =
        """
        {
          "error": {
            "code": "temporarily_unavailable",
            "details": {}
          }
        }
        """.trimIndent()

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResource("/fixtures/hotels-api/$name")).readText()

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

    private suspend fun ApplicationTestBuilder.confirmedSearchId(sessionId: String): String {
        val response = sendMessage(sessionId, "Да")
        assertEquals("show_hotel_results", response.nextAction())
        return response["hotelSearchId"]?.jsonPrimitive?.content.orEmpty().also { searchId ->
            assertTrue(searchId.isNotBlank())
        }
    }

    private suspend fun ApplicationTestBuilder.offersStatus(searchId: String): HttpStatusCode =
        client.get("/api/v1/hotel-searches/$searchId/offers").status

    private suspend fun ApplicationTestBuilder.offersBody(searchId: String): JsonObject {
        val response = client.get("/api/v1/hotel-searches/$searchId/offers")
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
