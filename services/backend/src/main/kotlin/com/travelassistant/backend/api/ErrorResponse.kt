package com.travelassistant.backend.api

import io.ktor.server.application.ApplicationCall
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ErrorResponse(
    val code: ErrorCode,
    val message: String,
    val requestId: String? = null,
    val details: Map<String, JsonElement>? = null,
)

@Serializable
enum class ErrorCode {
    @SerialName("NOT_IMPLEMENTED")
    NOT_IMPLEMENTED,

    @SerialName("NOT_FOUND")
    NOT_FOUND,

    @SerialName("VALIDATION_ERROR")
    VALIDATION_ERROR,

    @SerialName("SESSION_NOT_FOUND")
    SESSION_NOT_FOUND,

    @SerialName("INTERNAL_ERROR")
    INTERNAL_ERROR,
}

fun ApplicationCall.requestIdOrNull(): String? = request.headers["X-Request-ID"]
