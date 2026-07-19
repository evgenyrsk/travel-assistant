package com.travelassistant.backend.application.llm

class LlmCandidateRetryPolicy private constructor(
    val maximumAttempts: Int,
    private val retryInvalidCandidate: Boolean,
) {

    fun shouldRetry(
        attemptNumber: Int,
        response: LlmClientResponse,
        validationResult: LlmCandidateValidationResult,
    ): Boolean {
        if (attemptNumber >= maximumAttempts) {
            return false
        }

        return when (response) {
            is LlmClientResponse.RetryableFailure -> true
            is LlmClientResponse.Candidate ->
                retryInvalidCandidate && validationResult.isInvalidCandidate()

            LlmClientResponse.Empty,
            LlmClientResponse.Failure,
            -> false
        }
    }

    private fun LlmCandidateValidationResult.isInvalidCandidate(): Boolean =
        this is LlmCandidateValidationResult.Rejected &&
            reason == LlmCandidateValidationResult.Reason.INVALID_CANDIDATE

    companion object {
        val NO_RETRY = LlmCandidateRetryPolicy(
            maximumAttempts = 1,
            retryInvalidCandidate = false,
        )

        val SINGLE_RETRY = LlmCandidateRetryPolicy(
            maximumAttempts = 2,
            retryInvalidCandidate = true,
        )
    }
}
