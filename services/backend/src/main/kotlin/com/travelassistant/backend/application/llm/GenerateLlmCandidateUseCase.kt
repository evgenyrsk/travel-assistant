package com.travelassistant.backend.application.llm

import kotlinx.coroutines.CancellationException

class GenerateLlmCandidateUseCase(
    private val llmClient: LlmClient,
    private val validator: LlmCandidateValidator = LlmCandidateValidator(),
    private val retryPolicy: LlmCandidateRetryPolicy = LlmCandidateRetryPolicy.NO_RETRY,
) {

    suspend operator fun invoke(request: LlmCandidateRequest): LlmCandidateValidationResult {
        var attemptNumber = 1
        var semanticRejection: LlmCandidateValidationResult.Rejected? = null

        while (true) {
            val response = generateCandidate(request)
            val validationResult = validator.validate(response)
            if (
                validationResult is LlmCandidateValidationResult.Rejected &&
                validationResult.reason == LlmCandidateValidationResult.Reason.INVALID_CANDIDATE
            ) {
                semanticRejection = validationResult
            }

            if (!retryPolicy.shouldRetry(attemptNumber, response, validationResult)) {
                return when {
                    validationResult is LlmCandidateValidationResult.Accepted -> validationResult
                    semanticRejection != null &&
                        validationResult is LlmCandidateValidationResult.Rejected &&
                        validationResult.reason != LlmCandidateValidationResult.Reason.INVALID_CANDIDATE ->
                        semanticRejection

                    else -> validationResult
                }
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
