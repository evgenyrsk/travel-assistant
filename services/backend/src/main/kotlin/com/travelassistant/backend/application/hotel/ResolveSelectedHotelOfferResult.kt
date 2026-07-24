package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.hotel.HotelOffer

sealed interface ResolveSelectedHotelOfferResult {
    data class Resolved(
        val offer: HotelOffer,
    ) : ResolveSelectedHotelOfferResult

    data object SearchNotFound : ResolveSelectedHotelOfferResult

    data object OfferNotFound : ResolveSelectedHotelOfferResult
}
