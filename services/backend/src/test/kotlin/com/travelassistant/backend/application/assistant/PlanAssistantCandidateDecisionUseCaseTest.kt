package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmCandidateValidationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PlanAssistantCandidateDecisionUseCaseTest {

    private val planner = PlanAssistantCandidateDecisionUseCase()

    @Test
    fun turnsAcceptedCandidateIntoProceedDecision() {
        val candidate = interpretedHotelSearchCandidate()

        val decision = planner(LlmCandidateValidationResult.Accepted(candidate))

        assertEquals(
            AssistantCandidateDecision.ProceedWithCandidate(candidate),
            decision,
        )
    }

    @Test
    fun turnsAcceptedClarificationCandidateIntoAskClarificationDecision() {
        val candidate = LlmCandidate(
            outcome = LlmCandidate.Outcome.NEEDS_CLARIFICATION,
            intent = LlmCandidate.Intent.HOTEL_SEARCH,
            missingRequiredFields = listOf("stay_dates"),
            clarificationQuestion = "What are your stay dates?",
        )

        val decision = planner(LlmCandidateValidationResult.Accepted(candidate))

        assertEquals(
            AssistantCandidateDecision.AskClarification("What are your stay dates?"),
            decision,
        )
    }

    @Test
    fun turnsRejectedResultWithClarificationQuestionIntoAskClarificationDecision() {
        val rejected = LlmCandidateValidationResult.Rejected(
            reason = LlmCandidateValidationResult.Reason.INVALID_CANDIDATE,
            clarificationQuestion = "Could you clarify your destination?",
        )

        val decision = planner(rejected)

        assertEquals(
            AssistantCandidateDecision.AskClarification("Could you clarify your destination?"),
            decision,
        )
    }

    @Test
    fun turnsRejectedResultWithoutClarificationQuestionIntoSafeFallback() {
        val rejected = LlmCandidateValidationResult.Rejected(
            reason = LlmCandidateValidationResult.Reason.EMPTY_RESPONSE,
        )

        val decision = planner(rejected)

        assertEquals(
            AssistantCandidateDecision.Fallback(AssistantCandidateDecision.FallbackReason.EMPTY_RESPONSE),
            decision,
        )
    }

    @Test
    fun keepsFailureLikeResultInsideSafeFallback() {
        val rejected = LlmCandidateValidationResult.Rejected(
            reason = LlmCandidateValidationResult.Reason.CLIENT_FAILURE,
        )

        val decision = assertIs<AssistantCandidateDecision.Fallback>(
            planner(rejected),
        )

        assertEquals(AssistantCandidateDecision.FallbackReason.CLIENT_FAILURE, decision.reason)
    }

    @Test
    fun returnsSafeFallbackForAcceptedUnsupportedCandidate() {
        val candidate = LlmCandidate(
            outcome = LlmCandidate.Outcome.UNSUPPORTED,
            intent = LlmCandidate.Intent.UNSUPPORTED,
        )

        val decision = planner(LlmCandidateValidationResult.Accepted(candidate))

        assertEquals(
            AssistantCandidateDecision.Fallback(AssistantCandidateDecision.FallbackReason.UNSUPPORTED_INTENT),
            decision,
        )
    }

    @Test
    fun remainsDeterministicForSameValidationResult() {
        val result = LlmCandidateValidationResult.Accepted(interpretedHotelSearchCandidate())

        val firstDecision = planner(result)
        val secondDecision = planner(result)

        assertEquals(firstDecision, secondDecision)
    }

    @Test
    fun plansDecisionWithoutExternalDependencies() {
        val localPlanner = PlanAssistantCandidateDecisionUseCase()
        val rejected = LlmCandidateValidationResult.Rejected(
            reason = LlmCandidateValidationResult.Reason.INVALID_CANDIDATE,
        )

        val decision = localPlanner(rejected)

        assertEquals(
            AssistantCandidateDecision.Fallback(AssistantCandidateDecision.FallbackReason.INVALID_CANDIDATE),
            decision,
        )
    }

    private fun interpretedHotelSearchCandidate(): LlmCandidate =
        LlmCandidate(
            outcome = LlmCandidate.Outcome.INTERPRETED,
            intent = LlmCandidate.Intent.HOTEL_SEARCH,
            extractedConstraints = mapOf("destination" to "Rome"),
        )
}
