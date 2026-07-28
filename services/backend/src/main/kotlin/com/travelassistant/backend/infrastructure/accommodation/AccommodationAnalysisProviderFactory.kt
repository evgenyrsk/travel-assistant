package com.travelassistant.backend.infrastructure.accommodation

import io.ktor.client.HttpClient

internal object AccommodationAnalysisProviderFactory {
    fun create(
        config: AccommodationAnalysisProviderConfig,
        httpClientFactory: () -> HttpClient = ::createProductionAccommodationAnalysisHttpClient,
    ): AccommodationAnalysisProviderRuntime =
        when (config.mode) {
            AccommodationAnalysisProviderMode.FAKE -> AccommodationAnalysisProviderRuntime(
                client = FakeAccommodationAnalysisClient(),
            )

            AccommodationAnalysisProviderMode.OPENROUTER -> {
                val httpClient = httpClientFactory()
                AccommodationAnalysisProviderRuntime(
                    client = OpenRouterAccommodationAnalysisClient(
                        httpClient = httpClient,
                        config = checkNotNull(config.openRouter),
                    ),
                    closeAction = httpClient::close,
                )
            }

            AccommodationAnalysisProviderMode.INTERNAL_GATEWAY -> {
                val httpClient = httpClientFactory()
                AccommodationAnalysisProviderRuntime(
                    client = InternalGatewayAccommodationAnalysisClient(
                        httpClient = httpClient,
                        config = checkNotNull(config.internalGateway),
                    ),
                    closeAction = httpClient::close,
                )
            }
        }
}
