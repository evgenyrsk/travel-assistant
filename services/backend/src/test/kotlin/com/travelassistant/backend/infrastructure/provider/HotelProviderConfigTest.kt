package com.travelassistant.backend.infrastructure.provider

import kotlin.test.Test
import kotlin.test.assertEquals

class HotelProviderConfigTest {

    @Test
    fun defaultModeIsFake() {
        val config = HotelProviderConfig()

        assertEquals(HotelProviderMode.FAKE, config.mode)
    }

    @Test
    fun explicitRealModeIsPreserved() {
        val config = HotelProviderConfig(mode = HotelProviderMode.REAL)

        assertEquals(HotelProviderMode.REAL, config.mode)
    }

    @Test
    fun fromEnvironmentFallsBackToFakeWhenEnvIsUnset() {
        val config = HotelProviderConfig.fromEnvironment()

        assertEquals(HotelProviderMode.FAKE, config.mode)
    }
}
