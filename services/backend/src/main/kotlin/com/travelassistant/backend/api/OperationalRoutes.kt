package com.travelassistant.backend.api

import com.travelassistant.backend.application.observability.OperationalMetricsExporter
import com.travelassistant.backend.application.observability.ServiceReadiness
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureOperationalRoutes(
    readiness: ServiceReadiness,
    metricsExporter: OperationalMetricsExporter,
) {
    routing {
        get("/health/live") {
            call.respond(
                HttpStatusCode.OK,
                HealthResponse(
                    status = "ok",
                    service = SERVICE_NAME,
                    version = SERVICE_VERSION,
                ),
            )
        }
        get("/health/ready") {
            val isReady = readiness.isReady()
            call.respond(
                if (isReady) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable,
                HealthResponse(
                    status = if (isReady) "ok" else "not_ready",
                    service = SERVICE_NAME,
                    version = SERVICE_VERSION,
                ),
            )
        }
        get("/metrics") {
            call.respondText(
                text = metricsExporter.scrapeOpenMetrics(),
                contentType = ContentType.parse(metricsExporter.contentType),
            )
        }
    }
}

private const val SERVICE_NAME = "travel-assistant-backend"
private const val SERVICE_VERSION = "0.1.0"
