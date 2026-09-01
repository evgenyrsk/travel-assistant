package com.travelassistant.backend

import com.travelassistant.backend.application.hotel.SemanticHotelSearchLauncher
import com.travelassistant.backend.infrastructure.accommodation.AccommodationAnalysisProviderMode
import com.travelassistant.backend.infrastructure.provider.HotelProviderMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

class SemanticHotelSearchRuntimePolicyTest {

    @Test
    fun `blocks only real hotels with fake semantic analysis`() {
        val expectations = mapOf(
            (HotelProviderMode.FAKE to AccommodationAnalysisProviderMode.FAKE) to true,
            (HotelProviderMode.FAKE to AccommodationAnalysisProviderMode.OPENROUTER) to true,
            (HotelProviderMode.FAKE to AccommodationAnalysisProviderMode.INTERNAL_GATEWAY) to true,
            (HotelProviderMode.REAL to AccommodationAnalysisProviderMode.FAKE) to false,
            (HotelProviderMode.REAL to AccommodationAnalysisProviderMode.OPENROUTER) to true,
            (HotelProviderMode.REAL to AccommodationAnalysisProviderMode.INTERNAL_GATEWAY) to true,
        )

        expectations.forEach { (modes, expectedCompatibility) ->
            assertEquals(
                expectedCompatibility,
                SemanticHotelSearchRuntimePolicy.isCompatible(
                    hotelProviderMode = modes.first,
                    accommodationAnalysisMode = modes.second,
                ),
                "Unexpected compatibility for ${modes.first} + ${modes.second}",
            )
        }
    }

    @Test
    fun `does not create semantic runtime for incompatible modes`() {
        var enabledLauncherCreated = false

        val launcher = SemanticHotelSearchRuntimePolicy.createLauncher(
            hotelProviderMode = HotelProviderMode.REAL,
            accommodationAnalysisMode = AccommodationAnalysisProviderMode.FAKE,
        ) {
            enabledLauncherCreated = true
            SemanticHotelSearchLauncher { _, _ -> true }
        }

        assertFalse(enabledLauncherCreated)
        assertSame(SemanticHotelSearchLauncher.UNAVAILABLE, launcher)
    }
}
