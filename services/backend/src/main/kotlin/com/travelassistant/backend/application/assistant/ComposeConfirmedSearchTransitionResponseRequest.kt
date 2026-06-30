package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import java.time.Instant

data class ComposeConfirmedSearchTransitionResponseRequest(
    val sessionId: AssistantSessionId,
    val decision: PostConfirmationDecision.Confirmed,
    val pendingConfirmation: PendingProceedWithCandidateConfirmation?,
    val now: Instant,
)
