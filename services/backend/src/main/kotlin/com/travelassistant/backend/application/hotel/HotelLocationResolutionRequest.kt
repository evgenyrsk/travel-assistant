package com.travelassistant.backend.application.hotel

internal data class HotelLocationResolutionRequest(
    val query: String,
    val language: Language? = null,
) {
    enum class Language {
        RU,
        EN,
    }
}
