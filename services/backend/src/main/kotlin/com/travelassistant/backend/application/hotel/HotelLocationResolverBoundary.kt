package com.travelassistant.backend.application.hotel

internal fun interface HotelLocationResolverBoundary {
    suspend fun resolve(request: HotelLocationResolutionRequest): HotelLocationResolution
}
