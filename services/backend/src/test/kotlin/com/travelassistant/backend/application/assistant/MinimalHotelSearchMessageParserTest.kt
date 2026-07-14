package com.travelassistant.backend.application.assistant

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class MinimalHotelSearchMessageParserTest {

    private val parser = MinimalHotelSearchMessageParser()

    @Test
    fun parsesCompleteExplicitHotelSearchMessage() {
        val result = parser.parse(
            "hotel-search; destination=Rome; check-in=2026-07-01; " +
                "check-out=2026-07-04; adults=2; children=1; children-ages=7; rooms=1",
        ) as MinimalHotelSearchMessageParser.Result.Complete

        assertEquals("Rome", result.criteria.destination)
        assertEquals(LocalDate.parse("2026-07-01"), result.criteria.checkInDate)
        assertEquals(LocalDate.parse("2026-07-04"), result.criteria.checkOutDate)
        assertEquals(2, result.criteria.guests.adults)
        assertEquals(1, result.criteria.guests.children)
        assertEquals(listOf(7), result.criteria.guests.childrenAges)
        assertEquals(1, result.criteria.rooms)
    }

    @Test
    fun rejectsIncompleteExplicitHotelSearchMessage() {
        assertEquals(
            MinimalHotelSearchMessageParser.Result.Incomplete,
            parser.parse(
                "hotel-search; destination=Rome; check-in=2026-07-01; " +
                    "check-out=2026-07-04; adults=2",
            ),
        )
    }

    @Test
    fun rejectsInvalidOptionalChildrenCount() {
        assertEquals(
            MinimalHotelSearchMessageParser.Result.Incomplete,
            parser.parse(
                "hotel-search; destination=Rome; check-in=2026-07-01; " +
                    "check-out=2026-07-04; adults=2; children=unknown; rooms=1",
            ),
        )
    }

    @Test
    fun rejectsPositiveChildrenCountWithoutAges() {
        assertEquals(
            MinimalHotelSearchMessageParser.Result.Incomplete,
            parser.parse(
                "hotel-search; destination=Rome; check-in=2026-07-01; " +
                    "check-out=2026-07-04; adults=2; children=1; rooms=1",
            ),
        )
    }

    @Test
    fun acceptsChildAgeBoundariesInOriginalOrder() {
        val result = parser.parse(
            "hotel-search; destination=Rome; check-in=2026-07-01; " +
                "check-out=2026-07-04; adults=2; children=2; children-ages=17,0; rooms=1",
        ) as MinimalHotelSearchMessageParser.Result.Complete

        assertEquals(listOf(17, 0), result.criteria.guests.childrenAges)
    }

    @Test
    fun ignoresOrdinaryAssistantMessage() {
        assertEquals(
            MinimalHotelSearchMessageParser.Result.NotRequested,
            parser.parse("I want a hotel in Rome for two adults"),
        )
    }
}
