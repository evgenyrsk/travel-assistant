package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

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
        assertEquals("collecting_requirements", session.clarificationState.phase.apiValue)
        assertEquals(true, session.clarificationState.awaitingUserInput)
        assertEquals(0, session.clarificationState.acceptedUserMessageCount)
        assertEquals(fixedInstant, session.clarificationState.createdAt)
        assertEquals(fixedInstant, session.clarificationState.updatedAt)
        assertNull(session.clarificationState.lastMessageReceivedAt)
    }

    @Test
    fun acceptsUserMessageAsLocalIntakeOnlyAndUpdatesClarificationState() {
        val fixedInstant = Instant.parse("2026-06-04T00:00:00Z")
        val sessionStateStore = InMemoryAssistantSessionStateStore()
        val useCase = CreateAssistantSessionUseCase(
            clock = Clock.fixed(fixedInstant, ZoneOffset.UTC),
            idGenerator = AssistantSessionIdGenerator {
                AssistantSessionId("assistant-session-local-000001")
            },
            sessionStateStore = sessionStateStore,
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
        assertEquals("collecting_requirements", acceptedMessage.clarificationState.phase.apiValue)
        assertEquals(true, acceptedMessage.clarificationState.awaitingUserInput)
        assertEquals(1, acceptedMessage.clarificationState.acceptedUserMessageCount)
        assertEquals(fixedInstant, acceptedMessage.clarificationState.createdAt)
        assertEquals(fixedInstant, acceptedMessage.clarificationState.updatedAt)
        assertEquals(fixedInstant, acceptedMessage.clarificationState.lastMessageReceivedAt)
        assertEquals("clarification", acceptedMessage.assistantReply.type.apiValue)
        assertEquals(
            "I received your hotel request. Please share destination, dates, guests, and budget so I can continue.",
            acceptedMessage.assistantReply.message,
        )

        val storedSession = sessionStateStore.findById(session.id)
        assertEquals(1, storedSession?.clarificationState?.acceptedUserMessageCount)
        assertEquals(fixedInstant, storedSession?.clarificationState?.lastMessageReceivedAt)
    }

    @Test
    fun countsMultipleValidUserMessagesForSameLocalSession() {
        val fixedInstant = Instant.parse("2026-06-04T00:00:00Z")
        val sessionStateStore = InMemoryAssistantSessionStateStore()
        val useCase = CreateAssistantSessionUseCase(
            clock = Clock.fixed(fixedInstant, ZoneOffset.UTC),
            idGenerator = AssistantSessionIdGenerator {
                AssistantSessionId("assistant-session-local-000001")
            },
            sessionStateStore = sessionStateStore,
        )

        val session = useCase.createSession()

        useCase.acceptUserMessage(
            AcceptAssistantMessageCommand(
                sessionId = session.id,
                message = "I want a hotel in Rome",
            ),
        )
        val secondAcceptedMessage = useCase.acceptUserMessage(
            AcceptAssistantMessageCommand(
                sessionId = session.id,
                message = "For two adults",
            ),
        )

        assertEquals(2, secondAcceptedMessage.clarificationState.acceptedUserMessageCount)
        assertEquals(
            2,
            sessionStateStore.findById(session.id)?.clarificationState?.acceptedUserMessageCount,
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
