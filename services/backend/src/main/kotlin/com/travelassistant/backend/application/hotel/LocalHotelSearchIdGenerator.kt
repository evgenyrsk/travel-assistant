package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.hotel.HotelSearchId
import java.util.concurrent.atomic.AtomicInteger

class LocalHotelSearchIdGenerator(
    private val prefix: String = "hotel-search-local",
) : HotelSearchIdGenerator {
    private val nextValue = AtomicInteger(1)

    override fun nextId(): HotelSearchId {
        val suffix = nextValue.getAndIncrement().toString().padStart(6, '0')
        return HotelSearchId("$prefix-$suffix")
    }
}
