package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import java.time.Instant

data class ConfirmedSearchExecutionGuardRequest(
    val sessionId: AssistantSessionId,
    val commandPlan: ConfirmedSearchCreationCommandPlan.CommandReady,
    val pendingConfirmation: PendingProceedWithCandidateConfirmation?,
    val now: Instant,
)
