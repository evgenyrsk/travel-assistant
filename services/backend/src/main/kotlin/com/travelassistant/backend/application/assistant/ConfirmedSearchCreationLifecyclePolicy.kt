package com.travelassistant.backend.application.assistant

data class ConfirmedSearchCreationLifecyclePolicy(
    val pendingConsumption: PendingConsumption = PendingConsumption.CONSUME_AFTER_SEARCH_SUCCESS,
    val failureHandling: FailureHandling = FailureHandling.DO_NOT_CONSUME_ON_SEARCH_FAILURE,
    val duplicateConfirmationHandling: DuplicateConfirmationHandling =
        DuplicateConfirmationHandling.REQUIRES_IDEMPOTENCY_GUARD,
) {
    enum class PendingConsumption {
        CONSUME_AFTER_SEARCH_SUCCESS,
    }

    enum class FailureHandling {
        DO_NOT_CONSUME_ON_SEARCH_FAILURE,
    }

    enum class DuplicateConfirmationHandling {
        REQUIRES_IDEMPOTENCY_GUARD,
    }
}
