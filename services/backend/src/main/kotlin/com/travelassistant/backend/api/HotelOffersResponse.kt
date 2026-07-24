package com.travelassistant.backend.api

import com.travelassistant.backend.application.hotel.HotelNoOffersRefinementPlan
import com.travelassistant.backend.domain.hotel.HotelSearch
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class HotelOffersResponse(
    val searchId: String,
    val status: String,
    val offers: List<HotelOfferResponse>,
    val metadata: Metadata,
    val providerFacts: List<HotelOfferResponse.ProviderFact>,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val appliedPreferences: AppliedHotelSearchPreferencesResponse? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val refinementSuggestion: RefinementSuggestion? = null,
) {
    @Serializable
    data class Metadata(
        val resultCompleteness: String,
        val freshness: String,
        val providerState: String,
        val warnings: List<String>,
    )

    @Serializable
    data class RefinementSuggestion(
        val type: String,
        val preference: String,
        val message: String,
    )

    companion object {
        fun from(
            search: HotelSearch,
            refinementPlan: HotelNoOffersRefinementPlan =
                HotelNoOffersRefinementPlan.NotApplicable,
        ): HotelOffersResponse {
            val offers = search.offers.map(HotelOfferResponse::from)

            return HotelOffersResponse(
                searchId = search.id.value,
                status = search.status.apiValue,
                offers = offers,
                metadata = Metadata(
                    resultCompleteness = "complete",
                    freshness = "fresh",
                    providerState = "available",
                    warnings = emptyList(),
                ),
                providerFacts = offers.flatMap { it.providerFacts },
                appliedPreferences = AppliedHotelSearchPreferencesResponse.from(
                    search.criteria.preferences,
                ),
                refinementSuggestion = when (refinementPlan) {
                    HotelNoOffersRefinementPlan.NotApplicable -> null
                    is HotelNoOffersRefinementPlan.Suggestion ->
                        RefinementSuggestion(
                            type = "relax_preference",
                            preference = refinementPlan.preference.apiValue,
                            message = refinementPlan.message,
                        )
                },
            )
        }
    }
}
