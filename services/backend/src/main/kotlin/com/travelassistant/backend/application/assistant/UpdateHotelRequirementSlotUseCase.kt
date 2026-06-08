package com.travelassistant.backend.application.assistant

import java.time.Clock

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
