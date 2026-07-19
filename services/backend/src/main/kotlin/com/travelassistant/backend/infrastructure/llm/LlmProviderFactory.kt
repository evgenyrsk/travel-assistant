package com.travelassistant.backend.infrastructure.llm

import com.travelassistant.backend.application.llm.LlmClient
import com.travelassistant.backend.application.llm.LlmCandidateRetryPolicy
import io.ktor.client.HttpClient

internal object LlmProviderFactory {

    fun create(
        config: LlmProviderConfig,
        fakeClientFactory: () -> LlmClient,
        openRouterHttpClientFactory: () -> HttpClient = ::createProductionOpenRouterHttpClient,
        openRouterDiagnosticObserver: OpenRouterDiagnosticObserver =
            OpenRouterDiagnosticObserver.NONE,
    ): LlmProviderRuntime =
        when (config.mode) {
            LlmProviderMode.FAKE -> LlmProviderRuntime(
                client = fakeClientFactory(),
            )

            LlmProviderMode.OPENROUTER -> createOpenRouterRuntime(
                config = requireNotNull(config.openRouter),
                httpClientFactory = openRouterHttpClientFactory,
                diagnosticObserver = openRouterDiagnosticObserver,
            )
        }

    private fun createOpenRouterRuntime(
        config: OpenRouterConfig,
        httpClientFactory: () -> HttpClient,
        diagnosticObserver: OpenRouterDiagnosticObserver,
    ): LlmProviderRuntime {
        val httpClient = httpClientFactory()

        return LlmProviderRuntime(
            client = OpenRouterLlmClient(
                httpClient = httpClient,
                config = config,
                diagnosticObserver = diagnosticObserver,
            ),
            candidateRetryPolicy = LlmCandidateRetryPolicy.SINGLE_RETRY,
            closeAction = httpClient::close,
        )
    }
}
