package com.travelassistant.backend.infrastructure.llm

import com.travelassistant.backend.application.assistant.AssistantLlmDiagnosticEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class SafeLlmDiagnosticLoggerTest {

    @Test
    fun `formats only fixed OpenRouter diagnostic category`() {
        assertEquals(
            "component=llm source=openrouter event=RATE_LIMITED",
            SafeLlmDiagnosticLogger.messageFor(OpenRouterDiagnosticEvent.RATE_LIMITED),
        )
    }

    @Test
    fun `formats only fixed assistant fallback category`() {
        assertEquals(
            "component=llm source=assistant event=CANDIDATE_INVALID",
            SafeLlmDiagnosticLogger.messageFor(AssistantLlmDiagnosticEvent.CANDIDATE_INVALID),
        )
    }

    @Test
    fun `formats only fixed deterministic preference enrichment category`() {
        assertEquals(
            "component=llm source=assistant event=PREFERENCE_STARS_ENRICHED",
            SafeLlmDiagnosticLogger.messageFor(
                AssistantLlmDiagnosticEvent.PREFERENCE_STARS_ENRICHED,
            ),
        )
    }

    @Test
    fun `formats only fixed deterministic destination enrichment category`() {
        assertEquals(
            "component=llm source=assistant event=DESTINATION_ENRICHED",
            SafeLlmDiagnosticLogger.messageFor(
                AssistantLlmDiagnosticEvent.DESTINATION_ENRICHED,
            ),
        )
    }

    @Test
    fun `formats only fixed unsupported room count category`() {
        assertEquals(
            "component=llm source=assistant event=UNSUPPORTED_ROOM_COUNT",
            SafeLlmDiagnosticLogger.messageFor(
                AssistantLlmDiagnosticEvent.UNSUPPORTED_ROOM_COUNT,
            ),
        )
    }
}
