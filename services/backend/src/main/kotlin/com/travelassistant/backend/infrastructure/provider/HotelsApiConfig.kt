package com.travelassistant.backend.infrastructure.provider

import java.net.URI

data class HotelsApiConfig(
    val baseUrl: String,
    val tokenUrl: String,
    val clientId: String,
    val clientSecret: RedactedSecret,
    val scope: String,
    val connectTimeoutMillis: Long,
    val requestTimeoutMillis: Long,
    val userLanguage: String? = null,
    val sourcePlatform: String? = null,
    val appVersion: String? = null,
) {
    init {
        validateHttpUrl(BASE_URL_KEY, baseUrl)
        validateHttpUrl(TOKEN_URL_KEY, tokenUrl)
        validateNotBlank(CLIENT_ID_KEY, clientId)
        validateNotBlank(SCOPE_KEY, scope)
        validatePositiveTimeout(CONNECT_TIMEOUT_KEY, connectTimeoutMillis)
        validatePositiveTimeout(REQUEST_TIMEOUT_KEY, requestTimeoutMillis)
        validateOptionalHeader(USER_LANGUAGE_KEY, userLanguage)
        validateOptionalHeader(SOURCE_PLATFORM_KEY, sourcePlatform)
        validateOptionalHeader(APP_VERSION_KEY, appVersion)
    }

    private fun validateHttpUrl(configurationKey: String, value: String) {
        val uri = runCatching { URI(value) }.getOrNull()
        val isValid = uri != null &&
            uri.isAbsolute &&
            uri.scheme.lowercase() in ALLOWED_SCHEMES &&
            !uri.host.isNullOrBlank() &&
            uri.userInfo == null &&
            uri.query == null &&
            uri.fragment == null

        if (!isValid) {
            throw HotelProviderConfigurationException(
                configurationKey = configurationKey,
                reason = "must be an absolute HTTP(S) URL without credentials, query, or fragment",
            )
        }
    }

    private fun validateNotBlank(configurationKey: String, value: String) {
        if (value.isBlank()) {
            throw HotelProviderConfigurationException(
                configurationKey = configurationKey,
                reason = "must not be blank",
            )
        }
    }

    private fun validatePositiveTimeout(configurationKey: String, value: Long) {
        if (value <= 0) {
            throw HotelProviderConfigurationException(
                configurationKey = configurationKey,
                reason = "must be a positive integer",
            )
        }
    }

    private fun validateOptionalHeader(configurationKey: String, value: String?) {
        if (value != null && value.isBlank()) {
            throw HotelProviderConfigurationException(
                configurationKey = configurationKey,
                reason = "must be omitted or non-blank",
            )
        }
    }

    companion object {
        internal const val BASE_URL_KEY = "HOTELS_API_BASE_URL"
        internal const val TOKEN_URL_KEY = "HOTELS_API_TOKEN_URL"
        internal const val CLIENT_ID_KEY = "HOTELS_API_CLIENT_ID"
        internal const val CLIENT_SECRET_KEY = "HOTELS_API_CLIENT_SECRET"
        internal const val SCOPE_KEY = "HOTELS_API_SCOPE"
        internal const val CONNECT_TIMEOUT_KEY = "HOTELS_API_CONNECT_TIMEOUT_MS"
        internal const val REQUEST_TIMEOUT_KEY = "HOTELS_API_REQUEST_TIMEOUT_MS"
        internal const val USER_LANGUAGE_KEY = "HOTELS_API_USER_LANGUAGE"
        internal const val SOURCE_PLATFORM_KEY = "HOTELS_API_SOURCE_PLATFORM"
        internal const val APP_VERSION_KEY = "HOTELS_API_APP_VERSION"

        private val ALLOWED_SCHEMES = setOf("http", "https")
    }
}
