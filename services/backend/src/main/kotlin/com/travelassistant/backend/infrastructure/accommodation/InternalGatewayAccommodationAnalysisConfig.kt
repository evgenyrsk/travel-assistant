package com.travelassistant.backend.infrastructure.accommodation

import com.travelassistant.backend.infrastructure.llm.LlmProviderConfigurationException
import java.net.URI
import java.util.Locale

internal data class InternalGatewayAccommodationAnalysisConfig(
    val endpointUrl: String,
    val deploymentId: String,
    val accessToken: InternalGatewayAccessToken,
    val imageHosts: Set<String>,
    val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    val batchSize: Int = DEFAULT_BATCH_SIZE,
) {
    init {
        validateEndpointUrl()
        requireConfigured(
            DEPLOYMENT_ID_PATTERN.matches(deploymentId),
            DEPLOYMENT_ID_KEY,
            "must be a 1-200 character opaque deployment identifier",
        )
        requireConfigured(timeoutMillis > 0, TIMEOUT_KEY, "must be a positive integer")
        requireConfigured(batchSize in 1..MAX_BATCH_SIZE, BATCH_SIZE_KEY, "must be between 1 and 6")
        requireConfigured(imageHosts.isNotEmpty(), IMAGE_HOSTS_KEY, "must contain exact hosts")
        imageHosts.forEach { host ->
            requireConfigured(
                host == host.lowercase(Locale.ROOT) && HOST_PATTERN.matches(host),
                IMAGE_HOSTS_KEY,
                "must contain lowercase exact DNS hosts without wildcards or ports",
            )
        }
    }

    private fun validateEndpointUrl() {
        val uri = runCatching { URI(endpointUrl) }.getOrNull()
        requireConfigured(
            uri != null &&
                uri.isAbsolute &&
                uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null &&
                uri.query == null &&
                uri.fragment == null &&
                uri.path == ENDPOINT_PATH,
            ENDPOINT_URL_KEY,
            "must be an exact HTTPS $ENDPOINT_PATH URL without credentials, query, or fragment",
        )
    }

    private fun requireConfigured(condition: Boolean, key: String, reason: String) {
        if (!condition) {
            throw LlmProviderConfigurationException(key, reason)
        }
    }

    companion object {
        internal const val ENDPOINT_URL_KEY = "ACCOMMODATION_ANALYSIS_INTERNAL_GATEWAY_URL"
        internal const val DEPLOYMENT_ID_KEY = "ACCOMMODATION_ANALYSIS_DEPLOYMENT_ID"
        internal const val ACCESS_TOKEN_KEY = "ACCOMMODATION_ANALYSIS_INTERNAL_GATEWAY_TOKEN"
        internal const val IMAGE_HOSTS_KEY = "ACCOMMODATION_ANALYSIS_IMAGE_HOSTS"
        internal const val TIMEOUT_KEY = "ACCOMMODATION_ANALYSIS_TIMEOUT_MS"
        internal const val BATCH_SIZE_KEY = "ACCOMMODATION_ANALYSIS_BATCH_SIZE"
        internal const val ENDPOINT_PATH = "/v1/accommodation-analysis"
        internal const val DEFAULT_TIMEOUT_MILLIS = 15_000L
        internal const val DEFAULT_BATCH_SIZE = 6
        internal const val MAX_BATCH_SIZE = 6
        private val DEPLOYMENT_ID_PATTERN = Regex("[A-Za-z0-9][A-Za-z0-9._:/-]{0,199}")
        private val HOST_PATTERN = Regex(
            """(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+""" +
                """[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?""",
        )
    }
}
