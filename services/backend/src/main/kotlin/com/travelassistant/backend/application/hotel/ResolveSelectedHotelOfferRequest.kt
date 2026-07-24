package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.hotel.HotelSearchId

data class ResolveSelectedHotelOfferRequest(
    val searchId: HotelSearchId,
    val offerId: String,
)
