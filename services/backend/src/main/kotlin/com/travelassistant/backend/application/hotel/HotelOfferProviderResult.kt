package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.hotel.HotelOffer

sealed interface HotelOfferProviderResult {
    data class SearchCompleted(
        val offers: List<HotelOffer>,
    ) : HotelOfferProviderResult

    sealed interface NotCompleted : HotelOfferProviderResult

    data object LocationNotFound : NotCompleted

    data class LocationSelectionRequired(
        val suggestions: List<HotelLocationSuggestion>,
    ) : NotCompleted

    data class RequestRejected(
        val reason: RequestRejectionReason,
    ) : NotCompleted

    data class ResponseRejected(
        val reason: ResponseRejectionReason,
    ) : NotCompleted

    data class ProviderUnavailable(
        val reason: UnavailableReason,
    ) : NotCompleted

    enum class RequestRejectionReason {
        INVALID_DESTINATION,
        INVALID_DATE_RANGE,
        INVALID_OCCUPANCY,
        INVALID_PREFERENCES,
        UNKNOWN,
    }

    enum class ResponseRejectionReason {
        INVALID_PAYLOAD,
        INVALID_PROVIDER_REFERENCE,
        INVALID_HOTEL_DATA,
        INVALID_LOCATION_DATA,
        INVALID_PRICE,
        INVALID_CURRENCY,
        INVALID_REVIEW,
        INVALID_AVAILABILITY,
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
