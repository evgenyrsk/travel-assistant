package com.travelassistant.backend.infrastructure.accommodation

import com.travelassistant.backend.infrastructure.llm.LlmProviderConfigurationException

internal class InternalGatewayAccessToken private constructor(
    private val value: String,
) {
    fun reveal(): String = value

    override fun toString(): String = REDACTED_VALUE

    companion object {
        private const val REDACTED_VALUE = "[REDACTED]"

        fun of(value: String): InternalGatewayAccessToken {
            if (value.isBlank()) {
                throw LlmProviderConfigurationException(
                    configurationKey = InternalGatewayAccommodationAnalysisConfig.ACCESS_TOKEN_KEY,
                    reason = "must not be blank",
                )
            }
            return InternalGatewayAccessToken(value)
        }
    }
}
