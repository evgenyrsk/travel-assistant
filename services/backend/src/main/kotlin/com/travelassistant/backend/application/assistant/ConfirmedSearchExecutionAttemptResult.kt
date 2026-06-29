package com.travelassistant.backend.application.assistant

sealed interface ConfirmedSearchExecutionAttemptResult {
    val lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy
    val executionPolicy: ConfirmedSearchExecutionPolicy

    data class AttemptPreparedButExecutionBlocked(
        val attempt: ConfirmedSearchExecutionAttempt,
        val blocker: ExecutionBlocker = ExecutionBlocker.ATTEMPT_STORE_REQUIRED_BEFORE_EXECUTION,
        override val lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy,
        override val executionPolicy: ConfirmedSearchExecutionPolicy,
    ) : ConfirmedSearchExecutionAttemptResult

    data class DuplicateDetected(
        val originalAttempt: ConfirmedSearchExecutionAttempt,
        val duplicateAttempt: ConfirmedSearchExecutionAttempt,
        val reason: DuplicateReason,
        val blocker: ExecutionBlocker = ExecutionBlocker.ACTUAL_EXECUTION_NOT_CONNECTED,
        override val lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy,
        override val executionPolicy: ConfirmedSearchExecutionPolicy,
    ) : ConfirmedSearchExecutionAttemptResult

    data class Rejected(
        val reason: RejectionReason,
        override val lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy,
        override val executionPolicy: ConfirmedSearchExecutionPolicy,
    ) : ConfirmedSearchExecutionAttemptResult

    enum class ExecutionBlocker {
        ATTEMPT_STORE_REQUIRED_BEFORE_EXECUTION,
        ACTUAL_EXECUTION_NOT_CONNECTED,
    }

    enum class DuplicateReason {
        PREPARED,
        IN_PROGRESS,
        SUCCEEDED,
        FAILED,
        DUPLICATE_BLOCKED,
    }

    enum class RejectionReason {
        GUARD_REJECTED,
        ATTEMPT_KEY_MISMATCH,
        ATTEMPT_SESSION_MISMATCH,
        ATTEMPT_COMMAND_MISMATCH,
    }
}
