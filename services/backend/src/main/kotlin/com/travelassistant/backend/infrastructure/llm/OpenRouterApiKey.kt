package com.travelassistant.backend.infrastructure.llm

internal class OpenRouterApiKey private constructor(
    private val value: String,
) {
    fun reveal(): String = value

    override fun toString(): String = REDACTED_VALUE

    companion object {
        private const val REDACTED_VALUE = "[REDACTED]"

        fun of(value: String): OpenRouterApiKey {
            if (value.isBlank()) {
                throw LlmProviderConfigurationException(
                    configurationKey = OpenRouterConfig.API_KEY_KEY,
                    reason = "must not be blank",
                )
            }

            return OpenRouterApiKey(value)
        }
    }
}
