package com.travelassistant.backend.infrastructure.accommodation

import com.travelassistant.backend.application.accommodation.AccommodationAnalysisClient
import java.util.concurrent.atomic.AtomicBoolean

internal class AccommodationAnalysisProviderRuntime(
    val client: AccommodationAnalysisClient,
    private val closeAction: () -> Unit = {},
) : AutoCloseable {
    private val closed = AtomicBoolean(false)

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            closeAction()
        }
    }
}
