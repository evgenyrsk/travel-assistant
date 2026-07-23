package com.travelassistant.backend.application.llm

import com.travelassistant.backend.infrastructure.llm.FakeLlmClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class GenerateLlmCandidateUseCaseTest {

    @Test
    fun acceptsValidLlmCandidate() = runBlocking {
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
    fun returnsSafeFallbackForInvalidCandidate() = runBlocking {
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
    fun returnsSafeFallbackForEmptyCandidateResponse() = runBlocking {
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
    fun acceptsAmbiguousCandidateWithClarificationQuestion() = runBlocking {
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
    fun returnsSafeFallbackForClientFailureResponse() = runBlocking {
        var requestCount = 0
        val useCase = GenerateLlmCandidateUseCase(
            llmClient = LlmClient {
                requestCount += 1
                LlmClientResponse.Failure
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
        assertEquals(1, requestCount)
    }

    @Test
    fun convertsUnexpectedClientExceptionToSafeFallback() = runBlocking {
        var requestCount = 0
        val useCase = GenerateLlmCandidateUseCase(
            llmClient = LlmClient {
                requestCount += 1
                throw IllegalStateException("LLM candidate generation failed")
            },
            retryPolicy = LlmCandidateRetryPolicy.SINGLE_RETRY,
        )

        val rejected = assertIs<LlmCandidateValidationResult.Rejected>(
            useCase(safeRequest()),
        )

        assertEquals(LlmCandidateValidationResult.Reason.CLIENT_FAILURE, rejected.reason)
        assertEquals(1, requestCount)
        assertEquals(
            LlmCandidateValidationResult.FallbackAction.ASK_CLARIFICATION,
            rejected.fallbackAction,
        )
    }

    @Test
    fun cancellationFromClientIsPropagated() = runBlocking {
        var requestCount = 0
        val useCase = GenerateLlmCandidateUseCase(
            llmClient = LlmClient {
                requestCount += 1
                throw CancellationException("LLM candidate generation cancelled")
            },
            retryPolicy = LlmCandidateRetryPolicy.SINGLE_RETRY,
        )

        assertFailsWith<CancellationException> {
            useCase(safeRequest())
        }
        assertEquals(1, requestCount)
    }

    @Test
    fun retriesRetryableClientFailureOnceAndAcceptsSecondCandidate() = runBlocking {
        val candidate = hotelSearchCandidate()
        val responses = listOf(
            LlmClientResponse.RetryableFailure(
                LlmClientRetryableFailureReason.CLIENT_FAILURE,
            ),
            LlmClientResponse.Candidate(candidate),
        )
        var requestCount = 0
        val useCase = GenerateLlmCandidateUseCase(
            llmClient = LlmClient { responses[requestCount++] },
            retryPolicy = LlmCandidateRetryPolicy.SINGLE_RETRY,
        )

        assertEquals(
            LlmCandidateValidationResult.Accepted(candidate),
            useCase(safeRequest()),
        )
        assertEquals(2, requestCount)
    }

    @Test
    fun retriesRetryableEmptyResponseOnceAndAcceptsSecondCandidate() = runBlocking {
        val candidate = hotelSearchCandidate()
        val responses = listOf(
            LlmClientResponse.RetryableFailure(
                LlmClientRetryableFailureReason.EMPTY_RESPONSE,
            ),
            LlmClientResponse.Candidate(candidate),
        )
        var requestCount = 0
        val useCase = GenerateLlmCandidateUseCase(
            llmClient = LlmClient { responses[requestCount++] },
            retryPolicy = LlmCandidateRetryPolicy.SINGLE_RETRY,
        )

        assertEquals(
            LlmCandidateValidationResult.Accepted(candidate),
            useCase(safeRequest()),
        )
        assertEquals(2, requestCount)
    }

    @Test
    fun retriesSemanticallyInvalidCandidateOnceAndAcceptsSecondCandidate() = runBlocking {
        val invalidCandidate = LlmCandidate(
            outcome = LlmCandidate.Outcome.INTERPRETED,
            intent = LlmCandidate.Intent.HOTEL_SEARCH,
            extractedConstraints = mapOf("destination" to " "),
        )
        val candidate = hotelSearchCandidate()
        val responses = listOf(
            LlmClientResponse.Candidate(invalidCandidate),
            LlmClientResponse.Candidate(candidate),
        )
        var requestCount = 0
        val useCase = GenerateLlmCandidateUseCase(
            llmClient = LlmClient { responses[requestCount++] },
            retryPolicy = LlmCandidateRetryPolicy.SINGLE_RETRY,
        )

        assertEquals(
            LlmCandidateValidationResult.Accepted(candidate),
            useCase(safeRequest()),
        )
        assertEquals(2, requestCount)
    }

    @Test
    fun preservesSemanticRejectionWhenRetryReturnsEmptyContent() = runBlocking {
        val invalidCandidate = LlmCandidate(
            outcome = LlmCandidate.Outcome.INTERPRETED,
            intent = LlmCandidate.Intent.HOTEL_SEARCH,
            extractedConstraints = mapOf("destination" to " "),
        )
        val responses = listOf(
            LlmClientResponse.Candidate(invalidCandidate),
            LlmClientResponse.Empty,
        )
        var requestCount = 0
        val useCase = GenerateLlmCandidateUseCase(
            llmClient = LlmClient { responses[requestCount++] },
            retryPolicy = LlmCandidateRetryPolicy.SINGLE_RETRY,
        )

        val rejected = assertIs<LlmCandidateValidationResult.Rejected>(
            useCase(safeRequest()),
        )

        assertEquals(LlmCandidateValidationResult.Reason.INVALID_CANDIDATE, rejected.reason)
        assertEquals(2, requestCount)
    }

    @Test
    fun stopsAfterTwoRetryableFailures() = runBlocking {
        var requestCount = 0
        val useCase = GenerateLlmCandidateUseCase(
            llmClient = LlmClient {
                requestCount += 1
                LlmClientResponse.RetryableFailure(
                    LlmClientRetryableFailureReason.CLIENT_FAILURE,
                )
            },
            retryPolicy = LlmCandidateRetryPolicy.SINGLE_RETRY,
        )

        val rejected = assertIs<LlmCandidateValidationResult.Rejected>(
            useCase(safeRequest()),
        )

        assertEquals(LlmCandidateValidationResult.Reason.CLIENT_FAILURE, rejected.reason)
        assertEquals(2, requestCount)
    }

    @Test
    fun defaultPolicyDoesNotRetryInvalidCandidate() = runBlocking {
        var requestCount = 0
        val invalidCandidate = LlmCandidate(
            outcome = LlmCandidate.Outcome.INTERPRETED,
            intent = LlmCandidate.Intent.HOTEL_SEARCH,
            extractedConstraints = mapOf("destination" to " "),
        )
        val useCase = GenerateLlmCandidateUseCase(
            llmClient = LlmClient {
                requestCount += 1
                LlmClientResponse.Candidate(invalidCandidate)
            },
        )

        val rejected = assertIs<LlmCandidateValidationResult.Rejected>(
            useCase(safeRequest()),
        )

        assertEquals(LlmCandidateValidationResult.Reason.INVALID_CANDIDATE, rejected.reason)
        assertEquals(1, requestCount)
    }

    @Test
    fun cancellationFromSecondAttemptIsPropagated() = runBlocking {
        var requestCount = 0
        val useCase = GenerateLlmCandidateUseCase(
            llmClient = LlmClient {
                requestCount += 1
                if (requestCount == 1) {
                    LlmClientResponse.RetryableFailure(
                        LlmClientRetryableFailureReason.CLIENT_FAILURE,
                    )
                } else {
                    throw CancellationException("Second LLM attempt cancelled")
                }
            },
            retryPolicy = LlmCandidateRetryPolicy.SINGLE_RETRY,
        )

        assertFailsWith<CancellationException> {
            useCase(safeRequest())
        }
        assertEquals(2, requestCount)
    }

    @Test
    fun remainsDeterministicWithFakeLlmClient() = runBlocking {
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
