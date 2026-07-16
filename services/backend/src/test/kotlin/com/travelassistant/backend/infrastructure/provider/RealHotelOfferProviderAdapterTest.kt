package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.HotelOfferProviderResult
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RealHotelOfferProviderAdapterTest {

    @Test
    fun returnsTypedUnavailableWithoutExternalCalls() = runBlocking {
        val adapter = RealHotelOfferProviderAdapter()
        val criteria = HotelSearchCriteria(
            destination = "Rome",
            checkInDate = LocalDate.parse("2026-07-01"),
            checkOutDate = LocalDate.parse("2026-07-04"),
            guests = HotelSearchCriteria.Guests(adults = 2),
            rooms = 1,
        )

        val result = adapter.search(criteria)

        val unavailable = assertIs<HotelOfferProviderResult.ProviderUnavailable>(result)
        assertEquals(
            HotelOfferProviderResult.UnavailableReason.UNAVAILABLE,
            unavailable.reason,
        )
    }

    @Test
    fun returnsSameTypedUnavailableForAnyDestination() = runBlocking {
        val adapter = RealHotelOfferProviderAdapter()
        val criteria = HotelSearchCriteria(
            destination = "Paris",
            checkInDate = LocalDate.parse("2026-08-01"),
            checkOutDate = LocalDate.parse("2026-08-05"),
            guests = HotelSearchCriteria.Guests(adults = 1, childrenAges = listOf(7)),
            rooms = 1,
        )

        assertEquals(
            HotelOfferProviderResult.ProviderUnavailable(
                HotelOfferProviderResult.UnavailableReason.UNAVAILABLE,
            ),
            adapter.search(criteria),
        )
    }
}
