package com.travelassistant.backend.infrastructure.provider

class HotelProviderConfigurationException(
    val configurationKey: String,
    reason: String,
) : IllegalArgumentException("$configurationKey $reason")
