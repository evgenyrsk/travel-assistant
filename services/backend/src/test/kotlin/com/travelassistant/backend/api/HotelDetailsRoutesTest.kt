package com.travelassistant.backend.api

import com.travelassistant.backend.application.hotel.HotelDetailsProviderBoundary
import com.travelassistant.backend.application.hotel.HotelDetailsProviderResult
import com.travelassistant.backend.application.hotel.InMemoryHotelSearchStateStore
import com.travelassistant.backend.application.hotel.LoadSelectedHotelDetailsUseCase
import com.travelassistant.backend.application.hotel.ResolveSelectedHotelOfferUseCase
import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventName
import com.travelassistant.backend.application.observability.OperationalEventSink
import com.travelassistant.backend.application.observability.OperationalOutcome
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.hotel.HotelDetails
import com.travelassistant.backend.domain.hotel.HotelOffer
import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import com.travelassistant.backend.domain.hotel.HotelSearchId
import com.travelassistant.backend.domain.hotel.RankedHotelOffer
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class HotelDetailsRoutesTest {

    @Test
    fun `records bounded details and provider outcomes without identity leakage`() =
        testApplication {
            val events = mutableListOf<OperationalEvent>()
            application {
                detailsTestModule(
                    store = storeWithSearch(),
                    provider = HotelDetailsProviderBoundary {
                        HotelDetailsProviderResult.ProviderUnavailable(
                            HotelDetailsProviderResult.UnavailableReason.TIMEOUT,
                        )
                    },
                    eventSink = OperationalEventSink(events::add),
                )
            }

            assertEquals(HttpStatusCode.ServiceUnavailable, client.get(detailsPath()).status)

            assertEquals(
                OperationalOutcome.TIMEOUT,
                events.single {
                    it.name == OperationalEventName.DEPENDENCY_CALL_COMPLETED
                }.outcome,
            )
            assertEquals(
                OperationalOutcome.TIMEOUT,
                events.single {
                    it.name == OperationalEventName.HOTEL_DETAILS_COMPLETED
                }.outcome,
            )
            assertEquals(setOf(SEARCH_ID), events.mapNotNull { it.hotelSearchId }.toSet())
            assertFalse(events.toString().contains(OFFER_ID))
            assertFalse(events.toString().contains(PROVIDER_REFERENCE))
        }

    @Test
    fun `loads details only after search-bound offer selection without identity leakage`() =
        testApplication {
            val store = storeWithSearch()
            var capturedProviderReference: String? = null
            var providerCallCount = 0
            application {
                detailsTestModule(
                    store = store,
                    provider = HotelDetailsProviderBoundary { providerReference ->
                        providerCallCount++
                        capturedProviderReference = providerReference
                        HotelDetailsProviderResult.Loaded(
                            HotelDetails(
                                hotelName = "Отель Пример",
                                starRating = 4,
                                imageUrls = listOf("https://example.invalid/hotel.jpg"),
                            ),
                        )
                    },
                )
            }

            assertEquals(0, providerCallCount)
            val response = client.get(detailsPath())
            val bodyText = response.bodyAsText()
            val body = Json.parseToJsonElement(bodyText).jsonObject

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(1, providerCallCount)
            assertEquals(PROVIDER_REFERENCE, capturedProviderReference)
            assertEquals("Отель Пример", body.getValue("hotelName").jsonPrimitive.content)
            assertFalse(bodyText.contains(PROVIDER_REFERENCE))
            assertFalse(bodyText.contains("providerReference"))
            assertFalse(bodyText.contains("hotelId"))
        }

    @Test
    fun `returns distinct safe not-found outcomes without unnecessary provider calls`() {
        val cases = listOf(
            "/api/v1/hotel-searches/hotel-search-missing/offers/$OFFER_ID/details" to
                "HOTEL_SEARCH_NOT_FOUND",
            "/api/v1/hotel-searches/$SEARCH_ID/offers/hotel-offer-missing/details" to
                "HOTEL_OFFER_NOT_FOUND",
        )

        cases.forEach { (path, expectedCode) ->
            testApplication {
                var providerCallCount = 0
                application {
                    detailsTestModule(
                        store = storeWithSearch(),
                        provider = HotelDetailsProviderBoundary {
                            providerCallCount++
                            error("Provider must not be called")
                        },
                    )
                }

                val response = client.get(path)
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

                assertEquals(HttpStatusCode.NotFound, response.status)
                assertEquals(expectedCode, body.getValue("code").jsonPrimitive.content)
                assertEquals(0, providerCallCount)
            }
        }
    }

    @Test
    fun `maps provider details not found to safe 404`() = testApplication {
        application {
            detailsTestModule(
                store = storeWithSearch(),
                provider = HotelDetailsProviderBoundary {
                    HotelDetailsProviderResult.NotFound
                },
            )
        }

        val response = client.get(detailsPath())
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("HOTEL_DETAILS_NOT_FOUND", body.getValue("code").jsonPrimitive.content)
    }

    @Test
    fun `maps rejected response to safe 502 without internal reason`() = testApplication {
        application {
            detailsTestModule(
                store = storeWithSearch(),
                provider = HotelDetailsProviderBoundary {
                    HotelDetailsProviderResult.ResponseRejected(
                        HotelDetailsProviderResult.ResponseRejectionReason.INVALID_PAYLOAD,
                    )
                },
            )
        }

        val response = client.get(detailsPath())
        val bodyText = response.bodyAsText()
        val body = Json.parseToJsonElement(bodyText).jsonObject

        assertEquals(HttpStatusCode.BadGateway, response.status)
        assertEquals("PROVIDER_RESPONSE_INVALID", body.getValue("code").jsonPrimitive.content)
        assertFalse(bodyText.contains("INVALID_PAYLOAD"))
        assertFalse(bodyText.contains(PROVIDER_REFERENCE))
    }

    @Test
    fun `maps unavailable provider to safe 503 without internal reason`() = testApplication {
        application {
            detailsTestModule(
                store = storeWithSearch(),
                provider = HotelDetailsProviderBoundary {
                    HotelDetailsProviderResult.ProviderUnavailable(
                        HotelDetailsProviderResult.UnavailableReason.TIMEOUT,
                    )
                },
            )
        }

        val response = client.get(detailsPath())
        val bodyText = response.bodyAsText()
        val body = Json.parseToJsonElement(bodyText).jsonObject

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertEquals("PROVIDER_UNAVAILABLE", body.getValue("code").jsonPrimitive.content)
        assertFalse(bodyText.contains("TIMEOUT"))
        assertFalse(bodyText.contains(PROVIDER_REFERENCE))
    }

    private fun io.ktor.server.application.Application.detailsTestModule(
        store: InMemoryHotelSearchStateStore,
        provider: HotelDetailsProviderBoundary,
        eventSink: OperationalEventSink = OperationalEventSink.NONE,
    ) {
        configureSerialization()
        configureErrorHandling()
        routing {
            route("/api/v1") {
                hotelDetailsRoutes(
                    LoadSelectedHotelDetailsUseCase(
                        resolveSelectedOffer = ResolveSelectedHotelOfferUseCase(store),
                        hotelDetailsProvider = provider,
                        eventSink = eventSink,
                    ),
                )
            }
        }
    }

    private fun storeWithSearch(): InMemoryHotelSearchStateStore =
        InMemoryHotelSearchStateStore().apply {
            save(
                HotelSearch(
                    id = HotelSearchId(SEARCH_ID),
                    sessionId = AssistantSessionId("assistant-session-test"),
                    criteria = HotelSearchCriteria(
                        destination = "Казань",
                        checkInDate = LocalDate.parse("2026-08-10"),
                        checkOutDate = LocalDate.parse("2026-08-14"),
                        guests = HotelSearchCriteria.Guests(adults = 2),
                        rooms = 1,
                    ),
                    status = HotelSearch.Status.COMPLETED_WITH_OFFERS,
                    offers = listOf(
                        RankedHotelOffer(
                            offer = HotelOffer(
                                id = OFFER_ID,
                                providerReference = PROVIDER_REFERENCE,
                                hotelName = "Тестовый отель",
                                city = "Казань",
                                country = "Россия",
                                totalPrice = 12_000.0,
                                currency = "RUB",
                                rating = 8.7,
                                reviewCount = 42,
                                amenities = null,
                                availability = HotelOffer.Availability.AVAILABLE,
                                source = "test",
                                freshness = HotelOffer.Freshness.UNKNOWN,
                            ),
                            matchSummary = "Тестовое соответствие",
                        ),
                    ),
                ),
            )
        }

    private fun detailsPath(): String =
        "/api/v1/hotel-searches/$SEARCH_ID/offers/$OFFER_ID/details"

    private companion object {
        const val SEARCH_ID = "hotel-search-test"
        const val OFFER_ID = "hotel-offer-test"
        const val PROVIDER_REFERENCE = "provider-hotel-secret"
    }
}
