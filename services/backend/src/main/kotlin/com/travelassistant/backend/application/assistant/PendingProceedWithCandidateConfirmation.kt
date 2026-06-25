package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import java.time.Instant

data class PendingProceedWithCandidateConfirmation(
    val sessionId: AssistantSessionId,
    val criteria: ProceedWithCandidateCriteria,
    val proposal: ProceedWithCandidateConfirmationProposal,
    val createdAt: Instant,
    val updatedAt: Instant,
    val expiresAt: Instant,
    val status: PendingConfirmationStatus = PendingConfirmationStatus.PENDING,
) {
    fun isActiveAt(now: Instant): Boolean =
        statusAt(now) == PendingConfirmationStatus.PENDING

    fun statusAt(now: Instant): PendingConfirmationStatus =
        if (status == PendingConfirmationStatus.PENDING && !now.isBefore(expiresAt)) {
            PendingConfirmationStatus.EXPIRED
        } else {
            status
        }
}
