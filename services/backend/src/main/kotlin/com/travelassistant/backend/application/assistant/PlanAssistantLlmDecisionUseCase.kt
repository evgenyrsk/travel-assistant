package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.llm.GenerateLlmCandidateUseCase
import com.travelassistant.backend.application.llm.LlmCandidateRequest
import com.travelassistant.backend.application.llm.LlmCandidateValidationResult

class PlanAssistantLlmDecisionUseCase private constructor(
    private val generateCandidate: (LlmCandidateRequest) -> LlmCandidateValidationResult,
    private val planDecision: (LlmCandidateValidationResult) -> AssistantCandidateDecision,
) {

    constructor(
        generateLlmCandidateUseCase: GenerateLlmCandidateUseCase,
        planAssistantCandidateDecisionUseCase: PlanAssistantCandidateDecisionUseCase =
            PlanAssistantCandidateDecisionUseCase(),
    ) : this(
        generateCandidate = generateLlmCandidateUseCase::invoke,
        planDecision = planAssistantCandidateDecisionUseCase::invoke,
    )

    operator fun invoke(request: LlmCandidateRequest): AssistantCandidateDecision =
        try {
            val validationResult = generateCandidate(request)
            planDecision(validationResult)
        } catch (failure: RuntimeException) {
            safeFallback()
        }

    private fun safeFallback(): AssistantCandidateDecision =
        AssistantCandidateDecision.Fallback(
            AssistantCandidateDecision.FallbackReason.CLIENT_FAILURE,
        )

    companion object {
        internal fun fromSteps(
            generateCandidate: (LlmCandidateRequest) -> LlmCandidateValidationResult,
            planDecision: (LlmCandidateValidationResult) -> AssistantCandidateDecision,
        ): PlanAssistantLlmDecisionUseCase =
            PlanAssistantLlmDecisionUseCase(
                generateCandidate = generateCandidate,
                planDecision = planDecision,
            )
    }
}
