package com.travelassistant.backend.infrastructure.accommodation

import com.travelassistant.backend.infrastructure.llm.LlmProviderConfigurationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class AccommodationAnalysisProviderConfigTest {

    @Test
    fun `defaults to fake without OpenRouter configuration`() {
        val config = AccommodationAnalysisProviderConfig.fromEnvironment(emptyMap())

        assertEquals(AccommodationAnalysisProviderMode.FAKE, config.mode)
        assertEquals(null, config.openRouter)
        val runtime = AccommodationAnalysisProviderFactory.create(config)
        assertIs<FakeAccommodationAnalysisClient>(runtime.client)
        runtime.close()
    }

    @Test
    fun `parses opt in OpenRouter configuration with existing API key`() {
        val config = AccommodationAnalysisProviderConfig.fromEnvironment(
            mapOf(
                "ACCOMMODATION_ANALYSIS_MODE" to "openrouter",
                "ACCOMMODATION_ANALYSIS_EXTERNAL_CONTENT_APPROVED" to "true",
                "OPENROUTER_API_KEY" to "synthetic-key",
                "ACCOMMODATION_ANALYSIS_MODEL" to "synthetic/vision-model",
                "ACCOMMODATION_ANALYSIS_IMAGE_HOSTS" to
                    "images.example.test, cdn.example.test",
                "ACCOMMODATION_ANALYSIS_BASE_URL" to "https://router.example.test/v1/",
                "ACCOMMODATION_ANALYSIS_TIMEOUT_MS" to "1234",
                "ACCOMMODATION_ANALYSIS_BATCH_SIZE" to "4",
            ),
        )

        assertEquals(AccommodationAnalysisProviderMode.OPENROUTER, config.mode)
        assertEquals("synthetic/vision-model", config.openRouter?.model)
        assertEquals(setOf("images.example.test", "cdn.example.test"), config.openRouter?.imageHosts)
        assertEquals(1_234L, config.openRouter?.timeoutMillis)
        assertEquals(4, config.openRouter?.batchSize)
    }

    @Test
    fun `rejects missing image allowlist wildcard host and oversized batch`() {
        fun environment(hosts: String, batchSize: String) = mapOf(
            "ACCOMMODATION_ANALYSIS_MODE" to "OPENROUTER",
            "ACCOMMODATION_ANALYSIS_EXTERNAL_CONTENT_APPROVED" to "true",
            "OPENROUTER_API_KEY" to "synthetic-key",
            "ACCOMMODATION_ANALYSIS_MODEL" to "synthetic/vision-model",
            "ACCOMMODATION_ANALYSIS_IMAGE_HOSTS" to hosts,
            "ACCOMMODATION_ANALYSIS_BATCH_SIZE" to batchSize,
        )

        assertFailsWith<LlmProviderConfigurationException> {
            AccommodationAnalysisProviderConfig.fromEnvironment(
                environment("*.example.test", "5"),
            )
        }
        assertFailsWith<LlmProviderConfigurationException> {
            AccommodationAnalysisProviderConfig.fromEnvironment(
                environment("images.example.test", "6"),
            )
        }
        assertFailsWith<LlmProviderConfigurationException> {
            AccommodationAnalysisProviderConfig.fromEnvironment(
                environment("", "5"),
            )
        }
    }

    @Test
    fun `rejects OpenRouter activation without explicit external content approval`() {
        val error = assertFailsWith<LlmProviderConfigurationException> {
            AccommodationAnalysisProviderConfig.fromEnvironment(
                mapOf(
                    "ACCOMMODATION_ANALYSIS_MODE" to "OPENROUTER",
                    "OPENROUTER_API_KEY" to "synthetic-key",
                    "ACCOMMODATION_ANALYSIS_MODEL" to "synthetic/vision-model",
                    "ACCOMMODATION_ANALYSIS_IMAGE_HOSTS" to "images.example.test",
                ),
            )
        }

        assertEquals(
            "ACCOMMODATION_ANALYSIS_EXTERNAL_CONTENT_APPROVED",
            error.configurationKey,
        )
    }
}
