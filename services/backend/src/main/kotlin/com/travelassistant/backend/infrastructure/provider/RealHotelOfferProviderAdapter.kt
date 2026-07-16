package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.HotelOfferProviderBoundary
import com.travelassistant.backend.application.hotel.HotelOfferProviderResult
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria

class RealHotelOfferProviderAdapter : HotelOfferProviderBoundary {

    override suspend fun search(criteria: HotelSearchCriteria): HotelOfferProviderResult =
        HotelOfferProviderResult.ProviderUnavailable(
            HotelOfferProviderResult.UnavailableReason.UNAVAILABLE,
        )
}
