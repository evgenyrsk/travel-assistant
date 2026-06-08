package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.assistant.HotelRequirementSlotKey
import com.travelassistant.backend.domain.assistant.RequirementSlotStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class UpdateHotelRequirementSlotUseCaseTest {

    @Test
    fun marksKnownRequiredSlotAsCollectedFromExplicitStructuredInput() {
        val sessionStateStore = InMemoryAssistantSessionStateStore()
        val useCase = newCreateSessionUseCase(sessionStateStore)
        val updateUseCase = newUpdateSlotUseCase(sessionStateStore)
        val session = useCase.createSession()

        val result = updateUseCase.updateHotelRequirementSlot(
            UpdateHotelRequirementSlotCommand(
                sessionId = session.id,
                slotKey = "destination",
                slotStatus = RequirementSlotStatus.COLLECTED,
            ),
        )

        val updated = assertIs<UpdateHotelRequirementSlotResult.Updated>(result)
        val destinationSlot = updated.hotelRequirementsState.slots.first {
            it.key == HotelRequirementSlotKey.DESTINATION
        }

        assertEquals(RequirementSlotStatus.COLLECTED, destinationSlot.status)
        assertEquals(Instant.parse("2026-06-04T01:00:00Z"), updated.updatedAt)
        assertEquals(
            RequirementSlotStatus.COLLECTED,
            sessionStateStore.findById(session.id)
                ?.hotelRequirementsState
                ?.slots
                ?.first { it.key == HotelRequirementSlotKey.DESTINATION }
                ?.status,
        )
    }

    @Test
    fun recomputesCoveragePlanAfterSlotUpdate() {
        val sessionStateStore = InMemoryAssistantSessionStateStore()
        val useCase = newCreateSessionUseCase(sessionStateStore)
        val updateUseCase = newUpdateSlotUseCase(sessionStateStore)
        val session = useCase.createSession()

        val result = updateUseCase.updateHotelRequirementSlot(
            UpdateHotelRequirementSlotCommand(
                sessionId = session.id,
                slotKey = "destination",
                slotStatus = RequirementSlotStatus.COLLECTED,
            ),
        )

        val updated = assertIs<UpdateHotelRequirementSlotResult.Updated>(result)

        assertEquals(3, updated.hotelRequirementsCoveragePlan.requiredSlotCount)
        assertEquals(2, updated.hotelRequirementsCoveragePlan.missingRequiredSlotCount)
        assertEquals(
            listOf(
                HotelRequirementSlotKey.STAY_DATES,
                HotelRequirementSlotKey.GUESTS,
            ),
            updated.hotelRequirementsCoveragePlan.missingRequiredSlotKeys,
        )
        assertEquals(false, updated.hotelRequirementsCoveragePlan.requiredHotelSearchInputsComplete)
    }

    @Test
    fun changesNextMissingRequiredSlotDeterministicallyAfterCollectingFirstRequiredSlot() {
        val sessionStateStore = InMemoryAssistantSessionStateStore()
        val useCase = newCreateSessionUseCase(sessionStateStore)
        val updateUseCase = newUpdateSlotUseCase(sessionStateStore)
        val session = useCase.createSession()

        val result = updateUseCase.updateHotelRequirementSlot(
            UpdateHotelRequirementSlotCommand(
                sessionId = session.id,
                slotKey = "destination",
                slotStatus = RequirementSlotStatus.COLLECTED,
            ),
        )

        val updated = assertIs<UpdateHotelRequirementSlotResult.Updated>(result)

        assertEquals(
            HotelRequirementSlotKey.STAY_DATES,
            updated.hotelRequirementsCoveragePlan.nextMissingRequiredSlotKey,
        )
    }

    @Test
    fun completingAllRequiredSlotsMarksInternalCoverageAsComplete() {
        val sessionStateStore = InMemoryAssistantSessionStateStore()
        val useCase = newCreateSessionUseCase(sessionStateStore)
        val updateUseCase = newUpdateSlotUseCase(sessionStateStore)
        val session = useCase.createSession()

        updateUseCase.collectSlot(session.id, "destination")
        updateUseCase.collectSlot(session.id, "stay_dates")
        val result = updateUseCase.collectSlot(session.id, "guests")

        val updated = assertIs<UpdateHotelRequirementSlotResult.Updated>(result)

        assertEquals(0, updated.hotelRequirementsCoveragePlan.missingRequiredSlotCount)
        assertEquals(emptyList(), updated.hotelRequirementsCoveragePlan.missingRequiredSlotKeys)
        assertNull(updated.hotelRequirementsCoveragePlan.nextMissingRequiredSlotKey)
        assertEquals(true, updated.hotelRequirementsCoveragePlan.requiredHotelSearchInputsComplete)
    }

    @Test
    fun optionalPreferencesSlotDoesNotBlockRequiredCompletion() {
        val sessionStateStore = InMemoryAssistantSessionStateStore()
        val useCase = newCreateSessionUseCase(sessionStateStore)
        val updateUseCase = newUpdateSlotUseCase(sessionStateStore)
        val session = useCase.createSession()

        updateUseCase.collectSlot(session.id, "destination")
        updateUseCase.collectSlot(session.id, "stay_dates")
        val result = updateUseCase.collectSlot(session.id, "guests")

        val updated = assertIs<UpdateHotelRequirementSlotResult.Updated>(result)

        assertEquals(
            listOf(HotelRequirementSlotKey.PREFERENCES),
            updated.hotelRequirementsCoveragePlan.optionalSlotKeys,
        )
        assertEquals(
            RequirementSlotStatus.UNKNOWN,
            updated.hotelRequirementsState.slots
                .first { it.key == HotelRequirementSlotKey.PREFERENCES }
                .status,
        )
        assertEquals(true, updated.hotelRequirementsCoveragePlan.requiredHotelSearchInputsComplete)
    }

    @Test
    fun handlesUnknownSessionIdSafely() {
        val updateUseCase = newUpdateSlotUseCase(InMemoryAssistantSessionStateStore())

        val result = updateUseCase.updateHotelRequirementSlot(
            UpdateHotelRequirementSlotCommand(
                sessionId = AssistantSessionId("assistant-session-local-unknown"),
                slotKey = "destination",
                slotStatus = RequirementSlotStatus.COLLECTED,
            ),
        )

        val sessionNotFound = assertIs<UpdateHotelRequirementSlotResult.SessionNotFound>(result)

        assertEquals("assistant-session-local-unknown", sessionNotFound.sessionId.value)
    }

    @Test
    fun handlesUnknownSlotKeySafely() {
        val sessionStateStore = InMemoryAssistantSessionStateStore()
        val useCase = newCreateSessionUseCase(sessionStateStore)
        val updateUseCase = newUpdateSlotUseCase(sessionStateStore)
        val session = useCase.createSession()

        val result = updateUseCase.updateHotelRequirementSlot(
            UpdateHotelRequirementSlotCommand(
                sessionId = session.id,
                slotKey = "budget",
                slotStatus = RequirementSlotStatus.COLLECTED,
            ),
        )

        val unknownSlotKey = assertIs<UpdateHotelRequirementSlotResult.UnknownSlotKey>(result)

        assertEquals(session.id, unknownSlotKey.sessionId)
        assertEquals("budget", unknownSlotKey.slotKey)
        assertEquals(session.hotelRequirementsState, sessionStateStore.findById(session.id)?.hotelRequirementsState)
    }

    private fun newCreateSessionUseCase(
        sessionStateStore: AssistantSessionStateStore,
    ): CreateAssistantSessionUseCase =
        CreateAssistantSessionUseCase(
            clock = Clock.fixed(Instant.parse("2026-06-04T00:00:00Z"), ZoneOffset.UTC),
            idGenerator = AssistantSessionIdGenerator {
                AssistantSessionId("assistant-session-local-000001")
            },
            sessionStateStore = sessionStateStore,
        )

    private fun newUpdateSlotUseCase(
        sessionStateStore: AssistantSessionStateStore,
    ): UpdateHotelRequirementSlotUseCase =
        UpdateHotelRequirementSlotUseCase(
            clock = Clock.fixed(Instant.parse("2026-06-04T01:00:00Z"), ZoneOffset.UTC),
            sessionStateStore = sessionStateStore,
        )

    private fun UpdateHotelRequirementSlotUseCase.collectSlot(
        sessionId: AssistantSessionId,
        slotKey: String,
    ): UpdateHotelRequirementSlotResult =
        updateHotelRequirementSlot(
            UpdateHotelRequirementSlotCommand(
                sessionId = sessionId,
                slotKey = slotKey,
                slotStatus = RequirementSlotStatus.COLLECTED,
            ),
        )
}
