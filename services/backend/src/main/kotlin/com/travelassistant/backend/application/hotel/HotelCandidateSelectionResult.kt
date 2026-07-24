package com.travelassistant.backend.application.hotel

internal sealed interface HotelCandidateSelectionResult {
    data class Selected(
        val candidate: HotelLocationResolution.HotelCandidate,
    ) : HotelCandidateSelectionResult

    data object NotSelected : HotelCandidateSelectionResult

    data class SelectionRequired(
        val candidates: List<HotelLocationResolution.HotelCandidate>,
    ) : HotelCandidateSelectionResult
}
