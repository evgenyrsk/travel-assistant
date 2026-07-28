package com.travelassistant.backend.infrastructure.accommodation

import com.travelassistant.backend.infrastructure.llm.LlmProviderConfigurationException
import com.travelassistant.backend.infrastructure.llm.OpenRouterApiKey
import com.travelassistant.backend.infrastructure.llm.OpenRouterConfig
import java.util.Locale

internal data class AccommodationAnalysisProviderConfig(
    val mode: AccommodationAnalysisProviderMode = AccommodationAnalysisProviderMode.FAKE,
    val openRouter: OpenRouterAccommodationAnalysisConfig? = null,
) {
    init {
        if (mode == AccommodationAnalysisProviderMode.OPENROUTER && openRouter == null) {
            throw LlmProviderConfigurationException(
                OPENROUTER_CONFIG_KEY,
                "is required when $MODE_KEY=OPENROUTER",
            )
        }
    }

    companion object {
        internal const val MODE_KEY = "ACCOMMODATION_ANALYSIS_MODE"
        internal const val EXTERNAL_CONTENT_APPROVED_KEY =
            "ACCOMMODATION_ANALYSIS_EXTERNAL_CONTENT_APPROVED"
        private const val OPENROUTER_CONFIG_KEY = "ACCOMMODATION_ANALYSIS_OPENROUTER_CONFIG"

        fun fromEnvironment(
            environment: Map<String, String> = System.getenv(),
        ): AccommodationAnalysisProviderConfig {
            val mode = environment.optional(MODE_KEY)?.let { raw ->
                runCatching { AccommodationAnalysisProviderMode.valueOf(raw.uppercase(Locale.ROOT)) }
                    .getOrElse {
                        throw LlmProviderConfigurationException(
                            MODE_KEY,
                            "must be FAKE or OPENROUTER",
                        )
                    }
            } ?: AccommodationAnalysisProviderMode.FAKE
            if (mode == AccommodationAnalysisProviderMode.FAKE) {
                return AccommodationAnalysisProviderConfig()
            }
            if (!environment.optional(EXTERNAL_CONTENT_APPROVED_KEY).equals("true", true)) {
                throw LlmProviderConfigurationException(
                    EXTERNAL_CONTENT_APPROVED_KEY,
                    "must be true before OPENROUTER can receive provider content",
                )
            }

            return AccommodationAnalysisProviderConfig(
                mode = mode,
                openRouter = OpenRouterAccommodationAnalysisConfig(
                    apiKey = OpenRouterApiKey.of(environment.required(OpenRouterConfig.API_KEY_KEY)),
                    model = environment.required(OpenRouterAccommodationAnalysisConfig.MODEL_KEY),
                    providerEndpoint = environment.required(
                        OpenRouterAccommodationAnalysisConfig.PROVIDER_ENDPOINT_KEY,
                    ),
                    imageHosts = environment
                        .required(OpenRouterAccommodationAnalysisConfig.IMAGE_HOSTS_KEY)
                        .split(',')
                        .map(String::trim)
                        .filter(String::isNotEmpty)
                        .toSet(),
                    baseUrl = environment.optional(OpenRouterAccommodationAnalysisConfig.BASE_URL_KEY)
                        ?: OpenRouterAccommodationAnalysisConfig.DEFAULT_BASE_URL,
                    timeoutMillis = environment.optionalPositiveLong(
                        OpenRouterAccommodationAnalysisConfig.TIMEOUT_KEY,
                    ) ?: OpenRouterAccommodationAnalysisConfig.DEFAULT_TIMEOUT_MILLIS,
                    batchSize = environment.optionalPositiveInt(
                        OpenRouterAccommodationAnalysisConfig.BATCH_SIZE_KEY,
                    ) ?: OpenRouterAccommodationAnalysisConfig.DEFAULT_BATCH_SIZE,
                ),
            )
        }

        private fun Map<String, String>.required(key: String): String =
            optional(key) ?: throw LlmProviderConfigurationException(
                key,
                "is required when $MODE_KEY=OPENROUTER",
            )

        private fun Map<String, String>.optionalPositiveLong(key: String): Long? =
            optional(key)?.let { raw ->
                raw.toLongOrNull()?.takeIf { value -> value > 0 }
                    ?: throw LlmProviderConfigurationException(key, "must be a positive integer")
            }

        private fun Map<String, String>.optionalPositiveInt(key: String): Int? =
            optional(key)?.let { raw ->
                raw.toIntOrNull()?.takeIf { value -> value > 0 }
                    ?: throw LlmProviderConfigurationException(key, "must be a positive integer")
            }

        private fun Map<String, String>.optional(key: String): String? =
            this[key]?.trim()?.takeIf(String::isNotEmpty)
    }
}
