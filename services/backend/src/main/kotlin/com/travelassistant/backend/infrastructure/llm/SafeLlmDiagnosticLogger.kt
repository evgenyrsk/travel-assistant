package com.travelassistant.backend.infrastructure.llm

import com.travelassistant.backend.application.assistant.AssistantLlmDiagnosticEvent
import com.travelassistant.backend.application.assistant.AssistantLlmDiagnosticObserver
import com.travelassistant.backend.application.observability.OperationalComponent
import com.travelassistant.backend.application.observability.OperationalDependency
import com.travelassistant.backend.application.observability.OperationalDiagnostic
import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventName
import com.travelassistant.backend.application.observability.OperationalEventSink
import com.travelassistant.backend.application.observability.OperationalLevel
import com.travelassistant.backend.application.observability.OperationalOperation
import com.travelassistant.backend.application.observability.OperationalOutcome
import com.travelassistant.backend.application.observability.recordSafely
import com.travelassistant.backend.infrastructure.observability.JsonOperationalEventSink

internal class SafeLlmDiagnosticLogger(
    private val eventSink: OperationalEventSink = JsonOperationalEventSink(),
) : OpenRouterDiagnosticObserver,
    AssistantLlmDiagnosticObserver {

    override fun record(event: OpenRouterDiagnosticEvent) {
        eventSink.recordSafely(
            OperationalEvent(
                name = OperationalEventName.LLM_DIAGNOSTIC,
                component = OperationalComponent.LLM,
                level = event.toOperationalLevel(),
                operation = OperationalOperation.GENERATE_LLM_CANDIDATE,
                dependency = OperationalDependency.OPENROUTER,
                outcome = event.toOperationalOutcome(),
                diagnostic = event.toOperationalDiagnostic(),
            ),
        )
    }

    override fun record(event: AssistantLlmDiagnosticEvent) {
        eventSink.recordSafely(
            OperationalEvent(
                name = OperationalEventName.LLM_DIAGNOSTIC,
                component = OperationalComponent.ASSISTANT,
                level = if (
                    event == AssistantLlmDiagnosticEvent.DESTINATION_ENRICHED ||
                    event == AssistantLlmDiagnosticEvent.PREFERENCE_STARS_ENRICHED
                ) {
                    OperationalLevel.INFO
                } else {
                    OperationalLevel.WARNING
                },
                operation = OperationalOperation.GENERATE_LLM_CANDIDATE,
                outcome = event.toOperationalOutcome(),
                diagnostic = event.toOperationalDiagnostic(),
            ),
        )
    }

    companion object {
        internal fun messageFor(event: OpenRouterDiagnosticEvent): String =
            "component=llm source=openrouter event=${event.name}"

        internal fun messageFor(event: AssistantLlmDiagnosticEvent): String =
            "component=llm source=assistant event=${event.name}"
    }
}

private fun OpenRouterDiagnosticEvent.toOperationalOutcome(): OperationalOutcome =
    when (this) {
        OpenRouterDiagnosticEvent.CANDIDATE_DECODED -> OperationalOutcome.SUCCEEDED
        OpenRouterDiagnosticEvent.REQUEST_REJECTED -> OperationalOutcome.REQUEST_REJECTED
        OpenRouterDiagnosticEvent.AUTHENTICATION_FAILED -> OperationalOutcome.AUTHENTICATION_FAILED
        OpenRouterDiagnosticEvent.INSUFFICIENT_CREDITS -> OperationalOutcome.INSUFFICIENT_CREDITS
        OpenRouterDiagnosticEvent.TIMEOUT -> OperationalOutcome.TIMEOUT
        OpenRouterDiagnosticEvent.RATE_LIMITED -> OperationalOutcome.RATE_LIMITED
        OpenRouterDiagnosticEvent.PROVIDER_UNAVAILABLE -> OperationalOutcome.UNAVAILABLE
        OpenRouterDiagnosticEvent.INVALID_CANDIDATE -> OperationalOutcome.INVALID_CANDIDATE
        OpenRouterDiagnosticEvent.EMPTY_CHOICES,
        OpenRouterDiagnosticEvent.EMPTY_CONTENT,
        OpenRouterDiagnosticEvent.HTTP_FAILURE,
        OpenRouterDiagnosticEvent.IN_BAND_PROVIDER_ERROR,
        OpenRouterDiagnosticEvent.MALFORMED_RESPONSE,
        OpenRouterDiagnosticEvent.NETWORK_FAILURE,
        OpenRouterDiagnosticEvent.NON_JSON_RESPONSE,
        OpenRouterDiagnosticEvent.UNKNOWN_FAILURE,
        -> OperationalOutcome.FAILED
    }

private fun OpenRouterDiagnosticEvent.toOperationalLevel(): OperationalLevel =
    when (this) {
        OpenRouterDiagnosticEvent.CANDIDATE_DECODED -> OperationalLevel.INFO
        OpenRouterDiagnosticEvent.AUTHENTICATION_FAILED,
        OpenRouterDiagnosticEvent.INSUFFICIENT_CREDITS,
        -> OperationalLevel.ERROR
        else -> OperationalLevel.WARNING
    }

private fun OpenRouterDiagnosticEvent.toOperationalDiagnostic(): OperationalDiagnostic =
    OperationalDiagnostic.valueOf(name)

private fun AssistantLlmDiagnosticEvent.toOperationalOutcome(): OperationalOutcome =
    when (this) {
        AssistantLlmDiagnosticEvent.DESTINATION_ENRICHED,
        AssistantLlmDiagnosticEvent.PREFERENCE_STARS_ENRICHED,
        -> OperationalOutcome.SUCCEEDED
        AssistantLlmDiagnosticEvent.UNSUPPORTED_ROOM_COUNT,
        AssistantLlmDiagnosticEvent.CANDIDATE_UNSUPPORTED_INTENT,
        AssistantLlmDiagnosticEvent.CONFIRMATION_UNSUPPORTED_INTENT,
        AssistantLlmDiagnosticEvent.CONFIRMATION_UNSAFE_OR_UNSUPPORTED_OUTCOME,
        -> OperationalOutcome.UNSUPPORTED
        AssistantLlmDiagnosticEvent.CANDIDATE_INVALID,
        AssistantLlmDiagnosticEvent.CONFIRMATION_CONFLICTS_OR_WARNINGS,
        -> OperationalOutcome.INVALID_CANDIDATE
        AssistantLlmDiagnosticEvent.CANDIDATE_CLIENT_FAILURE -> OperationalOutcome.CLIENT_FAILURE
        AssistantLlmDiagnosticEvent.CANDIDATE_EMPTY_RESPONSE,
        AssistantLlmDiagnosticEvent.CANDIDATE_MISSING_CLARIFICATION,
        -> OperationalOutcome.FAILED
    }

private fun AssistantLlmDiagnosticEvent.toOperationalDiagnostic(): OperationalDiagnostic =
    OperationalDiagnostic.valueOf(name)
