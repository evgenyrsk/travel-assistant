package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HotelsApiHotelRatesRequestMapperTest {
    @Test
    fun `maps one room and keeps child ages in wire order`() {
        val result = assertIs<HotelsApiHotelRatesRequestMapper.Result.Mapped>(
            HotelsApiHotelRatesRequestMapper.map(criteria(childrenAges = listOf(0, 17))),
        )

        assertEquals("2026-08-01", result.request.checkinDate)
        assertEquals("2026-08-08", result.request.checkoutDate)
        assertEquals(2, result.request.guests.single().adultsCount)
        assertEquals(listOf(0, 17), result.request.guests.single().childrenAge)
        assertEquals(emptyList(), result.request.filters)
    }

    @Test
    fun `rejects unsupported room count and child age`() {
        val rooms = assertIs<HotelsApiHotelRatesRequestMapper.Result.Rejected>(
            HotelsApiHotelRatesRequestMapper.map(criteria(rooms = 2)),
        )
        val child = assertIs<HotelsApiHotelRatesRequestMapper.Result.Rejected>(
            HotelsApiHotelRatesRequestMapper.map(criteria(childrenAges = listOf(18))),
        )

        assertEquals(HotelsApiSearchMappingError.Issue.INVALID_ROOM_COUNT, rooms.error.issue)
        assertEquals(HotelsApiSearchMappingError.Issue.INVALID_CHILD_AGE, child.error.issue)
    }

    private fun criteria(
        rooms: Int? = 1,
        childrenAges: List<Int> = emptyList(),
    ): HotelSearchCriteria =
        HotelSearchCriteria(
            destination = "Cosmos ВДНХ",
            checkInDate = LocalDate.parse("2026-08-01"),
            checkOutDate = LocalDate.parse("2026-08-08"),
            guests = HotelSearchCriteria.Guests(
                adults = 2,
                childrenAges = childrenAges,
            ),
            rooms = rooms,
        )
}
