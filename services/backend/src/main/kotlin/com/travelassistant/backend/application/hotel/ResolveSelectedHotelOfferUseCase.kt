package com.travelassistant.backend.application.hotel

class ResolveSelectedHotelOfferUseCase(
    private val hotelSearchStateStore: HotelSearchStateStore,
) {
    operator fun invoke(
        request: ResolveSelectedHotelOfferRequest,
    ): ResolveSelectedHotelOfferResult {
        val search = hotelSearchStateStore.findById(request.searchId)
            ?: return ResolveSelectedHotelOfferResult.SearchNotFound
        val offer = search.offers
            .firstOrNull { rankedOffer -> rankedOffer.offer.id == request.offerId }
            ?.offer
            ?: return ResolveSelectedHotelOfferResult.OfferNotFound

        return ResolveSelectedHotelOfferResult.Resolved(offer)
    }
}
