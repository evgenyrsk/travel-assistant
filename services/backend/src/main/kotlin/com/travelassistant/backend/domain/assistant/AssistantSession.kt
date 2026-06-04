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

data class AssistantSession(
    val id: AssistantSessionId,
    val status: AssistantSessionStatus,
    val createdAt: Instant,
)
