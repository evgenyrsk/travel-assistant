package com.travelassistant.backend.application.assistant

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookingRequestBoundaryPolicyTest {
    private val policy = BookingRequestBoundaryPolicy()

    @Test
    fun `recognizes booking request without treating ordinary search as booking`() {
        assertTrue(policy.isBookingRequested("Хочу забронировать глемпинг"))
        assertTrue(policy.isBookingRequested("Помоги с бронированием"))
        assertFalse(policy.isBookingRequested("Хочу подобрать глемпинг"))
    }
}
