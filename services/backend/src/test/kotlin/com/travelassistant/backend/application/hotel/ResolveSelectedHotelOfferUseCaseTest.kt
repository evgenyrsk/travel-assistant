package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.hotel.HotelOffer
import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import com.travelassistant.backend.domain.hotel.HotelSearchId
import com.travelassistant.backend.domain.hotel.RankedHotelOffer
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ResolveSelectedHotelOfferUseCaseTest {

    @Test
    fun `resolves only an offer stored under the requested search`() {
        val store = InMemoryHotelSearchStateStore()
        val expected = offer(
            id = "hotel-offer-local-000001",
            providerReference = "provider-hotel-secret",
        )
        store.save(search("hotel-search-1", listOf(expected)))
        store.save(
            search(
                "hotel-search-2",
                listOf(offer("hotel-offer-local-000002", "other-provider-hotel")),
            ),
        )
        val useCase = ResolveSelectedHotelOfferUseCase(store)

        val result = assertIs<ResolveSelectedHotelOfferResult.Resolved>(
            useCase(
                ResolveSelectedHotelOfferRequest(
                    searchId = HotelSearchId("hotel-search-1"),
                    offerId = "hotel-offer-local-000001",
                ),
            ),
        )

        assertEquals(expected, result.offer)
        assertEquals("provider-hotel-secret", result.offer.providerReference)
    }

    @Test
    fun `distinguishes an unknown search from an unknown offer`() {
        val store = InMemoryHotelSearchStateStore()
        store.save(
            search(
                "hotel-search-1",
                listOf(offer("hotel-offer-local-000001", "provider-hotel-secret")),
            ),
        )
        val useCase = ResolveSelectedHotelOfferUseCase(store)

        assertIs<ResolveSelectedHotelOfferResult.SearchNotFound>(
            useCase(
                ResolveSelectedHotelOfferRequest(
                    searchId = HotelSearchId("hotel-search-missing"),
                    offerId = "hotel-offer-local-000001",
                ),
            ),
        )
        assertIs<ResolveSelectedHotelOfferResult.OfferNotFound>(
            useCase(
                ResolveSelectedHotelOfferRequest(
                    searchId = HotelSearchId("hotel-search-1"),
                    offerId = "hotel-offer-local-missing",
                ),
            ),
        )
    }

    @Test
    fun `does not resolve an offer that belongs to another search`() {
        val store = InMemoryHotelSearchStateStore()
        store.save(
            search(
                "hotel-search-1",
                listOf(offer("hotel-offer-local-000001", "provider-hotel-1")),
            ),
        )
        store.save(
            search(
                "hotel-search-2",
                listOf(offer("hotel-offer-local-000002", "provider-hotel-2")),
            ),
        )
        val useCase = ResolveSelectedHotelOfferUseCase(store)

        assertIs<ResolveSelectedHotelOfferResult.OfferNotFound>(
            useCase(
                ResolveSelectedHotelOfferRequest(
                    searchId = HotelSearchId("hotel-search-1"),
                    offerId = "hotel-offer-local-000002",
                ),
            ),
        )
    }

    private fun search(
        id: String,
        offers: List<HotelOffer>,
    ): HotelSearch =
        HotelSearch(
            id = HotelSearchId(id),
            sessionId = AssistantSessionId("assistant-session-test"),
            criteria = HotelSearchCriteria(
                destination = "Казань",
                checkInDate = LocalDate.parse("2026-08-10"),
                checkOutDate = LocalDate.parse("2026-08-14"),
                guests = HotelSearchCriteria.Guests(adults = 2),
                rooms = 1,
            ),
            status = HotelSearch.Status.COMPLETED_WITH_OFFERS,
            offers = offers.map { offer ->
                RankedHotelOffer(offer = offer, matchSummary = "Тестовое соответствие")
            },
        )

    private fun offer(
        id: String,
        providerReference: String,
    ): HotelOffer =
        HotelOffer(
            id = id,
            providerReference = providerReference,
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
        )
}
