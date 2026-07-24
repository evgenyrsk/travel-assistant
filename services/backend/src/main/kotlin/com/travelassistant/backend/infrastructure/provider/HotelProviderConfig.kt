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
            val mode = if (raw.isEmpty()) {
                HotelProviderMode.FAKE
            } else {
                runCatching { HotelProviderMode.valueOf(raw.uppercase()) }
                    .getOrElse {
                        throw HotelProviderConfigurationException(
                            configurationKey = MODE_KEY,
                            reason = "must be FAKE or REAL",
                        )
                    }
            }

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
                    userLanguage = environment.optional(HotelsApiConfig.USER_LANGUAGE_KEY)
                        ?.uppercase(),
                ),
            )
        }

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
