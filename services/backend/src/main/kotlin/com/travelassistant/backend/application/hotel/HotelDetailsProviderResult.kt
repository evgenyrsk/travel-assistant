package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.hotel.HotelDetails

sealed interface HotelDetailsProviderResult {
    data class Loaded(
        val details: HotelDetails,
    ) : HotelDetailsProviderResult

    data object NotFound : HotelDetailsProviderResult

    data class ResponseRejected(
        val reason: ResponseRejectionReason,
    ) : HotelDetailsProviderResult

    data class ProviderUnavailable(
        val reason: UnavailableReason,
    ) : HotelDetailsProviderResult

    enum class ResponseRejectionReason {
        INVALID_PAYLOAD,
        INVALID_PROVIDER_REFERENCE,
        INVALID_HOTEL_DATA,
        INVALID_LOCATION_DATA,
        UNKNOWN,
    }

    enum class UnavailableReason {
        TIMEOUT,
        RATE_LIMITED,
        AUTHENTICATION_FAILED,
        UNAVAILABLE,
        UNKNOWN,
    }
}
