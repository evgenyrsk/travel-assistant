package com.travelassistant.backend

import com.travelassistant.backend.api.configureHealthRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(
        factory = Netty,
        host = "0.0.0.0",
        port = System.getenv("PORT")?.toIntOrNull() ?: 8080,
        module = Application::module,
    ).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json { encodeDefaults = true })
    }

    configureHealthRoutes()
}
