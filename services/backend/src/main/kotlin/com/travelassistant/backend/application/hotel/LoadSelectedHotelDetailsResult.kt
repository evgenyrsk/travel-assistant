package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.hotel.HotelDetails

sealed interface LoadSelectedHotelDetailsResult {
    data class Loaded(
        val details: HotelDetails,
    ) : LoadSelectedHotelDetailsResult

    data object SearchNotFound : LoadSelectedHotelDetailsResult

    data object OfferNotFound : LoadSelectedHotelDetailsResult

    data object DetailsNotFound : LoadSelectedHotelDetailsResult

    data class ResponseRejected(
        val reason: HotelDetailsProviderResult.ResponseRejectionReason,
    ) : LoadSelectedHotelDetailsResult

    data class ProviderUnavailable(
        val reason: HotelDetailsProviderResult.UnavailableReason,
    ) : LoadSelectedHotelDetailsResult
}
