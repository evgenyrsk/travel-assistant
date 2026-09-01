package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.hotel.HotelDetails

class InMemoryHotelDetailsCache(
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
) : HotelDetailsCache {
    init {
        require(maxEntries > 0) { "Hotel details cache size must be positive" }
    }

    private val entries = object : LinkedHashMap<String, HotelDetails>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, HotelDetails>,
        ): Boolean = size > maxEntries
    }

    override fun find(providerReference: String): HotelDetails? =
        synchronized(entries) { entries[providerReference] }

    override fun save(providerReference: String, details: HotelDetails) {
        if (providerReference.isBlank()) return
        synchronized(entries) { entries[providerReference] = details }
    }

    internal fun size(): Int = synchronized(entries) { entries.size }

    companion object {
        const val DEFAULT_MAX_ENTRIES = 128
    }
}
