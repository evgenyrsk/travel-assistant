package com.travelassistant.backend.infrastructure.llm

internal enum class OpenRouterDiagnosticEvent {
    CANDIDATE_DECODED,
    REQUEST_REJECTED,
    AUTHENTICATION_FAILED,
    INSUFFICIENT_CREDITS,
    TIMEOUT,
    RATE_LIMITED,
    PROVIDER_UNAVAILABLE,
    HTTP_FAILURE,
    IN_BAND_PROVIDER_ERROR,
    NON_JSON_RESPONSE,
    MALFORMED_RESPONSE,
    EMPTY_CHOICES,
    EMPTY_CONTENT,
    INVALID_CANDIDATE,
    NETWORK_FAILURE,
    UNKNOWN_FAILURE,
}

internal fun interface OpenRouterDiagnosticObserver {
    fun record(event: OpenRouterDiagnosticEvent)

    companion object {
        val NONE = OpenRouterDiagnosticObserver { }
    }
}
