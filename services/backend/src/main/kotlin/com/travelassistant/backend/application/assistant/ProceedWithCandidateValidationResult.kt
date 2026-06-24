package com.travelassistant.backend.application.assistant

sealed interface ProceedWithCandidateValidationResult {
    data class Accepted(
        val criteria: ProceedWithCandidateCriteria,
    ) : ProceedWithCandidateValidationResult

    data class Rejected(
        val issues: Set<ProceedWithCandidateValidationIssue>,
        val clarificationHint: String? = null,
    ) : ProceedWithCandidateValidationResult
}
