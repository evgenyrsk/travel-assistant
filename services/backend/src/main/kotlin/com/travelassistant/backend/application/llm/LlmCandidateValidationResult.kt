package com.travelassistant.backend.application.llm

sealed interface LlmCandidateValidationResult {
    data class Accepted(
        val candidate: LlmCandidate,
    ) : LlmCandidateValidationResult

    data class Rejected(
        val reason: Reason,
        val fallbackAction: FallbackAction = FallbackAction.ASK_CLARIFICATION,
        val clarificationQuestion: String? = null,
    ) : LlmCandidateValidationResult

    enum class Reason {
        EMPTY_RESPONSE,
        CLIENT_FAILURE,
        INVALID_CANDIDATE,
    }

    enum class FallbackAction {
        ASK_CLARIFICATION,
    }
}
