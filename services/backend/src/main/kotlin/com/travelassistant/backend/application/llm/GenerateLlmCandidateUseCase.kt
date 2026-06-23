package com.travelassistant.backend.application.llm

class GenerateLlmCandidateUseCase(
    private val llmClient: LlmClient,
    private val validator: LlmCandidateValidator = LlmCandidateValidator(),
) {

    operator fun invoke(request: LlmCandidateRequest): LlmCandidateValidationResult {
        val response = try {
            llmClient.generateCandidate(request)
        } catch (failure: RuntimeException) {
            LlmClientResponse.Failure
        }

        return validator.validate(response)
    }
}
