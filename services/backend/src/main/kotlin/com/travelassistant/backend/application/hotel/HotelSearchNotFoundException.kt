package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.hotel.HotelSearchId

class HotelSearchNotFoundException(
    val searchId: HotelSearchId,
) : RuntimeException("Hotel search was not found: ${searchId.value}")
