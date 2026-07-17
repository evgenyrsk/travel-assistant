package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.HotelOfferProviderBoundary
import java.util.concurrent.atomic.AtomicBoolean

internal class HotelOfferProviderRuntime(
    val provider: HotelOfferProviderBoundary,
    private val closeAction: () -> Unit = {},
) : AutoCloseable {
    private val closed = AtomicBoolean(false)

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            closeAction()
        }
    }
}
