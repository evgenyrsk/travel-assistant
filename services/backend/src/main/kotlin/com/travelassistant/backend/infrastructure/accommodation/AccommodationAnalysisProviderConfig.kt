package com.travelassistant.backend.infrastructure.accommodation

import com.travelassistant.backend.infrastructure.llm.LlmProviderConfigurationException
import com.travelassistant.backend.infrastructure.llm.OpenRouterApiKey
import com.travelassistant.backend.infrastructure.llm.OpenRouterConfig
import java.util.Locale

internal data class AccommodationAnalysisProviderConfig(
    val mode: AccommodationAnalysisProviderMode = AccommodationAnalysisProviderMode.FAKE,
    val openRouter: OpenRouterAccommodationAnalysisConfig? = null,
    val internalGateway: InternalGatewayAccommodationAnalysisConfig? = null,
) {
    init {
        if (mode == AccommodationAnalysisProviderMode.OPENROUTER && openRouter == null) {
            throw LlmProviderConfigurationException(
                OPENROUTER_CONFIG_KEY,
                "is required when $MODE_KEY=OPENROUTER",
            )
        }
        if (
            mode == AccommodationAnalysisProviderMode.INTERNAL_GATEWAY &&
            internalGateway == null
        ) {
            throw LlmProviderConfigurationException(
                INTERNAL_GATEWAY_CONFIG_KEY,
                "is required when $MODE_KEY=INTERNAL_GATEWAY",
            )
        }
    }

    companion object {
        internal const val MODE_KEY = "ACCOMMODATION_ANALYSIS_MODE"
        internal const val EXTERNAL_CONTENT_APPROVED_KEY =
            "ACCOMMODATION_ANALYSIS_EXTERNAL_CONTENT_APPROVED"
        internal const val INTERNAL_CONTENT_APPROVED_KEY =
            "ACCOMMODATION_ANALYSIS_INTERNAL_CONTENT_APPROVED"
        private const val OPENROUTER_CONFIG_KEY = "ACCOMMODATION_ANALYSIS_OPENROUTER_CONFIG"
        private const val INTERNAL_GATEWAY_CONFIG_KEY =
            "ACCOMMODATION_ANALYSIS_INTERNAL_GATEWAY_CONFIG"

        fun fromEnvironment(
            environment: Map<String, String> = System.getenv(),
        ): AccommodationAnalysisProviderConfig {
            val mode = environment.optional(MODE_KEY)?.let { raw ->
                runCatching { AccommodationAnalysisProviderMode.valueOf(raw.uppercase(Locale.ROOT)) }
                    .getOrElse {
                        throw LlmProviderConfigurationException(
                            MODE_KEY,
                            "must be FAKE, OPENROUTER, or INTERNAL_GATEWAY",
                        )
                    }
            } ?: AccommodationAnalysisProviderMode.FAKE
            return when (mode) {
                AccommodationAnalysisProviderMode.FAKE -> AccommodationAnalysisProviderConfig()
                AccommodationAnalysisProviderMode.OPENROUTER -> openRouterConfig(environment)
                AccommodationAnalysisProviderMode.INTERNAL_GATEWAY ->
                    internalGatewayConfig(environment)
            }
        }

        private fun openRouterConfig(
            environment: Map<String, String>,
        ): AccommodationAnalysisProviderConfig {
            environment.requireApproval(
                EXTERNAL_CONTENT_APPROVED_KEY,
                "must be true before OPENROUTER can receive provider content",
            )
            return AccommodationAnalysisProviderConfig(
                mode = AccommodationAnalysisProviderMode.OPENROUTER,
                openRouter = OpenRouterAccommodationAnalysisConfig(
                    apiKey = OpenRouterApiKey.of(environment.required(OpenRouterConfig.API_KEY_KEY)),
                    model = environment.required(OpenRouterAccommodationAnalysisConfig.MODEL_KEY),
                    providerEndpoint = environment.required(
                        OpenRouterAccommodationAnalysisConfig.PROVIDER_ENDPOINT_KEY,
                    ),
                    imageHosts = environment.requiredImageHosts(
                        OpenRouterAccommodationAnalysisConfig.IMAGE_HOSTS_KEY,
                    ),
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

        private fun internalGatewayConfig(
            environment: Map<String, String>,
        ): AccommodationAnalysisProviderConfig {
            environment.requireApproval(
                INTERNAL_CONTENT_APPROVED_KEY,
                "must be true before INTERNAL_GATEWAY can receive provider content",
            )
            return AccommodationAnalysisProviderConfig(
                mode = AccommodationAnalysisProviderMode.INTERNAL_GATEWAY,
                internalGateway = InternalGatewayAccommodationAnalysisConfig(
                    endpointUrl = environment.required(
                        InternalGatewayAccommodationAnalysisConfig.ENDPOINT_URL_KEY,
                    ),
                    deploymentId = environment.required(
                        InternalGatewayAccommodationAnalysisConfig.DEPLOYMENT_ID_KEY,
                    ),
                    accessToken = InternalGatewayAccessToken.of(
                        environment.required(
                            InternalGatewayAccommodationAnalysisConfig.ACCESS_TOKEN_KEY,
                        ),
                    ),
                    imageHosts = environment.requiredImageHosts(
                        InternalGatewayAccommodationAnalysisConfig.IMAGE_HOSTS_KEY,
                    ),
                    timeoutMillis = environment.optionalPositiveLong(
                        InternalGatewayAccommodationAnalysisConfig.TIMEOUT_KEY,
                    ) ?: InternalGatewayAccommodationAnalysisConfig.DEFAULT_TIMEOUT_MILLIS,
                    batchSize = environment.optionalPositiveInt(
                        InternalGatewayAccommodationAnalysisConfig.BATCH_SIZE_KEY,
                    ) ?: InternalGatewayAccommodationAnalysisConfig.DEFAULT_BATCH_SIZE,
                ),
            )
        }

        private fun Map<String, String>.required(key: String): String =
            optional(key) ?: throw LlmProviderConfigurationException(
                key,
                "is required when $MODE_KEY selects a network adapter",
            )

        private fun Map<String, String>.requiredImageHosts(key: String): Set<String> =
            required(key)
                .split(',')
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toSet()

        private fun Map<String, String>.requireApproval(key: String, reason: String) {
            if (!optional(key).equals("true", true)) {
                throw LlmProviderConfigurationException(key, reason)
            }
        }

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
