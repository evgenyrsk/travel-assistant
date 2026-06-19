package com.travelassistant.backend.domain.provider

import com.travelassistant.backend.domain.hotel.HotelOffer
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria

/**
 * Provider-agnostic boundary for hotel offer sources.
 *
 * Implementations map their own source data into domain hotel offers. Real
 * provider contracts, credentials, retries, and provider-specific error
 * taxonomy remain outside the current boundary.
 */
fun interface HotelOfferProviderBoundary {
    fun search(criteria: HotelSearchCriteria): List<HotelOffer>
}
