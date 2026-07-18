package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import java.util.concurrent.ConcurrentHashMap

interface AssistantHotelConstraintsStore {
    fun save(
        sessionId: AssistantSessionId,
        constraints: AssistantHotelConstraints,
    ): AssistantHotelConstraints

    fun findBySession(sessionId: AssistantSessionId): AssistantHotelConstraints?
}

class InMemoryAssistantHotelConstraintsStore : AssistantHotelConstraintsStore {
    private val constraintsBySession =
        ConcurrentHashMap<AssistantSessionId, AssistantHotelConstraints>()

    override fun save(
        sessionId: AssistantSessionId,
        constraints: AssistantHotelConstraints,
    ): AssistantHotelConstraints {
        constraintsBySession[sessionId] = constraints
        return constraints
    }

    override fun findBySession(sessionId: AssistantSessionId): AssistantHotelConstraints? =
        constraintsBySession[sessionId]
}
