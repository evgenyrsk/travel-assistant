package com.travelassistant.backend.application.llm

import kotlinx.coroutines.CancellationException

class GenerateLlmCandidateUseCase(
    private val llmClient: LlmClient,
    private val validator: LlmCandidateValidator = LlmCandidateValidator(),
    private val retryPolicy: LlmCandidateRetryPolicy = LlmCandidateRetryPolicy.NO_RETRY,
) {

    suspend operator fun invoke(request: LlmCandidateRequest): LlmCandidateValidationResult {
        var attemptNumber = 1

        while (true) {
            val response = generateCandidate(request)
            val validationResult = validator.validate(response)

            if (!retryPolicy.shouldRetry(attemptNumber, response, validationResult)) {
                return validationResult
            }

            attemptNumber += 1
        }
    }

    private suspend fun generateCandidate(request: LlmCandidateRequest): LlmClientResponse =
        try {
            llmClient.generateCandidate(request)
        } catch (error: CancellationException) {
            throw error
        } catch (_: RuntimeException) {
            LlmClientResponse.Failure
        }
}
