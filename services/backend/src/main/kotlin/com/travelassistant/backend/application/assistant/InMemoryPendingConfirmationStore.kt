package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class InMemoryPendingConfirmationStore : PendingConfirmationStore {
    private val pendingConfirmations =
        ConcurrentHashMap<AssistantSessionId, PendingProceedWithCandidateConfirmation>()

    override fun save(
        pendingConfirmation: PendingProceedWithCandidateConfirmation,
    ): PendingProceedWithCandidateConfirmation {
        pendingConfirmations[pendingConfirmation.sessionId] = pendingConfirmation
        return pendingConfirmation
    }

    override fun findActiveBySession(
        sessionId: AssistantSessionId,
        now: Instant,
    ): PendingProceedWithCandidateConfirmation? =
        pendingConfirmations[sessionId]
            ?.takeIf { pendingConfirmation -> pendingConfirmation.isActiveAt(now) }

    override fun markConsumed(
        sessionId: AssistantSessionId,
        consumedAt: Instant,
    ): PendingProceedWithCandidateConfirmation? {
        val existingConfirmation = pendingConfirmations[sessionId] ?: return null
        val consumedConfirmation = existingConfirmation.copy(
            updatedAt = consumedAt,
            status = PendingConfirmationStatus.CONSUMED,
        )

        pendingConfirmations[sessionId] = consumedConfirmation
        return consumedConfirmation
    }
}
