package com.travelassistant.backend.application.assistant

sealed interface MapLlmHotelSearchPreferencesPatchResult {
    data class Mapped(
        val patch: HotelSearchPreferencesPatch,
    ) : MapLlmHotelSearchPreferencesPatchResult

    data class Rejected(
        val issues: Set<MapLlmHotelSearchPreferencesPatchIssue>,
    ) : MapLlmHotelSearchPreferencesPatchResult
}
