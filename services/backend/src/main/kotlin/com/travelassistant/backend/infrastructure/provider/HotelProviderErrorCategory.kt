package com.travelassistant.backend.infrastructure.provider

enum class HotelProviderErrorCategory {
    UNAVAILABLE,
    TIMEOUT,
    RATE_LIMITED,
    AUTHENTICATION_FAILED,
    INVALID_RESPONSE,
    MAPPING_FAILED,
    UNKNOWN,
}
