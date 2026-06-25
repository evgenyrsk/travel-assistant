package com.travelassistant.backend.application.assistant

sealed interface ProceedWithCandidateConfirmationPlan {
    data class ConfirmationRequired(
        val proposal: ProceedWithCandidateConfirmationProposal,
    ) : ProceedWithCandidateConfirmationPlan

    data class ClarificationRequired(
        val question: String,
        val reason: ClarificationReason,
    ) : ProceedWithCandidateConfirmationPlan

    data class Fallback(
        val reason: FallbackReason,
    ) : ProceedWithCandidateConfirmationPlan

    enum class ClarificationReason {
        CANDIDATE_CLARIFICATION_REQUESTED,
        MISSING_OR_INVALID_CRITERIA,
    }

    enum class FallbackReason {
        UNSUPPORTED_INTENT,
        UNSAFE_OR_UNSUPPORTED_OUTCOME,
        CONFLICTS_OR_WARNINGS,
    }
}
