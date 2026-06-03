package com.travelassistant.backend.api

import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureApiRoutes() {
    routing {
        route("/api/v1") {
            healthRoutes()
            assistantPlaceholderRoutes()
            hotelSearchPlaceholderRoutes()
        }
    }
}
