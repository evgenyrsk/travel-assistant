package com.travelassistant.backend.infrastructure.accommodation

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout

internal fun createProductionAccommodationAnalysisHttpClient(): HttpClient =
    HttpClient(CIO) {
        followRedirects = false
        install(HttpTimeout)
    }
