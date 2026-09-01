package com.travelassistant.backend.infrastructure.accommodation

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AccommodationAnalysisProviderFactoryTest {

    @Test
    fun `creates internal gateway adapter only for explicit mode`() {
        var clientCreations = 0
        val runtime = AccommodationAnalysisProviderFactory.create(
            config = AccommodationAnalysisProviderConfig(
                mode = AccommodationAnalysisProviderMode.INTERNAL_GATEWAY,
                internalGateway = InternalGatewayAccommodationAnalysisConfig(
                    endpointUrl = "https://semantic.internal.test/v1/accommodation-analysis",
                    deploymentId = "vision-balanced-v1",
                    accessToken = InternalGatewayAccessToken.of("synthetic-token"),
                    imageHosts = setOf("images.internal.test"),
                ),
            ),
            httpClientFactory = {
                clientCreations += 1
                HttpClient(MockEngine { error("Network call was not expected") })
            },
        )

        assertIs<InternalGatewayAccommodationAnalysisClient>(runtime.client)
        assertEquals(1, clientCreations)
        runtime.close()
    }
}
