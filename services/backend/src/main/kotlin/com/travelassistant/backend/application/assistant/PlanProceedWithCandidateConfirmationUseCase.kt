package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.hotel.HotelSearchPreferences

class PlanProceedWithCandidateConfirmationUseCase private constructor(
    private val validateCriteria: (
        AssistantCandidateDecision.ProceedWithCandidate,
        HotelSearchPreferences,
    ) -> ProceedWithCandidateValidationResult,
    private val buildProposal: (
        ProceedWithCandidateValidationResult.Accepted,
    ) -> ProceedWithCandidateConfirmationProposal,
) {

    constructor(
        criteriaValidator: ProceedWithCandidateCriteriaValidator =
            ProceedWithCandidateCriteriaValidator(),
        proposalBuilder: BuildProceedWithCandidateConfirmationProposalUseCase =
            BuildProceedWithCandidateConfirmationProposalUseCase(),
    ) : this(
        validateCriteria = { decision, preferences ->
            criteriaValidator(decision, preferences)
        },
        buildProposal = proposalBuilder::invoke,
    )

    operator fun invoke(
        decision: AssistantCandidateDecision.ProceedWithCandidate,
        preferences: HotelSearchPreferences = HotelSearchPreferences(),
    ): ProceedWithCandidateConfirmationPlan =
        when (val validationResult = validateCriteria(decision, preferences)) {
            is ProceedWithCandidateValidationResult.Accepted ->
                ProceedWithCandidateConfirmationPlan.ConfirmationRequired(
                    criteria = validationResult.criteria,
                    proposal = buildProposal(validationResult),
                )

            is ProceedWithCandidateValidationResult.Rejected ->
                planRejected(validationResult)
        }

    private fun planRejected(
        rejected: ProceedWithCandidateValidationResult.Rejected,
    ): ProceedWithCandidateConfirmationPlan {
        if (ProceedWithCandidateValidationIssue.UNSUPPORTED_ROOM_COUNT in rejected.issues) {
            return ProceedWithCandidateConfirmationPlan.ClarificationRequired(
                question = SINGLE_ROOM_ONLY_CLARIFICATION_MESSAGE,
                reason = ProceedWithCandidateConfirmationPlan.ClarificationReason
                    .MISSING_OR_INVALID_CRITERIA,
            )
        }

        if (ProceedWithCandidateValidationIssue.UNSUPPORTED_INTENT in rejected.issues) {
            return ProceedWithCandidateConfirmationPlan.Fallback(
                ProceedWithCandidateConfirmationPlan.FallbackReason.UNSUPPORTED_INTENT,
            )
        }

        if (
            ProceedWithCandidateValidationIssue.CONFLICTS_PRESENT in rejected.issues ||
            ProceedWithCandidateValidationIssue.BLOCKING_WARNINGS in rejected.issues
        ) {
            return ProceedWithCandidateConfirmationPlan.Fallback(
                ProceedWithCandidateConfirmationPlan.FallbackReason.CONFLICTS_OR_WARNINGS,
            )
        }

        if (
            ProceedWithCandidateValidationIssue.UNSUPPORTED_OUTCOME in rejected.issues &&
            rejected.clarificationHint.isNullOrBlank()
        ) {
            return ProceedWithCandidateConfirmationPlan.Fallback(
                ProceedWithCandidateConfirmationPlan.FallbackReason.UNSAFE_OR_UNSUPPORTED_OUTCOME,
            )
        }

        return ProceedWithCandidateConfirmationPlan.ClarificationRequired(
            question = rejected.safeClarificationQuestion(),
            reason = rejected.clarificationReason(),
        )
    }

    private fun ProceedWithCandidateValidationResult.Rejected.safeClarificationQuestion(): String =
        clarificationHint?.trim()?.takeIf(String::isNotEmpty) ?: DEFAULT_CLARIFICATION_QUESTION

    private fun ProceedWithCandidateValidationResult.Rejected.clarificationReason() =
        if (ProceedWithCandidateValidationIssue.CLARIFICATION_REQUIRED in issues) {
            ProceedWithCandidateConfirmationPlan.ClarificationReason.CANDIDATE_CLARIFICATION_REQUESTED
        } else {
            ProceedWithCandidateConfirmationPlan.ClarificationReason.MISSING_OR_INVALID_CRITERIA
        }

    private companion object {
        const val DEFAULT_CLARIFICATION_QUESTION =
            "Уточните направление, даты и состав гостей, чтобы я подготовил подтверждение поиска."
    }
}
