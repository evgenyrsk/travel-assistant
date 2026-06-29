package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.hotel.HotelSearchId
import java.time.Instant

data class ConfirmedSearchExecutionAttempt(
    val idempotencyKey: ConfirmedSearchExecutionIdempotencyKey,
    val sessionId: AssistantSessionId,
    val commandPlan: ConfirmedSearchCreationCommandPlan.CommandReady,
    val status: ConfirmedSearchExecutionAttemptStatus,
    val createdSearchId: HotelSearchId? = null,
    val failureReason: ConfirmedSearchExecutionAttemptFailureReason? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
