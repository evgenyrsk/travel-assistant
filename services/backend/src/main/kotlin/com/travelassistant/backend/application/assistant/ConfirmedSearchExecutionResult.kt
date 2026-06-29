package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.hotel.HotelSearchId

sealed interface ConfirmedSearchExecutionResult {
    val lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy
    val executionPolicy: ConfirmedSearchExecutionPolicy

    data class PreparedButNotExecuted(
        val commandPlan: ConfirmedSearchCreationCommandPlan.CommandReady,
        val reason: NotExecutedReason,
        override val lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy,
        override val executionPolicy: ConfirmedSearchExecutionPolicy,
    ) : ConfirmedSearchExecutionResult

    data class SearchCreated(
        val searchId: HotelSearchId,
        override val lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy,
        override val executionPolicy: ConfirmedSearchExecutionPolicy,
    ) : ConfirmedSearchExecutionResult

    data class SearchCreationFailed(
        val reason: FailureReason,
        override val lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy,
        override val executionPolicy: ConfirmedSearchExecutionPolicy,
    ) : ConfirmedSearchExecutionResult

    data class IdempotencyRequired(
        val commandPlan: ConfirmedSearchCreationCommandPlan.CommandReady,
        val reason: IdempotencyReason,
        override val lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy,
        override val executionPolicy: ConfirmedSearchExecutionPolicy,
    ) : ConfirmedSearchExecutionResult

    enum class NotExecutedReason {
        IDEMPOTENCY_GUARD_REQUIRED,
    }

    enum class FailureReason {
        COMMAND_REJECTED,
        SEARCH_CREATION_FAILED,
        SESSION_NOT_AVAILABLE,
        UNKNOWN,
    }

    enum class IdempotencyReason {
        REQUIRED_BEFORE_EXECUTION,
    }
}
