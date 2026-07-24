package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex

/**
 * Serializes the complete process-local message turn for one assistant session.
 * Different sessions remain independent.
 */
internal class AssistantSessionTurnCoordinator {
    private val mutexes = ConcurrentHashMap<AssistantSessionId, SessionMutex>()

    suspend fun <T> execute(
        sessionId: AssistantSessionId,
        action: suspend () -> T,
    ): T {
        val sessionMutex = register(sessionId)
        var locked = false
        try {
            sessionMutex.mutex.lock()
            locked = true
            return action()
        } finally {
            if (locked) {
                sessionMutex.mutex.unlock()
            }
            unregister(sessionId, sessionMutex)
        }
    }

    private fun register(sessionId: AssistantSessionId): SessionMutex =
        checkNotNull(
            mutexes.compute(sessionId) { _, current ->
                (current ?: SessionMutex()).also { sessionMutex ->
                    sessionMutex.users += 1
                }
            },
        )

    private fun unregister(
        sessionId: AssistantSessionId,
        expected: SessionMutex,
    ) {
        mutexes.compute(sessionId) { _, current ->
            check(current === expected)
            expected.users -= 1
            expected.takeIf { sessionMutex -> sessionMutex.users > 0 }
        }
    }

    private data class SessionMutex(
        val mutex: Mutex = Mutex(),
        var users: Int = 0,
    )
}
