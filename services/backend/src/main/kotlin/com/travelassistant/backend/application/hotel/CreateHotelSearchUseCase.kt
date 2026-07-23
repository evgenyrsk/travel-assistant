package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.application.assistant.AssistantSessionNotFoundException
import com.travelassistant.backend.application.assistant.AssistantSessionStateStore
import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.HotelSearchId
import com.travelassistant.backend.domain.hotel.HotelOfferRanker

class CreateHotelSearchUseCase(
    private val assistantSessionStateStore: AssistantSessionStateStore,
    private val hotelOfferProvider: HotelOfferProviderBoundary,
    private val hotelOfferRanker: HotelOfferRanker = HotelOfferRanker(),
    private val hotelSearchStateStore: HotelSearchStateStore = InMemoryHotelSearchStateStore(),
    private val idGenerator: HotelSearchIdGenerator = LocalHotelSearchIdGenerator(),
    private val offerIdGenerator: HotelOfferIdGenerator = LocalHotelOfferIdGenerator(),
) : HotelSearchBoundary {

    override suspend fun createSearch(command: CreateHotelSearchCommand): CreateHotelSearchResult {
        assistantSessionStateStore.findById(command.sessionId)
            ?: throw AssistantSessionNotFoundException(command.sessionId)

        return when (val providerResult = hotelOfferProvider.search(command.criteria)) {
            is HotelOfferProviderResult.SearchCompleted ->
                createAndSaveSearch(command, providerResult)

            is HotelOfferProviderResult.NotCompleted ->
                CreateHotelSearchResult.NotCreated(providerResult)
        }
    }

    private fun createAndSaveSearch(
        command: CreateHotelSearchCommand,
        providerResult: HotelOfferProviderResult.SearchCompleted,
    ): CreateHotelSearchResult.Created {
        val identifiedOffers = providerResult.offers.map { candidate ->
            candidate.identifiedBy(offerIdGenerator.nextId())
        }
        val rankedOffers = hotelOfferRanker.rank(identifiedOffers)
        val status = if (rankedOffers.isEmpty()) {
            HotelSearch.Status.COMPLETED_NO_OFFERS
        } else {
            HotelSearch.Status.COMPLETED_WITH_OFFERS
        }

        return CreateHotelSearchResult.Created(
            hotelSearchStateStore.save(
                HotelSearch(
                    id = idGenerator.nextId(),
                    sessionId = command.sessionId,
                    criteria = command.criteria,
                    status = status,
                    offers = rankedOffers,
                ),
            ),
        )
    }

    override fun getSearch(searchId: HotelSearchId): HotelSearch =
        hotelSearchStateStore.findById(searchId)
            ?: throw HotelSearchNotFoundException(searchId)
}
