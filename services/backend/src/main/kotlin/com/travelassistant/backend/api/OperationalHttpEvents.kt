package com.travelassistant.backend.api

import com.travelassistant.backend.application.observability.OperationalComponent
import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventName
import com.travelassistant.backend.application.observability.OperationalEventSink
import com.travelassistant.backend.application.observability.OperationalHttpMethod
import com.travelassistant.backend.application.observability.OperationalLevel
import com.travelassistant.backend.application.observability.OperationalOperation
import com.travelassistant.backend.application.observability.OperationalOutcome
import com.travelassistant.backend.application.observability.recordSafely
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import kotlin.math.max

fun Application.configureOperationalHttpEvents(
    eventSink: OperationalEventSink,
) {
    intercept(ApplicationCallPipeline.Monitoring) {
        val startedAt = System.nanoTime()
        val requestId = call.requestId()
        try {
            proceed()
        } finally {
            val statusCode = call.response.status()?.value
                ?: HttpStatusCode.InternalServerError.value
            eventSink.recordSafely(
                OperationalEvent(
                    name = OperationalEventName.HTTP_REQUEST_COMPLETED,
                    component = OperationalComponent.HTTP,
                    level = if (statusCode >= 500) {
                        OperationalLevel.ERROR
                    } else {
                        OperationalLevel.INFO
                    },
                    requestId = requestId,
                    operation = resolveHttpOperation(
                        method = call.request.httpMethod.value,
                        path = call.request.path(),
                    ),
                    method = call.request.httpMethod.value.toOperationalMethod(),
                    statusCode = statusCode,
                    outcome = statusCode.toOperationalOutcome(),
                    durationMillis = elapsedMillis(startedAt),
                ),
            )
        }
    }
}

internal fun resolveHttpOperation(
    method: String,
    path: String,
): OperationalOperation =
    when {
        method == "GET" && path == "/api/v1/health" -> OperationalOperation.LEGACY_HEALTH
        method == "GET" && path == "/health/live" -> OperationalOperation.LIVENESS
        method == "GET" && path == "/health/ready" -> OperationalOperation.READINESS
        method == "GET" && path == "/metrics" -> OperationalOperation.METRICS
        method == "POST" && path == "/api/v1/assistant/sessions" ->
            OperationalOperation.CREATE_ASSISTANT_SESSION
        method == "POST" && ASSISTANT_MESSAGE_PATH.matches(path) ->
            OperationalOperation.POST_ASSISTANT_MESSAGE
        method == "GET" && ASSISTANT_SHORTLIST_PATH.matches(path) ->
            OperationalOperation.READ_SHORTLIST
        method == "PUT" && ASSISTANT_SHORTLIST_OFFER_PATH.matches(path) ->
            OperationalOperation.UPSERT_SHORTLIST
        method == "DELETE" && ASSISTANT_SHORTLIST_OFFER_PATH.matches(path) ->
            OperationalOperation.DELETE_SHORTLIST
        method == "POST" && ASSISTANT_EXPLANATION_PATH.matches(path) ->
            OperationalOperation.CREATE_EXPLANATION
        method == "POST" && path == "/api/v1/hotel-searches" ->
            OperationalOperation.CREATE_HOTEL_SEARCH
        method == "GET" && HOTEL_OFFERS_PATH.matches(path) ->
            OperationalOperation.GET_HOTEL_OFFERS
        method == "GET" && HOTEL_DETAILS_PATH.matches(path) ->
            OperationalOperation.GET_HOTEL_DETAILS
        else -> OperationalOperation.UNMATCHED
    }

private fun String.toOperationalMethod(): OperationalHttpMethod =
    when (this) {
        "GET" -> OperationalHttpMethod.GET
        "POST" -> OperationalHttpMethod.POST
        "PUT" -> OperationalHttpMethod.PUT
        "DELETE" -> OperationalHttpMethod.DELETE
        else -> OperationalHttpMethod.OTHER
    }

private fun Int.toOperationalOutcome(): OperationalOutcome =
    when {
        this in 200..399 -> OperationalOutcome.SUCCEEDED
        this == 400 -> OperationalOutcome.VALIDATION_ERROR
        this == 404 -> OperationalOutcome.NOT_FOUND
        this == 502 -> OperationalOutcome.RESPONSE_REJECTED
        this == 503 -> OperationalOutcome.UNAVAILABLE
        else -> OperationalOutcome.FAILED
    }

internal fun elapsedMillis(startedAtNanos: Long): Long =
    max(0, (System.nanoTime() - startedAtNanos) / NANOS_PER_MILLISECOND)

private const val OPAQUE_SEGMENT = "[^/]+"
private val ASSISTANT_MESSAGE_PATH =
    Regex("/api/v1/assistant/sessions/$OPAQUE_SEGMENT/messages")
private val ASSISTANT_SHORTLIST_PATH =
    Regex("/api/v1/assistant/sessions/$OPAQUE_SEGMENT/shortlist")
private val ASSISTANT_SHORTLIST_OFFER_PATH =
    Regex("/api/v1/assistant/sessions/$OPAQUE_SEGMENT/shortlist/$OPAQUE_SEGMENT")
private val ASSISTANT_EXPLANATION_PATH =
    Regex("/api/v1/assistant/sessions/$OPAQUE_SEGMENT/explanations")
private val HOTEL_OFFERS_PATH =
    Regex("/api/v1/hotel-searches/$OPAQUE_SEGMENT/offers")
private val HOTEL_DETAILS_PATH =
    Regex("/api/v1/hotel-searches/$OPAQUE_SEGMENT/offers/$OPAQUE_SEGMENT/details")
private const val NANOS_PER_MILLISECOND = 1_000_000L
