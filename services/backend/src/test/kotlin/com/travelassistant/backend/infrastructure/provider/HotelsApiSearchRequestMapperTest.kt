package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.HotelLocationResolution
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import com.travelassistant.backend.domain.hotel.HotelSearchPreferences
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        assertEquals(emptyList(), result.request.filters)
        assertNull(result.request.offset)
        assertNull(result.request.limit)

        val body = HotelsApiJson.codec.parseToJsonElement(
            HotelsApiJson.codec.encodeToString(result.request),
        ).jsonObject
        assertFalse("filters" in body)
        assertFalse("sort" in body)
    }

    @Test
    fun `maps supported preferences to exact provider filters in deterministic order`() {
        val preferences = HotelSearchPreferences(
            maxTotalPrice = HotelSearchPreferences.MaxTotalPrice(
                amount = BigDecimal("80000.50"),
                currency = "RUB",
            ),
            stars = linkedSetOf(5, 4),
            minimumGuestRating = HotelSearchPreferences.MinimumGuestRating.EIGHT,
            freeCancellationRequired = true,
            breakfastIncludedRequired = true,
        )

        val mapped = assertIs<HotelsApiSearchRequestMapper.Result.Mapped>(
            HotelsApiSearchRequestMapper.map(
                location = location(),
                criteria = criteria(preferences = preferences),
            ),
        )
        val body = HotelsApiJson.codec.parseToJsonElement(
            HotelsApiJson.codec.encodeToString(mapped.request),
        ).jsonObject
        val filters = body.getValue("filters").jsonArray.map { it.jsonObject }

        assertEquals(
            listOf(
                "price",
                "stars",
                "review_rating",
                "free_cancellation_allowed",
                "meal_types",
            ),
            filters.map { it.getValue("filterId").jsonPrimitive.content },
        )
        assertEquals("range", filters[0].getValue("\$objectType").jsonPrimitive.content)
        assertEquals("0", filters[0].getValue("min").jsonPrimitive.content)
        assertEquals("80000.50", filters[0].getValue("max").jsonPrimitive.content)
        assertFalse(filters[0].getValue("max").jsonPrimitive.isString)
        assertEquals("array", filters[1].getValue("\$objectType").jsonPrimitive.content)
        assertEquals(
            listOf("4", "5"),
            filters[1].getValue("values").jsonArray.map { it.jsonPrimitive.content },
        )
        assertEquals("radio", filters[2].getValue("\$objectType").jsonPrimitive.content)
        assertEquals("8", filters[2].getValue("value").jsonPrimitive.content)
        assertEquals("boolean", filters[3].getValue("\$objectType").jsonPrimitive.content)
        assertEquals("true", filters[3].getValue("value").jsonPrimitive.content)
        assertEquals("array", filters[4].getValue("\$objectType").jsonPrimitive.content)
        assertEquals(
            listOf("breakfast"),
            filters[4].getValue("values").jsonArray.map { it.jsonPrimitive.content },
        )
        assertFalse("sort" in body)
    }

    @Test
    fun `rejects invalid provider preference values without producing a request`() {
        val invalidPreferences = listOf(
            HotelSearchPreferences(
                maxTotalPrice = HotelSearchPreferences.MaxTotalPrice(
                    amount = BigDecimal.ZERO,
                    currency = "RUB",
                ),
            ) to HotelsApiSearchMappingError.Issue.INVALID_MAX_TOTAL_PRICE,
            HotelSearchPreferences(
                maxTotalPrice = HotelSearchPreferences.MaxTotalPrice(
                    amount = BigDecimal("80000"),
                    currency = "USD",
                ),
            ) to HotelsApiSearchMappingError.Issue.UNSUPPORTED_MAX_TOTAL_PRICE_CURRENCY,
            HotelSearchPreferences(stars = setOf(6)) to
                HotelsApiSearchMappingError.Issue.INVALID_STARS,
        )

        invalidPreferences.forEach { (preferences, expectedIssue) ->
            val result = assertIs<HotelsApiSearchRequestMapper.Result.Rejected>(
                HotelsApiSearchRequestMapper.map(
                    location = location(),
                    criteria = criteria(preferences = preferences),
                ),
            )

            assertEquals(expectedIssue, result.error.issue)
        }
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
        preferences: HotelSearchPreferences = HotelSearchPreferences(),
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
            preferences = preferences,
        )
}
