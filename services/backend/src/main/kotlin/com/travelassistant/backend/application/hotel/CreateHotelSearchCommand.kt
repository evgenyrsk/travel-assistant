package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria

data class CreateHotelSearchCommand(
    val sessionId: AssistantSessionId,
    val criteria: HotelSearchCriteria,
)
