package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.HotelDetailsProviderResult
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FakeHotelDetailsProviderTest {

    private val provider = FakeHotelDetailsProvider()

    @Test
    fun `returns deterministic details for fake search references`() = runBlocking {
        val first = assertIs<HotelDetailsProviderResult.Loaded>(
            provider.load("local-fake-kazan-001"),
        )
        val second = assertIs<HotelDetailsProviderResult.Loaded>(
            provider.load("local-fake-kazan-001"),
        )

        assertEquals(first, second)
        assertEquals("Demo Central Hotel", first.details.hotelName)
    }

    @Test
    fun `rejects blank and does not invent details for foreign references`() = runBlocking {
        assertEquals(
            HotelDetailsProviderResult.ResponseRejected(
                HotelDetailsProviderResult.ResponseRejectionReason.INVALID_PROVIDER_REFERENCE,
            ),
            provider.load(" "),
        )
        assertEquals(
            HotelDetailsProviderResult.NotFound,
            provider.load("foreign-reference"),
        )
    }
}
