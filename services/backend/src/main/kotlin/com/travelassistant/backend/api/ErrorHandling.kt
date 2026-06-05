package com.travelassistant.backend.api

import com.travelassistant.backend.application.assistant.AssistantSessionNotFoundException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.json.JsonPrimitive

fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<AssistantSessionNotFoundException> { call, error ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    code = ErrorCode.SESSION_NOT_FOUND,
                    message = "Assistant session was not found.",
                    requestId = call.requestIdOrNull(),
                    details = mapOf(
                        "sessionId" to JsonPrimitive(error.sessionId.value),
                    ),
                ),
            )
        }

        status(HttpStatusCode.NotFound) { call, _ ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    code = ErrorCode.NOT_FOUND,
                    message = "Requested backend route was not found.",
                    requestId = call.requestIdOrNull(),
                ),
            )
        }

        exception<Throwable> { call, _ ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    code = ErrorCode.INTERNAL_ERROR,
                    message = "Internal backend error.",
                    requestId = call.requestIdOrNull(),
                ),
            )
        }
    }
}
