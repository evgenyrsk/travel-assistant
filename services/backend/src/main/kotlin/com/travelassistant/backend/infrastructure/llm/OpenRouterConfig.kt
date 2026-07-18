package com.travelassistant.backend.infrastructure.llm

import java.net.URI

internal data class OpenRouterConfig(
    val apiKey: OpenRouterApiKey,
    val model: String,
    val baseUrl: String = DEFAULT_BASE_URL,
    val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) {
    init {
        validateModel()
        validateBaseUrl()
        validateTimeout()
    }

    private fun validateModel() {
        if (model.isBlank()) {
            throw LlmProviderConfigurationException(
                configurationKey = MODEL_KEY,
                reason = "must not be blank",
            )
        }
    }

    private fun validateBaseUrl() {
        val uri = runCatching { URI(baseUrl) }.getOrNull()
        val isValid = uri != null &&
            uri.isAbsolute &&
            uri.scheme.equals("https", ignoreCase = true) &&
            !uri.host.isNullOrBlank() &&
            uri.userInfo == null &&
            uri.query == null &&
            uri.fragment == null

        if (!isValid) {
            throw LlmProviderConfigurationException(
                configurationKey = BASE_URL_KEY,
                reason = "must be an absolute HTTPS URI without credentials, query, or fragment",
            )
        }
    }

    private fun validateTimeout() {
        if (timeoutMillis <= 0) {
            throw LlmProviderConfigurationException(
                configurationKey = TIMEOUT_KEY,
                reason = "must be a positive integer",
            )
        }
    }

    companion object {
        internal const val API_KEY_KEY = "OPENROUTER_API_KEY"
        internal const val MODEL_KEY = "OPENROUTER_MODEL"
        internal const val BASE_URL_KEY = "OPENROUTER_BASE_URL"
        internal const val TIMEOUT_KEY = "OPENROUTER_TIMEOUT_MS"

        internal const val DEFAULT_BASE_URL = "https://openrouter.ai/api/v1/"
        internal const val DEFAULT_TIMEOUT_MILLIS = 30_000L
    }
}
