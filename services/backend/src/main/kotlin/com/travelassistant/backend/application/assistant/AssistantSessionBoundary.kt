package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSession
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.assistant.AssistantSessionStatus
import java.time.Clock
import java.util.concurrent.atomic.AtomicInteger

/**
 * Minimal Stage 7.3 boundary for local assistant session creation.
 *
 * This boundary intentionally does not define persistence, retrieval, LLM
 * orchestration, provider calls, or production request/response contracts.
 */
interface AssistantSessionBoundary {
    fun createSession(): AssistantSession
}

fun interface AssistantSessionIdGenerator {
    fun nextId(): AssistantSessionId
}

class LocalAssistantSessionIdGenerator(
    private val prefix: String = "assistant-session-local",
) : AssistantSessionIdGenerator {
    private val nextValue = AtomicInteger(1)

    override fun nextId(): AssistantSessionId {
        val suffix = nextValue.getAndIncrement().toString().padStart(6, '0')
        return AssistantSessionId("$prefix-$suffix")
    }
}

class CreateAssistantSessionUseCase(
    private val clock: Clock = Clock.systemUTC(),
    private val idGenerator: AssistantSessionIdGenerator = LocalAssistantSessionIdGenerator(),
) : AssistantSessionBoundary {

    override fun createSession(): AssistantSession =
        AssistantSession(
            id = idGenerator.nextId(),
            status = AssistantSessionStatus.COLLECTING_REQUIREMENTS,
            createdAt = clock.instant(),
        )
}
