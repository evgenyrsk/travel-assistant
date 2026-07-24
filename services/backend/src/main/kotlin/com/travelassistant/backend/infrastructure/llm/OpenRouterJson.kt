package com.travelassistant.backend.infrastructure.llm

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
internal object OpenRouterJson {
    val wireCodec: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = false
        coerceInputValues = false
        encodeDefaults = false
    }

    val candidateCodec: Json = Json {
        ignoreUnknownKeys = false
        explicitNulls = true
        isLenient = false
        coerceInputValues = false
        encodeDefaults = false
    }
}
