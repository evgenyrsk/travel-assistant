package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmCandidateValidationResult

sealed interface AssistantCandidateDecision {
    data class ProceedWithCandidate(
        val candidate: LlmCandidate,
    ) : AssistantCandidateDecision

    data class AskClarification(
        val question: String,
        val candidate: LlmCandidate? = null,
    ) : AssistantCandidateDecision

    data class Fallback(
        val reason: FallbackReason,
    ) : AssistantCandidateDecision

    enum class FallbackReason {
        EMPTY_RESPONSE,
        CLIENT_FAILURE,
        INVALID_CANDIDATE,
        UNSUPPORTED_INTENT,
        MISSING_CLARIFICATION,
    }

    companion object {
        fun fallbackFor(reason: LlmCandidateValidationResult.Reason): Fallback =
            Fallback(
                when (reason) {
                    LlmCandidateValidationResult.Reason.EMPTY_RESPONSE -> FallbackReason.EMPTY_RESPONSE
                    LlmCandidateValidationResult.Reason.CLIENT_FAILURE -> FallbackReason.CLIENT_FAILURE
                    LlmCandidateValidationResult.Reason.INVALID_CANDIDATE -> FallbackReason.INVALID_CANDIDATE
                },
            )
    }
}
