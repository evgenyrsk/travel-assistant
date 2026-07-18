package com.travelassistant.backend.infrastructure.llm

internal class LlmProviderConfigurationException(
    val configurationKey: String,
    reason: String,
) : IllegalArgumentException("$configurationKey $reason")
