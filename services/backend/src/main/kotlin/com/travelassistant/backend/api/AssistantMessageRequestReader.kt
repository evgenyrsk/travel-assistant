package com.travelassistant.backend.api

import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.contentType
import java.time.ZoneId
import kotlinx.serialization.decodeFromString

internal const val ASSISTANT_MESSAGE_MAX_CODE_POINTS = 4_000

internal sealed interface AssistantMessageRequestReadResult {
    data object NoBody : AssistantMessageRequestReadResult

    data class Accepted(
        val message: String,
        val clientTimeZone: ZoneId?,
    ) : AssistantMessageRequestReadResult

    data class Invalid(
        val field: String,
        val message: String,
    ) : AssistantMessageRequestReadResult
}

internal suspend fun ApplicationCall.readAssistantMessageRequest(): AssistantMessageRequestReadResult {
    val body = when (val readResult = readBoundedRequestBody()) {
        BoundedRequestBodyReadResult.NoBody ->
            return AssistantMessageRequestReadResult.NoBody

        BoundedRequestBodyReadResult.TooLarge ->
            return AssistantMessageRequestReadResult.Invalid(
                field = "body",
                message = "Assistant request body is too large.",
            )

        is BoundedRequestBodyReadResult.Accepted -> readResult.text
    }
    if (body.isBlank() || request.contentType().withoutParameters() != ContentType.Application.Json) {
        return invalidBody()
    }

    val request = runCatching {
        ApiJson.decodeFromString<AssistantMessageRequest>(body)
    }.getOrElse {
        return invalidBody()
    }
    val message = request.message
    if (message.isNullOrBlank()) {
        return AssistantMessageRequestReadResult.Invalid(
            field = "message",
            message = "Message text must be present and not blank.",
        )
    }
    if (message.codePointCount(0, message.length) > ASSISTANT_MESSAGE_MAX_CODE_POINTS) {
        return AssistantMessageRequestReadResult.Invalid(
            field = "message",
            message = "Message text must not exceed 4000 Unicode characters.",
        )
    }

    return AssistantMessageRequestReadResult.Accepted(
        message = message,
        clientTimeZone = request.clientContext?.timezone.toZoneIdOrNull(),
    )
}

private fun String?.toZoneIdOrNull(): ZoneId? =
    this
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.let { value -> runCatching { ZoneId.of(value) }.getOrNull() }

private fun invalidBody(): AssistantMessageRequestReadResult.Invalid =
    AssistantMessageRequestReadResult.Invalid(
        field = "body",
        message = "Assistant request body must be valid JSON.",
    )
