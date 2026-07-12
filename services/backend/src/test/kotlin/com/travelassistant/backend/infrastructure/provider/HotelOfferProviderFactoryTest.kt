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
        val config = completeRealConfig()

        val provider = HotelOfferProviderFactory.create(config)

        assertIs<RealHotelOfferProviderAdapter>(provider)
    }

    @Test
    fun defaultConfigCreatesFakeHotelOfferProvider() {
        val config = HotelProviderConfig()

        val provider = HotelOfferProviderFactory.create(config)

        assertIs<FakeHotelOfferProvider>(provider)
    }

    private fun completeRealConfig(): HotelProviderConfig =
        HotelProviderConfig(
            mode = HotelProviderMode.REAL,
            hotelsApi = HotelsApiConfig(
                baseUrl = "https://hotels-api.test",
                tokenUrl = "https://identity.test/oauth/token",
                clientId = "hotels-client",
                clientSecret = RedactedSecret.of("synthetic-secret"),
                scope = "hotels.search",
                connectTimeoutMillis = 2_000,
                requestTimeoutMillis = 5_000,
            ),
        )
}
