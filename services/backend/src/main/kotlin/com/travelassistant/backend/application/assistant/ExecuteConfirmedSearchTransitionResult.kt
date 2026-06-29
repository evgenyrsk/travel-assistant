package com.travelassistant.backend.application.assistant

sealed interface ExecuteConfirmedSearchTransitionResult {
    val lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy
    val executionPolicy: ConfirmedSearchExecutionPolicy

    data class Transitioned(
        val attempt: ConfirmedSearchExecutionAttempt,
        val executionResult: ConfirmedSearchExecutionResult,
        val pendingConsumptionDecision: PendingConsumptionDecision,
        override val lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy,
        override val executionPolicy: ConfirmedSearchExecutionPolicy,
    ) : ExecuteConfirmedSearchTransitionResult

    data class DuplicateDetected(
        val existingAttempt: ConfirmedSearchExecutionAttempt,
        val duplicateReason: ConfirmedSearchExecutionAttemptResult.DuplicateReason,
        val pendingConsumptionDecision: PendingConsumptionDecision,
        override val lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy,
        override val executionPolicy: ConfirmedSearchExecutionPolicy,
    ) : ExecuteConfirmedSearchTransitionResult

    data class GuardRejected(
        val attemptRejectionReason: ConfirmedSearchExecutionAttemptResult.RejectionReason,
        override val lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy,
        override val executionPolicy: ConfirmedSearchExecutionPolicy,
    ) : ExecuteConfirmedSearchTransitionResult

    data class StoreRejected(
        val reason: ConfirmedSearchExecutionAttemptStoreResult.RejectionReason,
        override val lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy,
        override val executionPolicy: ConfirmedSearchExecutionPolicy,
    ) : ExecuteConfirmedSearchTransitionResult

    enum class PendingConsumptionDecision {
        CONSUME_AFTER_SUCCESSFUL_RECORDING,
        DO_NOT_CONSUME,
    }
}
