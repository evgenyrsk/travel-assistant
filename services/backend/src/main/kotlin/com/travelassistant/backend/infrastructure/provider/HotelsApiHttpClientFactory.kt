package com.travelassistant.backend.infrastructure.provider

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout

internal fun createProductionHotelsApiHttpClient(): HttpClient =
    HttpClient(CIO) {
        install(HttpTimeout)
    }
