package com.travelassistant.backend.infrastructure.provider

data class HotelProviderConfig(
    val mode: HotelProviderMode = HotelProviderMode.FAKE,
) {
    companion object {
        private const val ENV_KEY = "HOTEL_PROVIDER_MODE"

        fun fromEnvironment(): HotelProviderConfig {
            val raw = System.getenv(ENV_KEY)?.trim().orEmpty()
            val mode = runCatching { HotelProviderMode.valueOf(raw.uppercase()) }
                .getOrDefault(HotelProviderMode.FAKE)
            return HotelProviderConfig(mode = mode)
        }
    }
}
