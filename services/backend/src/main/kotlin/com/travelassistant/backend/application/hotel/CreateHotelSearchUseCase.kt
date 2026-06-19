package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.application.assistant.AssistantSessionNotFoundException
import com.travelassistant.backend.application.assistant.AssistantSessionStateStore
import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.HotelSearchId
import com.travelassistant.backend.domain.provider.HotelOfferProviderBoundary

class CreateHotelSearchUseCase(
    private val assistantSessionStateStore: AssistantSessionStateStore,
    private val hotelOfferProvider: HotelOfferProviderBoundary,
    private val hotelSearchStateStore: HotelSearchStateStore = InMemoryHotelSearchStateStore(),
    private val idGenerator: HotelSearchIdGenerator = LocalHotelSearchIdGenerator(),
) : HotelSearchBoundary {

    override fun createSearch(command: CreateHotelSearchCommand): HotelSearch {
        assistantSessionStateStore.findById(command.sessionId)
            ?: throw AssistantSessionNotFoundException(command.sessionId)

        val offers = hotelOfferProvider.search(command.criteria)
        val status = if (offers.isEmpty()) {
            HotelSearch.Status.COMPLETED_NO_OFFERS
        } else {
            HotelSearch.Status.COMPLETED_WITH_OFFERS
        }

        return hotelSearchStateStore.save(
            HotelSearch(
                id = idGenerator.nextId(),
                sessionId = command.sessionId,
                criteria = command.criteria,
                status = status,
                offers = offers,
            ),
        )
    }

    override fun getSearch(searchId: HotelSearchId): HotelSearch =
        hotelSearchStateStore.findById(searchId)
            ?: throw HotelSearchNotFoundException(searchId)
}
