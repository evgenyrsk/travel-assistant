package com.travelassistant.backend.application.hotel

class LoadSelectedHotelDetailsUseCase(
    private val resolveSelectedOffer: ResolveSelectedHotelOfferUseCase,
    private val hotelDetailsProvider: HotelDetailsProviderBoundary,
) {
    suspend operator fun invoke(
        request: ResolveSelectedHotelOfferRequest,
    ): LoadSelectedHotelDetailsResult =
        when (val selected = resolveSelectedOffer(request)) {
            ResolveSelectedHotelOfferResult.SearchNotFound ->
                LoadSelectedHotelDetailsResult.SearchNotFound
            ResolveSelectedHotelOfferResult.OfferNotFound ->
                LoadSelectedHotelDetailsResult.OfferNotFound
            is ResolveSelectedHotelOfferResult.Resolved ->
                loadProviderDetails(selected.offer.providerReference)
        }

    private suspend fun loadProviderDetails(
        providerReference: String,
    ): LoadSelectedHotelDetailsResult =
        when (val result = hotelDetailsProvider.load(providerReference)) {
            is HotelDetailsProviderResult.Loaded ->
                LoadSelectedHotelDetailsResult.Loaded(result.details)
            HotelDetailsProviderResult.NotFound ->
                LoadSelectedHotelDetailsResult.DetailsNotFound
            is HotelDetailsProviderResult.ResponseRejected ->
                LoadSelectedHotelDetailsResult.ResponseRejected(result.reason)
            is HotelDetailsProviderResult.ProviderUnavailable ->
                LoadSelectedHotelDetailsResult.ProviderUnavailable(result.reason)
        }
}
