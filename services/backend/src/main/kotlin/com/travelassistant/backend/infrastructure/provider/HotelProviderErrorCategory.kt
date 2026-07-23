package com.travelassistant.backend.infrastructure.provider

enum class HotelProviderErrorCategory {
    NOT_FOUND,
    UNAVAILABLE,
    TIMEOUT,
    RATE_LIMITED,
    AUTHENTICATION_FAILED,
    INVALID_RESPONSE,
    MAPPING_FAILED,
    UNKNOWN,
}
