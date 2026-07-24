package com.travelassistant.backend.application.llm

import com.travelassistant.backend.application.observability.OperationalComponent
import com.travelassistant.backend.application.observability.OperationalDependency
import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventName
import com.travelassistant.backend.application.observability.OperationalEventSink
import com.travelassistant.backend.application.observability.OperationalLevel
import com.travelassistant.backend.application.observability.OperationalOperation
import com.travelassistant.backend.application.observability.OperationalOutcome
import com.travelassistant.backend.application.observability.recordSafely
import kotlinx.coroutines.CancellationException
import kotlin.math.max

class GenerateLlmCandidateUseCase(
    private val llmClient: LlmClient,
    private val validator: LlmCandidateValidator = LlmCandidateValidator(),
    private val retryPolicy: LlmCandidateRetryPolicy = LlmCandidateRetryPolicy.NO_RETRY,
    private val eventSink: OperationalEventSink = OperationalEventSink.NONE,
) {

    suspend operator fun invoke(request: LlmCandidateRequest): LlmCandidateValidationResult {
        var attemptNumber = 1
        var semanticRejection: LlmCandidateValidationResult.Rejected? = null

        while (true) {
            val startedAt = System.nanoTime()
            val response = generateCandidate(request)
            val validationResult = validator.validate(response)
            eventSink.recordSafely(
                OperationalEvent(
                    name = OperationalEventName.DEPENDENCY_CALL_COMPLETED,
                    component = OperationalComponent.LLM,
                    level = validationResult.toOperationalLevel(),
                    operation = OperationalOperation.GENERATE_LLM_CANDIDATE,
                    dependency = OperationalDependency.LLM,
                    outcome = validationResult.toOperationalOutcome(),
                    durationMillis = elapsedMillis(startedAt),
                ),
            )
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

    private fun LlmCandidateValidationResult.toOperationalLevel(): OperationalLevel =
        if (this is LlmCandidateValidationResult.Accepted) {
            OperationalLevel.INFO
        } else {
            OperationalLevel.WARNING
        }

    private fun LlmCandidateValidationResult.toOperationalOutcome(): OperationalOutcome =
        when (this) {
            is LlmCandidateValidationResult.Accepted -> OperationalOutcome.SUCCEEDED
            is LlmCandidateValidationResult.Rejected -> when (reason) {
                LlmCandidateValidationResult.Reason.EMPTY_RESPONSE -> OperationalOutcome.FAILED
                LlmCandidateValidationResult.Reason.CLIENT_FAILURE -> OperationalOutcome.CLIENT_FAILURE
                LlmCandidateValidationResult.Reason.INVALID_CANDIDATE ->
                    OperationalOutcome.INVALID_CANDIDATE
            }
        }

    private fun elapsedMillis(startedAtNanos: Long): Long =
        max(0, (System.nanoTime() - startedAtNanos) / NANOS_PER_MILLISECOND)

    private companion object {
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
