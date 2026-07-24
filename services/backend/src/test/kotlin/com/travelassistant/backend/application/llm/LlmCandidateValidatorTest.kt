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
    fun acceptsValidTypedPreferencePatchWithoutMakingPreferencesRequired() {
        val candidate = LlmCandidate(
            outcome = LlmCandidate.Outcome.INTERPRETED,
            intent = LlmCandidate.Intent.HOTEL_SEARCH,
            extractedConstraints = mapOf("destination" to "Rome"),
            preferencePatch = LlmHotelSearchPreferencesPatch(
                maxTotalPrice = LlmHotelSearchPreferencesPatch.MaxTotalPrice("80000"),
                stars = setOf(4, 5),
                minimumGuestRating = 8,
                freeCancellationRequired = true,
            ),
        )

        assertEquals(
            LlmCandidateValidationResult.Accepted(candidate),
            validator.validate(LlmClientResponse.Candidate(candidate)),
        )
    }

    @Test
    fun rejectsInvalidOrConflictingPreferencePatch() {
        val invalidPatches = listOf(
            LlmHotelSearchPreferencesPatch(
                maxTotalPrice = LlmHotelSearchPreferencesPatch.MaxTotalPrice("0"),
            ),
            LlmHotelSearchPreferencesPatch(stars = setOf(4, 6)),
            LlmHotelSearchPreferencesPatch(minimumGuestRating = 10),
            LlmHotelSearchPreferencesPatch(freeCancellationRequired = false),
            LlmHotelSearchPreferencesPatch(breakfastIncludedRequired = false),
            LlmHotelSearchPreferencesPatch(
                minimumGuestRating = 8,
                clear = setOf(LlmHotelSearchPreferencesPatch.Field.MINIMUM_GUEST_RATING),
            ),
        )

        invalidPatches.forEach { patch ->
            val result = assertIs<LlmCandidateValidationResult.Rejected>(
                validator.validate(
                    LlmClientResponse.Candidate(
                        LlmCandidate(
                            outcome = LlmCandidate.Outcome.INTERPRETED,
                            intent = LlmCandidate.Intent.HOTEL_SEARCH,
                            extractedConstraints = mapOf("destination" to "Rome"),
                            preferencePatch = patch,
                        ),
                    ),
                ),
            )

            assertEquals(LlmCandidateValidationResult.Reason.INVALID_CANDIDATE, result.reason)
        }
    }

    @Test
    fun rejectsPreferenceAsMissingRequiredField() {
        val result = assertIs<LlmCandidateValidationResult.Rejected>(
            validator.validate(
                LlmClientResponse.Candidate(
                    LlmCandidate(
                        outcome = LlmCandidate.Outcome.NEEDS_CLARIFICATION,
                        intent = LlmCandidate.Intent.HOTEL_SEARCH,
                        missingRequiredFields = listOf("min-guest-rating"),
                        clarificationQuestion = "Какой рейтинг нужен?",
                    ),
                ),
            ),
        )

        assertEquals(LlmCandidateValidationResult.Reason.INVALID_CANDIDATE, result.reason)
    }

    @Test
    fun rejectsPreferencePatchForUnsupportedIntent() {
        val result = assertIs<LlmCandidateValidationResult.Rejected>(
            validator.validate(
                LlmClientResponse.Candidate(
                    LlmCandidate(
                        outcome = LlmCandidate.Outcome.UNSUPPORTED,
                        intent = LlmCandidate.Intent.UNSUPPORTED,
                        preferencePatch = LlmHotelSearchPreferencesPatch(stars = setOf(5)),
                    ),
                ),
            ),
        )

        assertEquals(LlmCandidateValidationResult.Reason.INVALID_CANDIDATE, result.reason)
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

    @Test
    fun mapsRetryableFailuresToExistingSafeReasons() {
        val cases = listOf(
            LlmClientRetryableFailureReason.EMPTY_RESPONSE to
                LlmCandidateValidationResult.Reason.EMPTY_RESPONSE,
            LlmClientRetryableFailureReason.CLIENT_FAILURE to
                LlmCandidateValidationResult.Reason.CLIENT_FAILURE,
            LlmClientRetryableFailureReason.INVALID_CANDIDATE to
                LlmCandidateValidationResult.Reason.INVALID_CANDIDATE,
        )

        cases.forEach { (clientReason, validationReason) ->
            val rejected = assertIs<LlmCandidateValidationResult.Rejected>(
                validator.validate(LlmClientResponse.RetryableFailure(clientReason)),
            )

            assertEquals(validationReason, rejected.reason)
        }
    }
}
