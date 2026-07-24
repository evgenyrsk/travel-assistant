package com.travelassistant.backend.infrastructure.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class HotelProviderExceptionTest {

    @Test
    fun preservesCategoryAndMessage() {
        val exception = HotelProviderException(
            category = HotelProviderErrorCategory.UNAVAILABLE,
            message = "Provider is down",
        )

        assertEquals(HotelProviderErrorCategory.UNAVAILABLE, exception.category)
        assertEquals("Provider is down", exception.message)
        assertNull(exception.cause)
    }

    @Test
    fun preservesCategoryMessageAndCause() {
        val cause = RuntimeException("connection refused")
        val exception = HotelProviderException(
            category = HotelProviderErrorCategory.TIMEOUT,
            message = "Provider timed out",
            cause = cause,
        )

        assertEquals(HotelProviderErrorCategory.TIMEOUT, exception.category)
        assertEquals("Provider timed out", exception.message)
        assertSame(cause, exception.cause)
    }

    @Test
    fun isRuntimeException() {
        val exception = HotelProviderException(
            category = HotelProviderErrorCategory.UNKNOWN,
            message = "Unknown error",
        )

        assertIs<RuntimeException>(exception)
    }

    @Test
    fun allCategoriesCanBeUsedInException() {
        HotelProviderErrorCategory.entries.forEach { category ->
            val exception = HotelProviderException(
                category = category,
                message = "Test: ${category.name}",
            )
            assertEquals(category, exception.category)
        }
    }
}
