package com.travelassistant.backend.infrastructure.llm

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout

internal fun createProductionOpenRouterHttpClient(): HttpClient =
    HttpClient(CIO) {
        applyOpenRouterHttpClientPolicy()
    }

internal fun <T : HttpClientEngineConfig> HttpClientConfig<T>.applyOpenRouterHttpClientPolicy() {
    followRedirects = false
    install(HttpTimeout)
}
