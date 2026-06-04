package com.travelassistant.backend.api

import com.travelassistant.backend.application.assistant.AcceptAssistantMessageCommand
import com.travelassistant.backend.application.assistant.AssistantSessionBoundary
import com.travelassistant.backend.application.assistant.CreateAssistantSessionUseCase
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

fun Route.assistantPlaceholderRoutes() {
    val createAssistantSession: AssistantSessionBoundary = CreateAssistantSessionUseCase()

    route("/assistant/sessions") {
        post {
            val session = createAssistantSession.createSession()

            call.respond(
                HttpStatusCode.Created,
                AssistantSessionCreatedResponse(
                    sessionId = session.id.value,
                    status = session.status.apiValue,
                    createdAt = session.createdAt.toString(),
                ),
            )
        }

        post("/{sessionId}/messages") {
            val request = runCatching {
                call.receiveNullable<AssistantMessageIntakeRequest>()
            }.getOrNull()
            val messageText = request?.message

            if (messageText.isNullOrBlank()) {
                call.respondValidationError(
                    field = "message",
                    message = "Message text must be present and not blank.",
                )
                return@post
            }

            val acceptedMessage = createAssistantSession.acceptUserMessage(
                AcceptAssistantMessageCommand(
                    sessionId = AssistantSessionId(checkNotNull(call.parameters["sessionId"])),
                    message = messageText,
                ),
            )

            call.respond(
                HttpStatusCode.OK,
                AssistantMessageIntakeResponse(
                    sessionId = acceptedMessage.sessionId.value,
                    status = acceptedMessage.status.apiValue,
                    receivedAt = acceptedMessage.receivedAt.toString(),
                    assistantReply = AssistantReplyResponse(
                        replyType = acceptedMessage.assistantReply.type.apiValue,
                        message = acceptedMessage.assistantReply.message,
                    ),
                ),
            )
        }

        get("/{sessionId}/shortlist") {
            call.respondNotImplementedPlaceholder("assistant.session.shortlist.read")
        }

        put("/{sessionId}/shortlist/{offerId}") {
            call.respondNotImplementedPlaceholder("assistant.session.shortlist.upsert")
        }

        delete("/{sessionId}/shortlist/{offerId}") {
            call.respondNotImplementedPlaceholder("assistant.session.shortlist.delete")
        }

        post("/{sessionId}/explanations") {
            call.respondNotImplementedPlaceholder("assistant.session.explanation")
        }
    }
}

@Serializable
data class AssistantSessionCreatedResponse(
    val sessionId: String,
    val status: String,
    val createdAt: String,
)

@Serializable
data class AssistantMessageIntakeRequest(
    val message: String? = null,
)

@Serializable
data class AssistantMessageIntakeResponse(
    val sessionId: String,
    val status: String,
    val receivedAt: String,
    val assistantReply: AssistantReplyResponse,
)

@Serializable
data class AssistantReplyResponse(
    val replyType: String,
    val message: String,
)

suspend fun io.ktor.server.application.ApplicationCall.respondValidationError(
    field: String,
    message: String,
) {
    respond(
        HttpStatusCode.BadRequest,
        ErrorResponse(
            code = ErrorCode.VALIDATION_ERROR,
            message = "Request validation failed.",
            requestId = requestIdOrNull(),
            details = mapOf(
                "field" to JsonPrimitive(field),
                "message" to JsonPrimitive(message),
            ),
        ),
    )
}
