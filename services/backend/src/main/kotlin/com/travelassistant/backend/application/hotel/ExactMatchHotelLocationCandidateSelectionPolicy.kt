package com.travelassistant.backend.application.hotel

import java.text.Normalizer
import java.util.Locale

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

        val normalizedQuery = normalize(query)
        val exactMatches = uniqueCandidates.filter { candidate ->
            normalize(candidate.name) == normalizedQuery ||
                normalize(candidate.signature) == normalizedQuery
        }

        return if (exactMatches.size == 1) {
            HotelLocationCandidateSelectionResult.Selected(exactMatches.single())
        } else {
            HotelLocationCandidateSelectionResult.SelectionRequired(uniqueCandidates)
        }
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKC)
            .trim()
            .replace(REPEATED_WHITESPACE, " ")
            .lowercase(Locale.ROOT)
            .replace('ё', 'е')

    private companion object {
        val REPEATED_WHITESPACE = Regex("\\s+")
    }
}
