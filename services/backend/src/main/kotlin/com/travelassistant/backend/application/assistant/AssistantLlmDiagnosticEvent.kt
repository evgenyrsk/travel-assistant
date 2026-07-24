package com.travelassistant.backend.application.assistant

enum class AssistantLlmDiagnosticEvent {
    DESTINATION_ENRICHED,
    PREFERENCE_STARS_ENRICHED,
    UNSUPPORTED_ROOM_COUNT,
    CANDIDATE_EMPTY_RESPONSE,
    CANDIDATE_CLIENT_FAILURE,
    CANDIDATE_INVALID,
    CANDIDATE_UNSUPPORTED_INTENT,
    CANDIDATE_MISSING_CLARIFICATION,
    CONFIRMATION_UNSUPPORTED_INTENT,
    CONFIRMATION_UNSAFE_OR_UNSUPPORTED_OUTCOME,
    CONFIRMATION_CONFLICTS_OR_WARNINGS,
}

fun interface AssistantLlmDiagnosticObserver {
    fun record(event: AssistantLlmDiagnosticEvent)

    companion object {
        val NONE = AssistantLlmDiagnosticObserver { }
    }
}
