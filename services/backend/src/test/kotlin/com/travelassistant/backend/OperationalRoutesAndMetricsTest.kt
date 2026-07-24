package com.travelassistant.backend

import com.travelassistant.backend.api.HealthResponse
import com.travelassistant.backend.api.ApiJson
import com.travelassistant.backend.application.llm.LlmClient
import com.travelassistant.backend.application.observability.OPEN_METRICS_CONTENT_TYPE
import com.travelassistant.backend.application.observability.OperationalComponent
import com.travelassistant.backend.application.observability.OperationalDependency
import com.travelassistant.backend.application.observability.OperationalError
import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventName
import com.travelassistant.backend.application.observability.OperationalHttpMethod
import com.travelassistant.backend.application.observability.OperationalOperation
import com.travelassistant.backend.application.observability.OperationalOutcome
import com.travelassistant.backend.application.observability.ServiceReadiness
import com.travelassistant.backend.infrastructure.observability.PrometheusOperationalMetrics
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OperationalRoutesAndMetricsTest {

    @Test
    fun `keeps legacy health and serves local live ready and OpenMetrics endpoints`() =
        testApplication {
            val readiness = ServiceReadiness()
            val metrics = PrometheusOperationalMetrics(readiness)
            application {
                moduleWithAssistantLlm(
                    llmClient = LlmClient { error("Health checks must not call the LLM") },
                    realHotelHttpClientFactory = {
                        error("Health checks must not create or call a real hotel client")
                    },
                    eventSink = metrics,
                    metricsExporter = metrics,
                    readiness = readiness,
                )
            }

            val legacyHealth = client.get("/api/v1/health")
            assertEquals(HttpStatusCode.OK, legacyHealth.status)
            assertEquals(
                HealthResponse(
                    status = "ok",
                    service = "travel-assistant-backend",
                    version = "0.1.0",
                ),
                ApiJson.decodeFromString(legacyHealth.bodyAsText()),
            )

            assertEquals(HttpStatusCode.OK, client.get("/health/live").status)
            assertEquals(HttpStatusCode.OK, client.get("/health/ready").status)

            val metricsResponse = client.get("/metrics")
            val scrape = metricsResponse.bodyAsText()
            assertEquals(HttpStatusCode.OK, metricsResponse.status)
            assertEquals(OPEN_METRICS_CONTENT_TYPE, metricsResponse.headers["Content-Type"])
            assertTrue(scrape.endsWith("# EOF\n"))
            assertTrue(scrape.contains("travel_assistant_readiness 1.0"))

            readiness.markNotReady()
            assertEquals(HttpStatusCode.ServiceUnavailable, client.get("/health/ready").status)
            metrics.close()
        }

    @Test
    fun `exports bounded counters timers readiness build and aggregate JVM metrics`() {
        val readiness = ServiceReadiness(initiallyReady = true)
        val metrics = PrometheusOperationalMetrics(readiness)

        metrics.record(httpStarted(OperationalOperation.CREATE_ASSISTANT_SESSION))
        metrics.record(
            httpCompleted(
                operation = OperationalOperation.CREATE_ASSISTANT_SESSION,
                statusCode = 201,
                durationMillis = 25,
            ),
        )
        metrics.record(
            OperationalEvent(
                name = OperationalEventName.ASSISTANT_TURN_COMPLETED,
                component = OperationalComponent.ASSISTANT,
                requestId = "request-id-must-not-be-a-label",
                sessionId = "session-id-must-not-be-a-label",
                outcome = OperationalOutcome.CLARIFICATION,
            ),
        )
        metrics.record(
            OperationalEvent(
                name = OperationalEventName.DEPENDENCY_CALL_COMPLETED,
                component = OperationalComponent.ACCOMMODATION_ANALYSIS,
                dependency = OperationalDependency.ACCOMMODATION_ANALYZER,
                operation = OperationalOperation.ACCOMMODATION_COARSE_ANALYSIS,
                outcome = OperationalOutcome.SUCCEEDED,
                durationMillis = 90,
            ),
        )
        metrics.record(
            OperationalEvent(
                name = OperationalEventName.HOTEL_SEARCH_COMPLETED,
                component = OperationalComponent.HOTEL_SEARCH,
                sessionId = "session-id-must-not-be-a-label",
                hotelSearchId = "search-id-must-not-be-a-label",
                operation = OperationalOperation.CREATE_HOTEL_SEARCH,
                outcome = OperationalOutcome.NO_OFFERS,
                durationMillis = 40,
            ),
        )
        metrics.record(
            OperationalEvent(
                name = OperationalEventName.HOTEL_DETAILS_COMPLETED,
                component = OperationalComponent.HOTEL_DETAILS,
                hotelSearchId = "search-id-must-not-be-a-label",
                operation = OperationalOperation.GET_HOTEL_DETAILS,
                outcome = OperationalOutcome.UNAVAILABLE,
                durationMillis = 50,
            ),
        )
        metrics.record(
            OperationalEvent(
                name = OperationalEventName.DEPENDENCY_CALL_COMPLETED,
                component = OperationalComponent.PROVIDER,
                dependency = OperationalDependency.HOTEL_PROVIDER,
                operation = OperationalOperation.PROVIDER_HOTEL_SEARCH,
                outcome = OperationalOutcome.TIMEOUT,
                durationMillis = 75,
            ),
        )
        metrics.record(
            OperationalEvent(
                name = OperationalEventName.UNEXPECTED_ERROR,
                component = OperationalComponent.HTTP,
                error = OperationalError.from(IllegalStateException("raw-message-must-not-leak")),
            ),
        )

        metrics.record(httpStarted(OperationalOperation.METRICS))
        metrics.record(httpCompleted(OperationalOperation.METRICS, 200, 1))
        metrics.record(httpStarted(OperationalOperation.LIVENESS))
        metrics.record(httpCompleted(OperationalOperation.LIVENESS, 200, 1))
        metrics.record(httpCompleted(OperationalOperation.READINESS, 503, 1))

        val scrape = metrics.scrapeOpenMetrics()

        assertTrue(scrape.endsWith("# EOF\n"))
        assertTrue(scrape.contains("travel_assistant_http_requests_total"))
        assertTrue(scrape.contains("travel_assistant_http_request_duration_seconds_count"))
        assertTrue(scrape.contains("travel_assistant_http_active_requests 0.0"))
        assertTrue(scrape.contains("travel_assistant_assistant_turns_total"))
        assertTrue(scrape.contains("travel_assistant_hotel_searches_total"))
        assertTrue(scrape.contains("travel_assistant_hotel_search_duration_seconds_count"))
        assertTrue(scrape.contains("travel_assistant_hotel_details_total"))
        assertTrue(scrape.contains("travel_assistant_dependency_calls_total"))
        assertTrue(scrape.contains("dependency=\"accommodation_analyzer\""))
        assertTrue(scrape.contains("operation=\"accommodation_coarse_analysis\""))
        assertTrue(scrape.contains("travel_assistant_unexpected_errors_total 1.0"))
        assertTrue(scrape.contains("travel_assistant_readiness 1.0"))
        assertTrue(scrape.contains("travel_assistant_build_info 1.0"))
        assertTrue(scrape.contains("jvm_memory_used_bytes"))
        assertTrue(scrape.contains("jvm_gc_collections"))
        assertTrue(scrape.contains("jvm_threads_live"))
        assertTrue(scrape.contains("process_uptime_seconds"))

        assertFalse(scrape.contains("operation=\"metrics\""))
        assertFalse(scrape.contains("operation=\"liveness\""))
        assertTrue(scrape.contains("operation=\"readiness\",status_class=\"5xx\""))
        listOf(
            "request-id-must-not-be-a-label",
            "session-id-must-not-be-a-label",
            "search-id-must-not-be-a-label",
            "raw-message-must-not-leak",
        ).forEach { forbiddenValue ->
            assertFalse(scrape.contains(forbiddenValue))
        }
        assertApplicationMetricLabelsAreBounded(scrape)
        metrics.close()
    }

    private fun httpStarted(operation: OperationalOperation): OperationalEvent =
        OperationalEvent(
            name = OperationalEventName.HTTP_REQUEST_STARTED,
            component = OperationalComponent.HTTP,
            operation = operation,
            method = OperationalHttpMethod.GET,
        )

    private fun httpCompleted(
        operation: OperationalOperation,
        statusCode: Int,
        durationMillis: Long,
    ): OperationalEvent =
        OperationalEvent(
            name = OperationalEventName.HTTP_REQUEST_COMPLETED,
            component = OperationalComponent.HTTP,
            requestId = "request-id-must-not-be-a-label",
            operation = operation,
            method = OperationalHttpMethod.GET,
            statusCode = statusCode,
            outcome = OperationalOutcome.SUCCEEDED,
            durationMillis = durationMillis,
        )

    private fun assertApplicationMetricLabelsAreBounded(scrape: String) {
        val allowedLabels = setOf(
            "operation",
            "method",
            "status_class",
            "dependency",
            "outcome",
        )
        scrape.lineSequence()
            .filter { line -> line.startsWith("travel_assistant_") && '{' in line }
            .forEach { line ->
                val labels = line.substringAfter('{').substringBefore('}')
                    .split(',')
                    .filter(String::isNotBlank)
                    .map { label -> label.substringBefore('=') }
                    .toSet()
                assertTrue(labels.all(allowedLabels::contains), "Unexpected labels in: $line")
            }
    }
}
