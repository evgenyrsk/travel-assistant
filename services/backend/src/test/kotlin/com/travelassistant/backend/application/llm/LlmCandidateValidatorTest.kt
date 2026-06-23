package com.travelassistant.backend.application.llm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LlmCandidateValidatorTest {

    private val validator = LlmCandidateValidator()

    @Test
    fun acceptsValidHotelSearchCandidate() {
        val candidate = LlmCandidate(
            outcome = LlmCandidate.Outcome.INTERPRETED,
            intent = LlmCandidate.Intent.HOTEL_SEARCH,
            extractedConstraints = mapOf("destination" to "Rome"),
        )

        val result = validator.validate(LlmClientResponse.Candidate(candidate))

        assertEquals(
            LlmCandidateValidationResult.Accepted(candidate),
            result,
        )
    }

    @Test
    fun acceptsAmbiguousCandidateWithClarificationQuestion() {
        val candidate = LlmCandidate(
            outcome = LlmCandidate.Outcome.AMBIGUOUS,
            intent = LlmCandidate.Intent.UNKNOWN,
            conflicts = listOf("destination"),
            clarificationQuestion = "Which destination should I use?",
        )

        val result = validator.validate(LlmClientResponse.Candidate(candidate))

        assertEquals(
            LlmCandidateValidationResult.Accepted(candidate),
            result,
        )
    }

    @Test
    fun rejectsInvalidCandidateWithSafeFallback() {
        val invalidCandidate = LlmCandidate(
            outcome = LlmCandidate.Outcome.NEEDS_CLARIFICATION,
            intent = LlmCandidate.Intent.HOTEL_SEARCH,
            missingRequiredFields = listOf("stay_dates"),
            clarificationQuestion = " ",
        )

        val rejected = assertIs<LlmCandidateValidationResult.Rejected>(
            validator.validate(LlmClientResponse.Candidate(invalidCandidate)),
        )

        assertEquals(LlmCandidateValidationResult.Reason.INVALID_CANDIDATE, rejected.reason)
        assertEquals(
            LlmCandidateValidationResult.FallbackAction.ASK_CLARIFICATION,
            rejected.fallbackAction,
        )
    }

    @Test
    fun rejectsEmptyResponseWithSafeFallback() {
        val rejected = assertIs<LlmCandidateValidationResult.Rejected>(
            validator.validate(LlmClientResponse.Empty),
        )

        assertEquals(LlmCandidateValidationResult.Reason.EMPTY_RESPONSE, rejected.reason)
        assertEquals(
            LlmCandidateValidationResult.FallbackAction.ASK_CLARIFICATION,
            rejected.fallbackAction,
        )
    }

    @Test
    fun rejectsClientFailureWithSafeFallback() {
        val rejected = assertIs<LlmCandidateValidationResult.Rejected>(
            validator.validate(LlmClientResponse.Failure),
        )

        assertEquals(LlmCandidateValidationResult.Reason.CLIENT_FAILURE, rejected.reason)
        assertEquals(
            LlmCandidateValidationResult.FallbackAction.ASK_CLARIFICATION,
            rejected.fallbackAction,
        )
    }
}
