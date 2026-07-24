package com.travelassistant.backend.infrastructure.observability

import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventSink
import com.travelassistant.backend.application.observability.recordSafely

class CompositeOperationalEventSink(
    private vararg val sinks: OperationalEventSink,
) : OperationalEventSink {
    override fun record(event: OperationalEvent) {
        sinks.forEach { sink -> sink.recordSafely(event) }
    }
}
