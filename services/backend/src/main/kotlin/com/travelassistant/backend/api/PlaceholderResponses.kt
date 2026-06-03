package com.travelassistant.backend.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.serialization.json.JsonPrimitive

suspend fun ApplicationCall.respondNotImplementedPlaceholder(boundary: String) {
    respond(
        HttpStatusCode.NotImplemented,
        ErrorResponse(
            code = ErrorCode.NOT_IMPLEMENTED,
            message = "This hotel-only MVP backend boundary is a Stage 7.2 placeholder.",
            requestId = requestIdOrNull(),
            details = mapOf("boundary" to JsonPrimitive(boundary)),
        ),
    )
}
