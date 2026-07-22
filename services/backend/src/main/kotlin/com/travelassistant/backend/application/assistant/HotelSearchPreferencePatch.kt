package com.travelassistant.backend.application.assistant

sealed interface HotelSearchPreferencePatch<out T> {
    data object Keep : HotelSearchPreferencePatch<Nothing>

    data object Clear : HotelSearchPreferencePatch<Nothing>

    data class Set<T>(
        val value: T,
    ) : HotelSearchPreferencePatch<T>
}
