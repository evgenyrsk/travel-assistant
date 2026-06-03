package com.travelassistant.backend.api

import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.hotelSearchPlaceholderRoutes() {
    route("/hotel-searches") {
        post {
            call.respondNotImplementedPlaceholder("hotel.search.create")
        }

        get("/{searchId}/offers") {
            call.respondNotImplementedPlaceholder("hotel.search.offers.read")
        }
    }
}
