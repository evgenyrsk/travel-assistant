package com.travelassistant.backend.infrastructure.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HotelProviderConfigTest {

    @Test
    fun `default mode is fake without Hotels API settings`() {
        val config = HotelProviderConfig()

        assertEquals(HotelProviderMode.FAKE, config.mode)
        assertNull(config.hotelsApi)
    }

    @Test
    fun `fake mode ignores incomplete Hotels API environment`() {
        val config = HotelProviderConfig.fromEnvironment(
            mapOf(
                "HOTEL_PROVIDER_MODE" to "FAKE",
                HotelsApiConfig.CLIENT_SECRET_KEY to "must-not-be-read",
            ),
        )

        assertEquals(HotelProviderMode.FAKE, config.mode)
        assertNull(config.hotelsApi)
    }

    @Test
    fun `real mode accepts complete explicit settings`() {
        val config = HotelProviderConfig(
            mode = HotelProviderMode.REAL,
            hotelsApi = completeHotelsApiConfig(),
        )

        assertEquals(HotelProviderMode.REAL, config.mode)
        assertEquals("https://hotels-api.test", config.hotelsApi?.baseUrl)
        assertEquals(2_000, config.hotelsApi?.connectTimeoutMillis)
        assertEquals("ru", config.hotelsApi?.userLanguage)
    }

    @Test
    fun `real environment is parsed into typed Hotels API settings`() {
        val config = HotelProviderConfig.fromEnvironment(completeRealEnvironment())

        assertEquals(HotelProviderMode.REAL, config.mode)
        assertEquals("https://hotels-api.test", config.hotelsApi?.baseUrl)
        assertEquals("https://identity.test/oauth/token", config.hotelsApi?.tokenUrl)
        assertEquals("hotels-client", config.hotelsApi?.clientId)
        assertEquals("hotels.search", config.hotelsApi?.scope)
        assertEquals(2_000, config.hotelsApi?.connectTimeoutMillis)
        assertEquals(5_000, config.hotelsApi?.requestTimeoutMillis)
        assertEquals("ru", config.hotelsApi?.userLanguage)
        assertEquals("travel-assistant", config.hotelsApi?.sourcePlatform)
        assertEquals("stage-9-test", config.hotelsApi?.appVersion)
    }

    @Test
    fun `real mode rejects missing Hotels API settings`() {
        val error = assertFailsWith<HotelProviderConfigurationException> {
            HotelProviderConfig(mode = HotelProviderMode.REAL)
        }

        assertEquals("HOTELS_API_CONFIG", error.configurationKey)
    }

    @Test
    fun `real environment rejects every missing required value`() {
        val requiredKeys = listOf(
            HotelsApiConfig.BASE_URL_KEY,
            HotelsApiConfig.TOKEN_URL_KEY,
            HotelsApiConfig.CLIENT_ID_KEY,
            HotelsApiConfig.CLIENT_SECRET_KEY,
            HotelsApiConfig.SCOPE_KEY,
            HotelsApiConfig.CONNECT_TIMEOUT_KEY,
            HotelsApiConfig.REQUEST_TIMEOUT_KEY,
        )

        requiredKeys.forEach { missingKey ->
            val environment = completeRealEnvironment().toMutableMap().apply {
                remove(missingKey)
            }

            val error = assertFailsWith<HotelProviderConfigurationException> {
                HotelProviderConfig.fromEnvironment(environment)
            }

            assertEquals(missingKey, error.configurationKey)
        }
    }

    @Test
    fun `invalid Hotels API URLs are rejected`() {
        listOf(
            HotelsApiConfig.BASE_URL_KEY to "relative/path",
            HotelsApiConfig.BASE_URL_KEY to "ftp://hotels-api.test",
            HotelsApiConfig.TOKEN_URL_KEY to "https://identity.test/token?secret=value",
        ).forEach { (configurationKey, invalidValue) ->
            val environment = completeRealEnvironment().toMutableMap().apply {
                this[configurationKey] = invalidValue
            }

            val error = assertFailsWith<HotelProviderConfigurationException> {
                HotelProviderConfig.fromEnvironment(environment)
            }

            assertEquals(configurationKey, error.configurationKey)
        }
    }

    @Test
    fun `invalid Hotels API timeouts are rejected`() {
        listOf(
            HotelsApiConfig.CONNECT_TIMEOUT_KEY to "0",
            HotelsApiConfig.REQUEST_TIMEOUT_KEY to "not-a-number",
        ).forEach { (configurationKey, invalidValue) ->
            val environment = completeRealEnvironment().toMutableMap().apply {
                this[configurationKey] = invalidValue
            }

            val error = assertFailsWith<HotelProviderConfigurationException> {
                HotelProviderConfig.fromEnvironment(environment)
            }

            assertEquals(configurationKey, error.configurationKey)
        }
    }

    @Test
    fun `blank explicit client secret is rejected`() {
        val error = assertFailsWith<HotelProviderConfigurationException> {
            RedactedSecret.of("   ")
        }

        assertEquals(HotelsApiConfig.CLIENT_SECRET_KEY, error.configurationKey)
    }

    @Test
    fun `blank optional header values are rejected in explicit config`() {
        val invalidConfigs = listOf(
            HotelsApiConfig.USER_LANGUAGE_KEY to { completeHotelsApiConfig().copy(userLanguage = " ") },
            HotelsApiConfig.SOURCE_PLATFORM_KEY to { completeHotelsApiConfig().copy(sourcePlatform = " ") },
            HotelsApiConfig.APP_VERSION_KEY to { completeHotelsApiConfig().copy(appVersion = " ") },
        )

        invalidConfigs.forEach { (configurationKey, createConfig) ->
            val error = assertFailsWith<HotelProviderConfigurationException> {
                createConfig()
            }

            assertEquals(configurationKey, error.configurationKey)
        }
    }

    @Test
    fun `client secret is redacted from config and error text`() {
        val secretValue = "synthetic-secret-value"
        val environment = completeRealEnvironment().toMutableMap().apply {
            this[HotelsApiConfig.CLIENT_SECRET_KEY] = secretValue
        }
        val config = HotelProviderConfig.fromEnvironment(environment)

        assertEquals("[REDACTED]", config.hotelsApi?.clientSecret.toString())
        assertFalse(config.toString().contains(secretValue))

        val invalidEnvironment = environment.toMutableMap().apply {
            this[HotelsApiConfig.BASE_URL_KEY] = "invalid-url"
        }
        val error = assertFailsWith<HotelProviderConfigurationException> {
            HotelProviderConfig.fromEnvironment(invalidEnvironment)
        }

        assertFalse(error.message.orEmpty().contains(secretValue))
        assertTrue(error.message.orEmpty().contains(HotelsApiConfig.BASE_URL_KEY))
    }

    private fun completeHotelsApiConfig(): HotelsApiConfig =
        HotelsApiConfig(
            baseUrl = "https://hotels-api.test",
            tokenUrl = "https://identity.test/oauth/token",
            clientId = "hotels-client",
            clientSecret = RedactedSecret.of("synthetic-secret"),
            scope = "hotels.search",
            connectTimeoutMillis = 2_000,
            requestTimeoutMillis = 5_000,
            userLanguage = "ru",
            sourcePlatform = "travel-assistant",
            appVersion = "stage-9-test",
        )

    private fun completeRealEnvironment(): Map<String, String> =
        mapOf(
            "HOTEL_PROVIDER_MODE" to "REAL",
            HotelsApiConfig.BASE_URL_KEY to "https://hotels-api.test",
            HotelsApiConfig.TOKEN_URL_KEY to "https://identity.test/oauth/token",
            HotelsApiConfig.CLIENT_ID_KEY to "hotels-client",
            HotelsApiConfig.CLIENT_SECRET_KEY to "synthetic-secret",
            HotelsApiConfig.SCOPE_KEY to "hotels.search",
            HotelsApiConfig.CONNECT_TIMEOUT_KEY to "2000",
            HotelsApiConfig.REQUEST_TIMEOUT_KEY to "5000",
            HotelsApiConfig.USER_LANGUAGE_KEY to "ru",
            HotelsApiConfig.SOURCE_PLATFORM_KEY to "travel-assistant",
            HotelsApiConfig.APP_VERSION_KEY to "stage-9-test",
        )
}
