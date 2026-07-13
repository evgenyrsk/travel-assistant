package com.travelassistant.backend.infrastructure.provider

data class HotelProviderConfig(
    val mode: HotelProviderMode = HotelProviderMode.FAKE,
    val hotelsApi: HotelsApiConfig? = null,
) {
    init {
        if (mode == HotelProviderMode.REAL && hotelsApi == null) {
            throw HotelProviderConfigurationException(
                configurationKey = HOTELS_API_CONFIG_KEY,
                reason = "is required when HOTEL_PROVIDER_MODE=REAL",
            )
        }
    }

    companion object {
        private const val MODE_KEY = "HOTEL_PROVIDER_MODE"
        private const val HOTELS_API_CONFIG_KEY = "HOTELS_API_CONFIG"

        fun fromEnvironment(environment: Map<String, String> = System.getenv()): HotelProviderConfig {
            val raw = environment[MODE_KEY]?.trim().orEmpty()
            val mode = runCatching { HotelProviderMode.valueOf(raw.uppercase()) }
                .getOrDefault(HotelProviderMode.FAKE)

            if (mode == HotelProviderMode.FAKE) {
                return HotelProviderConfig()
            }

            return HotelProviderConfig(
                mode = HotelProviderMode.REAL,
                hotelsApi = HotelsApiConfig(
                    publicTarget = HotelsApiTargetConfig.public(
                        baseUrl = environment.optional(HotelsApiTargetConfig.PUBLIC_BASE_URL_KEY)
                            ?: HotelsApiTargetConfig.DEFAULT_PUBLIC_BASE_URL,
                        timeoutMillis = environment.optionalPositiveLong(
                            HotelsApiTargetConfig.PUBLIC_TIMEOUT_KEY,
                        ) ?: HotelsApiTargetConfig.DEFAULT_PUBLIC_TIMEOUT_MILLIS,
                    ),
                    privateTarget = HotelsApiTargetConfig.private(
                        baseUri = environment.optional(HotelsApiTargetConfig.PRIVATE_BASE_URI_KEY)
                            ?: HotelsApiTargetConfig.DEFAULT_PRIVATE_BASE_URI,
                        timeoutMillis = environment.optionalPositiveLong(
                            HotelsApiTargetConfig.PRIVATE_TIMEOUT_KEY,
                        ) ?: HotelsApiTargetConfig.DEFAULT_PRIVATE_TIMEOUT_MILLIS,
                    ),
                    jwtAuth = HotelsApiJwtAuthConfig(
                        issuer = environment.optional(HotelsApiJwtAuthConfig.ISSUER_KEY)
                            ?: HotelsApiJwtAuthConfig.DEFAULT_ISSUER,
                        audience = environment.optional(HotelsApiJwtAuthConfig.AUDIENCE_KEY)
                            ?: HotelsApiJwtAuthConfig.DEFAULT_AUDIENCE,
                        privateKey = RedactedSecret.of(
                            value = environment.required(HotelsApiJwtAuthConfig.PRIVATE_KEY_KEY),
                            configurationKey = HotelsApiJwtAuthConfig.PRIVATE_KEY_KEY,
                        ),
                    ),
                    userLanguage = environment.optional(HotelsApiConfig.USER_LANGUAGE_KEY),
                    sourcePlatform = environment.optional(HotelsApiConfig.SOURCE_PLATFORM_KEY),
                    appVersion = environment.optional(HotelsApiConfig.APP_VERSION_KEY),
                ),
            )
        }

        private fun Map<String, String>.required(configurationKey: String): String =
            this[configurationKey]
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: throw HotelProviderConfigurationException(
                    configurationKey = configurationKey,
                    reason = "is required when HOTEL_PROVIDER_MODE=REAL",
                )

        private fun Map<String, String>.optionalPositiveLong(configurationKey: String): Long? {
            val raw = optional(configurationKey) ?: return null
            return raw.toLongOrNull()
                ?: throw HotelProviderConfigurationException(
                    configurationKey = configurationKey,
                    reason = "must be a positive integer",
                )
        }

        private fun Map<String, String>.optional(configurationKey: String): String? =
            this[configurationKey]?.trim()?.takeIf(String::isNotEmpty)
    }
}
