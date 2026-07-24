package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.hotel.HotelOffer
import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import com.travelassistant.backend.domain.hotel.HotelSearchId
import com.travelassistant.backend.domain.hotel.HotelSearchPreferences
import com.travelassistant.backend.domain.hotel.RankedHotelOffer
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PlanHotelNoOffersRefinementUseCaseTest {

    private val useCase = PlanHotelNoOffersRefinementUseCase()

    @Test
    fun `does not suggest refinement for a search with offers`() {
        val result = useCase(
            search(
                status = HotelSearch.Status.COMPLETED_WITH_OFFERS,
                offers = listOf(rankedOffer()),
                preferences = allPreferences(),
            ),
        )

        assertEquals(HotelNoOffersRefinementPlan.NotApplicable, result)
    }

    @Test
    fun `does not suggest refinement when no active preference can be relaxed`() {
        val result = useCase(search())

        assertEquals(HotelNoOffersRefinementPlan.NotApplicable, result)
    }

    @Test
    fun `suggests one active preference in deterministic safety order`() {
        assertSuggestion(
            preferences = allPreferences(),
            expected = HotelNoOffersRefinementPlan.Preference.MINIMUM_GUEST_RATING,
        )
        assertSuggestion(
            preferences = allPreferences().copy(minimumGuestRating = null),
            expected = HotelNoOffersRefinementPlan.Preference.STARS,
        )
        assertSuggestion(
            preferences = allPreferences().copy(
                minimumGuestRating = null,
                stars = emptySet(),
            ),
            expected = HotelNoOffersRefinementPlan.Preference.FREE_CANCELLATION_REQUIRED,
        )
        assertSuggestion(
            preferences = allPreferences().copy(
                minimumGuestRating = null,
                stars = emptySet(),
                freeCancellationRequired = false,
            ),
            expected = HotelNoOffersRefinementPlan.Preference.BREAKFAST_INCLUDED_REQUIRED,
        )
        assertSuggestion(
            preferences = allPreferences().copy(
                minimumGuestRating = null,
                stars = emptySet(),
                freeCancellationRequired = false,
                breakfastIncludedRequired = false,
            ),
            expected = HotelNoOffersRefinementPlan.Preference.MAX_TOTAL_PRICE,
        )
    }

    @Test
    fun `planning does not mutate stored search criteria`() {
        val original = search(preferences = allPreferences())
        val criteriaBefore = original.criteria

        useCase(original)

        assertEquals(criteriaBefore, original.criteria)
    }

    private fun assertSuggestion(
        preferences: HotelSearchPreferences,
        expected: HotelNoOffersRefinementPlan.Preference,
    ) {
        val result = assertIs<HotelNoOffersRefinementPlan.Suggestion>(
            useCase(search(preferences = preferences)),
        )

        assertEquals(expected, result.preference)
        assertEquals(true, result.message.contains("подтвердить новый поиск"))
    }

    private fun allPreferences(): HotelSearchPreferences =
        HotelSearchPreferences(
            maxTotalPrice = HotelSearchPreferences.MaxTotalPrice(
                amount = BigDecimal("80000"),
                currency = "RUB",
            ),
            stars = linkedSetOf(4, 5),
            minimumGuestRating = HotelSearchPreferences.MinimumGuestRating.EIGHT,
            freeCancellationRequired = true,
            breakfastIncludedRequired = true,
        )

    private fun search(
        status: HotelSearch.Status = HotelSearch.Status.COMPLETED_NO_OFFERS,
        offers: List<RankedHotelOffer> = emptyList(),
        preferences: HotelSearchPreferences = HotelSearchPreferences(),
    ): HotelSearch =
        HotelSearch(
            id = HotelSearchId("hotel-search-test"),
            sessionId = AssistantSessionId("assistant-session-test"),
            criteria = HotelSearchCriteria(
                destination = "Казань",
                checkInDate = LocalDate.parse("2026-08-10"),
                checkOutDate = LocalDate.parse("2026-08-14"),
                guests = HotelSearchCriteria.Guests(adults = 2),
                rooms = 1,
                preferences = preferences,
            ),
            status = status,
            offers = offers,
        )

    private fun rankedOffer(): RankedHotelOffer =
        RankedHotelOffer(
            offer = HotelOffer(
                id = "offer-1",
                providerReference = "provider-hotel-1",
                hotelName = "Отель",
                city = "Казань",
                country = "Россия",
                totalPrice = 42000.0,
                currency = "RUB",
                rating = 8.5,
                reviewCount = 100,
                amenities = null,
                availability = HotelOffer.Availability.AVAILABLE,
                source = "test",
                freshness = HotelOffer.Freshness.FRESH,
            ),
            matchSummary = "Доступно.",
        )
}
