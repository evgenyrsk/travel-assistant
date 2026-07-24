package com.travelassistant.backend.infrastructure.accommodation

import com.travelassistant.backend.infrastructure.llm.LlmProviderConfigurationException
import com.travelassistant.backend.infrastructure.llm.OpenRouterApiKey
import java.net.URI
import java.util.Locale

internal data class OpenRouterAccommodationAnalysisConfig(
    val apiKey: OpenRouterApiKey,
    val model: String,
    val imageHosts: Set<String>,
    val baseUrl: String = DEFAULT_BASE_URL,
    val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    val batchSize: Int = DEFAULT_BATCH_SIZE,
) {
    init {
        requireConfigured(model.isNotBlank(), MODEL_KEY, "must not be blank")
        validateBaseUrl()
        requireConfigured(timeoutMillis > 0, TIMEOUT_KEY, "must be a positive integer")
        requireConfigured(batchSize in 1..MAX_BATCH_SIZE, BATCH_SIZE_KEY, "must be between 1 and 5")
        requireConfigured(imageHosts.isNotEmpty(), IMAGE_HOSTS_KEY, "must contain exact hosts")
        imageHosts.forEach { host ->
            requireConfigured(
                host == host.lowercase(Locale.ROOT) && HOST_PATTERN.matches(host),
                IMAGE_HOSTS_KEY,
                "must contain lowercase exact DNS hosts without wildcards or ports",
            )
        }
    }

    private fun validateBaseUrl() {
        val uri = runCatching { URI(baseUrl) }.getOrNull()
        requireConfigured(
            uri != null &&
                uri.isAbsolute &&
                uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null &&
                uri.query == null &&
                uri.fragment == null,
            BASE_URL_KEY,
            "must be an absolute HTTPS URI without credentials, query, or fragment",
        )
    }

    private fun requireConfigured(
        condition: Boolean,
        key: String,
        reason: String,
    ) {
        if (!condition) {
            throw LlmProviderConfigurationException(key, reason)
        }
    }

    companion object {
        internal const val MODEL_KEY = "ACCOMMODATION_ANALYSIS_MODEL"
        internal const val BASE_URL_KEY = "ACCOMMODATION_ANALYSIS_BASE_URL"
        internal const val TIMEOUT_KEY = "ACCOMMODATION_ANALYSIS_TIMEOUT_MS"
        internal const val BATCH_SIZE_KEY = "ACCOMMODATION_ANALYSIS_BATCH_SIZE"
        internal const val IMAGE_HOSTS_KEY = "ACCOMMODATION_ANALYSIS_IMAGE_HOSTS"
        internal const val DEFAULT_BASE_URL = "https://openrouter.ai/api/v1/"
        internal const val DEFAULT_TIMEOUT_MILLIS = 15_000L
        internal const val DEFAULT_BATCH_SIZE = 5
        internal const val MAX_BATCH_SIZE = 5
        private val HOST_PATTERN = Regex(
            """(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+""" +
                """[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?""",
        )
    }
}
