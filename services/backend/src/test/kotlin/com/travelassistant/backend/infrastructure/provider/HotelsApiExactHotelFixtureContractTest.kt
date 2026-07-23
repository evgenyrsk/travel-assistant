package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import com.travelassistant.backend.domain.hotel.HotelSearchPreferences
import java.time.LocalDate
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HotelsApiExactHotelFixtureContractTest {
    @Test
    fun `provider fixtures keep hotel identity separate and map breakfast rate`() {
        val autocomplete = HotelsApiJson.codec.decodeFromString<
            HotelsApiAutocompleteResponseDto,
        >(fixture("stage-14-7/autocomplete-exact-hotel.json"))
        val resolution = HotelsApiAutocompleteLocationMapper.map(autocomplete)

        assertTrue(resolution.candidates.isEmpty())
        assertEquals("hotel-example-001", resolution.hotelCandidates.single().providerReference)

        val details = HotelsApiJson.codec.decodeFromString<HotelsApiHotelDetailsResponseDto>(
            fixture("stage-13-1/hotel-details-success.json"),
        )
        val rates = HotelsApiJson.codec.decodeFromString<HotelsApiHotelRatesResponseDto>(
            fixture("stage-14-7/hotel-rates-success.json"),
        )
        val mapped = assertIs<HotelsApiExactHotelResponseMapper.Result.Mapped>(
            HotelsApiExactHotelResponseMapper.map(
                candidate = resolution.hotelCandidates.single(),
                criteria = criteria(),
                details = details,
                rates = rates,
            ),
        )

        val offer = mapped.offers.single()
        assertEquals("hotel-example-001", offer.providerReference)
        assertEquals(42_000.0, offer.totalPrice)
        assertEquals("RUB", offer.currency)
        assertEquals(true, offer.breakfastIncluded)
        assertNull(offer.rating)
        assertNull(offer.reviewCount)
    }

    private fun criteria(): HotelSearchCriteria =
        HotelSearchCriteria(
            destination = "Отель Пример",
            checkInDate = LocalDate.parse("2026-08-01"),
            checkOutDate = LocalDate.parse("2026-08-08"),
            guests = HotelSearchCriteria.Guests(adults = 2),
            rooms = 1,
            preferences = HotelSearchPreferences(
                breakfastIncludedRequired = true,
            ),
        )

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader.getResource("fixtures/hotels-api/$name"))
            .readText()
}
