package com.travelassistant.backend.infrastructure.provider

data class HotelsApiJwtAuthConfig(
    val issuer: String = DEFAULT_ISSUER,
    val audience: String = DEFAULT_AUDIENCE,
    val privateKey: RedactedSecret,
) {
    init {
        validateNotBlank(ISSUER_KEY, issuer)
        validateNotBlank(AUDIENCE_KEY, audience)
    }

    private fun validateNotBlank(configurationKey: String, value: String) {
        if (value.isBlank()) {
            throw HotelProviderConfigurationException(
                configurationKey = configurationKey,
                reason = "must not be blank",
            )
        }
    }

    companion object {
        internal const val ISSUER_KEY = "HOTELS_API_JWT_ISSUER"
        internal const val AUDIENCE_KEY = "HOTELS_API_JWT_AUDIENCE"
        internal const val PRIVATE_KEY_KEY = "HOTELS_API_JWT_PRIVATE_KEY"

        internal const val DEFAULT_ISSUER = "HOTELSSEARCHAPI"
        internal const val DEFAULT_AUDIENCE = "HOTELSAPI"
    }
}
