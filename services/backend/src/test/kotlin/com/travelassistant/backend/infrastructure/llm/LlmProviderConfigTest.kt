package com.travelassistant.backend.infrastructure.llm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class LlmProviderConfigTest {

    @Test
    fun `default mode is fake without OpenRouter settings`() {
        val config = LlmProviderConfig.fromEnvironment(emptyMap())

        assertEquals(LlmProviderMode.FAKE, config.mode)
        assertNull(config.openRouter)
    }

    @Test
    fun `fake mode ignores incomplete OpenRouter environment`() {
        val config = LlmProviderConfig.fromEnvironment(
            mapOf(
                LlmProviderConfig.MODE_KEY to "FAKE",
                OpenRouterConfig.BASE_URL_KEY to "not-a-url",
                OpenRouterConfig.API_KEY_KEY to "must-not-be-read",
            ),
        )

        assertEquals(LlmProviderMode.FAKE, config.mode)
        assertNull(config.openRouter)
    }

    @Test
    fun `openrouter mode uses safe defaults and required operator values`() {
        val config = LlmProviderConfig.fromEnvironment(
            mapOf(
                LlmProviderConfig.MODE_KEY to "openrouter",
                OpenRouterConfig.API_KEY_KEY to "synthetic-api-key",
                OpenRouterConfig.MODEL_KEY to "provider/model-under-test",
            ),
        )

        assertEquals(LlmProviderMode.OPENROUTER, config.mode)
        assertEquals(OpenRouterConfig.DEFAULT_BASE_URL, config.openRouter?.baseUrl)
        assertEquals(OpenRouterConfig.DEFAULT_TIMEOUT_MILLIS, config.openRouter?.timeoutMillis)
        assertEquals("provider/model-under-test", config.openRouter?.model)
    }

    @Test
    fun `openrouter mode accepts base URL and timeout overrides`() {
        val config = LlmProviderConfig.fromEnvironment(
            mapOf(
                LlmProviderConfig.MODE_KEY to "OPENROUTER",
                OpenRouterConfig.API_KEY_KEY to "synthetic-api-key",
                OpenRouterConfig.MODEL_KEY to "provider/model-under-test",
                OpenRouterConfig.BASE_URL_KEY to "https://openrouter.test/api/v1/",
                OpenRouterConfig.TIMEOUT_KEY to "12000",
            ),
        )

        assertEquals("https://openrouter.test/api/v1/", config.openRouter?.baseUrl)
        assertEquals(12_000L, config.openRouter?.timeoutMillis)
    }

    @Test
    fun `openrouter mode requires API key and model independently`() {
        val missingValues = listOf(
            mapOf(
                LlmProviderConfig.MODE_KEY to "OPENROUTER",
                OpenRouterConfig.MODEL_KEY to "provider/model-under-test",
            ) to OpenRouterConfig.API_KEY_KEY,
            mapOf(
                LlmProviderConfig.MODE_KEY to "OPENROUTER",
                OpenRouterConfig.API_KEY_KEY to "synthetic-api-key",
            ) to OpenRouterConfig.MODEL_KEY,
        )

        missingValues.forEach { (environment, expectedKey) ->
            val error = assertFailsWith<LlmProviderConfigurationException> {
                LlmProviderConfig.fromEnvironment(environment)
            }

            assertEquals(expectedKey, error.configurationKey)
        }
    }

    @Test
    fun `invalid mode URL and timeout fail closed`() {
        val invalidCases = listOf(
            mapOf(LlmProviderConfig.MODE_KEY to "unexpected") to LlmProviderConfig.MODE_KEY,
            openRouterEnvironment(
                OpenRouterConfig.BASE_URL_KEY to "http://openrouter.test/api/v1/",
            ) to OpenRouterConfig.BASE_URL_KEY,
            openRouterEnvironment(
                OpenRouterConfig.BASE_URL_KEY to "https://user:password@openrouter.test/api/v1/",
            ) to OpenRouterConfig.BASE_URL_KEY,
            openRouterEnvironment(
                OpenRouterConfig.TIMEOUT_KEY to "0",
            ) to OpenRouterConfig.TIMEOUT_KEY,
            openRouterEnvironment(
                OpenRouterConfig.TIMEOUT_KEY to "not-a-number",
            ) to OpenRouterConfig.TIMEOUT_KEY,
        )

        invalidCases.forEach { (environment, expectedKey) ->
            val error = assertFailsWith<LlmProviderConfigurationException> {
                LlmProviderConfig.fromEnvironment(environment)
            }

            assertEquals(expectedKey, error.configurationKey)
        }
    }

    @Test
    fun `API key stays redacted in typed configuration and errors`() {
        val secret = "synthetic-secret-that-must-not-leak"
        val apiKey = OpenRouterApiKey.of(secret)
        val config = OpenRouterConfig(
            apiKey = apiKey,
            model = "provider/model-under-test",
        )

        assertEquals("[REDACTED]", apiKey.toString())
        assertFalse(config.toString().contains(secret))

        val error = assertFailsWith<LlmProviderConfigurationException> {
            OpenRouterConfig(
                apiKey = apiKey,
                model = "provider/model-under-test",
                baseUrl = "not-a-url",
            )
        }
        assertFalse(error.message.orEmpty().contains(secret))
    }

    private fun openRouterEnvironment(
        override: Pair<String, String>,
    ): Map<String, String> =
        mapOf(
            LlmProviderConfig.MODE_KEY to "OPENROUTER",
            OpenRouterConfig.API_KEY_KEY to "synthetic-api-key",
            OpenRouterConfig.MODEL_KEY to "provider/model-under-test",
            override,
        )
}
