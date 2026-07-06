package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.domain.hotel.HotelOffer
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import com.travelassistant.backend.domain.provider.HotelOfferProviderBoundary

class RealHotelOfferProviderAdapter : HotelOfferProviderBoundary {

    override fun search(criteria: HotelSearchCriteria): List<HotelOffer> = emptyList()
}
