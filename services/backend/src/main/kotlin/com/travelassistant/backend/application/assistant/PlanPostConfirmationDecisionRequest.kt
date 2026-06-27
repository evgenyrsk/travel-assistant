package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import java.time.Instant

data class PlanPostConfirmationDecisionRequest(
    val sessionId: AssistantSessionId,
    val replyText: String,
    val now: Instant,
)
