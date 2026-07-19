package com.travelassistant.backend.infrastructure.llm

import com.travelassistant.backend.application.llm.LlmClient
import com.travelassistant.backend.application.llm.LlmCandidateRetryPolicy
import java.util.concurrent.atomic.AtomicBoolean

internal class LlmProviderRuntime(
    val client: LlmClient,
    val candidateRetryPolicy: LlmCandidateRetryPolicy = LlmCandidateRetryPolicy.NO_RETRY,
    private val closeAction: () -> Unit = {},
) : AutoCloseable {
    private val closed = AtomicBoolean(false)

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            closeAction()
        }
    }
}
