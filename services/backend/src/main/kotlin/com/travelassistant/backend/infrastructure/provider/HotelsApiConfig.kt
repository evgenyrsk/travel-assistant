package com.travelassistant.backend.infrastructure.provider

data class HotelsApiConfig(
    val publicTarget: HotelsApiTargetConfig = HotelsApiTargetConfig.publicDefault(),
    val userLanguage: String? = null,
) {
    init {
        if (userLanguage != null && userLanguage !in SUPPORTED_LANGUAGES) {
            throw HotelProviderConfigurationException(
                configurationKey = USER_LANGUAGE_KEY,
                reason = "must be omitted or one of RU, EN",
            )
        }
    }

    companion object {
        internal const val USER_LANGUAGE_KEY = "HOTELS_API_USER_LANGUAGE"

        private val SUPPORTED_LANGUAGES = setOf("RU", "EN")
    }
}
