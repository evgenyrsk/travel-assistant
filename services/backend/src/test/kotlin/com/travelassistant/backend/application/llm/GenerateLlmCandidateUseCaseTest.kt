package com.travelassistant.backend.application.llm

import com.travelassistant.backend.infrastructure.llm.FakeLlmClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GenerateLlmCandidateUseCaseTest {

    @Test
    fun acceptsValidLlmCandidate() {
        val candidate = hotelSearchCandidate()
        val useCase = GenerateLlmCandidateUseCase(
            llmClient = FakeLlmClient(LlmClientResponse.Candidate(candidate)),
        )

        val result = useCase(safeRequest())

        assertEquals(
            LlmCandidateValidationResult.Accepted(candidate),
            result,
        )
    }

    @Test
    fun returnsSafeFallbackForInvalidCandidate() {
        val invalidCandidate = LlmCandidate(
            outcome = LlmCandidate.Outcome.INTERPRETED,
            intent = LlmCandidate.Intent.HOTEL_SEARCH,
            extractedConstraints = mapOf("destination" to " "),
        )
        val useCase = GenerateLlmCandidateUseCase(
            llmClient = FakeLlmClient(LlmClientResponse.Candidate(invalidCandidate)),
        )

        val rejected = assertIs<LlmCandidateValidationResult.Rejected>(
            useCase(safeRequest()),
        )

        assertEquals(LlmCandidateValidationResult.Reason.INVALID_CANDIDATE, rejected.reason)
        assertEquals(
            LlmCandidateValidationResult.FallbackAction.ASK_CLARIFICATION,
            rejected.fallbackAction,
        )
    }

    @Test
    fun returnsSafeFallbackForEmptyCandidateResponse() {
        val useCase = GenerateLlmCandidateUseCase(
            llmClient = FakeLlmClient(LlmClientResponse.Empty),
        )

        val rejected = assertIs<LlmCandidateValidationResult.Rejected>(
            useCase(safeRequest()),
        )

        assertEquals(LlmCandidateValidationResult.Reason.EMPTY_RESPONSE, rejected.reason)
        assertEquals(
            LlmCandidateValidationResult.FallbackAction.ASK_CLARIFICATION,
            rejected.fallbackAction,
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
        val useCase = GenerateLlmCandidateUseCase(
            llmClient = FakeLlmClient(LlmClientResponse.Candidate(candidate)),
        )

        val result = useCase(safeRequest())

        assertEquals(
            LlmCandidateValidationResult.Accepted(candidate),
            result,
        )
    }

    @Test
    fun returnsSafeFallbackForClientFailureResponse() {
        val useCase = GenerateLlmCandidateUseCase(
            llmClient = FakeLlmClient(LlmClientResponse.Failure),
        )

        val rejected = assertIs<LlmCandidateValidationResult.Rejected>(
            useCase(safeRequest()),
        )

        assertEquals(LlmCandidateValidationResult.Reason.CLIENT_FAILURE, rejected.reason)
        assertEquals(
            LlmCandidateValidationResult.FallbackAction.ASK_CLARIFICATION,
            rejected.fallbackAction,
        )
    }

    @Test
    fun convertsUnexpectedClientExceptionToSafeFallback() {
        val useCase = GenerateLlmCandidateUseCase(
            llmClient = LlmClient {
                throw IllegalStateException("LLM candidate generation failed")
            },
        )

        val rejected = assertIs<LlmCandidateValidationResult.Rejected>(
            useCase(safeRequest()),
        )

        assertEquals(LlmCandidateValidationResult.Reason.CLIENT_FAILURE, rejected.reason)
        assertEquals(
            LlmCandidateValidationResult.FallbackAction.ASK_CLARIFICATION,
            rejected.fallbackAction,
        )
    }

    @Test
    fun remainsDeterministicWithFakeLlmClient() {
        val candidate = hotelSearchCandidate()
        val useCase = GenerateLlmCandidateUseCase(
            llmClient = FakeLlmClient(LlmClientResponse.Candidate(candidate)),
        )

        val firstResult = useCase(safeRequest())
        val secondResult = useCase(safeRequest())

        assertEquals(firstResult, secondResult)
    }

    private fun safeRequest(): LlmCandidateRequest =
        LlmCandidateRequest(
            userMessage = "Find a hotel in Rome for two adults.",
            confirmedConstraints = mapOf("destination" to "Rome"),
            missingRequiredFields = listOf("stay_dates"),
        )

    private fun hotelSearchCandidate(): LlmCandidate =
        LlmCandidate(
            outcome = LlmCandidate.Outcome.INTERPRETED,
            intent = LlmCandidate.Intent.HOTEL_SEARCH,
            extractedConstraints = mapOf("destination" to "Rome"),
        )
}
