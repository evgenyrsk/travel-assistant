package com.travelassistant.backend.application.assistant

enum class ConfirmedSearchExecutionAttemptStatus {
    PREPARED,
    IN_PROGRESS,
    SUCCEEDED,
    FAILED,
    DUPLICATE_BLOCKED,
}
