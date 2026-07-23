package com.travelassistant.backend.application.hotel

fun interface HotelDetailsProviderBoundary {
    suspend fun load(providerReference: String): HotelDetailsProviderResult
}
