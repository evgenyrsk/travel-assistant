package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CreateAssistantSessionUseCaseTest {

    @Test
    fun createsDeterministicLocalAssistantSession() {
        val fixedInstant = Instant.parse("2026-06-04T00:00:00Z")
        val useCase = CreateAssistantSessionUseCase(
            clock = Clock.fixed(fixedInstant, ZoneOffset.UTC),
            idGenerator = AssistantSessionIdGenerator {
                AssistantSessionId("assistant-session-local-000001")
            },
            sessionStateStore = InMemoryAssistantSessionStateStore(),
        )

        val session = useCase.createSession()

        assertEquals("assistant-session-local-000001", session.id.value)
        assertEquals("collecting_requirements", session.status.apiValue)
        assertEquals(fixedInstant, session.createdAt)
    }

    @Test
    fun acceptsUserMessageAsLocalIntakeOnly() {
        val fixedInstant = Instant.parse("2026-06-04T00:00:00Z")
        val useCase = CreateAssistantSessionUseCase(
            clock = Clock.fixed(fixedInstant, ZoneOffset.UTC),
            idGenerator = AssistantSessionIdGenerator {
                AssistantSessionId("assistant-session-local-000001")
            },
            sessionStateStore = InMemoryAssistantSessionStateStore(),
        )

        val session = useCase.createSession()

        val acceptedMessage = useCase.acceptUserMessage(
            AcceptAssistantMessageCommand(
                sessionId = session.id,
                message = "I want a hotel in Rome",
            ),
        )

        assertEquals("assistant-session-local-000001", acceptedMessage.sessionId.value)
        assertEquals("collecting_requirements", acceptedMessage.status.apiValue)
        assertEquals(fixedInstant, acceptedMessage.receivedAt)
        assertEquals("clarification", acceptedMessage.assistantReply.type.apiValue)
        assertEquals(
            "I received your hotel request. Please share destination, dates, guests, and budget so I can continue.",
            acceptedMessage.assistantReply.message,
        )
    }

    @Test
    fun rejectsUserMessageForUnknownLocalSession() {
        val fixedInstant = Instant.parse("2026-06-04T00:00:00Z")
        val useCase = CreateAssistantSessionUseCase(
            clock = Clock.fixed(fixedInstant, ZoneOffset.UTC),
            idGenerator = AssistantSessionIdGenerator {
                AssistantSessionId("assistant-session-local-000001")
            },
            sessionStateStore = InMemoryAssistantSessionStateStore(),
        )

        val error = assertFailsWith<AssistantSessionNotFoundException> {
            useCase.acceptUserMessage(
                AcceptAssistantMessageCommand(
                    sessionId = AssistantSessionId("assistant-session-local-unknown"),
                    message = "I want a hotel in Rome",
                ),
            )
        }

        assertEquals("assistant-session-local-unknown", error.sessionId.value)
    }
}
