package com.travelassistant.backend.application.hotel

internal fun interface HotelLocationCandidateSelectionPolicy {
    fun select(
        query: String,
        candidates: List<HotelLocationResolution.Candidate>,
    ): HotelLocationCandidateSelectionResult
}
