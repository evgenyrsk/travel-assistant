package com.travelassistant.backend.api

import com.travelassistant.backend.application.assistant.AssistantSessionBoundary
import com.travelassistant.backend.application.assistant.CreateAssistantSessionUseCase
import io.ktor.server.application.call
import io.ktor.http.HttpStatusCode
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
            call.respondNotImplementedPlaceholder("assistant.session.message")
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
