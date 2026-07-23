package com.travelassistant.backend.application.hotel

import java.util.concurrent.atomic.AtomicInteger

class LocalHotelOfferIdGenerator(
    private val prefix: String = "hotel-offer-local",
) : HotelOfferIdGenerator {
    private val nextValue = AtomicInteger(1)

    override fun nextId(): String {
        val suffix = nextValue.getAndIncrement().toString().padStart(6, '0')
        return "$prefix-$suffix"
    }
}
