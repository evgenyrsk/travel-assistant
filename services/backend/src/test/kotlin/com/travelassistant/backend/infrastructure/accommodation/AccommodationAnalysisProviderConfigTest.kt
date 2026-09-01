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
                "ACCOMMODATION_ANALYSIS_PROVIDER_ENDPOINT" to "synthetic/eu",
                "ACCOMMODATION_ANALYSIS_IMAGE_HOSTS" to
                    "images.example.test, cdn.example.test",
                "ACCOMMODATION_ANALYSIS_BASE_URL" to "https://router.example.test/v1/",
                "ACCOMMODATION_ANALYSIS_TIMEOUT_MS" to "1234",
                "ACCOMMODATION_ANALYSIS_BATCH_SIZE" to "4",
            ),
        )

        assertEquals(AccommodationAnalysisProviderMode.OPENROUTER, config.mode)
        assertEquals("synthetic/vision-model", config.openRouter?.model)
        assertEquals("synthetic/eu", config.openRouter?.providerEndpoint)
        assertEquals(setOf("images.example.test", "cdn.example.test"), config.openRouter?.imageHosts)
        assertEquals(1_234L, config.openRouter?.timeoutMillis)
        assertEquals(4, config.openRouter?.batchSize)
    }

    @Test
    fun `parses opt in internal gateway configuration`() {
        val config = AccommodationAnalysisProviderConfig.fromEnvironment(
            mapOf(
                "ACCOMMODATION_ANALYSIS_MODE" to "internal_gateway",
                "ACCOMMODATION_ANALYSIS_INTERNAL_CONTENT_APPROVED" to "true",
                "ACCOMMODATION_ANALYSIS_INTERNAL_GATEWAY_URL" to
                    "https://semantic.internal.test/v1/accommodation-analysis",
                "ACCOMMODATION_ANALYSIS_DEPLOYMENT_ID" to "vision-balanced-v1",
                "ACCOMMODATION_ANALYSIS_INTERNAL_GATEWAY_TOKEN" to "synthetic-token",
                "ACCOMMODATION_ANALYSIS_IMAGE_HOSTS" to "images.internal.test",
                "ACCOMMODATION_ANALYSIS_TIMEOUT_MS" to "4321",
                "ACCOMMODATION_ANALYSIS_BATCH_SIZE" to "6",
            ),
        )

        assertEquals(AccommodationAnalysisProviderMode.INTERNAL_GATEWAY, config.mode)
        assertEquals("vision-balanced-v1", config.internalGateway?.deploymentId)
        assertEquals(4_321L, config.internalGateway?.timeoutMillis)
        assertEquals(6, config.internalGateway?.batchSize)
        assertEquals("[REDACTED]", config.internalGateway?.accessToken.toString())
    }

    @Test
    fun `rejects missing image allowlist wildcard host and oversized batch`() {
        fun environment(hosts: String, batchSize: String) = mapOf(
            "ACCOMMODATION_ANALYSIS_MODE" to "OPENROUTER",
            "ACCOMMODATION_ANALYSIS_EXTERNAL_CONTENT_APPROVED" to "true",
            "OPENROUTER_API_KEY" to "synthetic-key",
            "ACCOMMODATION_ANALYSIS_MODEL" to "synthetic/vision-model",
            "ACCOMMODATION_ANALYSIS_PROVIDER_ENDPOINT" to "synthetic/eu",
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
    fun `rejects missing or non exact provider endpoint`() {
        fun environment(endpoint: String?) = buildMap {
            put("ACCOMMODATION_ANALYSIS_MODE", "OPENROUTER")
            put("ACCOMMODATION_ANALYSIS_EXTERNAL_CONTENT_APPROVED", "true")
            put("OPENROUTER_API_KEY", "synthetic-key")
            put("ACCOMMODATION_ANALYSIS_MODEL", "synthetic/vision-model")
            put("ACCOMMODATION_ANALYSIS_IMAGE_HOSTS", "images.example.test")
            endpoint?.let { put("ACCOMMODATION_ANALYSIS_PROVIDER_ENDPOINT", it) }
        }

        listOf(null, "synthetic,*", "synthetic/*", "Synthetic/eu").forEach { endpoint ->
            val error = assertFailsWith<LlmProviderConfigurationException> {
                AccommodationAnalysisProviderConfig.fromEnvironment(environment(endpoint))
            }
            assertEquals("ACCOMMODATION_ANALYSIS_PROVIDER_ENDPOINT", error.configurationKey)
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
                    "ACCOMMODATION_ANALYSIS_PROVIDER_ENDPOINT" to "synthetic/eu",
                    "ACCOMMODATION_ANALYSIS_IMAGE_HOSTS" to "images.example.test",
                ),
            )
        }

        assertEquals(
            "ACCOMMODATION_ANALYSIS_EXTERNAL_CONTENT_APPROVED",
            error.configurationKey,
        )
    }

    @Test
    fun `rejects internal gateway activation without approval or exact contract config`() {
        fun environment() = mutableMapOf(
            "ACCOMMODATION_ANALYSIS_MODE" to "INTERNAL_GATEWAY",
            "ACCOMMODATION_ANALYSIS_INTERNAL_CONTENT_APPROVED" to "true",
            "ACCOMMODATION_ANALYSIS_INTERNAL_GATEWAY_URL" to
                "https://semantic.internal.test/v1/accommodation-analysis",
            "ACCOMMODATION_ANALYSIS_DEPLOYMENT_ID" to "vision-balanced-v1",
            "ACCOMMODATION_ANALYSIS_INTERNAL_GATEWAY_TOKEN" to "synthetic-token",
            "ACCOMMODATION_ANALYSIS_IMAGE_HOSTS" to "images.internal.test",
        )

        val missingApproval = environment().also {
            it.remove("ACCOMMODATION_ANALYSIS_INTERNAL_CONTENT_APPROVED")
        }
        assertEquals(
            "ACCOMMODATION_ANALYSIS_INTERNAL_CONTENT_APPROVED",
            assertFailsWith<LlmProviderConfigurationException> {
                AccommodationAnalysisProviderConfig.fromEnvironment(missingApproval)
            }.configurationKey,
        )

        val wrongPath = environment().also {
            it["ACCOMMODATION_ANALYSIS_INTERNAL_GATEWAY_URL"] =
                "https://semantic.internal.test/v1/chat/completions"
        }
        assertEquals(
            "ACCOMMODATION_ANALYSIS_INTERNAL_GATEWAY_URL",
            assertFailsWith<LlmProviderConfigurationException> {
                AccommodationAnalysisProviderConfig.fromEnvironment(wrongPath)
            }.configurationKey,
        )

        val implicitDeployment = environment().also {
            it.remove("ACCOMMODATION_ANALYSIS_DEPLOYMENT_ID")
        }
        assertEquals(
            "ACCOMMODATION_ANALYSIS_DEPLOYMENT_ID",
            assertFailsWith<LlmProviderConfigurationException> {
                AccommodationAnalysisProviderConfig.fromEnvironment(implicitDeployment)
            }.configurationKey,
        )

        val missingToken = environment().also {
            it.remove("ACCOMMODATION_ANALYSIS_INTERNAL_GATEWAY_TOKEN")
        }
        assertEquals(
            "ACCOMMODATION_ANALYSIS_INTERNAL_GATEWAY_TOKEN",
            assertFailsWith<LlmProviderConfigurationException> {
                AccommodationAnalysisProviderConfig.fromEnvironment(missingToken)
            }.configurationKey,
        )

        val wildcardImageHost = environment().also {
            it["ACCOMMODATION_ANALYSIS_IMAGE_HOSTS"] = "*.internal.test"
        }
        assertEquals(
            "ACCOMMODATION_ANALYSIS_IMAGE_HOSTS",
            assertFailsWith<LlmProviderConfigurationException> {
                AccommodationAnalysisProviderConfig.fromEnvironment(wildcardImageHost)
            }.configurationKey,
        )
    }
}
