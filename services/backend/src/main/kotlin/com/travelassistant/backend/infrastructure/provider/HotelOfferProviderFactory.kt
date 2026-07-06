package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.domain.provider.HotelOfferProviderBoundary

object HotelOfferProviderFactory {

    fun create(config: HotelProviderConfig): HotelOfferProviderBoundary =
        when (config.mode) {
            HotelProviderMode.FAKE -> FakeHotelOfferProvider()
            HotelProviderMode.REAL -> RealHotelOfferProviderAdapter()
        }
}
