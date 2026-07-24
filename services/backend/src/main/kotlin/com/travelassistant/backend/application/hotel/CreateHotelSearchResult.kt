package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.hotel.HotelSearch

sealed interface CreateHotelSearchResult {
    data class Created(
        val search: HotelSearch,
    ) : CreateHotelSearchResult

    data class NotCreated(
        val outcome: HotelOfferProviderResult.NotCompleted,
    ) : CreateHotelSearchResult
}
