package com.travelassistant.backend.infrastructure.provider

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.HttpTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HotelOfferProviderFactoryTest {

    @Test
    fun fakeModeCreatesFakeHotelOfferProvider() {
        val config = HotelProviderConfig(mode = HotelProviderMode.FAKE)

        val runtime = HotelOfferProviderFactory.create(config)

        assertIs<FakeHotelOfferProvider>(runtime.provider)
        assertIs<FakeHotelDetailsProvider>(runtime.detailsProvider)
        runtime.close()
    }

    @Test
    fun realModeCreatesRealHotelOfferProviderAdapter() {
        val config = completeRealConfig()
        var httpClientCreations = 0

        val runtime = HotelOfferProviderFactory.create(
            config = config,
            realHttpClientFactory = {
                httpClientCreations++
                mockHttpClient()
            },
        )

        assertIs<RealHotelOfferProviderAdapter>(runtime.provider)
        assertIs<HotelsApiHotelDetailsProviderAdapter>(runtime.detailsProvider)
        assertEquals(1, httpClientCreations)
        runtime.close()
    }

    @Test
    fun defaultConfigCreatesFakeHotelOfferProvider() {
        val config = HotelProviderConfig()

        val runtime = HotelOfferProviderFactory.create(config)

        assertIs<FakeHotelOfferProvider>(runtime.provider)
        assertIs<FakeHotelDetailsProvider>(runtime.detailsProvider)
        runtime.close()
    }

    @Test
    fun runtimeClosesOwnedResourceOnlyOnce() {
        var closeCount = 0
        val runtime = HotelOfferProviderRuntime(
            provider = FakeHotelOfferProvider(),
            detailsProvider = FakeHotelDetailsProvider(),
            closeAction = { closeCount++ },
        )

        runtime.close()
        runtime.close()

        assertEquals(1, closeCount)
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
