package com.travelassistant.backend.api

import com.travelassistant.backend.application.assistant.AcceptAssistantMessageCommand
import com.travelassistant.backend.application.assistant.AcceptedAssistantMessage
import com.travelassistant.backend.application.assistant.AssistantSessionBoundary
import com.travelassistant.backend.application.assistant.CreateAssistantSessionUseCase
import com.travelassistant.backend.domain.assistant.AssistantSession
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

fun Route.assistantPlaceholderRoutes() {
    val createAssistantSession: AssistantSessionBoundary = CreateAssistantSessionUseCase()

    route("/assistant/sessions") {
        post {
            val request = runCatching {
                call.receiveNullable<AssistantMessageRequest>()
            }.getOrNull()
            val initialMessageText = request?.message

            if (request != null && initialMessageText.isNullOrBlank()) {
                call.respondValidationError(
                    field = "message",
                    message = "Message text must be present and not blank.",
                )
                return@post
            }

            val session = createAssistantSession.createSession()
            val response = if (initialMessageText != null) {
                val acceptedMessage = createAssistantSession.acceptUserMessage(
                    AcceptAssistantMessageCommand(
                        sessionId = session.id,
                        message = initialMessageText,
                    ),
                )
                AssistantMessageResponse.fromAcceptedMessage(acceptedMessage)
            } else {
                AssistantMessageResponse.fromSession(session)
            }

            call.respond(
                HttpStatusCode.Created,
                response,
            )
        }

        post("/{sessionId}/messages") {
            val request = runCatching {
                call.receiveNullable<AssistantMessageRequest>()
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
                AssistantMessageResponse.fromAcceptedMessage(acceptedMessage),
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
data class AssistantMessageRequest(
    val message: String? = null,
    val clientContext: AssistantClientContext? = null,
)

@Serializable
data class AssistantClientContext(
    val locale: String? = null,
    val timezone: String? = null,
)

@Serializable
data class AssistantMessageResponse(
    val session: AssistantSessionResponse,
    val assistantMessage: AssistantMessageBodyResponse,
    val nextAction: String = "ask_clarification",
) {
    companion object {
        private const val PLACEHOLDER_ASSISTANT_MESSAGE =
            "I received your hotel request. Please share destination, dates, guests, and budget so I can continue."

        fun fromSession(session: AssistantSession): AssistantMessageResponse =
            AssistantMessageResponse(
                session = AssistantSessionResponse(
                    sessionId = session.id.value,
                    status = session.status.apiValue,
                    createdAt = session.createdAt.toString(),
                    updatedAt = session.createdAt.toString(),
                ),
                assistantMessage = AssistantMessageBodyResponse(
                    role = "assistant",
                    content = PLACEHOLDER_ASSISTANT_MESSAGE,
                ),
            )

        fun fromAcceptedMessage(
            acceptedMessage: AcceptedAssistantMessage,
        ): AssistantMessageResponse =
            AssistantMessageResponse(
                session = AssistantSessionResponse(
                    sessionId = acceptedMessage.sessionId.value,
                    status = acceptedMessage.status.apiValue,
                    createdAt = acceptedMessage.clarificationState.createdAt.toString(),
                    updatedAt = acceptedMessage.receivedAt.toString(),
                ),
                assistantMessage = AssistantMessageBodyResponse(
                    role = "assistant",
                    content = acceptedMessage.assistantReply.message,
                ),
            )
    }
}

@Serializable
data class AssistantSessionResponse(
    val sessionId: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class AssistantMessageBodyResponse(
    val role: String,
    val content: String,
)

suspend fun io.ktor.server.application.ApplicationCall.respondValidationError(
    field: String,
    message: String,
) {
    respond(
        HttpStatusCode.BadRequest,
        ValidationErrorResponse(
            code = ErrorCode.VALIDATION_ERROR,
            message = "Request validation failed.",
            requestId = requestIdOrNull(),
            fields = listOf(
                ValidationErrorField(
                    field = field,
                    message = message,
                ),
            ),
        ),
    )
}
