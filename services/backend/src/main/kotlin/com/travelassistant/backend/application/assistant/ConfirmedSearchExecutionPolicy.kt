package com.travelassistant.backend.application.assistant

data class ConfirmedSearchExecutionPolicy(
    val pendingConsumption: PendingConsumption =
        PendingConsumption.CONSUME_AFTER_FUTURE_SEARCH_SUCCESS,
    val failureResponse: FailureResponse =
        FailureResponse.OMIT_SEARCH_ID_ON_FAILURE,
    val duplicateHandling: DuplicateHandling =
        DuplicateHandling.REQUIRE_IDEMPOTENCY_GUARD_BEFORE_EXECUTION,
    val routeContext: RouteContext =
        RouteContext.REQUIRE_ACTIVE_PENDING_CONFIRMATION,
) {
    enum class PendingConsumption {
        CONSUME_AFTER_FUTURE_SEARCH_SUCCESS,
    }

    enum class FailureResponse {
        OMIT_SEARCH_ID_ON_FAILURE,
    }

    enum class DuplicateHandling {
        REQUIRE_IDEMPOTENCY_GUARD_BEFORE_EXECUTION,
    }

    enum class RouteContext {
        REQUIRE_ACTIVE_PENDING_CONFIRMATION,
    }
}
