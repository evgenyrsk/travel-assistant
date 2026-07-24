package com.travelassistant.backend.api

import com.travelassistant.backend.application.assistant.AcceptAssistantMessageCommand
import com.travelassistant.backend.application.assistant.AcceptedAssistantMessage
import com.travelassistant.backend.application.assistant.AssistantResponseSemantics
import com.travelassistant.backend.application.assistant.AssistantSessionBoundary
import com.travelassistant.backend.domain.assistant.AssistantSession
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.application.observability.OperationalComponent
import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventName
import com.travelassistant.backend.application.observability.OperationalEventSink
import com.travelassistant.backend.application.observability.OperationalOperation
import com.travelassistant.backend.application.observability.OperationalOutcome
import com.travelassistant.backend.application.observability.recordSafely
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

fun Route.assistantPlaceholderRoutes(
    createAssistantSession: AssistantSessionBoundary,
    eventSink: OperationalEventSink = OperationalEventSink.NONE,
) {
    route("/assistant/sessions") {
        post {
            val initialMessage = when (val request = call.readAssistantMessageRequest()) {
                AssistantMessageRequestReadResult.NoBody -> null
                is AssistantMessageRequestReadResult.Accepted -> request
                is AssistantMessageRequestReadResult.Invalid -> {
                    call.respondValidationError(
                        field = request.field,
                        message = request.message,
                    )
                    return@post
                }
            }

            val session = createAssistantSession.createSession()
            eventSink.recordSafely(
                OperationalEvent(
                    name = OperationalEventName.ASSISTANT_SESSION_CREATED,
                    component = OperationalComponent.ASSISTANT,
                    requestId = call.requestId(),
                    sessionId = session.id.value,
                    operation = OperationalOperation.CREATE_ASSISTANT_SESSION,
                    outcome = OperationalOutcome.CREATED,
                ),
            )
            val response = if (initialMessage != null) {
                val acceptedMessage = createAssistantSession.acceptUserMessage(
                    AcceptAssistantMessageCommand(
                        sessionId = session.id,
                        message = initialMessage.message,
                        clientTimeZone = initialMessage.clientTimeZone,
                    ),
                )
                recordAssistantTurn(call.requestId(), acceptedMessage, eventSink)
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
            val messageRequest = when (val request = call.readAssistantMessageRequest()) {
                AssistantMessageRequestReadResult.NoBody -> {
                    call.respondValidationError(
                        field = "message",
                        message = "Message text must be present and not blank.",
                    )
                    return@post
                }

                is AssistantMessageRequestReadResult.Accepted -> request
                is AssistantMessageRequestReadResult.Invalid -> {
                    call.respondValidationError(
                        field = request.field,
                        message = request.message,
                    )
                    return@post
                }
            }

            val acceptedMessage = createAssistantSession.acceptUserMessage(
                AcceptAssistantMessageCommand(
                    sessionId = AssistantSessionId(checkNotNull(call.parameters["sessionId"])),
                    message = messageRequest.message,
                    clientTimeZone = messageRequest.clientTimeZone,
                ),
            )
            recordAssistantTurn(call.requestId(), acceptedMessage, eventSink)

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

private fun recordAssistantTurn(
    requestId: String,
    acceptedMessage: AcceptedAssistantMessage,
    eventSink: OperationalEventSink,
) {
    eventSink.recordSafely(
        OperationalEvent(
            name = OperationalEventName.ASSISTANT_TURN_COMPLETED,
            component = OperationalComponent.ASSISTANT,
            requestId = requestId,
            sessionId = acceptedMessage.sessionId.value,
            hotelSearchId = acceptedMessage.hotelSearchId?.value,
            operation = OperationalOperation.POST_ASSISTANT_MESSAGE,
            outcome = when (acceptedMessage.nextAction) {
                com.travelassistant.backend.application.assistant.AssistantNextAction.ASK_CLARIFICATION ->
                    OperationalOutcome.CLARIFICATION
                com.travelassistant.backend.application.assistant.AssistantNextAction.SHOW_HOTEL_RESULTS ->
                    OperationalOutcome.RESULTS
                com.travelassistant.backend.application.assistant.AssistantNextAction.SHOW_BOUNDARY_MESSAGE ->
                    OperationalOutcome.UNSUPPORTED
            },
        ),
    )
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

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AssistantMessageResponse(
    val session: AssistantSessionResponse,
    val assistantMessage: AssistantMessageBodyResponse,
    val nextAction: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val hotelSearchId: String? = null,
) {
    companion object {
        private const val PLACEHOLDER_ASSISTANT_MESSAGE =
            "Расскажите, куда и когда планируете поездку и кто едет с вами."

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
                nextAction = AssistantResponseSemantics.nextActionFor(
                    session.hotelRequirementsCoveragePlan,
                ).apiValue,
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
                nextAction = acceptedMessage.nextAction.apiValue,
                hotelSearchId = acceptedMessage.hotelSearchId?.value,
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
            requestId = requestId(),
            fields = listOf(
                ValidationErrorField(
                    field = field,
                    message = message,
                ),
            ),
        ),
    )
}
