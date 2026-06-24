package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmCandidateValidationResult

class PlanAssistantCandidateDecisionUseCase {

    operator fun invoke(
        validationResult: LlmCandidateValidationResult,
    ): AssistantCandidateDecision =
        when (validationResult) {
            is LlmCandidateValidationResult.Accepted -> decisionFor(validationResult.candidate)
            is LlmCandidateValidationResult.Rejected -> decisionFor(validationResult)
        }

    private fun decisionFor(candidate: LlmCandidate): AssistantCandidateDecision =
        when (candidate.outcome) {
            LlmCandidate.Outcome.INTERPRETED ->
                AssistantCandidateDecision.ProceedWithCandidate(candidate)

            LlmCandidate.Outcome.NEEDS_CLARIFICATION,
            LlmCandidate.Outcome.AMBIGUOUS ->
                candidate.clarificationQuestion
                    ?.takeIf(String::isNotBlank)
                    ?.let(AssistantCandidateDecision::AskClarification)
                    ?: AssistantCandidateDecision.Fallback(
                        AssistantCandidateDecision.FallbackReason.MISSING_CLARIFICATION,
                    )

            LlmCandidate.Outcome.UNSUPPORTED ->
                AssistantCandidateDecision.Fallback(
                    AssistantCandidateDecision.FallbackReason.UNSUPPORTED_INTENT,
                )
        }

    private fun decisionFor(
        rejected: LlmCandidateValidationResult.Rejected,
    ): AssistantCandidateDecision =
        rejected.clarificationQuestion
            ?.takeIf(String::isNotBlank)
            ?.let(AssistantCandidateDecision::AskClarification)
            ?: AssistantCandidateDecision.fallbackFor(rejected.reason)
}
