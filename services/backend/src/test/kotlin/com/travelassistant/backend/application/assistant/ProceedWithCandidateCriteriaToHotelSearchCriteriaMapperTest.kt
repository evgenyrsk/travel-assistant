package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import com.travelassistant.backend.domain.hotel.HotelSearchPreferences
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ProceedWithCandidateCriteriaToHotelSearchCriteriaMapperTest {

    private val mapper = ProceedWithCandidateCriteriaToHotelSearchCriteriaMapper()

    @Test
    fun mapsAllRequiredFieldsToHotelSearchCriteria() {
        val result = mapper(completeCriteria())

        assertEquals(
            HotelSearchCriteria(
                destination = "Rome",
                checkInDate = LocalDate.parse("2026-07-01"),
                checkOutDate = LocalDate.parse("2026-07-04"),
                guests = HotelSearchCriteria.Guests(
                    adults = 2,
                    childrenAges = listOf(7),
                ),
                rooms = 1,
            ),
            result,
        )
    }

    @Test
    fun preservesDatesWithoutNormalization() {
        val result = mapper(
            completeCriteria(
                checkInDate = LocalDate.parse("2026-12-30"),
                checkOutDate = LocalDate.parse("2027-01-03"),
            ),
        )

        assertEquals(LocalDate.parse("2026-12-30"), result.checkInDate)
        assertEquals(LocalDate.parse("2027-01-03"), result.checkOutDate)
    }

    @Test
    fun mapsGuestsAndRoomsWithoutSilentDefaults() {
        val result = mapper(
            completeCriteria(
                guests = ProceedWithCandidateCriteria.Guests(
                    adults = 3,
                    childrenAges = emptyList(),
                ),
                rooms = 2,
            ),
        )

        assertEquals(3, result.guests.adults)
        assertEquals(0, result.guests.children)
        assertEquals(emptyList(), result.guests.childrenAges)
        assertEquals(2, result.rooms)
    }

    @Test
    fun mapsAcceptedDestinationAsTypedCriteriaValue() {
        val result = mapper(
            completeCriteria(
                destination = "Rome Centro",
            ),
        )

        assertEquals("Rome Centro", result.destination)
    }

    @Test
    fun preservesProviderNeutralPreferencesWithoutProviderMapping() {
        val preferences = HotelSearchPreferences(
            stars = setOf(4, 5),
            minimumGuestRating = HotelSearchPreferences.MinimumGuestRating.EIGHT,
            freeCancellationRequired = true,
        )

        val result = mapper(completeCriteria(preferences = preferences))

        assertEquals(preferences, result.preferences)
    }

    @Test
    fun remainsDeterministicForSameCriteria() {
        val criteria = completeCriteria()

        val firstResult = mapper(criteria)
        val secondResult = mapper(criteria)

        assertEquals(firstResult, secondResult)
    }

    @Test
    fun returnsCriteriaOnlyWithoutSearchSideEffects() {
        val result = mapper(completeCriteria())
        val resultText = result.toString()

        listOf(
            "hotelSearchId",
            "show_hotel_results",
            "Hotel search created",
            "LlmCandidate",
            "candidatePayload",
            "modelResponse",
        ).forEach { forbidden ->
            assertFalse(
                resultText.contains(forbidden),
                "Mapped hotel criteria must not expose $forbidden",
            )
        }
    }

    private fun completeCriteria(
        destination: String = "Rome",
        checkInDate: LocalDate = LocalDate.parse("2026-07-01"),
        checkOutDate: LocalDate = LocalDate.parse("2026-07-04"),
        guests: ProceedWithCandidateCriteria.Guests = ProceedWithCandidateCriteria.Guests(
            adults = 2,
            childrenAges = listOf(7),
        ),
        rooms: Int = 1,
        preferences: HotelSearchPreferences = HotelSearchPreferences(),
    ): ProceedWithCandidateCriteria =
        ProceedWithCandidateCriteria(
            destination = destination,
            checkInDate = checkInDate,
            checkOutDate = checkOutDate,
            guests = guests,
            rooms = rooms,
            preferences = preferences,
        )
}
