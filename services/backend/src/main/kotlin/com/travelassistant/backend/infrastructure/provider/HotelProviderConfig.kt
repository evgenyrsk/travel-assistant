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
                    baseUrl = environment.required(HotelsApiConfig.BASE_URL_KEY),
                    tokenUrl = environment.required(HotelsApiConfig.TOKEN_URL_KEY),
                    clientId = environment.required(HotelsApiConfig.CLIENT_ID_KEY),
                    clientSecret = RedactedSecret.of(
                        environment.required(HotelsApiConfig.CLIENT_SECRET_KEY),
                    ),
                    scope = environment.required(HotelsApiConfig.SCOPE_KEY),
                    connectTimeoutMillis = environment.requiredPositiveLong(
                        HotelsApiConfig.CONNECT_TIMEOUT_KEY,
                    ),
                    requestTimeoutMillis = environment.requiredPositiveLong(
                        HotelsApiConfig.REQUEST_TIMEOUT_KEY,
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

        private fun Map<String, String>.requiredPositiveLong(configurationKey: String): Long {
            val raw = required(configurationKey)
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
