package com.travelassistant.backend.api

import com.travelassistant.backend.application.hotel.CreateHotelSearchResult
import com.travelassistant.backend.application.hotel.HotelOfferProviderResult
import com.travelassistant.backend.application.hotel.HotelSearchBoundary
import com.travelassistant.backend.application.hotel.PlanHotelNoOffersRefinementUseCase
import com.travelassistant.backend.domain.hotel.HotelSearchId
import io.ktor.http.HttpStatusCode
import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.request.contentType
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.decodeFromString

fun Route.hotelSearchRoutes(
    hotelSearchBoundary: HotelSearchBoundary,
    planNoOffersRefinement: PlanHotelNoOffersRefinementUseCase =
        PlanHotelNoOffersRefinementUseCase(),
) {
    route("/hotel-searches") {
        post {
            val request = when (val body = call.readBoundedRequestBody()) {
                is BoundedRequestBodyReadResult.Accepted ->
                    if (
                        call.request.contentType().withoutParameters() ==
                        ContentType.Application.Json
                    ) {
                        runCatching {
                            ApiJson.decodeFromString<HotelSearchRequest>(body.text)
                        }.getOrNull()
                    } else {
                        null
                    }

                BoundedRequestBodyReadResult.NoBody,
                BoundedRequestBodyReadResult.TooLarge,
                -> null
            }

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
                    when (val result = hotelSearchBoundary.createSearch(validation.command)) {
                        is CreateHotelSearchResult.Created ->
                            call.respond(
                                HttpStatusCode.Accepted,
                                HotelSearchResponse.from(result.search),
                            )

                        is CreateHotelSearchResult.NotCreated ->
                            call.respondNotCreated(result.outcome)
                    }
                }
            }
        }

        get("/{searchId}/offers") {
            val searchId = HotelSearchId(checkNotNull(call.parameters["searchId"]))
            val search = hotelSearchBoundary.getSearch(searchId)

            call.respond(
                HttpStatusCode.OK,
                HotelOffersResponse.from(
                    search = search,
                    refinementPlan = planNoOffersRefinement(search),
                ),
            )
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.respondNotCreated(
    outcome: HotelOfferProviderResult.NotCompleted,
) {
    when (outcome) {
        HotelOfferProviderResult.LocationNotFound -> {
            respondValidationError(
                field = "criteria.destination",
                message = "Destination could not be matched.",
            )
        }

        is HotelOfferProviderResult.LocationSelectionRequired -> {
            respondValidationError(
                field = "criteria.destination",
                message = "Destination is ambiguous. Please provide a more specific location.",
            )
        }

        is HotelOfferProviderResult.RequestRejected -> {
            respondValidationError(
                field = "criteria",
                message = "Hotel search criteria could not be accepted.",
            )
        }

        is HotelOfferProviderResult.ResponseRejected,
        is HotelOfferProviderResult.ProviderUnavailable,
        -> {
            respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    code = ErrorCode.INTERNAL_ERROR,
                    message = "Hotel search could not be completed.",
                    requestId = requestIdOrNull(),
                ),
            )
        }
    }
}
