package com.travelassistant.backend.infrastructure.observability

import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventSink
import java.time.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class JsonOperationalEventSink(
    private val serviceName: String = DEFAULT_SERVICE_NAME,
    private val serviceVersion: String = DEFAULT_SERVICE_VERSION,
    private val clock: Clock = Clock.systemUTC(),
    private val writeLine: (String) -> Unit = System.out::println,
) : OperationalEventSink {
    private val writerLock = Any()

    override fun record(event: OperationalEvent) {
        runCatching {
            val line = Json.encodeToString(event.toJsonObject())
            synchronized(writerLock) {
                writeLine(line)
            }
        }
    }

    private fun OperationalEvent.toJsonObject(): JsonObject =
        JsonObject(
            buildMap {
                put("schema_version", JsonPrimitive(SCHEMA_VERSION))
                put("timestamp", JsonPrimitive(clock.instant().toString()))
                put("level", JsonPrimitive(level.wireValue))
                put("service", JsonPrimitive(serviceName))
                put("version", JsonPrimitive(serviceVersion))
                put("event", JsonPrimitive(name.wireValue))
                put("component", JsonPrimitive(component.wireValue))
                requestId?.let { put("request_id", JsonPrimitive(it)) }
                sessionId?.let { put("session_id", JsonPrimitive(it)) }
                hotelSearchId?.let { put("hotel_search_id", JsonPrimitive(it)) }
                operation?.let { put("operation", JsonPrimitive(it.wireValue)) }
                method?.let { put("method", JsonPrimitive(it.wireValue)) }
                statusCode?.let { put("status_code", JsonPrimitive(it)) }
                dependency?.let { put("dependency", JsonPrimitive(it.wireValue)) }
                outcome?.let { put("outcome", JsonPrimitive(it.wireValue)) }
                diagnostic?.let { put("diagnostic", JsonPrimitive(it.wireValue)) }
                durationMillis?.let { put("duration_ms", JsonPrimitive(it)) }
                offerCount?.let { put("offer_count", JsonPrimitive(it)) }
                error?.let { operationalError ->
                    put("exception_class", JsonPrimitive(operationalError.exceptionType))
                    put(
                        "cause_classes",
                        JsonArray(operationalError.causeTypes.map(::JsonPrimitive)),
                    )
                    put(
                        "stack_frames",
                        JsonArray(operationalError.stackFrames.map(::JsonPrimitive)),
                    )
                }
            },
        )

    private companion object {
        const val SCHEMA_VERSION = "1"
        const val DEFAULT_SERVICE_NAME = "travel-assistant-backend"
        const val DEFAULT_SERVICE_VERSION = "0.1.0"
    }
}
