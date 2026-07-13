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
                HotelsApiJwtAuthConfig.PRIVATE_KEY_KEY to "must-not-be-read",
            ),
        )

        assertEquals(HotelProviderMode.FAKE, config.mode)
        assertNull(config.hotelsApi)
    }

    @Test
    fun `real environment uses confirmed public private and JWT defaults`() {
        val config = HotelProviderConfig.fromEnvironment(
            mapOf(
                "HOTEL_PROVIDER_MODE" to "REAL",
                HotelsApiJwtAuthConfig.PRIVATE_KEY_KEY to "synthetic-private-key",
            ),
        )

        val hotelsApi = config.hotelsApi
        assertEquals(HotelProviderMode.REAL, config.mode)
        assertEquals("https://hotels.tbank.ru/", hotelsApi?.publicTarget?.baseUri)
        assertEquals(60_000, hotelsApi?.publicTarget?.timeoutMillis)
        assertEquals(HotelsApiTargetConfig.DEFAULT_PRIVATE_BASE_URI, hotelsApi?.privateTarget?.baseUri)
        assertEquals(10_000, hotelsApi?.privateTarget?.timeoutMillis)
        assertEquals(HotelsApiJwtAuthConfig.DEFAULT_ISSUER, hotelsApi?.jwtAuth?.issuer)
        assertEquals(HotelsApiJwtAuthConfig.DEFAULT_AUDIENCE, hotelsApi?.jwtAuth?.audience)
    }

    @Test
    fun `real environment accepts target JWT and header overrides`() {
        val config = HotelProviderConfig.fromEnvironment(completeRealEnvironment())
        val hotelsApi = config.hotelsApi

        assertEquals("https://public-hotels.test/", hotelsApi?.publicTarget?.baseUri)
        assertEquals(12_000, hotelsApi?.publicTarget?.timeoutMillis)
        assertEquals("https://private-hotels.test/", hotelsApi?.privateTarget?.baseUri)
        assertEquals(3_000, hotelsApi?.privateTarget?.timeoutMillis)
        assertEquals("TEST-ISSUER", hotelsApi?.jwtAuth?.issuer)
        assertEquals("TEST-AUDIENCE", hotelsApi?.jwtAuth?.audience)
        assertEquals("ru", hotelsApi?.userLanguage)
        assertEquals("travel-assistant", hotelsApi?.sourcePlatform)
        assertEquals("stage-9-test", hotelsApi?.appVersion)
    }

    @Test
    fun `real mode rejects missing Hotels API settings`() {
        val error = assertFailsWith<HotelProviderConfigurationException> {
            HotelProviderConfig(mode = HotelProviderMode.REAL)
        }

        assertEquals("HOTELS_API_CONFIG", error.configurationKey)
    }

    @Test
    fun `real environment rejects missing private key`() {
        val error = assertFailsWith<HotelProviderConfigurationException> {
            HotelProviderConfig.fromEnvironment(mapOf("HOTEL_PROVIDER_MODE" to "REAL"))
        }

        assertEquals(HotelsApiJwtAuthConfig.PRIVATE_KEY_KEY, error.configurationKey)
    }

    @Test
    fun `old OAuth environment keys are not required`() {
        val config = HotelProviderConfig.fromEnvironment(
            mapOf(
                "HOTEL_PROVIDER_MODE" to "REAL",
                HotelsApiJwtAuthConfig.PRIVATE_KEY_KEY to "synthetic-private-key",
            ),
        )

        assertEquals(HotelProviderMode.REAL, config.mode)
    }

    @Test
    fun `invalid target URLs are rejected`() {
        listOf(
            HotelsApiTargetConfig.PUBLIC_BASE_URL_KEY to "relative/path",
            HotelsApiTargetConfig.PUBLIC_BASE_URL_KEY to "ftp://public-hotels.test",
            HotelsApiTargetConfig.PRIVATE_BASE_URI_KEY to "https://private-hotels.test/path?secret=value",
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
    fun `invalid target timeouts are rejected`() {
        listOf(
            HotelsApiTargetConfig.PUBLIC_TIMEOUT_KEY to "0",
            HotelsApiTargetConfig.PRIVATE_TIMEOUT_KEY to "not-a-number",
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
    fun `blank explicit issuer and audience are rejected`() {
        listOf(
            HotelsApiJwtAuthConfig.ISSUER_KEY to { completeHotelsApiConfig().copy(
                jwtAuth = completeJwtAuth().copy(issuer = " "),
            ) },
            HotelsApiJwtAuthConfig.AUDIENCE_KEY to { completeHotelsApiConfig().copy(
                jwtAuth = completeJwtAuth().copy(audience = " "),
            ) },
        ).forEach { (configurationKey, createConfig) ->
            val error = assertFailsWith<HotelProviderConfigurationException> { createConfig() }

            assertEquals(configurationKey, error.configurationKey)
        }
    }

    @Test
    fun `blank private key is rejected`() {
        val error = assertFailsWith<HotelProviderConfigurationException> {
            RedactedSecret.of("   ", HotelsApiJwtAuthConfig.PRIVATE_KEY_KEY)
        }

        assertEquals(HotelsApiJwtAuthConfig.PRIVATE_KEY_KEY, error.configurationKey)
    }

    @Test
    fun `blank optional header values are rejected in explicit config`() {
        val invalidConfigs = listOf(
            HotelsApiConfig.USER_LANGUAGE_KEY to { completeHotelsApiConfig().copy(userLanguage = " ") },
            HotelsApiConfig.SOURCE_PLATFORM_KEY to { completeHotelsApiConfig().copy(sourcePlatform = " ") },
            HotelsApiConfig.APP_VERSION_KEY to { completeHotelsApiConfig().copy(appVersion = " ") },
        )

        invalidConfigs.forEach { (configurationKey, createConfig) ->
            val error = assertFailsWith<HotelProviderConfigurationException> { createConfig() }

            assertEquals(configurationKey, error.configurationKey)
        }
    }

    @Test
    fun `private key is redacted from config and error text`() {
        val secretValue = "synthetic-private-key-value"
        val environment = completeRealEnvironment().toMutableMap().apply {
            this[HotelsApiJwtAuthConfig.PRIVATE_KEY_KEY] = secretValue
        }
        val config = HotelProviderConfig.fromEnvironment(environment)

        assertEquals("[REDACTED]", config.hotelsApi?.jwtAuth?.privateKey.toString())
        assertFalse(config.toString().contains(secretValue))

        val invalidEnvironment = environment.toMutableMap().apply {
            this[HotelsApiTargetConfig.PUBLIC_BASE_URL_KEY] = "invalid-url"
        }
        val error = assertFailsWith<HotelProviderConfigurationException> {
            HotelProviderConfig.fromEnvironment(invalidEnvironment)
        }

        assertFalse(error.message.orEmpty().contains(secretValue))
        assertTrue(error.message.orEmpty().contains(HotelsApiTargetConfig.PUBLIC_BASE_URL_KEY))
    }

    private fun completeHotelsApiConfig(): HotelsApiConfig =
        HotelsApiConfig(
            publicTarget = HotelsApiTargetConfig.public("https://public-hotels.test/", 12_000),
            privateTarget = HotelsApiTargetConfig.private("https://private-hotels.test/", 3_000),
            jwtAuth = completeJwtAuth(),
            userLanguage = "ru",
            sourcePlatform = "travel-assistant",
            appVersion = "stage-9-test",
        )

    private fun completeJwtAuth(): HotelsApiJwtAuthConfig =
        HotelsApiJwtAuthConfig(
            issuer = "TEST-ISSUER",
            audience = "TEST-AUDIENCE",
            privateKey = RedactedSecret.of(
                "synthetic-private-key",
                HotelsApiJwtAuthConfig.PRIVATE_KEY_KEY,
            ),
        )

    private fun completeRealEnvironment(): Map<String, String> =
        mapOf(
            "HOTEL_PROVIDER_MODE" to "REAL",
            HotelsApiTargetConfig.PUBLIC_BASE_URL_KEY to "https://public-hotels.test/",
            HotelsApiTargetConfig.PUBLIC_TIMEOUT_KEY to "12000",
            HotelsApiTargetConfig.PRIVATE_BASE_URI_KEY to "https://private-hotels.test/",
            HotelsApiTargetConfig.PRIVATE_TIMEOUT_KEY to "3000",
            HotelsApiJwtAuthConfig.ISSUER_KEY to "TEST-ISSUER",
            HotelsApiJwtAuthConfig.AUDIENCE_KEY to "TEST-AUDIENCE",
            HotelsApiJwtAuthConfig.PRIVATE_KEY_KEY to "synthetic-private-key",
            HotelsApiConfig.USER_LANGUAGE_KEY to "ru",
            HotelsApiConfig.SOURCE_PLATFORM_KEY to "travel-assistant",
            HotelsApiConfig.APP_VERSION_KEY to "stage-9-test",
        )
}
