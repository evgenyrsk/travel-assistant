package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.hotel.CreateHotelSearchCommand
import com.travelassistant.backend.application.hotel.CreateHotelSearchResult
import com.travelassistant.backend.application.hotel.HotelOfferProviderResult
import com.travelassistant.backend.application.hotel.HotelSearchBoundary
import com.travelassistant.backend.domain.assistant.AssistantSession

class AssistantHotelSearchHandoffUseCase(
    private val assistantSessionBoundary: AssistantSessionBoundary,
    private val hotelSearchBoundary: HotelSearchBoundary,
    private val messageParser: MinimalHotelSearchMessageParser = MinimalHotelSearchMessageParser(),
) : AssistantSessionBoundary {

    override fun createSession(): AssistantSession =
        assistantSessionBoundary.createSession()

    override suspend fun acceptUserMessage(command: AcceptAssistantMessageCommand): AcceptedAssistantMessage {
        val acceptedMessage = assistantSessionBoundary.acceptUserMessage(command)

        return when (val parseResult = messageParser.parse(command.message)) {
            MinimalHotelSearchMessageParser.Result.NotRequested -> acceptedMessage

            MinimalHotelSearchMessageParser.Result.Incomplete ->
                acceptedMessage.copy(
                    assistantReply = AssistantReply(
                        type = AssistantReplyType.CLARIFICATION,
                        message = INCOMPLETE_SEARCH_MESSAGE,
                    ),
                    nextAction = AssistantNextAction.ASK_CLARIFICATION,
                    hotelSearchId = null,
                )

            is MinimalHotelSearchMessageParser.Result.Complete -> {
                when (val searchResult = hotelSearchBoundary.createSearch(
                    CreateHotelSearchCommand(
                        sessionId = acceptedMessage.sessionId,
                        criteria = parseResult.criteria,
                    ),
                )) {
                    is CreateHotelSearchResult.Created ->
                        acceptedMessage.copy(
                            assistantReply = AssistantReply(
                                type = AssistantReplyType.HOTEL_SEARCH_RESULTS,
                                message = "Поиск завершён. Подходящие варианты готовы.",
                            ),
                            nextAction = AssistantNextAction.SHOW_HOTEL_RESULTS,
                            hotelSearchId = searchResult.search.id,
                        )

                    is CreateHotelSearchResult.NotCreated ->
                        acceptedMessage.copy(
                            assistantReply = AssistantReply(
                                type = AssistantReplyType.CLARIFICATION,
                                message = safeClarificationFor(searchResult.outcome),
                            ),
                            nextAction = AssistantNextAction.ASK_CLARIFICATION,
                            hotelSearchId = null,
                        )
                }
            }
        }
    }

    private fun safeClarificationFor(
        outcome: HotelOfferProviderResult.NotCompleted,
    ): String =
        when (outcome) {
            HotelOfferProviderResult.LocationNotFound ->
                LOCATION_NOT_FOUND_MESSAGE

            is HotelOfferProviderResult.LocationSelectionRequired ->
                LOCATION_SELECTION_REQUIRED_MESSAGE

            is HotelOfferProviderResult.RequestRejected ->
                SEARCH_REQUEST_REJECTED_MESSAGE

            is HotelOfferProviderResult.ResponseRejected,
            is HotelOfferProviderResult.ProviderUnavailable,
            ->
                SEARCH_TEMPORARILY_UNAVAILABLE_MESSAGE
        }

    private companion object {
        const val INCOMPLETE_SEARCH_MESSAGE =
            "Укажите направление, даты заезда и выезда, количество взрослых и номеров."

        const val LOCATION_NOT_FOUND_MESSAGE =
            "Не удалось определить направление. Уточните город или место."

        const val LOCATION_SELECTION_REQUIRED_MESSAGE =
            "Найдено несколько подходящих направлений. Уточните город или место."

        const val SEARCH_REQUEST_REJECTED_MESSAGE =
            "Не удалось безопасно подготовить поиск. Проверьте направление, даты, состав гостей и количество номеров."

        const val SEARCH_TEMPORARILY_UNAVAILABLE_MESSAGE =
            "Сейчас не удалось завершить поиск отелей. Попробуйте ещё раз."
    }
}
