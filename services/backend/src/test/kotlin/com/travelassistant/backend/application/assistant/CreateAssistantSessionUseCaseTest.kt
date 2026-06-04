package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateAssistantSessionUseCaseTest {

    @Test
    fun createsDeterministicLocalAssistantSession() {
        val fixedInstant = Instant.parse("2026-06-04T00:00:00Z")
        val useCase = CreateAssistantSessionUseCase(
            clock = Clock.fixed(fixedInstant, ZoneOffset.UTC),
            idGenerator = AssistantSessionIdGenerator {
                AssistantSessionId("assistant-session-local-000001")
            },
        )

        val session = useCase.createSession()

        assertEquals("assistant-session-local-000001", session.id.value)
        assertEquals("collecting_requirements", session.status.apiValue)
        assertEquals(fixedInstant, session.createdAt)
    }
}
