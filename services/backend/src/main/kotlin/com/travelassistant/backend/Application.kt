package com.travelassistant.backend

import com.travelassistant.backend.api.configureApiRoutes
import com.travelassistant.backend.api.configureErrorHandling
import com.travelassistant.backend.api.configureSerialization
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    embeddedServer(
        factory = Netty,
        host = "0.0.0.0",
        port = System.getenv("PORT")?.toIntOrNull() ?: 8080,
        module = Application::module,
    ).start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureErrorHandling()
    configureApiRoutes()
}
