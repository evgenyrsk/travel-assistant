package com.travelassistant.backend.api

import com.travelassistant.backend.application.hotel.HotelSearchBoundary
import com.travelassistant.backend.domain.hotel.HotelSearchId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.hotelSearchRoutes(hotelSearchBoundary: HotelSearchBoundary) {
    route("/hotel-searches") {
        post {
            val request = runCatching {
                call.receiveNullable<HotelSearchRequest>()
            }.getOrNull()

            if (request == null) {
                call.respondValidationError(
                    field = "body",
                    message = "Hotel search request body must be valid JSON.",
                )
                return@post
            }

            when (val validation = request.validate()) {
                is HotelSearchRequest.ValidationResult.Invalid -> {
                    call.respondValidationError(
                        field = validation.field,
                        message = validation.message,
                    )
                }

                is HotelSearchRequest.ValidationResult.Valid -> {
                    val search = hotelSearchBoundary.createSearch(validation.command)
                    call.respond(
                        HttpStatusCode.Accepted,
                        HotelSearchResponse.from(search),
                    )
                }
            }
        }

        get("/{searchId}/offers") {
            val searchId = HotelSearchId(checkNotNull(call.parameters["searchId"]))
            val search = hotelSearchBoundary.getSearch(searchId)

            call.respond(
                HttpStatusCode.OK,
                HotelOffersResponse.from(search),
            )
        }
    }
}
