package com.travelassistant.backend.application.assistant

class PlanProceedWithCandidateConfirmationUseCase private constructor(
    private val validateCriteria: (
        AssistantCandidateDecision.ProceedWithCandidate,
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
        validateCriteria = criteriaValidator::invoke,
        buildProposal = proposalBuilder::invoke,
    )

    operator fun invoke(
        decision: AssistantCandidateDecision.ProceedWithCandidate,
    ): ProceedWithCandidateConfirmationPlan =
        when (val validationResult = validateCriteria(decision)) {
            is ProceedWithCandidateValidationResult.Accepted ->
                ProceedWithCandidateConfirmationPlan.ConfirmationRequired(
                    proposal = buildProposal(validationResult),
                )

            is ProceedWithCandidateValidationResult.Rejected ->
                planRejected(validationResult)
        }

    private fun planRejected(
        rejected: ProceedWithCandidateValidationResult.Rejected,
    ): ProceedWithCandidateConfirmationPlan {
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
            "Please confirm the destination, dates, guests, and rooms before I prepare a hotel search confirmation."
    }
}
