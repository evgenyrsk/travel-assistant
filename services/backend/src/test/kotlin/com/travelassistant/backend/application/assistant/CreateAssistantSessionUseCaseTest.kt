package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.assistant.HotelRequirementSlotKey
import com.travelassistant.backend.domain.assistant.RequirementSlotStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
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
        assertEquals(fixedInstant, session.hotelRequirementsState.createdAt)
        assertEquals(fixedInstant, session.hotelRequirementsState.updatedAt)
        assertEquals(
            listOf(
                HotelRequirementSlotKey.DESTINATION,
                HotelRequirementSlotKey.STAY_DATES,
                HotelRequirementSlotKey.GUESTS,
                HotelRequirementSlotKey.PREFERENCES,
            ),
            session.hotelRequirementsState.slots.map { it.key },
        )
        assertEquals(
            listOf(
                RequirementSlotStatus.MISSING,
                RequirementSlotStatus.MISSING,
                RequirementSlotStatus.MISSING,
                RequirementSlotStatus.UNKNOWN,
            ),
            session.hotelRequirementsState.slots.map { it.status },
        )
        assertEquals(
            listOf(true, true, true, false),
            session.hotelRequirementsState.slots.map { it.requiredForHotelSearch },
        )
        assertEquals(3, session.hotelRequirementsCoveragePlan.requiredSlotCount)
        assertEquals(3, session.hotelRequirementsCoveragePlan.missingRequiredSlotCount)
        assertEquals(
            listOf(
                HotelRequirementSlotKey.DESTINATION,
                HotelRequirementSlotKey.STAY_DATES,
                HotelRequirementSlotKey.GUESTS,
            ),
            session.hotelRequirementsCoveragePlan.missingRequiredSlotKeys,
        )
        assertEquals(
            listOf(HotelRequirementSlotKey.PREFERENCES),
            session.hotelRequirementsCoveragePlan.optionalSlotKeys,
        )
        assertEquals(
            HotelRequirementSlotKey.DESTINATION,
            session.hotelRequirementsCoveragePlan.nextMissingRequiredSlotKey,
        )
        assertEquals(false, session.hotelRequirementsCoveragePlan.requiredHotelSearchInputsComplete)
    }

    @Test
    fun acceptsUserMessageAsLocalIntakeOnlyAndUpdatesClarificationStateWithoutFillingSlots() = runBlocking {
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
        val initialHotelRequirementsState = session.hotelRequirementsState
        val initialHotelRequirementsCoveragePlan = session.hotelRequirementsCoveragePlan

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
        assertEquals(initialHotelRequirementsState, acceptedMessage.hotelRequirementsState)
        assertEquals(initialHotelRequirementsCoveragePlan, acceptedMessage.hotelRequirementsCoveragePlan)
        assertEquals("clarification", acceptedMessage.assistantReply.type.apiValue)
        assertEquals(
            "I received your hotel request. Please share destination, dates, guests, and budget so I can continue.",
            acceptedMessage.assistantReply.message,
        )
        assertEquals(AssistantNextAction.ASK_CLARIFICATION, acceptedMessage.nextAction)
        assertNull(acceptedMessage.hotelSearchId)

        val storedSession = sessionStateStore.findById(session.id)
        assertEquals(1, storedSession?.clarificationState?.acceptedUserMessageCount)
        assertEquals(fixedInstant, storedSession?.clarificationState?.lastMessageReceivedAt)
        assertEquals(initialHotelRequirementsState, storedSession?.hotelRequirementsState)
        assertEquals(initialHotelRequirementsCoveragePlan, storedSession?.hotelRequirementsCoveragePlan)
    }

    @Test
    fun countsMultipleValidUserMessagesForSameLocalSession() = runBlocking {
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
    fun rejectsUserMessageForUnknownLocalSession() = runBlocking {
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
