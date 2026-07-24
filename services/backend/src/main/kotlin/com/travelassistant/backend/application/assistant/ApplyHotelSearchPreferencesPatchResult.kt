package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.hotel.HotelSearchPreferences

sealed interface ApplyHotelSearchPreferencesPatchResult {
    data class Applied(
        val preferences: HotelSearchPreferences,
    ) : ApplyHotelSearchPreferencesPatchResult

    data class Rejected(
        val currentPreferences: HotelSearchPreferences,
        val issues: Set<HotelSearchPreferencesPatchIssue>,
    ) : ApplyHotelSearchPreferencesPatchResult
}
