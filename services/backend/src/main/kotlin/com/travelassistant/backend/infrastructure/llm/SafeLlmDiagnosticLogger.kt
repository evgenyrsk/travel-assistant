package com.travelassistant.backend.infrastructure.llm

import com.travelassistant.backend.application.assistant.AssistantLlmDiagnosticEvent
import com.travelassistant.backend.application.assistant.AssistantLlmDiagnosticObserver
import java.util.logging.Level
import java.util.logging.Logger

internal object SafeLlmDiagnosticLogger :
    OpenRouterDiagnosticObserver,
    AssistantLlmDiagnosticObserver {

    private val logger = Logger.getLogger(SafeLlmDiagnosticLogger::class.java.name)

    override fun record(event: OpenRouterDiagnosticEvent) {
        logger.log(
            if (event == OpenRouterDiagnosticEvent.CANDIDATE_DECODED) Level.INFO else Level.WARNING,
            messageFor(event),
        )
    }

    override fun record(event: AssistantLlmDiagnosticEvent) {
        logger.log(
            if (
                event == AssistantLlmDiagnosticEvent.DESTINATION_ENRICHED ||
                event == AssistantLlmDiagnosticEvent.PREFERENCE_STARS_ENRICHED
            ) {
                Level.INFO
            } else {
                Level.WARNING
            },
            messageFor(event),
        )
    }

    internal fun messageFor(event: OpenRouterDiagnosticEvent): String =
        "component=llm source=openrouter event=${event.name}"

    internal fun messageFor(event: AssistantLlmDiagnosticEvent): String =
        "component=llm source=assistant event=${event.name}"
}
