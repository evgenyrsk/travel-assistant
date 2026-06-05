package com.travelassistant.backend.domain.assistant

import java.time.Instant

/**
 * Opaque current-session identity.
 *
 * Stage 7.3 uses local process-only identifiers. This is not an account
 * identity, persistent saved trip, auth subject, or accepted API contract.
 */
@JvmInline
value class AssistantSessionId(val value: String)

enum class AssistantSessionStatus(val apiValue: String) {
    COLLECTING_REQUIREMENTS("collecting_requirements"),
}

enum class AssistantClarificationPhase(val apiValue: String) {
    COLLECTING_REQUIREMENTS("collecting_requirements"),
}

data class AssistantClarificationState(
    val phase: AssistantClarificationPhase,
    val awaitingUserInput: Boolean,
    val acceptedUserMessageCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastMessageReceivedAt: Instant? = null,
) {
    fun recordAcceptedUserMessage(receivedAt: Instant): AssistantClarificationState =
        copy(
            awaitingUserInput = true,
            acceptedUserMessageCount = acceptedUserMessageCount + 1,
            updatedAt = receivedAt,
            lastMessageReceivedAt = receivedAt,
        )
}

data class AssistantSession(
    val id: AssistantSessionId,
    val status: AssistantSessionStatus,
    val createdAt: Instant,
    val clarificationState: AssistantClarificationState,
    val hotelRequirementsState: HotelRequirementsState,
) {
    fun recordAcceptedUserMessage(receivedAt: Instant): AssistantSession =
        copy(
            clarificationState = clarificationState.recordAcceptedUserMessage(receivedAt),
        )
}
