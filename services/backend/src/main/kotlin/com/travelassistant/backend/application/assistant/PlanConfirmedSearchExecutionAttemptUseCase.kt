package com.travelassistant.backend.application.assistant

import java.time.Instant

class PlanConfirmedSearchExecutionAttemptUseCase {

    operator fun invoke(
        guardResult: ConfirmedSearchExecutionGuardResult,
        now: Instant,
        existingAttempt: ConfirmedSearchExecutionAttempt? = null,
    ): ConfirmedSearchExecutionAttemptResult =
        when (guardResult) {
            is ConfirmedSearchExecutionGuardResult.AllowedButBlockedUntilIdempotencyGuard ->
                planAttempt(
                    guardResult = guardResult,
                    now = now,
                    existingAttempt = existingAttempt,
                )

            is ConfirmedSearchExecutionGuardResult.Rejected ->
                ConfirmedSearchExecutionAttemptResult.Rejected(
                    reason = ConfirmedSearchExecutionAttemptResult.RejectionReason.GUARD_REJECTED,
                    lifecyclePolicy = guardResult.lifecyclePolicy,
                    executionPolicy = guardResult.executionPolicy,
                )
        }

    private fun planAttempt(
        guardResult: ConfirmedSearchExecutionGuardResult.AllowedButBlockedUntilIdempotencyGuard,
        now: Instant,
        existingAttempt: ConfirmedSearchExecutionAttempt?,
    ): ConfirmedSearchExecutionAttemptResult {
        val commandPlan = guardResult.commandPlan
        val idempotencyKey = ConfirmedSearchExecutionIdempotencyKey.from(commandPlan)

        existingAttempt?.let { attempt ->
            val rejectionReason = attempt.rejectionReasonFor(
                idempotencyKey = idempotencyKey,
                commandPlan = commandPlan,
            )
            if (rejectionReason != null) {
                return ConfirmedSearchExecutionAttemptResult.Rejected(
                    reason = rejectionReason,
                    lifecyclePolicy = guardResult.lifecyclePolicy,
                    executionPolicy = guardResult.executionPolicy,
                )
            }

            return ConfirmedSearchExecutionAttemptResult.DuplicateDetected(
                originalAttempt = attempt,
                duplicateAttempt = ConfirmedSearchExecutionAttempt(
                    idempotencyKey = idempotencyKey,
                    sessionId = commandPlan.command.sessionId,
                    commandPlan = commandPlan,
                    status = ConfirmedSearchExecutionAttemptStatus.DUPLICATE_BLOCKED,
                    createdSearchId = attempt.createdSearchId,
                    createdAt = now,
                    updatedAt = now,
                ),
                reason = attempt.status.toDuplicateReason(),
                lifecyclePolicy = guardResult.lifecyclePolicy,
                executionPolicy = guardResult.executionPolicy,
            )
        }

        return ConfirmedSearchExecutionAttemptResult.AttemptPreparedButExecutionBlocked(
            attempt = ConfirmedSearchExecutionAttempt(
                idempotencyKey = idempotencyKey,
                sessionId = commandPlan.command.sessionId,
                commandPlan = commandPlan,
                status = ConfirmedSearchExecutionAttemptStatus.PREPARED,
                createdAt = now,
                updatedAt = now,
            ),
            lifecyclePolicy = guardResult.lifecyclePolicy,
            executionPolicy = guardResult.executionPolicy,
        )
    }

    private fun ConfirmedSearchExecutionAttempt.rejectionReasonFor(
        idempotencyKey: ConfirmedSearchExecutionIdempotencyKey,
        commandPlan: ConfirmedSearchCreationCommandPlan.CommandReady,
    ): ConfirmedSearchExecutionAttemptResult.RejectionReason? =
        when {
            this.idempotencyKey != idempotencyKey ->
                ConfirmedSearchExecutionAttemptResult.RejectionReason.ATTEMPT_KEY_MISMATCH

            sessionId != commandPlan.command.sessionId ->
                ConfirmedSearchExecutionAttemptResult.RejectionReason.ATTEMPT_SESSION_MISMATCH

            this.commandPlan.command != commandPlan.command ->
                ConfirmedSearchExecutionAttemptResult.RejectionReason.ATTEMPT_COMMAND_MISMATCH

            else -> null
        }

    private fun ConfirmedSearchExecutionAttemptStatus.toDuplicateReason():
        ConfirmedSearchExecutionAttemptResult.DuplicateReason =
        when (this) {
            ConfirmedSearchExecutionAttemptStatus.PREPARED ->
                ConfirmedSearchExecutionAttemptResult.DuplicateReason.PREPARED

            ConfirmedSearchExecutionAttemptStatus.IN_PROGRESS ->
                ConfirmedSearchExecutionAttemptResult.DuplicateReason.IN_PROGRESS

            ConfirmedSearchExecutionAttemptStatus.SUCCEEDED ->
                ConfirmedSearchExecutionAttemptResult.DuplicateReason.SUCCEEDED

            ConfirmedSearchExecutionAttemptStatus.FAILED ->
                ConfirmedSearchExecutionAttemptResult.DuplicateReason.FAILED

            ConfirmedSearchExecutionAttemptStatus.DUPLICATE_BLOCKED ->
                ConfirmedSearchExecutionAttemptResult.DuplicateReason.DUPLICATE_BLOCKED
        }
}
