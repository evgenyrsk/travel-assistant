package com.travelassistant.backend.api

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String = "ok",
    val service: String = "travel-assistant-backend",
    val version: String = "0.1.0",
)
