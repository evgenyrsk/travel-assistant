package com.travelassistant.backend.infrastructure.provider

import java.net.URI

class HotelsApiTargetConfig private constructor(
    val baseUri: String,
    val timeoutMillis: Long,
    private val baseUriConfigurationKey: String,
    private val timeoutConfigurationKey: String,
) {
    init {
        validateHttpUri()
        validatePositiveTimeout()
    }

    private fun validateHttpUri() {
        val uri = runCatching { URI(baseUri) }.getOrNull()
        val isValid = uri != null &&
            uri.isAbsolute &&
            uri.scheme.lowercase() in ALLOWED_SCHEMES &&
            !uri.host.isNullOrBlank() &&
            uri.userInfo == null &&
            uri.query == null &&
            uri.fragment == null

        if (!isValid) {
            throw HotelProviderConfigurationException(
                configurationKey = baseUriConfigurationKey,
                reason = "must be an absolute HTTP(S) URI without credentials, query, or fragment",
            )
        }
    }

    private fun validatePositiveTimeout() {
        if (timeoutMillis <= 0) {
            throw HotelProviderConfigurationException(
                configurationKey = timeoutConfigurationKey,
                reason = "must be a positive integer",
            )
        }
    }

    companion object {
        internal const val PUBLIC_BASE_URL_KEY = "HOTELS_API_PUBLIC_BASE_URL"
        internal const val PUBLIC_TIMEOUT_KEY = "HOTELS_API_PUBLIC_TIMEOUT_MS"
        internal const val PRIVATE_BASE_URI_KEY = "HOTELS_API_PRIVATE_BASE_URI"
        internal const val PRIVATE_TIMEOUT_KEY = "HOTELS_API_PRIVATE_TIMEOUT_MS"

        internal const val DEFAULT_PUBLIC_BASE_URL = "https://hotels.tcsbank.ru/"
        internal const val DEFAULT_PUBLIC_TIMEOUT_MILLIS = 60_000L
        internal const val DEFAULT_PRIVATE_BASE_URI = "https://hotels-private.tcsbank.ru/"
        internal const val DEFAULT_PRIVATE_TIMEOUT_MILLIS = 10_000L

        fun publicDefault(): HotelsApiTargetConfig =
            public(DEFAULT_PUBLIC_BASE_URL, DEFAULT_PUBLIC_TIMEOUT_MILLIS)

        fun privateDefault(): HotelsApiTargetConfig =
            private(DEFAULT_PRIVATE_BASE_URI, DEFAULT_PRIVATE_TIMEOUT_MILLIS)

        fun public(baseUrl: String, timeoutMillis: Long): HotelsApiTargetConfig =
            HotelsApiTargetConfig(
                baseUri = baseUrl,
                timeoutMillis = timeoutMillis,
                baseUriConfigurationKey = PUBLIC_BASE_URL_KEY,
                timeoutConfigurationKey = PUBLIC_TIMEOUT_KEY,
            )

        fun private(baseUri: String, timeoutMillis: Long): HotelsApiTargetConfig =
            HotelsApiTargetConfig(
                baseUri = baseUri,
                timeoutMillis = timeoutMillis,
                baseUriConfigurationKey = PRIVATE_BASE_URI_KEY,
                timeoutConfigurationKey = PRIVATE_TIMEOUT_KEY,
            )

        private val ALLOWED_SCHEMES = setOf("http", "https")
    }
}
