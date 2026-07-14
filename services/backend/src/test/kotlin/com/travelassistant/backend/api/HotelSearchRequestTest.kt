package com.travelassistant.backend.api

import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.serialization.json.Json

class HotelSearchRequestTest {

    @Test
    fun decodesTransitionalChildrenField() {
        val request = Json.decodeFromString<HotelSearchRequest>(
            """
            {
              "sessionId": "assistant-session-local-000123",
              "criteria": {
                "destination": "Rome",
                "checkInDate": "2026-07-01",
                "checkOutDate": "2026-07-04",
                "guests": {"adults": 2, "children": 0},
                "rooms": 1
              }
            }
            """.trimIndent(),
        )

        assertEquals(0, request.criteria?.guests?.children)
        assertEquals(null, request.criteria?.guests?.childrenAges)
    }

    @Test
    fun acceptsChildAgeBoundariesAndDerivesCount() {
        val result = request(
            guests = HotelSearchRequest.Guests(
                adults = 2,
                childrenAges = listOf(17, 0),
            ),
        ).validate()

        val valid = assertIs<HotelSearchRequest.ValidationResult.Valid>(result)
        assertEquals(
            HotelSearchCriteria.Guests(
                adults = 2,
                childrenAges = listOf(17, 0),
            ),
            valid.command.criteria.guests,
        )
        assertEquals(2, valid.command.criteria.guests.children)
    }

    @Test
    fun acceptsTransitionalZeroChildrenWithoutAges() {
        val result = request(
            guests = HotelSearchRequest.Guests(
                adults = 2,
                children = 0,
            ),
        ).validate()

        val valid = assertIs<HotelSearchRequest.ValidationResult.Valid>(result)
        assertEquals(emptyList(), valid.command.criteria.guests.childrenAges)
    }

    @Test
    fun rejectsPositiveChildrenCountWithoutAges() {
        val result = request(
            guests = HotelSearchRequest.Guests(
                adults = 2,
                children = 1,
            ),
        ).validate()

        assertInvalidField(result, "criteria.guests.childrenAges")
    }

    @Test
    fun rejectsMismatchedChildrenCountAndAges() {
        val result = request(
            guests = HotelSearchRequest.Guests(
                adults = 2,
                children = 2,
                childrenAges = listOf(7),
            ),
        ).validate()

        assertInvalidField(result, "criteria.guests.childrenAges")
    }

    @Test
    fun rejectsChildAgesOutsideInclusiveRange() {
        listOf(-1, 18).forEach { invalidAge ->
            val result = request(
                guests = HotelSearchRequest.Guests(
                    adults = 2,
                    childrenAges = listOf(invalidAge),
                ),
            ).validate()

            assertInvalidField(result, "criteria.guests.childrenAges")
        }
    }

    private fun request(guests: HotelSearchRequest.Guests): HotelSearchRequest =
        HotelSearchRequest(
            sessionId = "assistant-session-local-000123",
            criteria = HotelSearchRequest.Criteria(
                destination = "Rome",
                checkInDate = "2026-07-01",
                checkOutDate = "2026-07-04",
                guests = guests,
                rooms = 1,
            ),
        )

    private fun assertInvalidField(
        result: HotelSearchRequest.ValidationResult,
        field: String,
    ) {
        val invalid = assertIs<HotelSearchRequest.ValidationResult.Invalid>(result)
        assertEquals(field, invalid.field)
    }
}
