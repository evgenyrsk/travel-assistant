package com.travelassistant.backend.application.assistant

enum class ConfirmedSearchExecutionAttemptFailureReason {
    SEARCH_CREATION_FAILED,
    EXECUTION_STATE_UNKNOWN,
    STALE_EXECUTION;

    fun isRetryAllowed(): Boolean =
        this == SEARCH_CREATION_FAILED || this == STALE_EXECUTION
}
