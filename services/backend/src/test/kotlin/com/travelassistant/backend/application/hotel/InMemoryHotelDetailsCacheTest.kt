package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.hotel.HotelDetails
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemoryHotelDetailsCacheTest {

    @Test
    fun `evicts least recently used entry at bounded capacity`() {
        val cache = InMemoryHotelDetailsCache(maxEntries = 2)
        cache.save("one", HotelDetails("One"))
        cache.save("two", HotelDetails("Two"))
        cache.find("one")
        cache.save("three", HotelDetails("Three"))

        assertEquals("One", cache.find("one")?.hotelName)
        assertNull(cache.find("two"))
        assertEquals("Three", cache.find("three")?.hotelName)
        assertEquals(2, cache.size())
    }
}
