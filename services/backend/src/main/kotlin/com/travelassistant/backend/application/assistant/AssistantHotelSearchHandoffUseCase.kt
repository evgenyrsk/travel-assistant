package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.hotel.CreateHotelSearchCommand
import com.travelassistant.backend.application.hotel.HotelSearchBoundary
import com.travelassistant.backend.domain.assistant.AssistantSession

class AssistantHotelSearchHandoffUseCase(
    private val assistantSessionBoundary: AssistantSessionBoundary,
    private val hotelSearchBoundary: HotelSearchBoundary,
    private val messageParser: MinimalHotelSearchMessageParser = MinimalHotelSearchMessageParser(),
) : AssistantSessionBoundary {

    override fun createSession(): AssistantSession =
        assistantSessionBoundary.createSession()

    override fun acceptUserMessage(command: AcceptAssistantMessageCommand): AcceptedAssistantMessage {
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
                val search = hotelSearchBoundary.createSearch(
                    CreateHotelSearchCommand(
                        sessionId = acceptedMessage.sessionId,
                        criteria = parseResult.criteria,
                    ),
                )

                acceptedMessage.copy(
                    assistantReply = AssistantReply(
                        type = AssistantReplyType.HOTEL_SEARCH_RESULTS,
                        message = "Hotel search created. Ranked offers are ready.",
                    ),
                    nextAction = AssistantNextAction.SHOW_HOTEL_RESULTS,
                    hotelSearchId = search.id,
                )
            }
        }
    }

    private companion object {
        const val INCOMPLETE_SEARCH_MESSAGE =
            "I need a complete hotel-search request with destination, check-in, check-out, adults, and rooms."
    }
}
