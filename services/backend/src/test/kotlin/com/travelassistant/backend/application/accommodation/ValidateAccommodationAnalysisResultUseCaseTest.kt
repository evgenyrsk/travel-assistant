package com.travelassistant.backend.application.accommodation

import com.travelassistant.backend.domain.hotel.AccommodationConcept
import com.travelassistant.backend.domain.hotel.AccommodationEvidenceSource
import com.travelassistant.backend.domain.hotel.AccommodationMatchVerdict
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ValidateAccommodationAnalysisResultUseCaseTest {
    private val useCase = ValidateAccommodationAnalysisResultUseCase()

    @Test
    fun `accepts complete result and normalizes evidence policy`() {
        val request = request("c01", "c02", "c03", "c04")
        val result = AccommodationAnalysisResult.Completed(
            decisions = listOf(
                decision(
                    "c01",
                    AccommodationMatchVerdict.MATCH,
                    evidence(
                        AccommodationEvidenceSource.NAME,
                        AccommodationAnalysisResult.Signal.EXPLICIT_GLAMPING_LABEL,
                    ),
                ),
                decision(
                    "c02",
                    AccommodationMatchVerdict.MATCH,
                    evidence(
                        AccommodationEvidenceSource.IMAGE,
                        AccommodationAnalysisResult.Signal.IMAGE_GLAMPING_STRUCTURE,
                    ),
                ),
                decision("c03", AccommodationMatchVerdict.NO_MATCH),
                decision("c04", AccommodationMatchVerdict.UNKNOWN),
            ),
        )

        val accepted = assertIs<
            ValidateAccommodationAnalysisResultUseCase.ValidationResult.Accepted
            >(useCase(request, result))

        assertEquals(
            AccommodationMatchVerdict.MATCH,
            accepted.matchesByCandidateId.getValue("c01").verdict,
        )
        assertEquals(
            AccommodationMatchVerdict.PROBABLE,
            accepted.matchesByCandidateId.getValue("c02").verdict,
        )
        assertEquals(
            AccommodationMatchVerdict.NO_MATCH,
            accepted.matchesByCandidateId.getValue("c03").verdict,
        )
        assertEquals(
            AccommodationMatchVerdict.UNKNOWN,
            accepted.matchesByCandidateId.getValue("c04").verdict,
        )
    }

    @Test
    fun `two independent signals allow match while contradictory evidence caps probable`() {
        val request = request("c01", "c02")
        val twoSignals = setOf(
            evidence(
                AccommodationEvidenceSource.DESCRIPTION,
                AccommodationAnalysisResult.Signal.GLAMPING_STRUCTURE,
            ),
            evidence(
                AccommodationEvidenceSource.DESCRIPTION,
                AccommodationAnalysisResult.Signal.NATURE_SETTING,
            ),
        )
        val result = AccommodationAnalysisResult.Completed(
            listOf(
                decision("c01", AccommodationMatchVerdict.MATCH, *twoSignals.toTypedArray()),
                decision(
                    "c02",
                    AccommodationMatchVerdict.MATCH,
                    *twoSignals.toTypedArray(),
                    evidence(
                        AccommodationEvidenceSource.IMAGE,
                        AccommodationAnalysisResult.Signal.STANDARD_HOTEL_FORMAT,
                    ),
                ),
            ),
        )

        val accepted = assertIs<
            ValidateAccommodationAnalysisResultUseCase.ValidationResult.Accepted
            >(useCase(request, result))

        assertEquals(
            AccommodationMatchVerdict.MATCH,
            accepted.matchesByCandidateId.getValue("c01").verdict,
        )
        assertEquals(
            AccommodationMatchVerdict.PROBABLE,
            accepted.matchesByCandidateId.getValue("c02").verdict,
        )
    }

    @Test
    fun `rejects duplicate missing and unknown candidate results`() {
        val rejected = assertIs<
            ValidateAccommodationAnalysisResultUseCase.ValidationResult.Rejected
            >(
            useCase(
                request("c01", "c02"),
                AccommodationAnalysisResult.Completed(
                    listOf(
                        decision("c01", AccommodationMatchVerdict.UNKNOWN),
                        decision("c01", AccommodationMatchVerdict.UNKNOWN),
                        decision("unexpected", AccommodationMatchVerdict.UNKNOWN),
                    ),
                ),
            ),
        )

        assertEquals(
            setOf(
                ValidateAccommodationAnalysisResultUseCase.Issue.DUPLICATE_RESULTS,
                ValidateAccommodationAnalysisResultUseCase.Issue.UNKNOWN_CANDIDATE_RESULTS,
                ValidateAccommodationAnalysisResultUseCase.Issue.MISSING_CANDIDATE_RESULTS,
            ),
            rejected.issues,
        )
    }

    private fun request(vararg ids: String): AccommodationAnalysisRequest =
        AccommodationAnalysisRequest(
            concept = AccommodationConcept.GLAMPING,
            candidates = ids.map { id ->
                AccommodationAnalysisRequest.Candidate(id, "Synthetic candidate")
            },
        )

    private fun decision(
        candidateId: String,
        verdict: AccommodationMatchVerdict,
        vararg evidence: AccommodationAnalysisResult.Evidence,
    ): AccommodationAnalysisResult.Decision =
        AccommodationAnalysisResult.Decision(candidateId, verdict, evidence.toSet())

    private fun evidence(
        source: AccommodationEvidenceSource,
        signal: AccommodationAnalysisResult.Signal,
    ): AccommodationAnalysisResult.Evidence =
        AccommodationAnalysisResult.Evidence(source, signal)
}
