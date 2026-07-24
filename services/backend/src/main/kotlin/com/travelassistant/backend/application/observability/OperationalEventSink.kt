package com.travelassistant.backend.application.observability

fun interface OperationalEventSink {
    fun record(event: OperationalEvent)

    companion object {
        val NONE = OperationalEventSink { }
    }
}

fun OperationalEventSink.recordSafely(event: OperationalEvent) {
    runCatching { record(event) }
}
