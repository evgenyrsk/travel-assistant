package com.travelassistant.backend.application.accommodation

import com.travelassistant.backend.domain.hotel.AccommodationSemanticMatch

class MergeAccommodationAnalysisUseCase {

    operator fun invoke(
        coarseMatches: Map<String, AccommodationSemanticMatch>,
        deepMatches: Map<String, AccommodationSemanticMatch>,
        expectedDeepCandidateIds: Set<String>,
    ): Result =
        Result(
            matchesByCandidateId = coarseMatches + deepMatches,
            partial = expectedDeepCandidateIds.any { candidateId -> candidateId !in deepMatches },
        )

    data class Result(
        val matchesByCandidateId: Map<String, AccommodationSemanticMatch>,
        val partial: Boolean,
    )
}
