package com.travelassistant.backend.application.observability

import java.util.concurrent.atomic.AtomicBoolean

class ServiceReadiness(
    initiallyReady: Boolean = false,
) {
    private val ready = AtomicBoolean(initiallyReady)

    fun markReady() {
        ready.set(true)
    }

    fun markNotReady() {
        ready.set(false)
    }

    fun isReady(): Boolean = ready.get()
}
