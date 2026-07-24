package com.travelassistant.backend.domain.hotel

data class RankedHotelOffer(
    val offer: HotelOffer,
    val matchSummary: String,
    val semanticMatch: AccommodationSemanticMatch? = null,
)
