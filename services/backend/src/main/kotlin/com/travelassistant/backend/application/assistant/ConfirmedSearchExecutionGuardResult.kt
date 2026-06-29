package com.travelassistant.backend.application.assistant

sealed interface ConfirmedSearchExecutionGuardResult {
    val lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy
    val executionPolicy: ConfirmedSearchExecutionPolicy

    data class AllowedButBlockedUntilIdempotencyGuard(
        val commandPlan: ConfirmedSearchCreationCommandPlan.CommandReady,
        val pendingConfirmation: PendingProceedWithCandidateConfirmation,
        val blocker: ExecutionBlocker = ExecutionBlocker.IDEMPOTENCY_GUARD_REQUIRED,
        override val lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy,
        override val executionPolicy: ConfirmedSearchExecutionPolicy,
    ) : ConfirmedSearchExecutionGuardResult

    data class Rejected(
        val reason: RejectionReason,
        override val lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy,
        override val executionPolicy: ConfirmedSearchExecutionPolicy,
    ) : ConfirmedSearchExecutionGuardResult

    enum class ExecutionBlocker {
        IDEMPOTENCY_GUARD_REQUIRED,
    }

    enum class RejectionReason {
        NO_ACTIVE_PENDING_CONFIRMATION,
        PENDING_CONFIRMATION_EXPIRED,
        PENDING_CONFIRMATION_CONSUMED,
        SESSION_MISMATCH,
        CRITERIA_MISMATCH,
    }
}
