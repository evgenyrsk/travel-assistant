package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.assistant.RequirementSlotStatus

data class UpdateHotelRequirementSlotCommand(
    val sessionId: AssistantSessionId,
    val slotKey: String,
    val slotStatus: RequirementSlotStatus,
)
