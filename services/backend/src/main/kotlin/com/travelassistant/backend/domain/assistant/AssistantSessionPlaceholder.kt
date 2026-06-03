package com.travelassistant.backend.domain.assistant

/**
 * Opaque current-session identity placeholder.
 *
 * This is not an account identity, persistent saved trip, auth subject,
 * or accepted API contract.
 */
@JvmInline
value class AssistantSessionId(val value: String)
