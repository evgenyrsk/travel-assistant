package com.travelassistant.backend.api

import com.travelassistant.backend.application.hotel.CreateHotelSearchCommand
import com.travelassistant.backend.application.hotel.CreateHotelSearchResult
import com.travelassistant.backend.application.hotel.HotelLocationSuggestion
import com.travelassistant.backend.application.hotel.HotelOfferProviderResult
import com.travelassistant.backend.application.hotel.HotelSearchBoundary
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import com.travelassistant.backend.domain.hotel.HotelSearchId
import com.travelassistant.backend.domain.hotel.HotelSearchPreferences
import com.travelassistant.backend.module
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HotelSearchRoutesTest {

    @Test
    fun createsHotelSearchAndReturnsDeterministicFakeOffers() = testApplication {
        application {
            module()
        }

        val sessionResponse = client.post("/api/v1/assistant/sessions")
        val sessionBody = Json.parseToJsonElement(sessionResponse.bodyAsText()).jsonObject
        val sessionId = sessionBody["session"]
            ?.jsonObject
            ?.get("sessionId")
            ?.jsonPrimitive
            ?.content
            .orEmpty()

        val createSearchResponse = client.post("/api/v1/hotel-searches") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(validSearchBody(sessionId))
        }
        val createSearchBody = Json.parseToJsonElement(createSearchResponse.bodyAsText()).jsonObject
        val searchId = createSearchBody["searchId"]?.jsonPrimitive?.content.orEmpty()

        assertEquals(
            HttpStatusCode.Accepted,
            createSearchResponse.status,
            createSearchResponse.bodyAsText(),
        )
        assertEquals("hotel-search-local-000001", searchId)
        assertEquals(sessionId, createSearchBody["sessionId"]?.jsonPrimitive?.content)
        assertEquals("completed_with_offers", createSearchBody["status"]?.jsonPrimitive?.content)
        assertEquals(
            "Rome",
            createSearchBody["criteria"]?.jsonObject?.get("destination")?.jsonPrimitive?.content,
        )

        val offersResponse = client.get("/api/v1/hotel-searches/$searchId/offers")
        val offersBody = Json.parseToJsonElement(offersResponse.bodyAsText()).jsonObject
        val offers = offersBody["offers"]?.jsonArray.orEmpty()
        val firstOffer = offers.first().jsonObject

        assertEquals(HttpStatusCode.OK, offersResponse.status)
        assertEquals(searchId, offersBody["searchId"]?.jsonPrimitive?.content)
        assertEquals("completed_with_offers", offersBody["status"]?.jsonPrimitive?.content)
        assertEquals(2, offers.size)
        assertEquals("fake-offer-rome-001", firstOffer["offerId"]?.jsonPrimitive?.content)
        assertEquals("Rome Central Hotel", firstOffer["hotelName"]?.jsonPrimitive?.content)
        assertEquals("Rome", firstOffer["location"]?.jsonObject?.get("city")?.jsonPrimitive?.content)
        assertEquals("Italy", firstOffer["location"]?.jsonObject?.get("country")?.jsonPrimitive?.content)
        assertEquals("EUR", firstOffer["price"]?.jsonObject?.get("currency")?.jsonPrimitive?.content)
        assertEquals("local_fake_provider", firstOffer["source"]?.jsonPrimitive?.content)
        assertEquals(
            "Доступно; выше размещены варианты с лучшим рейтингом, затем — с меньшей общей ценой за проживание.",
            firstOffer["matchSummary"]?.jsonPrimitive?.content,
        )
        assertTrue(firstOffer["amenities"]?.jsonArray?.isNotEmpty() == true)
    }

    @Test
    fun rejectsHotelSearchWithoutDestination() = testApplication {
        application {
            module()
        }

        val sessionResponse = client.post("/api/v1/assistant/sessions")
        val sessionBody = Json.parseToJsonElement(sessionResponse.bodyAsText()).jsonObject
        val sessionId = sessionBody["session"]
            ?.jsonObject
            ?.get("sessionId")
            ?.jsonPrimitive
            ?.content
            .orEmpty()

        val response = client.post("/api/v1/hotel-searches") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """
                {
                  "sessionId": "$sessionId",
                  "criteria": {
                    "checkInDate": "2026-07-01",
                    "checkOutDate": "2026-07-04",
                    "guests": {"adults": 2},
                    "rooms": 1
                  }
                }
                """.trimIndent(),
            )
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("VALIDATION_ERROR", body["code"]?.jsonPrimitive?.content)
        assertEquals(
            "criteria.destination",
            body["fields"]?.jsonArray?.first()?.jsonObject?.get("field")?.jsonPrimitive?.content,
        )
    }

    @Test
    fun acceptsVisibleRoomCountAssumptionWithoutAddingSilentDefault() = testApplication {
        application {
            module()
        }

        val sessionResponse = client.post("/api/v1/assistant/sessions")
        val sessionBody = Json.parseToJsonElement(sessionResponse.bodyAsText()).jsonObject
        val sessionId = sessionBody["session"]
            ?.jsonObject
            ?.get("sessionId")
            ?.jsonPrimitive
            ?.content
            .orEmpty()

        val response = client.post("/api/v1/hotel-searches") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """
                {
                  "sessionId": "$sessionId",
                  "criteria": {
                    "destination": "Rome",
                    "checkInDate": "2026-07-01",
                    "checkOutDate": "2026-07-04",
                    "guests": {"adults": 2},
                    "derivedAssumptions": [
                      {
                        "category": "room_count",
                        "field": "rooms",
                        "value": 1,
                        "reason": "One room is sufficient for two adults."
                      }
                    ]
                  }
                }
                """.trimIndent(),
            )
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val criteria = body["criteria"]?.jsonObject

        assertEquals(HttpStatusCode.Accepted, response.status)
        assertEquals(false, criteria?.containsKey("rooms"))
    }

    @Test
    fun returnsStructuredNotFoundForUnknownAssistantSession() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/v1/hotel-searches") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(validSearchBody("assistant-session-local-unknown"))
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
    fun returnsStructuredNotFoundForUnknownHotelSearch() = testApplication {
        application {
            module()
        }

        val response = client.get("/api/v1/hotel-searches/hotel-search-local-unknown/offers")
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("HOTEL_SEARCH_NOT_FOUND", body["code"]?.jsonPrimitive?.content)
        assertEquals("Hotel search was not found.", body["message"]?.jsonPrimitive?.content)
        assertEquals(
            "hotel-search-local-unknown",
            body["details"]?.jsonObject?.get("searchId")?.jsonPrimitive?.content,
        )
    }

    @Test
    fun `returns one typed refinement suggestion for a completed empty search`() =
        testApplication {
            val boundary = ConfigurableHotelSearchBoundary().apply {
                storedSearch = emptySearchWithPreferences()
            }
            application {
                configureSerialization()
                configureErrorHandling()
                routing {
                    route("/api/v1") {
                        hotelSearchRoutes(boundary)
                    }
                }
            }

            val response = client.get(
                "/api/v1/hotel-searches/hotel-search-local-empty/offers",
            )
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val suggestion = body.getValue("refinementSuggestion").jsonObject

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("completed_no_offers", body.getValue("status").jsonPrimitive.content)
            assertTrue(body.getValue("offers").jsonArray.isEmpty())
            assertEquals("relax_preference", suggestion.getValue("type").jsonPrimitive.content)
            assertEquals(
                "minimumGuestRating",
                suggestion.getValue("preference").jsonPrimitive.content,
            )
            assertTrue(
                suggestion.getValue("message").jsonPrimitive.content.contains(
                    "подтвердить новый поиск",
                ),
            )
            assertEquals(0, boundary.createSearchCalls)
        }

    @Test
    fun mapsTypedNotCreatedOutcomesToExistingSafeSchemas() = testApplication {
        val boundary = ConfigurableHotelSearchBoundary()
        application {
            configureSerialization()
            configureErrorHandling()
            routing {
                route("/api/v1") {
                    hotelSearchRoutes(boundary)
                }
            }
        }

        val locationOutcomes = listOf(
            HotelOfferProviderResult.LocationNotFound,
            HotelOfferProviderResult.LocationSelectionRequired(
                suggestions = listOf(
                    HotelLocationSuggestion(
                        name = "Rome",
                        signature = "City, Italy",
                        typeCode = "city",
                        typeName = "City",
                    ),
                ),
            ),
        )
        locationOutcomes.forEach { outcome ->
            boundary.nextResult = CreateHotelSearchResult.NotCreated(outcome)

            val response = client.post("/api/v1/hotel-searches") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(validSearchBody("assistant-session-local-test"))
            }
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("VALIDATION_ERROR", body["code"]?.jsonPrimitive?.content)
            assertEquals(
                "criteria.destination",
                body["fields"]?.jsonArray?.first()?.jsonObject?.get("field")?.jsonPrimitive?.content,
            )
            assertEquals(false, body.containsKey("searchId"))
            assertEquals(false, body.toString().contains("City, Italy"))
        }

        boundary.nextResult = CreateHotelSearchResult.NotCreated(
            HotelOfferProviderResult.RequestRejected(
                HotelOfferProviderResult.RequestRejectionReason.INVALID_OCCUPANCY,
            ),
        )
        val rejectedResponse = client.post("/api/v1/hotel-searches") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(validSearchBody("assistant-session-local-test"))
        }
        val rejectedBody = Json.parseToJsonElement(rejectedResponse.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.BadRequest, rejectedResponse.status)
        assertEquals("VALIDATION_ERROR", rejectedBody["code"]?.jsonPrimitive?.content)
        assertEquals(
            "criteria",
            rejectedBody["fields"]?.jsonArray?.first()?.jsonObject?.get("field")?.jsonPrimitive?.content,
        )
        assertEquals(false, rejectedBody.toString().contains("INVALID_OCCUPANCY"))

        listOf(
            HotelOfferProviderResult.ResponseRejected(
                HotelOfferProviderResult.ResponseRejectionReason.INVALID_PAYLOAD,
            ),
            HotelOfferProviderResult.ProviderUnavailable(
                HotelOfferProviderResult.UnavailableReason.UNAVAILABLE,
            ),
        ).forEach { outcome ->
            boundary.nextResult = CreateHotelSearchResult.NotCreated(outcome)

            val response = client.post("/api/v1/hotel-searches") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(validSearchBody("assistant-session-local-test"))
            }
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertEquals("INTERNAL_ERROR", body["code"]?.jsonPrimitive?.content)
            assertEquals("Hotel search could not be completed.", body["message"]?.jsonPrimitive?.content)
            assertEquals(false, body.containsKey("searchId"))
            assertEquals(false, body.toString().contains("INVALID_PAYLOAD"))
            assertEquals(false, body.toString().contains("UNAVAILABLE"))
        }
    }

    private fun validSearchBody(sessionId: String): String =
        """
        {
          "sessionId": "$sessionId",
          "criteria": {
            "destination": "Rome",
            "checkInDate": "2026-07-01",
            "checkOutDate": "2026-07-04",
            "guests": {
              "adults": 2,
              "children": 0
            },
            "rooms": 1
          }
        }
        """.trimIndent()

    private fun emptySearchWithPreferences(): HotelSearch =
        HotelSearch(
            id = HotelSearchId("hotel-search-local-empty"),
            sessionId = AssistantSessionId("assistant-session-local-test"),
            criteria = HotelSearchCriteria(
                destination = "Казань",
                checkInDate = LocalDate.parse("2026-08-10"),
                checkOutDate = LocalDate.parse("2026-08-14"),
                guests = HotelSearchCriteria.Guests(adults = 2),
                rooms = 1,
                preferences = HotelSearchPreferences(
                    maxTotalPrice = HotelSearchPreferences.MaxTotalPrice(
                        amount = BigDecimal("80000"),
                        currency = "RUB",
                    ),
                    stars = setOf(4, 5),
                    minimumGuestRating = HotelSearchPreferences.MinimumGuestRating.EIGHT,
                    freeCancellationRequired = true,
                ),
            ),
            status = HotelSearch.Status.COMPLETED_NO_OFFERS,
            offers = emptyList(),
        )

    private class ConfigurableHotelSearchBoundary : HotelSearchBoundary {
        var nextResult: CreateHotelSearchResult =
            CreateHotelSearchResult.NotCreated(HotelOfferProviderResult.LocationNotFound)
        var storedSearch: HotelSearch? = null
        var createSearchCalls: Int = 0

        override suspend fun createSearch(
            command: CreateHotelSearchCommand,
        ): CreateHotelSearchResult {
            createSearchCalls += 1
            return nextResult
        }

        override fun getSearch(searchId: HotelSearchId): HotelSearch =
            checkNotNull(storedSearch).also { search ->
                check(search.id == searchId)
            }
    }
}
