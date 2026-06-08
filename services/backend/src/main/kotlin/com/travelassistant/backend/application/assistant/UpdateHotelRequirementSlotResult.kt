package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.assistant.HotelRequirementsCoveragePlan
import com.travelassistant.backend.domain.assistant.HotelRequirementsState
import java.time.Instant

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
