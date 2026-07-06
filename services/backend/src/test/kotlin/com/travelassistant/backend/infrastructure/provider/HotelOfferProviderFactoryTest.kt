package com.travelassistant.backend.infrastructure.provider

import kotlin.test.Test
import kotlin.test.assertIs

class HotelOfferProviderFactoryTest {

    @Test
    fun fakeModeCreatesFakeHotelOfferProvider() {
        val config = HotelProviderConfig(mode = HotelProviderMode.FAKE)

        val provider = HotelOfferProviderFactory.create(config)

        assertIs<FakeHotelOfferProvider>(provider)
    }

    @Test
    fun realModeCreatesRealHotelOfferProviderAdapter() {
        val config = HotelProviderConfig(mode = HotelProviderMode.REAL)

        val provider = HotelOfferProviderFactory.create(config)

        assertIs<RealHotelOfferProviderAdapter>(provider)
    }

    @Test
    fun defaultConfigCreatesFakeHotelOfferProvider() {
        val config = HotelProviderConfig()

        val provider = HotelOfferProviderFactory.create(config)

        assertIs<FakeHotelOfferProvider>(provider)
    }
}
