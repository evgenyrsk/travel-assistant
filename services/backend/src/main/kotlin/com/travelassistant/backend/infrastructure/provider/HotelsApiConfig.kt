package com.travelassistant.backend.infrastructure.provider

data class HotelsApiConfig(
    val publicTarget: HotelsApiTargetConfig = HotelsApiTargetConfig.publicDefault(),
    val privateTarget: HotelsApiTargetConfig = HotelsApiTargetConfig.privateDefault(),
    val jwtAuth: HotelsApiJwtAuthConfig,
    val userLanguage: String? = null,
    val sourcePlatform: String? = null,
    val appVersion: String? = null,
) {
    init {
        validateOptionalHeader(USER_LANGUAGE_KEY, userLanguage)
        validateOptionalHeader(SOURCE_PLATFORM_KEY, sourcePlatform)
        validateOptionalHeader(APP_VERSION_KEY, appVersion)
    }

    private fun validateOptionalHeader(configurationKey: String, value: String?) {
        if (value != null && value.isBlank()) {
            throw HotelProviderConfigurationException(
                configurationKey = configurationKey,
                reason = "must be omitted or non-blank",
            )
        }
    }

    companion object {
        internal const val USER_LANGUAGE_KEY = "HOTELS_API_USER_LANGUAGE"
        internal const val SOURCE_PLATFORM_KEY = "HOTELS_API_SOURCE_PLATFORM"
        internal const val APP_VERSION_KEY = "HOTELS_API_APP_VERSION"
    }
}
