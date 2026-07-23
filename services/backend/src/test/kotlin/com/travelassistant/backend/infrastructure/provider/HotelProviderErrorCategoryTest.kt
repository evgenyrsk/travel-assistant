package com.travelassistant.backend.infrastructure.provider

import kotlin.test.Test
import kotlin.test.assertEquals

class HotelProviderErrorCategoryTest {

    @Test
    fun containsAllExpectedCategories() {
        val expected = setOf(
            HotelProviderErrorCategory.NOT_FOUND,
            HotelProviderErrorCategory.UNAVAILABLE,
            HotelProviderErrorCategory.TIMEOUT,
            HotelProviderErrorCategory.RATE_LIMITED,
            HotelProviderErrorCategory.AUTHENTICATION_FAILED,
            HotelProviderErrorCategory.INVALID_RESPONSE,
            HotelProviderErrorCategory.MAPPING_FAILED,
            HotelProviderErrorCategory.UNKNOWN,
        )

        assertEquals(expected, HotelProviderErrorCategory.entries.toSet())
    }

    @Test
    fun categoryCountIsStable() {
        assertEquals(8, HotelProviderErrorCategory.entries.size)
    }
}
