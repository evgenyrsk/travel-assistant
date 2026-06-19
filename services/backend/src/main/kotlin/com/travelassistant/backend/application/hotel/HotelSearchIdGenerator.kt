package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.hotel.HotelSearchId

fun interface HotelSearchIdGenerator {
    fun nextId(): HotelSearchId
}
