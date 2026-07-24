package com.travelassistant.backend.api

import com.travelassistant.backend.application.assistant.AssistantSessionNotFoundException
import com.travelassistant.backend.application.hotel.HotelDetailsNotFoundException
import com.travelassistant.backend.application.hotel.HotelOfferNotFoundException
import com.travelassistant.backend.application.hotel.HotelSearchNotFoundException
import com.travelassistant.backend.application.observability.OperationalComponent
import com.travelassistant.backend.application.observability.OperationalError
import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventName
import com.travelassistant.backend.application.observability.OperationalEventSink
import com.travelassistant.backend.application.observability.OperationalLevel
import com.travelassistant.backend.application.observability.recordSafely
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.json.JsonPrimitive

fun Application.configureErrorHandling(
    eventSink: OperationalEventSink = OperationalEventSink.NONE,
) {
    install(StatusPages) {
        exception<AssistantSessionNotFoundException> { call, error ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    code = ErrorCode.SESSION_NOT_FOUND,
                    message = "Assistant session was not found.",
                    requestId = call.requestId(),
                    details = mapOf(
                        "sessionId" to JsonPrimitive(error.sessionId.value),
                    ),
                ),
            )
        }

        exception<HotelSearchNotFoundException> { call, error ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    code = ErrorCode.HOTEL_SEARCH_NOT_FOUND,
                    message = "Hotel search was not found.",
                    requestId = call.requestId(),
                    details = mapOf(
                        "searchId" to JsonPrimitive(error.searchId.value),
                    ),
                ),
            )
        }

        exception<HotelOfferNotFoundException> { call, _ ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    code = ErrorCode.HOTEL_OFFER_NOT_FOUND,
                    message = "Hotel offer was not found.",
                    requestId = call.requestId(),
                ),
            )
        }

        exception<HotelDetailsNotFoundException> { call, _ ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    code = ErrorCode.HOTEL_DETAILS_NOT_FOUND,
                    message = "Hotel details were not found.",
                    requestId = call.requestId(),
                ),
            )
        }

        status(HttpStatusCode.NotFound) { call, _ ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    code = ErrorCode.NOT_FOUND,
                    message = "Requested backend route was not found.",
                    requestId = call.requestId(),
                ),
            )
        }

        exception<Throwable> { call, error ->
            eventSink.recordSafely(
                OperationalEvent(
                    name = OperationalEventName.UNEXPECTED_ERROR,
                    component = OperationalComponent.HTTP,
                    level = OperationalLevel.ERROR,
                    requestId = call.requestId(),
                    error = OperationalError.from(error),
                ),
            )
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    code = ErrorCode.INTERNAL_ERROR,
                    message = "Internal backend error.",
                    requestId = call.requestId(),
                ),
            )
        }
    }
}
