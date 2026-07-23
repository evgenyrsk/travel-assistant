package com.travelassistant.backend.application.hotel

internal class ExactMatchHotelLocationCandidateSelectionPolicy :
    HotelLocationCandidateSelectionPolicy {

    override fun select(
        query: String,
        candidates: List<HotelLocationResolution.Candidate>,
    ): HotelLocationCandidateSelectionResult {
        val uniqueCandidates = candidates.distinctBy { it.destinationId }

        if (uniqueCandidates.isEmpty()) {
            return HotelLocationCandidateSelectionResult.NotFound
        }
        if (uniqueCandidates.size == 1) {
            return HotelLocationCandidateSelectionResult.Selected(uniqueCandidates.single())
        }

        val normalizedQuery = HotelSearchCandidateTextNormalizer.normalize(query)
        val exactMatches = uniqueCandidates.filter { candidate ->
            HotelSearchCandidateTextNormalizer.normalize(candidate.name) == normalizedQuery ||
                HotelSearchCandidateTextNormalizer.normalize(candidate.signature) == normalizedQuery
        }

        return if (exactMatches.size == 1) {
            HotelLocationCandidateSelectionResult.Selected(exactMatches.single())
        } else {
            HotelLocationCandidateSelectionResult.SelectionRequired(uniqueCandidates)
        }
    }

}
