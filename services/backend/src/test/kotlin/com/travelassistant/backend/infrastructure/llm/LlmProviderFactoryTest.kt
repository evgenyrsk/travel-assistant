package com.travelassistant.backend.infrastructure.llm

import com.travelassistant.backend.application.llm.LlmClientResponse
import com.travelassistant.backend.application.llm.LlmCandidateRetryPolicy
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.HttpTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class LlmProviderFactoryTest {

    @Test
    fun `fake mode creates fake client without creating OpenRouter HTTP client`() {
        val fakeClient = FakeLlmClient(LlmClientResponse.Empty)
        var openRouterClientCreations = 0

        val runtime = LlmProviderFactory.create(
            config = LlmProviderConfig(),
            fakeClientFactory = { fakeClient },
            openRouterHttpClientFactory = {
                openRouterClientCreations += 1
                mockHttpClient()
            },
        )

        assertSame(fakeClient, runtime.client)
        assertSame(LlmCandidateRetryPolicy.NO_RETRY, runtime.candidateRetryPolicy)
        assertEquals(0, openRouterClientCreations)
        runtime.close()
    }

    @Test
    fun `OpenRouter mode creates adapter without creating fake client`() {
        var fakeClientCreations = 0
        var openRouterClientCreations = 0

        val runtime = LlmProviderFactory.create(
            config = openRouterProviderConfig(),
            fakeClientFactory = {
                fakeClientCreations += 1
                FakeLlmClient(LlmClientResponse.Empty)
            },
            openRouterHttpClientFactory = {
                openRouterClientCreations += 1
                mockHttpClient()
            },
        )

        assertIs<OpenRouterLlmClient>(runtime.client)
        assertSame(LlmCandidateRetryPolicy.SINGLE_RETRY, runtime.candidateRetryPolicy)
        assertEquals(0, fakeClientCreations)
        assertEquals(1, openRouterClientCreations)
        runtime.close()
    }

    @Test
    fun `runtime closes owned resource only once`() {
        var closeCount = 0
        val runtime = LlmProviderRuntime(
            client = FakeLlmClient(LlmClientResponse.Empty),
            closeAction = { closeCount += 1 },
        )

        runtime.close()
        runtime.close()

        assertEquals(1, closeCount)
    }

    private fun openRouterProviderConfig(): LlmProviderConfig =
        LlmProviderConfig(
            mode = LlmProviderMode.OPENROUTER,
            openRouter = OpenRouterConfig(
                apiKey = OpenRouterApiKey.of("synthetic-openrouter-api-key"),
                model = "test/model",
                baseUrl = "https://openrouter.test/api/v1/",
                timeoutMillis = 5_000,
            ),
        )

    private fun mockHttpClient(): HttpClient =
        HttpClient(MockEngine { respondOk() }) {
            install(HttpTimeout)
        }
}
