package com.travelassistant.backend.infrastructure.provider

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.HttpTimeout

internal fun <T : HttpClientEngineConfig> HttpClientConfig<T>.applyHotelsApiQaPolicy() {
    followRedirects = false
    install(HttpTimeout)
}
