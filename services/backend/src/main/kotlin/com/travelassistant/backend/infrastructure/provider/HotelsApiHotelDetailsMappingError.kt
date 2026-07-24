package com.travelassistant.backend.infrastructure.provider

internal data class HotelsApiHotelDetailsMappingError(
    val issue: Issue,
) {
    enum class Issue {
        INVALID_PROVIDER_REFERENCE,
        INVALID_HOTEL_NAME,
        INVALID_STAR_RATING,
        INVALID_LOCATION,
        INVALID_CHECK_IN_TIME,
        INVALID_CHECK_OUT_TIME,
    }
}
