package com.travelassistant.backend.api

import com.travelassistant.backend.domain.hotel.HotelSearch
import kotlinx.serialization.Serializable

@Serializable
data class HotelOffersResponse(
    val searchId: String,
    val status: String,
    val offers: List<HotelOfferResponse>,
    val metadata: Metadata,
    val providerFacts: List<HotelOfferResponse.ProviderFact>,
) {
    @Serializable
    data class Metadata(
        val resultCompleteness: String,
        val freshness: String,
        val providerState: String,
        val warnings: List<String>,
    )

    companion object {
        fun from(search: HotelSearch): HotelOffersResponse {
            val offers = search.offers.map(HotelOfferResponse::from)

            return HotelOffersResponse(
                searchId = search.id.value,
                status = search.status.apiValue,
                offers = offers,
                metadata = Metadata(
                    resultCompleteness = "complete",
                    freshness = "fresh",
                    providerState = "available",
                    warnings = listOf(
                        "Offers preserve fake-provider order; ranking is not applied.",
                    ),
                ),
                providerFacts = offers.flatMap { it.providerFacts },
            )
        }
    }
}
