package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSession
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.assistant.AssistantSessionStatus
import java.time.Clock
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

/**
 * Minimal Stage 7.3-7.6 boundary for local assistant session behavior.
 *
 * This boundary intentionally does not define durable persistence, retrieval
 * endpoints, message history, LLM orchestration, provider calls, or production
 * request/response contracts.
 */
interface AssistantSessionBoundary {
    fun createSession(): AssistantSession

    fun acceptUserMessage(command: AcceptAssistantMessageCommand): AcceptedAssistantMessage
}

data class AcceptAssistantMessageCommand(
    val sessionId: AssistantSessionId,
    val message: String,
)

data class AcceptedAssistantMessage(
    val sessionId: AssistantSessionId,
    val status: AssistantSessionStatus,
    val receivedAt: Instant,
    val assistantReply: AssistantReply,
)

enum class AssistantReplyType(val apiValue: String) {
    CLARIFICATION("clarification"),
}

data class AssistantReply(
    val type: AssistantReplyType,
    val message: String,
)

class AssistantSessionNotFoundException(
    val sessionId: AssistantSessionId,
) : RuntimeException("Assistant session was not found: ${sessionId.value}")

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
    private val sessionStateStore: AssistantSessionStateStore = InMemoryAssistantSessionStateStore(),
) : AssistantSessionBoundary {

    override fun createSession(): AssistantSession =
        sessionStateStore.save(
            AssistantSession(
                id = idGenerator.nextId(),
                status = AssistantSessionStatus.COLLECTING_REQUIREMENTS,
                createdAt = clock.instant(),
            ),
        )

    override fun acceptUserMessage(command: AcceptAssistantMessageCommand): AcceptedAssistantMessage {
        val session = sessionStateStore.findById(command.sessionId)
            ?: throw AssistantSessionNotFoundException(command.sessionId)

        return AcceptedAssistantMessage(
            sessionId = session.id,
            status = session.status,
            receivedAt = clock.instant(),
            assistantReply = AssistantReply(
                type = AssistantReplyType.CLARIFICATION,
                message = "I received your hotel request. Please share destination, dates, guests, and budget so I can continue.",
            ),
        )
    }
}
