package com.travelassistant.backend.application.assistant

sealed interface ConfirmedSearchExecutionAttemptStoreResult {
    data class Stored(
        val attempt: ConfirmedSearchExecutionAttempt,
    ) : ConfirmedSearchExecutionAttemptStoreResult

    data class Duplicate(
        val existingAttempt: ConfirmedSearchExecutionAttempt,
    ) : ConfirmedSearchExecutionAttemptStoreResult

    data class Rejected(
        val reason: RejectionReason,
        val existingAttempt: ConfirmedSearchExecutionAttempt? = null,
    ) : ConfirmedSearchExecutionAttemptStoreResult

    enum class RejectionReason {
        ATTEMPT_NOT_FOUND,
        PREPARED_ATTEMPT_REQUIRED,
        ATTEMPT_NOT_IN_PROGRESS,
    }
}
