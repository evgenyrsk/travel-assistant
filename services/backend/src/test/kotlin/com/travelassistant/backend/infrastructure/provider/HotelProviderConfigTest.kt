package com.travelassistant.backend.infrastructure.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

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
                HotelsApiTargetConfig.PUBLIC_BASE_URL_KEY to "not-a-url",
                HotelsApiJwtAuthConfig.PRIVATE_KEY_KEY to "must-not-be-read",
            ),
        )

        assertEquals(HotelProviderMode.FAKE, config.mode)
        assertNull(config.hotelsApi)
    }

    @Test
    fun `real environment uses confirmed public defaults without private JWT settings`() {
        val config = HotelProviderConfig.fromEnvironment(
            mapOf("HOTEL_PROVIDER_MODE" to "REAL"),
        )

        val hotelsApi = config.hotelsApi
        assertEquals(HotelProviderMode.REAL, config.mode)
        assertEquals("https://hotels.tbank.ru/", hotelsApi?.publicTarget?.baseUri)
        assertEquals(60_000, hotelsApi?.publicTarget?.timeoutMillis)
        assertNull(hotelsApi?.userLanguage)
    }

    @Test
    fun `real environment accepts public target and language overrides`() {
        val config = HotelProviderConfig.fromEnvironment(
            mapOf(
                "HOTEL_PROVIDER_MODE" to "REAL",
                HotelsApiTargetConfig.PUBLIC_BASE_URL_KEY to "https://public-hotels.test/",
                HotelsApiTargetConfig.PUBLIC_TIMEOUT_KEY to "12000",
                HotelsApiConfig.USER_LANGUAGE_KEY to "ru",
            ),
        )

        val hotelsApi = config.hotelsApi
        assertEquals("https://public-hotels.test/", hotelsApi?.publicTarget?.baseUri)
        assertEquals(12_000, hotelsApi?.publicTarget?.timeoutMillis)
        assertEquals("RU", hotelsApi?.userLanguage)
    }

    @Test
    fun `real mode rejects missing typed Hotels API settings`() {
        val error = assertFailsWith<HotelProviderConfigurationException> {
            HotelProviderConfig(mode = HotelProviderMode.REAL)
        }

        assertEquals("HOTELS_API_CONFIG", error.configurationKey)
    }

    @Test
    fun `invalid explicit provider mode is rejected instead of falling back to fake`() {
        val error = assertFailsWith<HotelProviderConfigurationException> {
            HotelProviderConfig.fromEnvironment(
                mapOf("HOTEL_PROVIDER_MODE" to "unexpected"),
            )
        }

        assertEquals("HOTEL_PROVIDER_MODE", error.configurationKey)
    }

    @Test
    fun `private and JWT environment settings are not required by public real flow`() {
        val config = HotelProviderConfig.fromEnvironment(
            mapOf(
                "HOTEL_PROVIDER_MODE" to "REAL",
                HotelsApiTargetConfig.PRIVATE_BASE_URI_KEY to "not-a-uri",
                HotelsApiTargetConfig.PRIVATE_TIMEOUT_KEY to "not-a-number",
                HotelsApiJwtAuthConfig.ISSUER_KEY to " ",
                HotelsApiJwtAuthConfig.AUDIENCE_KEY to " ",
            ),
        )

        assertEquals(HotelProviderMode.REAL, config.mode)
        assertEquals(
            HotelsApiTargetConfig.DEFAULT_PUBLIC_BASE_URL,
            config.hotelsApi?.publicTarget?.baseUri,
        )
    }

    @Test
    fun `invalid public target URLs are rejected`() {
        listOf(
            "relative/path",
            "ftp://public-hotels.test",
            "https://public-hotels.test/path?secret=value",
        ).forEach { invalidValue ->
            val error = assertFailsWith<HotelProviderConfigurationException> {
                HotelProviderConfig.fromEnvironment(
                    mapOf(
                        "HOTEL_PROVIDER_MODE" to "REAL",
                        HotelsApiTargetConfig.PUBLIC_BASE_URL_KEY to invalidValue,
                    ),
                )
            }

            assertEquals(HotelsApiTargetConfig.PUBLIC_BASE_URL_KEY, error.configurationKey)
        }
    }

    @Test
    fun `invalid public timeout is rejected`() {
        listOf("0", "-1", "not-a-number").forEach { invalidValue ->
            val error = assertFailsWith<HotelProviderConfigurationException> {
                HotelProviderConfig.fromEnvironment(
                    mapOf(
                        "HOTEL_PROVIDER_MODE" to "REAL",
                        HotelsApiTargetConfig.PUBLIC_TIMEOUT_KEY to invalidValue,
                    ),
                )
            }

            assertEquals(HotelsApiTargetConfig.PUBLIC_TIMEOUT_KEY, error.configurationKey)
        }
    }

    @Test
    fun `unsupported public language is rejected`() {
        val error = assertFailsWith<HotelProviderConfigurationException> {
            HotelProviderConfig.fromEnvironment(
                mapOf(
                    "HOTEL_PROVIDER_MODE" to "REAL",
                    HotelsApiConfig.USER_LANGUAGE_KEY to "DE",
                ),
            )
        }

        assertEquals(HotelsApiConfig.USER_LANGUAGE_KEY, error.configurationKey)
    }

    @Test
    fun `standalone future private key remains redacted`() {
        val secretValue = "synthetic-private-key-value"
        val jwt = HotelsApiJwtAuthConfig(
            privateKey = RedactedSecret.of(
                secretValue,
                HotelsApiJwtAuthConfig.PRIVATE_KEY_KEY,
            ),
        )

        assertEquals("[REDACTED]", jwt.privateKey.toString())
        assertFalse(jwt.toString().contains(secretValue))
    }
}
