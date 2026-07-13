package com.travelassistant.backend.infrastructure.provider

class RedactedSecret private constructor(
    private val value: String,
) {
    internal fun reveal(): String = value

    override fun toString(): String = REDACTED_VALUE

    companion object {
        private const val REDACTED_VALUE = "[REDACTED]"

        fun of(value: String, configurationKey: String): RedactedSecret {
            if (value.isBlank()) {
                throw HotelProviderConfigurationException(
                    configurationKey = configurationKey,
                    reason = "must not be blank",
                )
            }

            return RedactedSecret(value)
        }
    }
}
