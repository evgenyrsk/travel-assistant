package com.travelassistant.backend.application.hotel

internal sealed interface HotelLocationCandidateSelectionResult {
    data class Selected(
        val candidate: HotelLocationResolution.Candidate,
    ) : HotelLocationCandidateSelectionResult

    data object NotFound : HotelLocationCandidateSelectionResult

    data class SelectionRequired(
        val candidates: List<HotelLocationResolution.Candidate>,
    ) : HotelLocationCandidateSelectionResult
}
