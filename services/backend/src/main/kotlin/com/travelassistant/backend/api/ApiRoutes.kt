package com.travelassistant.backend.api

import com.travelassistant.backend.application.assistant.AssistantSessionBoundary
import com.travelassistant.backend.application.hotel.HotelSearchBoundary
import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureApiRoutes(
    assistantSessionBoundary: AssistantSessionBoundary,
    hotelSearchBoundary: HotelSearchBoundary,
) {
    routing {
        route("/api/v1") {
            healthRoutes()
            assistantPlaceholderRoutes(assistantSessionBoundary)
            hotelSearchRoutes(hotelSearchBoundary)
        }
    }
}
