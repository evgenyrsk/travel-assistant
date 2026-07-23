package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.ExactMatchHotelLocationCandidateSelectionPolicy
import com.travelassistant.backend.application.hotel.ExactNamedHotelCandidateSelectionPolicy
import com.travelassistant.backend.application.hotel.HotelLocationResolutionRequest
import io.ktor.client.HttpClient

object HotelOfferProviderFactory {

    internal fun create(
        config: HotelProviderConfig,
        realHttpClientFactory: () -> HttpClient = ::createProductionHotelsApiHttpClient,
    ): HotelOfferProviderRuntime =
        when (config.mode) {
            HotelProviderMode.FAKE -> HotelOfferProviderRuntime(
                provider = FakeHotelOfferProvider(),
                detailsProvider = FakeHotelDetailsProvider(),
            )

            HotelProviderMode.REAL -> createRealRuntime(
                config = requireNotNull(config.hotelsApi),
                httpClientFactory = realHttpClientFactory,
            )
        }

    private fun createRealRuntime(
        config: HotelsApiConfig,
        httpClientFactory: () -> HttpClient,
    ): HotelOfferProviderRuntime {
        val httpClient = httpClientFactory()
        val transport = PublicHotelsApiHttpTransport(
            httpClient = httpClient,
            publicTarget = config.publicTarget,
        )
        val orchestrator = HotelsApiSearchOrchestrator(
            locationResolver = PublicHotelsApiLocationResolverAdapter(transport),
            locationSelectionPolicy = ExactMatchHotelLocationCandidateSelectionPolicy(),
            hotelSelectionPolicy = ExactNamedHotelCandidateSelectionPolicy(),
            exactHotelSearchOrchestrator = HotelsApiExactHotelSearchOrchestrator(transport),
            transport = transport,
        )
        val language = config.userLanguage?.let(HotelLocationResolutionRequest.Language::valueOf)

        return HotelOfferProviderRuntime(
            provider = RealHotelOfferProviderAdapter(
                search = orchestrator::search,
                language = language,
            ),
            detailsProvider = HotelsApiHotelDetailsProviderAdapter(
                transport = transport,
                language = language,
            ),
            closeAction = httpClient::close,
        )
    }
}
