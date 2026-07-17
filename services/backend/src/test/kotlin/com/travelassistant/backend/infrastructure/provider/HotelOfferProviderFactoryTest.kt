package com.travelassistant.backend.infrastructure.provider

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.HttpTimeout
import kotlin.test.Test
import kotlin.test.assertIs

class HotelOfferProviderFactoryTest {

    @Test
    fun fakeModeCreatesFakeHotelOfferProvider() {
        val config = HotelProviderConfig(mode = HotelProviderMode.FAKE)

        val runtime = HotelOfferProviderFactory.create(config)

        assertIs<FakeHotelOfferProvider>(runtime.provider)
        runtime.close()
    }

    @Test
    fun realModeCreatesRealHotelOfferProviderAdapter() {
        val config = completeRealConfig()

        val runtime = HotelOfferProviderFactory.create(
            config = config,
            realHttpClientFactory = ::mockHttpClient,
        )

        assertIs<RealHotelOfferProviderAdapter>(runtime.provider)
        runtime.close()
    }

    @Test
    fun defaultConfigCreatesFakeHotelOfferProvider() {
        val config = HotelProviderConfig()

        val runtime = HotelOfferProviderFactory.create(config)

        assertIs<FakeHotelOfferProvider>(runtime.provider)
        runtime.close()
    }

    private fun completeRealConfig(): HotelProviderConfig =
        HotelProviderConfig(
            mode = HotelProviderMode.REAL,
            hotelsApi = HotelsApiConfig(),
        )

    private fun mockHttpClient(): HttpClient =
        HttpClient(MockEngine { respondOk() }) {
            install(HttpTimeout)
        }
}
