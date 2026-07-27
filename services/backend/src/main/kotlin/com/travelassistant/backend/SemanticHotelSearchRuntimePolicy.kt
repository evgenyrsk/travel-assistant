package com.travelassistant.backend

import com.travelassistant.backend.application.hotel.SemanticHotelSearchLauncher
import com.travelassistant.backend.infrastructure.accommodation.AccommodationAnalysisProviderMode
import com.travelassistant.backend.infrastructure.provider.HotelProviderMode

internal object SemanticHotelSearchRuntimePolicy {
    fun createLauncher(
        hotelProviderMode: HotelProviderMode,
        accommodationAnalysisMode: AccommodationAnalysisProviderMode,
        enabledLauncherFactory: () -> SemanticHotelSearchLauncher,
    ): SemanticHotelSearchLauncher =
        if (isCompatible(hotelProviderMode, accommodationAnalysisMode)) {
            enabledLauncherFactory()
        } else {
            SemanticHotelSearchLauncher.UNAVAILABLE
        }

    fun isCompatible(
        hotelProviderMode: HotelProviderMode,
        accommodationAnalysisMode: AccommodationAnalysisProviderMode,
    ): Boolean =
        hotelProviderMode != HotelProviderMode.REAL ||
            accommodationAnalysisMode != AccommodationAnalysisProviderMode.FAKE
}
