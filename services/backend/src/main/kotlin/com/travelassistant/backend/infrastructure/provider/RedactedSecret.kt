package com.travelassistant.backend.infrastructure.provider

class RedactedSecret private constructor(
    private val value: String,
) {
    internal fun reveal(): String = value

    override fun toString(): String = REDACTED_VALUE

    companion object {
        private const val REDACTED_VALUE = "[REDACTED]"
        private const val CLIENT_SECRET_KEY = "HOTELS_API_CLIENT_SECRET"

        fun of(value: String): RedactedSecret {
            if (value.isBlank()) {
                throw HotelProviderConfigurationException(
                    configurationKey = CLIENT_SECRET_KEY,
                    reason = "must not be blank",
                )
            }

            return RedactedSecret(value)
        }
    }
}
