package com.travelassistant.backend.infrastructure.provider

class HotelProviderException(
    val category: HotelProviderErrorCategory,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
