package com.travelassistant.backend.application.assistant

class PlanConfirmedSearchExecutionGuardUseCase(
    private val criteriaMapper: ProceedWithCandidateCriteriaToHotelSearchCriteriaMapper =
        ProceedWithCandidateCriteriaToHotelSearchCriteriaMapper(),
) {

    operator fun invoke(
        request: ConfirmedSearchExecutionGuardRequest,
    ): ConfirmedSearchExecutionGuardResult {
        val lifecyclePolicy = request.commandPlan.lifecyclePolicy
        val pendingConfirmation = request.pendingConfirmation
            ?: return rejected(
                reason = ConfirmedSearchExecutionGuardResult.RejectionReason
                    .NO_ACTIVE_PENDING_CONFIRMATION,
                lifecyclePolicy = lifecyclePolicy,
            )

        return when {
            pendingConfirmation.statusAt(request.now) == PendingConfirmationStatus.EXPIRED ->
                rejected(
                    reason = ConfirmedSearchExecutionGuardResult.RejectionReason
                        .PENDING_CONFIRMATION_EXPIRED,
                    lifecyclePolicy = lifecyclePolicy,
                )

            pendingConfirmation.statusAt(request.now) == PendingConfirmationStatus.CONSUMED ->
                rejected(
                    reason = ConfirmedSearchExecutionGuardResult.RejectionReason
                        .PENDING_CONFIRMATION_CONSUMED,
                    lifecyclePolicy = lifecyclePolicy,
                )

            pendingConfirmation.sessionId != request.sessionId ->
                rejected(
                    reason = ConfirmedSearchExecutionGuardResult.RejectionReason.SESSION_MISMATCH,
                    lifecyclePolicy = lifecyclePolicy,
                )

            request.commandPlan.command.sessionId != request.sessionId ->
                rejected(
                    reason = ConfirmedSearchExecutionGuardResult.RejectionReason.SESSION_MISMATCH,
                    lifecyclePolicy = lifecyclePolicy,
                )

            criteriaMapper(pendingConfirmation.criteria) != request.commandPlan.command.criteria ->
                rejected(
                    reason = ConfirmedSearchExecutionGuardResult.RejectionReason.CRITERIA_MISMATCH,
                    lifecyclePolicy = lifecyclePolicy,
                )

            else ->
                ConfirmedSearchExecutionGuardResult.AllowedButBlockedUntilIdempotencyGuard(
                    commandPlan = request.commandPlan,
                    pendingConfirmation = pendingConfirmation,
                    lifecyclePolicy = lifecyclePolicy,
                    executionPolicy = ConfirmedSearchExecutionPolicy(),
                )
        }
    }

    private fun rejected(
        reason: ConfirmedSearchExecutionGuardResult.RejectionReason,
        lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy,
    ): ConfirmedSearchExecutionGuardResult =
        ConfirmedSearchExecutionGuardResult.Rejected(
            reason = reason,
            lifecyclePolicy = lifecyclePolicy,
            executionPolicy = ConfirmedSearchExecutionPolicy(),
        )
}
