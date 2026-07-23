package com.travelassistant.backend.application.hotel

import kotlin.test.Test
import kotlin.test.assertEquals

class LocalHotelOfferIdGeneratorTest {

    @Test
    fun `generates process-local offer identifiers without provider input`() {
        val generator = LocalHotelOfferIdGenerator(prefix = "hotel-offer-test")

        assertEquals("hotel-offer-test-000001", generator.nextId())
        assertEquals("hotel-offer-test-000002", generator.nextId())
    }
}
