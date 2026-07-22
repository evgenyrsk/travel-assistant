package com.travelassistant.backend.infrastructure.provider

internal data class HotelsApiSearchMappingError(
    val issue: Issue,
    val providerReference: String? = null,
) {
    enum class Issue {
        INVALID_DESTINATION_ID,
        INVALID_DATE_RANGE,
        INVALID_ROOM_COUNT,
        INVALID_ADULTS_COUNT,
        INVALID_CHILD_AGE,
        INVALID_MAX_TOTAL_PRICE,
        UNSUPPORTED_MAX_TOTAL_PRICE_CURRENCY,
        INVALID_STARS,
        INVALID_PROVIDER_REFERENCE,
        INVALID_HOTEL_NAME,
        INVALID_STAR_RATING,
        INVALID_LOCATION,
        INVALID_PRICE,
        INVALID_CURRENCY,
        INVALID_REVIEW,
        INVALID_AVAILABILITY,
        INVALID_CANCELLATION,
    }
}
