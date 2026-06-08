package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.assistant.HotelRequirementsCoveragePlan
import com.travelassistant.backend.domain.assistant.HotelRequirementsState
import com.travelassistant.backend.domain.assistant.RequirementSlotStatus
import java.time.Clock
import java.time.Instant

interface HotelRequirementsSlotUpdateBoundary {
    fun updateHotelRequirementSlot(
        command: UpdateHotelRequirementSlotCommand,
    ): UpdateHotelRequirementSlotResult
}

data class UpdateHotelRequirementSlotCommand(
    val sessionId: AssistantSessionId,
    val slotKey: String,
    val slotStatus: RequirementSlotStatus,
)

sealed interface UpdateHotelRequirementSlotResult {
    data class Updated(
        val sessionId: AssistantSessionId,
        val updatedAt: Instant,
        val hotelRequirementsState: HotelRequirementsState,
        val hotelRequirementsCoveragePlan: HotelRequirementsCoveragePlan,
    ) : UpdateHotelRequirementSlotResult

    data class SessionNotFound(
        val sessionId: AssistantSessionId,
    ) : UpdateHotelRequirementSlotResult

    data class UnknownSlotKey(
        val sessionId: AssistantSessionId,
        val slotKey: String,
    ) : UpdateHotelRequirementSlotResult
}

class UpdateHotelRequirementSlotUseCase(
    private val clock: Clock = Clock.systemUTC(),
    private val sessionStateStore: AssistantSessionStateStore,
) : HotelRequirementsSlotUpdateBoundary {

    override fun updateHotelRequirementSlot(
        command: UpdateHotelRequirementSlotCommand,
    ): UpdateHotelRequirementSlotResult {
        val session = sessionStateStore.findById(command.sessionId)
            ?: return UpdateHotelRequirementSlotResult.SessionNotFound(command.sessionId)
        val slotKey = session.hotelRequirementsState.slots
            .firstOrNull { it.key.value == command.slotKey }
            ?.key
            ?: return UpdateHotelRequirementSlotResult.UnknownSlotKey(
                sessionId = command.sessionId,
                slotKey = command.slotKey,
            )
        val updatedAt = clock.instant()
        val updatedSession = session.updateHotelRequirementSlot(
            slotKey = slotKey,
            slotStatus = command.slotStatus,
            updatedAt = updatedAt,
        )
            ?: return UpdateHotelRequirementSlotResult.UnknownSlotKey(
                sessionId = command.sessionId,
                slotKey = command.slotKey,
            )

        sessionStateStore.save(updatedSession)

        return UpdateHotelRequirementSlotResult.Updated(
            sessionId = updatedSession.id,
            updatedAt = updatedAt,
            hotelRequirementsState = updatedSession.hotelRequirementsState,
            hotelRequirementsCoveragePlan = updatedSession.hotelRequirementsCoveragePlan,
        )
    }
}
