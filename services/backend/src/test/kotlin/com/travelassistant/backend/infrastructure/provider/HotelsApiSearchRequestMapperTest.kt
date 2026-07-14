package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.HotelLocationResolution
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class HotelsApiSearchRequestMapperTest {

    @Test
    fun `maps explicit location candidate and one guest group using date-only format`() {
        val criteria = criteria(
            destination = "hotel-master-reference-must-not-be-used",
            childrenAges = listOf(17, 0),
        )

        val result = assertIs<HotelsApiSearchRequestMapper.Result.Mapped>(
            HotelsApiSearchRequestMapper.map(location(destinationId = 77), criteria),
        )

        assertEquals(77, result.request.destinationId)
        assertEquals("2026-07-18", result.request.checkinDate)
        assertEquals("2026-07-19", result.request.checkoutDate)
        assertEquals(1, result.request.guests.size)
        assertEquals(2, result.request.guests.single().adultsCount)
        assertEquals(listOf(17, 0), result.request.guests.single().childrenAge)
        assertNull(result.request.offset)
        assertNull(result.request.limit)
    }

    @Test
    fun `rejects room counts other than exactly one`() {
        listOf(null, 2).forEach { rooms ->
            val result = assertIs<HotelsApiSearchRequestMapper.Result.Rejected>(
                HotelsApiSearchRequestMapper.map(location(), criteria(rooms = rooms)),
            )

            assertEquals(
                HotelsApiSearchMappingError.Issue.INVALID_ROOM_COUNT,
                result.error.issue,
            )
        }
    }

    @Test
    fun `accepts child age boundaries and rejects values outside them`() {
        val accepted = assertIs<HotelsApiSearchRequestMapper.Result.Mapped>(
            HotelsApiSearchRequestMapper.map(
                location(),
                criteria(childrenAges = listOf(0, 17)),
            ),
        )
        assertEquals(listOf(0, 17), accepted.request.guests.single().childrenAge)

        listOf(-1, 18).forEach { invalidAge ->
            val rejected = assertIs<HotelsApiSearchRequestMapper.Result.Rejected>(
                HotelsApiSearchRequestMapper.map(
                    location(),
                    criteria(childrenAges = listOf(invalidAge)),
                ),
            )
            assertEquals(
                HotelsApiSearchMappingError.Issue.INVALID_CHILD_AGE,
                rejected.error.issue,
            )
        }
    }

    @Test
    fun `mapping is deterministic and never adds pagination`() {
        val criteria = criteria(childrenAges = listOf(7))

        val first = HotelsApiSearchRequestMapper.map(location(), criteria)
        val second = HotelsApiSearchRequestMapper.map(location(), criteria)

        assertEquals(first, second)
    }

    private fun location(destinationId: Int = 77): HotelLocationResolution.Candidate =
        HotelLocationResolution.Candidate(
            destinationId = destinationId,
            name = "Казань",
            signature = "Казань, Россия",
            type = HotelLocationResolution.Type(
                code = "city",
                name = "Город",
            ),
        )

    private fun criteria(
        destination: String = "Казань",
        childrenAges: List<Int> = emptyList(),
        rooms: Int? = 1,
    ): HotelSearchCriteria =
        HotelSearchCriteria(
            destination = destination,
            checkInDate = LocalDate.parse("2026-07-18"),
            checkOutDate = LocalDate.parse("2026-07-19"),
            guests = HotelSearchCriteria.Guests(
                adults = 2,
                childrenAges = childrenAges,
            ),
            rooms = rooms,
        )
}
