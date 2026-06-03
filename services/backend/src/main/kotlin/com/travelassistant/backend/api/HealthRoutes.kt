package com.travelassistant.backend.api

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureHealthRoutes() {
    routing {
        route("/api/v1") {
            get("/health") {
                call.respond(HealthResponse())
            }
        }
    }
}
