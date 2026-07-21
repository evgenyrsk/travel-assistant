package com.travelassistant.backend.api

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
internal val ApiJson = Json {
    encodeDefaults = true
    explicitNulls = false
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(ApiJson)
    }
}
