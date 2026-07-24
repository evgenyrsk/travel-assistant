package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.hotel.HotelSearchCriteria

/**
 * Application-owned boundary for provider-backed hotel offer searches.
 *
 * Implementations translate provider DTOs, location resolution, and safe
 * provider failures into [HotelOfferProviderResult].
 */
fun interface HotelOfferProviderBoundary {
    suspend fun search(criteria: HotelSearchCriteria): HotelOfferProviderResult
}
