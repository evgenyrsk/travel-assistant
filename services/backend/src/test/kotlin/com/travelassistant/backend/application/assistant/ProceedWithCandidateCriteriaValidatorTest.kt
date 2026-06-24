package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.llm.LlmCandidate
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProceedWithCandidateCriteriaValidatorTest {

    private val validator = ProceedWithCandidateCriteriaValidator()

    @Test
    fun acceptsCompleteSafeCandidate() {
        val result = validator(proceedWithCandidate())

        val accepted = assertIs<ProceedWithCandidateValidationResult.Accepted>(result)
        assertEquals("Rome", accepted.criteria.destination)
        assertEquals(LocalDate.parse("2026-07-01"), accepted.criteria.checkInDate)
        assertEquals(LocalDate.parse("2026-07-04"), accepted.criteria.checkOutDate)
        assertEquals(2, accepted.criteria.guests.adults)
        assertEquals(1, accepted.criteria.guests.children)
        assertEquals(1, accepted.criteria.rooms)
    }

    @Test
    fun rejectsMissingDestination() {
        val result = validateWithConstraints(
            "destination" to " ",
        )

        assertRejectedWith(result, ProceedWithCandidateValidationIssue.MISSING_DESTINATION)
    }

    @Test
    fun rejectsInvalidDateRange() {
        val result = validateWithConstraints(
            "check-out" to "2026-06-30",
        )

        assertRejectedWith(result, ProceedWithCandidateValidationIssue.INVALID_DATE_RANGE)
    }

    @Test
    fun rejectsAdultCountBelowOne() {
        val result = validateWithConstraints(
            "adults" to "0",
        )

        assertRejectedWith(result, ProceedWithCandidateValidationIssue.INVALID_ADULTS)
    }

    @Test
    fun rejectsNegativeChildrenCount() {
        val result = validateWithConstraints(
            "children" to "-1",
        )

        assertRejectedWith(result, ProceedWithCandidateValidationIssue.INVALID_CHILDREN)
    }

    @Test
    fun rejectsMissingOrInvalidRooms() {
        val missingRooms = validator(
            proceedWithCandidate(
                completeCandidate(
                    constraints = completeConstraints() - "rooms",
                ),
            ),
        )
        val invalidRooms = validateWithConstraints(
            "rooms" to "0",
        )

        assertRejectedWith(missingRooms, ProceedWithCandidateValidationIssue.MISSING_ROOMS)
        assertRejectedWith(invalidRooms, ProceedWithCandidateValidationIssue.INVALID_ROOMS)
    }

    @Test
    fun rejectsCandidateWithMissingRequiredFields() {
        val result = validator(
            proceedWithCandidate(
                completeCandidate(
                    missingRequiredFields = listOf("destination"),
                ),
            ),
        )

        assertRejectedWith(result, ProceedWithCandidateValidationIssue.MISSING_REQUIRED_FIELDS)
    }

    @Test
    fun rejectsCandidateWithConflicts() {
        val result = validator(
            proceedWithCandidate(
                completeCandidate(
                    conflicts = listOf("Two different destinations were detected."),
                ),
            ),
        )

        assertRejectedWith(result, ProceedWithCandidateValidationIssue.CONFLICTS_PRESENT)
    }

    @Test
    fun rejectsCandidateWithClarificationQuestion() {
        val result = validator(
            proceedWithCandidate(
                completeCandidate(
                    clarificationQuestion = "Which Rome did you mean?",
                ),
            ),
        )

        val rejected = assertIs<ProceedWithCandidateValidationResult.Rejected>(result)
        assertTrue(ProceedWithCandidateValidationIssue.CLARIFICATION_REQUIRED in rejected.issues)
        assertEquals("Which Rome did you mean?", rejected.clarificationHint)
    }

    @Test
    fun rejectsCandidateWithBlockingWarnings() {
        val result = validator(
            proceedWithCandidate(
                completeCandidate(
                    warnings = listOf("Destination may be ambiguous."),
                ),
            ),
        )

        assertRejectedWith(result, ProceedWithCandidateValidationIssue.BLOCKING_WARNINGS)
    }

    @Test
    fun rejectsUnsupportedOrNonHotelIntent() {
        val result = validator(
            proceedWithCandidate(
                completeCandidate(
                    intent = LlmCandidate.Intent.UNSUPPORTED,
                ),
            ),
        )

        assertRejectedWith(result, ProceedWithCandidateValidationIssue.UNSUPPORTED_INTENT)
    }

    @Test
    fun rejectsNonInterpretedOutcome() {
        val result = validator(
            proceedWithCandidate(
                completeCandidate(
                    outcome = LlmCandidate.Outcome.AMBIGUOUS,
                    clarificationQuestion = "What are your dates?",
                ),
            ),
        )

        assertRejectedWith(result, ProceedWithCandidateValidationIssue.UNSUPPORTED_OUTCOME)
    }

    @Test
    fun rejectsPartialCurrentCandidateWhenRequiredFieldsAreAbsent() {
        val result = validator(
            proceedWithCandidate(
                LlmCandidate(
                    outcome = LlmCandidate.Outcome.INTERPRETED,
                    intent = LlmCandidate.Intent.HOTEL_SEARCH,
                    extractedConstraints = mapOf("destination" to "Rome"),
                ),
            ),
        )

        val rejected = assertIs<ProceedWithCandidateValidationResult.Rejected>(result)
        assertEquals(
            setOf(
                ProceedWithCandidateValidationIssue.MISSING_CHECK_IN_DATE,
                ProceedWithCandidateValidationIssue.MISSING_CHECK_OUT_DATE,
                ProceedWithCandidateValidationIssue.MISSING_ADULTS,
                ProceedWithCandidateValidationIssue.MISSING_ROOMS,
            ),
            rejected.issues,
        )
    }

    @Test
    fun remainsDeterministicForSameCandidate() {
        val decision = proceedWithCandidate()

        val firstResult = validator(decision)
        val secondResult = validator(decision)

        assertEquals(firstResult, secondResult)
    }

    @Test
    fun validatesWithoutProviderNetworkOrApiKeyDependency() {
        val localValidator = ProceedWithCandidateCriteriaValidator()

        val result = localValidator(proceedWithCandidate())

        assertIs<ProceedWithCandidateValidationResult.Accepted>(result)
    }

    private fun validateWithConstraints(
        vararg overrides: Pair<String, String>,
    ): ProceedWithCandidateValidationResult =
        validator(
            proceedWithCandidate(
                completeCandidate(
                    constraints = completeConstraints() + overrides,
                ),
            ),
        )

    private fun proceedWithCandidate(
        candidate: LlmCandidate = completeCandidate(),
    ): AssistantCandidateDecision.ProceedWithCandidate =
        AssistantCandidateDecision.ProceedWithCandidate(candidate)

    private fun completeCandidate(
        outcome: LlmCandidate.Outcome = LlmCandidate.Outcome.INTERPRETED,
        intent: LlmCandidate.Intent = LlmCandidate.Intent.HOTEL_SEARCH,
        constraints: Map<String, String> = completeConstraints(),
        missingRequiredFields: List<String> = emptyList(),
        conflicts: List<String> = emptyList(),
        clarificationQuestion: String? = null,
        warnings: List<String> = emptyList(),
    ): LlmCandidate =
        LlmCandidate(
            outcome = outcome,
            intent = intent,
            extractedConstraints = constraints,
            missingRequiredFields = missingRequiredFields,
            conflicts = conflicts,
            clarificationQuestion = clarificationQuestion,
            warnings = warnings,
        )

    private fun completeConstraints(): Map<String, String> =
        mapOf(
            "destination" to "Rome",
            "check-in" to "2026-07-01",
            "check-out" to "2026-07-04",
            "adults" to "2",
            "children" to "1",
            "rooms" to "1",
        )

    private fun assertRejectedWith(
        result: ProceedWithCandidateValidationResult,
        issue: ProceedWithCandidateValidationIssue,
    ) {
        val rejected = assertIs<ProceedWithCandidateValidationResult.Rejected>(result)
        assertTrue(issue in rejected.issues)
    }
}
