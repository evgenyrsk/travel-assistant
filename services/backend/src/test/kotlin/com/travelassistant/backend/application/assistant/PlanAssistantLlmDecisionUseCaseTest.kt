package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.llm.GenerateLlmCandidateUseCase
import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmCandidateRequest
import com.travelassistant.backend.application.llm.LlmCandidateValidationResult
import com.travelassistant.backend.application.llm.LlmClientResponse
import com.travelassistant.backend.infrastructure.llm.FakeLlmClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class PlanAssistantLlmDecisionUseCaseTest {

    @Test
    fun returnsProceedDecisionForValidCandidate() = runBlocking {
        val candidate = interpretedHotelSearchCandidate()
        val pipeline = pipelineFor(LlmClientResponse.Candidate(candidate))

        val decision = pipeline(safeRequest())

        assertEquals(
            AssistantCandidateDecision.ProceedWithCandidate(candidate),
            decision,
        )
    }

    @Test
    fun returnsAskClarificationDecisionForClarificationCandidate() = runBlocking {
        val candidate = LlmCandidate(
            outcome = LlmCandidate.Outcome.NEEDS_CLARIFICATION,
            intent = LlmCandidate.Intent.HOTEL_SEARCH,
            missingRequiredFields = listOf("stay_dates"),
            clarificationQuestion = "What are your stay dates?",
        )
        val pipeline = pipelineFor(LlmClientResponse.Candidate(candidate))

        val decision = pipeline(safeRequest())

        assertEquals(
            AssistantCandidateDecision.AskClarification(
                question = "What are your stay dates?",
                candidate = candidate,
            ),
            decision,
        )
    }

    @Test
    fun returnsSafeFallbackForInvalidCandidate() = runBlocking {
        val invalidCandidate = LlmCandidate(
            outcome = LlmCandidate.Outcome.INTERPRETED,
            intent = LlmCandidate.Intent.HOTEL_SEARCH,
            extractedConstraints = mapOf("destination" to " "),
        )
        val pipeline = pipelineFor(LlmClientResponse.Candidate(invalidCandidate))

        val decision = pipeline(safeRequest())

        assertEquals(
            AssistantCandidateDecision.Fallback(AssistantCandidateDecision.FallbackReason.INVALID_CANDIDATE),
            decision,
        )
    }

    @Test
    fun returnsSafeFallbackForEmptyCandidateResponse() = runBlocking {
        val pipeline = pipelineFor(LlmClientResponse.Empty)

        val decision = pipeline(safeRequest())

        assertEquals(
            AssistantCandidateDecision.Fallback(AssistantCandidateDecision.FallbackReason.EMPTY_RESPONSE),
            decision,
        )
    }

    @Test
    fun returnsSafeFallbackForFakeLlmFailure() = runBlocking {
        val pipeline = pipelineFor(LlmClientResponse.Failure)

        val decision = pipeline(safeRequest())

        assertEquals(
            AssistantCandidateDecision.Fallback(AssistantCandidateDecision.FallbackReason.CLIENT_FAILURE),
            decision,
        )
    }

    @Test
    fun keepsUnexpectedInternalExceptionInsideSafeFallback() = runBlocking {
        val pipeline = PlanAssistantLlmDecisionUseCase.fromSteps(
            generateCandidate = {
                LlmCandidateValidationResult.Accepted(interpretedHotelSearchCandidate())
            },
            planDecision = {
                throw IllegalStateException("decision planning failed")
            },
        )

        val decision = assertIs<AssistantCandidateDecision.Fallback>(
            pipeline(safeRequest()),
        )

        assertEquals(AssistantCandidateDecision.FallbackReason.CLIENT_FAILURE, decision.reason)
    }

    @Test
    fun cancellationFromGenerationIsPropagated() = runBlocking {
        val pipeline = PlanAssistantLlmDecisionUseCase.fromSteps(
            generateCandidate = {
                throw CancellationException("LLM decision planning cancelled")
            },
            planDecision = {
                error("Decision planning must not run after cancellation")
            },
        )

        assertFailsWith<CancellationException> {
            pipeline(safeRequest())
        }
    }

    @Test
    fun remainsDeterministicWithFakeLlmClient() = runBlocking {
        val candidate = interpretedHotelSearchCandidate()
        val pipeline = pipelineFor(LlmClientResponse.Candidate(candidate))

        val firstDecision = pipeline(safeRequest())
        val secondDecision = pipeline(safeRequest())

        assertEquals(firstDecision, secondDecision)
    }

    @Test
    fun plansDecisionWithoutExternalDependencies() = runBlocking {
        val pipeline = pipelineFor(LlmClientResponse.Empty)

        val decision = pipeline(safeRequest())

        assertEquals(
            AssistantCandidateDecision.Fallback(AssistantCandidateDecision.FallbackReason.EMPTY_RESPONSE),
            decision,
        )
    }

    private fun pipelineFor(response: LlmClientResponse): PlanAssistantLlmDecisionUseCase =
        PlanAssistantLlmDecisionUseCase(
            generateLlmCandidateUseCase = GenerateLlmCandidateUseCase(
                llmClient = FakeLlmClient(response),
            ),
        )

    private fun safeRequest(): LlmCandidateRequest =
        LlmCandidateRequest(
            userMessage = "Find a hotel in Rome for two adults.",
            confirmedConstraints = mapOf("destination" to "Rome"),
            missingRequiredFields = listOf("stay_dates"),
        )

    private fun interpretedHotelSearchCandidate(): LlmCandidate =
        LlmCandidate(
            outcome = LlmCandidate.Outcome.INTERPRETED,
            intent = LlmCandidate.Intent.HOTEL_SEARCH,
            extractedConstraints = mapOf("destination" to "Rome"),
        )
}
