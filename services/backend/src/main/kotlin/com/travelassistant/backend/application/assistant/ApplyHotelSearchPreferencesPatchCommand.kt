package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId

data class ApplyHotelSearchPreferencesPatchCommand(
    val sessionId: AssistantSessionId,
    val patch: HotelSearchPreferencesPatch,
)
