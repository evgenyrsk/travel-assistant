package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSession
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-local Stage 7.6-7.9 state boundary for local assistant sessions,
 * minimal clarification metadata, foundation-only hotel slot metadata, and
 * internal slot coverage planning metadata.
 *
 * This is not durable persistence, account storage, session history,
 * multi-instance coordination, or a production repository contract.
 */
interface AssistantSessionStateStore {
    fun save(session: AssistantSession): AssistantSession

    fun findById(sessionId: AssistantSessionId): AssistantSession?
}

class InMemoryAssistantSessionStateStore : AssistantSessionStateStore {
    private val sessions = ConcurrentHashMap<AssistantSessionId, AssistantSession>()

    override fun save(session: AssistantSession): AssistantSession {
        sessions[session.id] = session
        return session
    }

    override fun findById(sessionId: AssistantSessionId): AssistantSession? = sessions[sessionId]
}
