package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.hotel.HotelSearch

sealed interface HotelSearchStateTransitionResult {
    data class Updated(val search: HotelSearch) : HotelSearchStateTransitionResult

    data object NotFound : HotelSearchStateTransitionResult

    data class UnexpectedStatus(
        val search: HotelSearch,
    ) : HotelSearchStateTransitionResult
}
