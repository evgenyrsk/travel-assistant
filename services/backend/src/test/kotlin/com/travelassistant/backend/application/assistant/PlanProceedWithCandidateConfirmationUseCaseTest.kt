package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.llm.LlmCandidate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class PlanProceedWithCandidateConfirmationUseCaseTest {

    private val useCase = PlanProceedWithCandidateConfirmationUseCase()

    @Test
    fun returnsConfirmationProposalPlanForCompleteSafeCandidate() {
        val plan = useCase(proceedWithCandidate())

        val confirmation = assertIs<ProceedWithCandidateConfirmationPlan.ConfirmationRequired>(plan)
        assertEquals(
            "Параметры hotel search: направление: Rome; заезд: 2026-07-01; " +
                "выезд: 2026-07-04; взрослые: 2; дети: 1; номера: 1.",
            confirmation.proposal.summary,
        )
        assertEquals(
            "Проверить отели по этим параметрам?",
            confirmation.proposal.confirmationQuestion,
        )
    }

    @Test
    fun returnsClarificationPlanForMissingDestinationWithoutProposal() {
        val plan = useCase(
            proceedWithCandidate(
                completeCandidate(
                    constraints = completeConstraints() + ("destination" to " "),
                ),
            ),
        )

        val clarification = assertIs<ProceedWithCandidateConfirmationPlan.ClarificationRequired>(plan)
        assertEquals(
            ProceedWithCandidateConfirmationPlan.ClarificationReason.MISSING_OR_INVALID_CRITERIA,
            clarification.reason,
        )
        assertEquals(
            "Please confirm the destination, dates, guests, and rooms before I prepare a hotel search confirmation.",
            clarification.question,
        )
    }

    @Test
    fun returnsClarificationPlanForInvalidDatesWithoutProposal() {
        val plan = useCase(
            proceedWithCandidate(
                completeCandidate(
                    constraints = completeConstraints() + ("check-out" to "2026-06-30"),
                ),
            ),
        )

        val clarification = assertIs<ProceedWithCandidateConfirmationPlan.ClarificationRequired>(plan)
        assertEquals(
            ProceedWithCandidateConfirmationPlan.ClarificationReason.MISSING_OR_INVALID_CRITERIA,
            clarification.reason,
        )
    }

    @Test
    fun returnsClarificationPlanWithSafeQuestionForCandidateClarificationHint() {
        val plan = useCase(
            proceedWithCandidate(
                completeCandidate(
                    clarificationQuestion = "Which Rome did you mean?",
                ),
            ),
        )

        val clarification = assertIs<ProceedWithCandidateConfirmationPlan.ClarificationRequired>(plan)
        assertEquals(
            ProceedWithCandidateConfirmationPlan.ClarificationReason.CANDIDATE_CLARIFICATION_REQUESTED,
            clarification.reason,
        )
        assertEquals("Which Rome did you mean?", clarification.question)
    }

    @Test
    fun returnsFallbackPlanForNonHotelIntent() {
        val plan = useCase(
            proceedWithCandidate(
                completeCandidate(
                    intent = LlmCandidate.Intent.UNSUPPORTED,
                ),
            ),
        )

        val fallback = assertIs<ProceedWithCandidateConfirmationPlan.Fallback>(plan)
        assertEquals(
            ProceedWithCandidateConfirmationPlan.FallbackReason.UNSUPPORTED_INTENT,
            fallback.reason,
        )
    }

    @Test
    fun returnsFallbackPlanForConflictsOrWarnings() {
        val conflictPlan = useCase(
            proceedWithCandidate(
                completeCandidate(
                    conflicts = listOf("Two destinations were detected."),
                ),
            ),
        )
        val warningPlan = useCase(
            proceedWithCandidate(
                completeCandidate(
                    warnings = listOf("Destination may be ambiguous."),
                ),
            ),
        )

        assertEquals(
            ProceedWithCandidateConfirmationPlan.Fallback(
                ProceedWithCandidateConfirmationPlan.FallbackReason.CONFLICTS_OR_WARNINGS,
            ),
            conflictPlan,
        )
        assertEquals(
            ProceedWithCandidateConfirmationPlan.Fallback(
                ProceedWithCandidateConfirmationPlan.FallbackReason.CONFLICTS_OR_WARNINGS,
            ),
            warningPlan,
        )
    }

    @Test
    fun doesNotExposeRawCandidateOrInternalValidationDetailsInUserFacingText() {
        val plan = useCase(
            proceedWithCandidate(
                completeCandidate(
                    constraints = completeConstraints() + ("destination" to " "),
                ),
            ),
        )

        val userFacingText = plan.userFacingText()
        listOf(
            "LlmCandidate",
            "raw candidate",
            "candidatePayload",
            "modelResponse",
            "extractedConstraints",
            "missingRequiredFields",
            "conflicts",
            "warnings",
            "ProceedWithCandidateValidationIssue",
            "MISSING_DESTINATION",
            "INVALID_DATE_RANGE",
        ).forEach { forbidden ->
            assertFalse(
                userFacingText.contains(forbidden),
                "User-facing text must not expose $forbidden",
            )
        }
    }

    @Test
    fun doesNotCreateHotelSearchOrHotelSearchId() {
        val plan = useCase(proceedWithCandidate())

        val userFacingText = plan.userFacingText()
        assertFalse(plan.toString().contains("hotelSearchId"))
        assertFalse(userFacingText.contains("hotelSearchId"))
        assertFalse(userFacingText.contains("show_hotel_results"))
        assertFalse(userFacingText.contains("Hotel search created"))
    }

    @Test
    fun remainsDeterministicForSameCandidate() {
        val decision = proceedWithCandidate()

        val firstPlan = useCase(decision)
        val secondPlan = useCase(decision)

        assertEquals(firstPlan, secondPlan)
    }

    @Test
    fun plansWithoutProviderNetworkOrApiKeyDependency() {
        val localUseCase = PlanProceedWithCandidateConfirmationUseCase()

        val plan = localUseCase(proceedWithCandidate())

        assertIs<ProceedWithCandidateConfirmationPlan.ConfirmationRequired>(plan)
    }

    private fun ProceedWithCandidateConfirmationPlan.userFacingText(): String =
        when (this) {
            is ProceedWithCandidateConfirmationPlan.ConfirmationRequired ->
                "${proposal.summary}\n${proposal.confirmationQuestion}"

            is ProceedWithCandidateConfirmationPlan.ClarificationRequired ->
                question

            is ProceedWithCandidateConfirmationPlan.Fallback ->
                ""
        }

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
}
