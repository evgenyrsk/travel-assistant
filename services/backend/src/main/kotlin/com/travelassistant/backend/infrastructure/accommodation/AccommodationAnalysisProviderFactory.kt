package com.travelassistant.backend.infrastructure.accommodation

import com.travelassistant.backend.infrastructure.llm.createProductionOpenRouterHttpClient
import io.ktor.client.HttpClient

internal object AccommodationAnalysisProviderFactory {
    fun create(
        config: AccommodationAnalysisProviderConfig,
        openRouterHttpClientFactory: () -> HttpClient = ::createProductionOpenRouterHttpClient,
    ): AccommodationAnalysisProviderRuntime =
        when (config.mode) {
            AccommodationAnalysisProviderMode.FAKE -> AccommodationAnalysisProviderRuntime(
                client = FakeAccommodationAnalysisClient(),
            )

            AccommodationAnalysisProviderMode.OPENROUTER -> {
                val httpClient = openRouterHttpClientFactory()
                AccommodationAnalysisProviderRuntime(
                    client = OpenRouterAccommodationAnalysisClient(
                        httpClient = httpClient,
                        config = checkNotNull(config.openRouter),
                    ),
                    closeAction = httpClient::close,
                )
            }
        }
}
