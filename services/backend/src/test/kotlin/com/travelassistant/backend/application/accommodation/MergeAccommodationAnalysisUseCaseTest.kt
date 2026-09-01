package com.travelassistant.backend.application.accommodation

import com.travelassistant.backend.domain.hotel.AccommodationConcept
import com.travelassistant.backend.domain.hotel.AccommodationEvidenceSource
import com.travelassistant.backend.domain.hotel.AccommodationMatchVerdict
import com.travelassistant.backend.domain.hotel.AccommodationSemanticMatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MergeAccommodationAnalysisUseCaseTest {
    private val useCase = MergeAccommodationAnalysisUseCase()

    @Test
    fun `deep match replaces coarse match`() {
        val result = useCase(
            coarseMatches = mapOf("c01" to match(AccommodationMatchVerdict.PROBABLE)),
            deepMatches = mapOf("c01" to match(AccommodationMatchVerdict.MATCH)),
            expectedDeepCandidateIds = setOf("c01"),
        )

        assertEquals(
            AccommodationMatchVerdict.MATCH,
            result.matchesByCandidateId.getValue("c01").verdict,
        )
        assertFalse(result.partial)
    }

    @Test
    fun `missing deep result preserves coarse verdict and marks partial`() {
        val coarse = match(AccommodationMatchVerdict.PROBABLE)
        val result = useCase(
            coarseMatches = mapOf("c01" to coarse, "c02" to coarse),
            deepMatches = mapOf("c01" to match(AccommodationMatchVerdict.MATCH)),
            expectedDeepCandidateIds = setOf("c01", "c02"),
        )

        assertEquals(coarse, result.matchesByCandidateId.getValue("c02"))
        assertTrue(result.partial)
    }

    private fun match(verdict: AccommodationMatchVerdict) =
        AccommodationSemanticMatch(
            concept = AccommodationConcept.GLAMPING,
            verdict = verdict,
            evidenceSources = setOf(AccommodationEvidenceSource.DESCRIPTION),
        )
}
