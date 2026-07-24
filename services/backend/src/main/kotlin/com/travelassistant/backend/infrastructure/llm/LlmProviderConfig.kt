package com.travelassistant.backend.infrastructure.llm

internal data class LlmProviderConfig(
    val mode: LlmProviderMode = LlmProviderMode.FAKE,
    val openRouter: OpenRouterConfig? = null,
) {
    init {
        if (mode == LlmProviderMode.OPENROUTER && openRouter == null) {
            throw LlmProviderConfigurationException(
                configurationKey = OPENROUTER_CONFIG_KEY,
                reason = "is required when LLM_PROVIDER_MODE=OPENROUTER",
            )
        }
    }

    companion object {
        internal const val MODE_KEY = "LLM_PROVIDER_MODE"
        private const val OPENROUTER_CONFIG_KEY = "OPENROUTER_CONFIG"

        fun fromEnvironment(environment: Map<String, String> = System.getenv()): LlmProviderConfig {
            val mode = parseMode(environment.optional(MODE_KEY))
            if (mode == LlmProviderMode.FAKE) {
                return LlmProviderConfig()
            }

            return LlmProviderConfig(
                mode = LlmProviderMode.OPENROUTER,
                openRouter = OpenRouterConfig(
                    apiKey = OpenRouterApiKey.of(environment.required(OpenRouterConfig.API_KEY_KEY)),
                    model = environment.required(OpenRouterConfig.MODEL_KEY),
                    baseUrl = environment.optional(OpenRouterConfig.BASE_URL_KEY)
                        ?: OpenRouterConfig.DEFAULT_BASE_URL,
                    timeoutMillis = environment.optionalPositiveLong(OpenRouterConfig.TIMEOUT_KEY)
                        ?: OpenRouterConfig.DEFAULT_TIMEOUT_MILLIS,
                ),
            )
        }

        private fun parseMode(rawMode: String?): LlmProviderMode {
            if (rawMode == null) {
                return LlmProviderMode.FAKE
            }

            return runCatching { LlmProviderMode.valueOf(rawMode.uppercase()) }
                .getOrElse {
                    throw LlmProviderConfigurationException(
                        configurationKey = MODE_KEY,
                        reason = "must be FAKE or OPENROUTER",
                    )
                }
        }

        private fun Map<String, String>.required(configurationKey: String): String =
            optional(configurationKey)
                ?: throw LlmProviderConfigurationException(
                    configurationKey = configurationKey,
                    reason = "is required when LLM_PROVIDER_MODE=OPENROUTER",
                )

        private fun Map<String, String>.optionalPositiveLong(configurationKey: String): Long? {
            val raw = optional(configurationKey) ?: return null
            return raw.toLongOrNull()
                ?.takeIf { value -> value > 0 }
                ?: throw LlmProviderConfigurationException(
                    configurationKey = configurationKey,
                    reason = "must be a positive integer",
                )
        }

        private fun Map<String, String>.optional(configurationKey: String): String? =
            this[configurationKey]?.trim()?.takeIf(String::isNotEmpty)
    }
}
