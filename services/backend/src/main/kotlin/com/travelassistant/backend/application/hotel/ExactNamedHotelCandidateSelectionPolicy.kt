package com.travelassistant.backend.application.hotel

internal class ExactNamedHotelCandidateSelectionPolicy : HotelCandidateSelectionPolicy {
    override fun select(
        query: String,
        candidates: List<HotelLocationResolution.HotelCandidate>,
        hasLocationCandidates: Boolean,
    ): HotelCandidateSelectionResult {
        val uniqueCandidates = candidates.distinctBy { candidate ->
            candidate.providerReference
        }
        if (uniqueCandidates.isEmpty()) {
            return HotelCandidateSelectionResult.NotSelected
        }

        val normalizedQuery = HotelSearchCandidateTextNormalizer.normalize(query)
        val exactMatches = uniqueCandidates.filter { candidate ->
            HotelSearchCandidateTextNormalizer.normalize(candidate.name) == normalizedQuery ||
                HotelSearchCandidateTextNormalizer.normalize(candidate.signature) == normalizedQuery
        }

        return when {
            exactMatches.size == 1 ->
                HotelCandidateSelectionResult.Selected(exactMatches.single())

            exactMatches.size > 1 ->
                HotelCandidateSelectionResult.SelectionRequired(exactMatches)

            !hasLocationCandidates && uniqueCandidates.size == 1 ->
                HotelCandidateSelectionResult.Selected(uniqueCandidates.single())

            !hasLocationCandidates ->
                HotelCandidateSelectionResult.SelectionRequired(uniqueCandidates)

            else -> HotelCandidateSelectionResult.NotSelected
        }
    }
}
