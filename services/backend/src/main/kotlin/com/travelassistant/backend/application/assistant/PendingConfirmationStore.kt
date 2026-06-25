package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import java.time.Instant

interface PendingConfirmationStore {
    fun save(
        pendingConfirmation: PendingProceedWithCandidateConfirmation,
    ): PendingProceedWithCandidateConfirmation

    fun findActiveBySession(
        sessionId: AssistantSessionId,
        now: Instant,
    ): PendingProceedWithCandidateConfirmation?

    fun markConsumed(
        sessionId: AssistantSessionId,
        consumedAt: Instant,
    ): PendingProceedWithCandidateConfirmation?
}
