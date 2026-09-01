package com.travelassistant.backend.application.accommodation

import com.travelassistant.backend.domain.hotel.AccommodationMatchVerdict
import com.travelassistant.backend.domain.hotel.AccommodationSemanticMatch

class ValidateAccommodationAnalysisResultUseCase {

    operator fun invoke(
        request: AccommodationAnalysisRequest,
        result: AccommodationAnalysisResult.Completed,
    ): ValidationResult {
        val expectedIds = request.candidates.map { candidate -> candidate.ephemeralCandidateId }
        val resultIds = result.decisions.map { decision -> decision.ephemeralCandidateId }
        val issues = linkedSetOf<Issue>()

        if (expectedIds.any(String::isBlank) || expectedIds.distinct().size != expectedIds.size) {
            issues += Issue.INVALID_REQUEST_CANDIDATE_IDS
        }
        if (resultIds.distinct().size != resultIds.size) {
            issues += Issue.DUPLICATE_RESULTS
        }
        if (resultIds.any { candidateId -> candidateId !in expectedIds }) {
            issues += Issue.UNKNOWN_CANDIDATE_RESULTS
        }
        if (expectedIds.any { candidateId -> candidateId !in resultIds }) {
            issues += Issue.MISSING_CANDIDATE_RESULTS
        }
        if (issues.isNotEmpty()) {
            return ValidationResult.Rejected(issues)
        }

        val matches = result.decisions.associate { decision ->
            val positiveEvidence = decision.evidence.filter { evidence ->
                evidence.signal.positive
            }
            val positiveSignals = positiveEvidence.map { evidence -> evidence.signal }.toSet()
            val normalizedVerdict = decision.verdict.normalized(
                hasExplicitLabel = AccommodationAnalysisResult.Signal.EXPLICIT_GLAMPING_LABEL in
                    positiveSignals,
                positiveSignalCount = positiveSignals.size,
                hasNegativeSignal = decision.evidence.any { evidence -> !evidence.signal.positive },
            )
            decision.ephemeralCandidateId to AccommodationSemanticMatch(
                concept = request.concept,
                verdict = normalizedVerdict,
                evidenceSources = if (
                    normalizedVerdict in VISIBLE_VERDICTS
                ) {
                    positiveEvidence.map { evidence -> evidence.source }.toSet()
                } else {
                    emptySet()
                },
            )
        }
        return ValidationResult.Accepted(matches)
    }

    private fun AccommodationMatchVerdict.normalized(
        hasExplicitLabel: Boolean,
        positiveSignalCount: Int,
        hasNegativeSignal: Boolean,
    ): AccommodationMatchVerdict =
        when (this) {
            AccommodationMatchVerdict.MATCH -> when {
                hasNegativeSignal && positiveSignalCount >= 1 ->
                    AccommodationMatchVerdict.PROBABLE
                hasExplicitLabel || positiveSignalCount >= MIN_SIGNALS_FOR_MATCH -> this
                positiveSignalCount == 1 -> AccommodationMatchVerdict.PROBABLE
                else -> AccommodationMatchVerdict.UNKNOWN
            }

            AccommodationMatchVerdict.PROBABLE ->
                if (positiveSignalCount >= 1) this else AccommodationMatchVerdict.UNKNOWN

            AccommodationMatchVerdict.NO_MATCH,
            AccommodationMatchVerdict.UNKNOWN,
            -> this
        }

    sealed interface ValidationResult {
        data class Accepted(
            val matchesByCandidateId: Map<String, AccommodationSemanticMatch>,
        ) : ValidationResult

        data class Rejected(
            val issues: Set<Issue>,
        ) : ValidationResult
    }

    enum class Issue {
        INVALID_REQUEST_CANDIDATE_IDS,
        DUPLICATE_RESULTS,
        UNKNOWN_CANDIDATE_RESULTS,
        MISSING_CANDIDATE_RESULTS,
    }

    private companion object {
        const val MIN_SIGNALS_FOR_MATCH = 2
        val VISIBLE_VERDICTS = setOf(
            AccommodationMatchVerdict.MATCH,
            AccommodationMatchVerdict.PROBABLE,
        )
    }
}
