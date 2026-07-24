package com.travelassistant.backend.api

import com.travelassistant.backend.application.hotel.HotelDetailsNotFoundException
import com.travelassistant.backend.application.hotel.HotelOfferNotFoundException
import com.travelassistant.backend.application.hotel.HotelSearchNotFoundException
import com.travelassistant.backend.application.hotel.LoadSelectedHotelDetailsResult
import com.travelassistant.backend.application.hotel.LoadSelectedHotelDetailsUseCase
import com.travelassistant.backend.application.hotel.ResolveSelectedHotelOfferRequest
import com.travelassistant.backend.domain.hotel.HotelSearchId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.hotelDetailsRoutes(
    loadSelectedHotelDetails: LoadSelectedHotelDetailsUseCase,
) {
    route("/hotel-searches") {
        get("/{searchId}/offers/{offerId}/details") {
            val searchId = checkNotNull(call.parameters["searchId"])
            val offerId = checkNotNull(call.parameters["offerId"])

            when (
                val result = loadSelectedHotelDetails(
                    ResolveSelectedHotelOfferRequest(
                        searchId = HotelSearchId(searchId),
                        offerId = offerId,
                    ),
                )
            ) {
                is LoadSelectedHotelDetailsResult.Loaded ->
                    call.respond(
                        HttpStatusCode.OK,
                        HotelDetailsResponse.from(result.details),
                    )
                LoadSelectedHotelDetailsResult.SearchNotFound ->
                    throw HotelSearchNotFoundException(HotelSearchId(searchId))
                LoadSelectedHotelDetailsResult.OfferNotFound ->
                    throw HotelOfferNotFoundException()
                LoadSelectedHotelDetailsResult.DetailsNotFound ->
                    throw HotelDetailsNotFoundException()
                is LoadSelectedHotelDetailsResult.ResponseRejected ->
                    call.respondSafeError(
                        status = HttpStatusCode.BadGateway,
                        code = ErrorCode.PROVIDER_RESPONSE_INVALID,
                        message = "Hotel details response could not be accepted.",
                    )
                is LoadSelectedHotelDetailsResult.ProviderUnavailable ->
                    call.respondSafeError(
                        status = HttpStatusCode.ServiceUnavailable,
                        code = ErrorCode.PROVIDER_UNAVAILABLE,
                        message = "Hotel details are temporarily unavailable.",
                    )
            }
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.respondSafeError(
    status: HttpStatusCode,
    code: ErrorCode,
    message: String,
) {
    respond(
        status,
        ErrorResponse(
            code = code,
            message = message,
            requestId = requestIdOrNull(),
        ),
    )
}
