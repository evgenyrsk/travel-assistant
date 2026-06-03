package com.travelassistant.backend.api

import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.assistantPlaceholderRoutes() {
    route("/assistant/sessions") {
        post {
            call.respondNotImplementedPlaceholder("assistant.session.create")
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
