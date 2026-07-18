package com.travelassistant.backend.application.llm

import kotlinx.coroutines.CancellationException

class GenerateLlmCandidateUseCase(
    private val llmClient: LlmClient,
    private val validator: LlmCandidateValidator = LlmCandidateValidator(),
) {

    suspend operator fun invoke(request: LlmCandidateRequest): LlmCandidateValidationResult {
        val response = try {
            llmClient.generateCandidate(request)
        } catch (error: CancellationException) {
            throw error
        } catch (failure: RuntimeException) {
            LlmClientResponse.Failure
        }

        return validator.validate(response)
    }
}
