package com.travelassistant.backend.infrastructure.observability

import com.travelassistant.backend.application.observability.OPEN_METRICS_CONTENT_TYPE
import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventName
import com.travelassistant.backend.application.observability.OperationalEventSink
import com.travelassistant.backend.application.observability.OperationalMetricsExporter
import com.travelassistant.backend.application.observability.OperationalOperation
import com.travelassistant.backend.application.observability.ServiceReadiness
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.exporter.common.TextFormat
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean
import java.lang.management.ThreadMXBean
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

class PrometheusOperationalMetrics(
    private val readiness: ServiceReadiness,
) : OperationalEventSink,
    OperationalMetricsExporter,
    AutoCloseable {
    private val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    private val activeHttpRequests = AtomicInteger(0)
    private val memoryBean: MemoryMXBean = ManagementFactory.getMemoryMXBean()
    private val threadBean: ThreadMXBean = ManagementFactory.getThreadMXBean()
    private val garbageCollectors: List<GarbageCollectorMXBean> =
        ManagementFactory.getGarbageCollectorMXBeans()
    private val runtimeBean = ManagementFactory.getRuntimeMXBean()
    private val operatingSystemBean = ManagementFactory.getOperatingSystemMXBean()

    init {
        registerServiceMetrics()
        registerJvmAndProcessMetrics()
    }

    override val contentType: String = OPEN_METRICS_CONTENT_TYPE

    override fun record(event: OperationalEvent) {
        runCatching {
            when (event.name) {
                OperationalEventName.HTTP_REQUEST_STARTED -> recordHttpStarted(event)
                OperationalEventName.HTTP_REQUEST_COMPLETED -> recordHttpCompleted(event)
                OperationalEventName.ASSISTANT_TURN_COMPLETED ->
                    incrementOutcomeCounter(ASSISTANT_TURNS, event)
                OperationalEventName.HOTEL_SEARCH_COMPLETED -> recordTimedOutcome(
                    counterName = HOTEL_SEARCHES,
                    timerName = HOTEL_SEARCH_DURATION,
                    event = event,
                )
                OperationalEventName.HOTEL_DETAILS_COMPLETED -> recordTimedOutcome(
                    counterName = HOTEL_DETAILS,
                    timerName = HOTEL_DETAILS_DURATION,
                    event = event,
                )
                OperationalEventName.DEPENDENCY_CALL_COMPLETED -> recordDependencyCall(event)
                OperationalEventName.UNEXPECTED_ERROR ->
                    Counter.builder(UNEXPECTED_ERRORS).register(registry).increment()
                OperationalEventName.HOTEL_SEARCH_STARTED,
                OperationalEventName.CONFIRMATION_OUTCOME,
                OperationalEventName.LLM_DIAGNOSTIC,
                OperationalEventName.ASSISTANT_SESSION_CREATED,
                OperationalEventName.SERVICE_LIFECYCLE,
                -> Unit
            }
        }
    }

    override fun scrapeOpenMetrics(): String =
        registry.scrape(TextFormat.CONTENT_TYPE_OPENMETRICS_100)

    override fun close() {
        registry.close()
    }

    private fun recordHttpStarted(event: OperationalEvent) {
        if (!event.isExcludedOperationalRequest()) {
            activeHttpRequests.incrementAndGet()
        }
    }

    private fun recordHttpCompleted(event: OperationalEvent) {
        if (!event.isExcludedOperationalRequest()) {
            activeHttpRequests.updateAndGet { current -> max(0, current - 1) }
        }
        if (event.shouldExcludeFromHttpStatistics()) {
            return
        }

        val tags = arrayOf(
            OPERATION_LABEL,
            event.operation?.wireValue ?: OperationalOperation.UNMATCHED.wireValue,
            METHOD_LABEL,
            event.method?.wireValue ?: "OTHER",
            STATUS_CLASS_LABEL,
            statusClass(event.statusCode),
        )
        Counter.builder(HTTP_REQUESTS)
            .tags(*tags)
            .register(registry)
            .increment()
        Timer.builder(HTTP_REQUEST_DURATION)
            .tags(*tags)
            .register(registry)
            .record(event.durationMillis ?: 0, TimeUnit.MILLISECONDS)
    }

    private fun incrementOutcomeCounter(
        name: String,
        event: OperationalEvent,
    ) {
        Counter.builder(name)
            .tag(OUTCOME_LABEL, event.outcome?.wireValue ?: "unknown")
            .register(registry)
            .increment()
    }

    private fun recordTimedOutcome(
        counterName: String,
        timerName: String,
        event: OperationalEvent,
    ) {
        val tags = arrayOf(
            OPERATION_LABEL,
            event.operation?.wireValue ?: OperationalOperation.UNMATCHED.wireValue,
            OUTCOME_LABEL,
            event.outcome?.wireValue ?: "unknown",
        )
        Counter.builder(counterName).tags(*tags).register(registry).increment()
        Timer.builder(timerName)
            .tags(*tags)
            .register(registry)
            .record(event.durationMillis ?: 0, TimeUnit.MILLISECONDS)
    }

    private fun recordDependencyCall(event: OperationalEvent) {
        val tags = arrayOf(
            DEPENDENCY_LABEL,
            event.dependency?.wireValue ?: "unknown",
            OPERATION_LABEL,
            event.operation?.wireValue ?: OperationalOperation.UNMATCHED.wireValue,
            OUTCOME_LABEL,
            event.outcome?.wireValue ?: "unknown",
        )
        Counter.builder(DEPENDENCY_CALLS).tags(*tags).register(registry).increment()
        Timer.builder(DEPENDENCY_CALL_DURATION)
            .tags(*tags)
            .register(registry)
            .record(event.durationMillis ?: 0, TimeUnit.MILLISECONDS)
    }

    private fun registerServiceMetrics() {
        Gauge.builder(HTTP_ACTIVE_REQUESTS, activeHttpRequests) { value ->
            value.get().toDouble()
        }
            .register(registry)
        Gauge.builder(READINESS) { if (readiness.isReady()) 1.0 else 0.0 }
            .register(registry)
        Gauge.builder(BUILD_INFO) { 1.0 }
            .description("Travel Assistant backend build 0.1.0")
            .register(registry)
    }

    private fun registerJvmAndProcessMetrics() {
        Gauge.builder(JVM_MEMORY_USED_BYTES) {
            memoryBean.heapMemoryUsage.used.toDouble() +
                memoryBean.nonHeapMemoryUsage.used.toDouble()
        }.register(registry)
        Gauge.builder(JVM_HEAP_USED_BYTES) { memoryBean.heapMemoryUsage.used.toDouble() }
            .register(registry)
        Gauge.builder(JVM_HEAP_MAX_BYTES) { memoryBean.heapMemoryUsage.max.toDouble() }
            .register(registry)
        Gauge.builder(JVM_NON_HEAP_USED_BYTES) { memoryBean.nonHeapMemoryUsage.used.toDouble() }
            .register(registry)
        Gauge.builder(JVM_GC_COLLECTIONS) {
            garbageCollectors.sumOf { collector -> max(0, collector.collectionCount) }.toDouble()
        }.register(registry)
        Gauge.builder(JVM_GC_TIME_SECONDS) {
            garbageCollectors.sumOf { collector -> max(0, collector.collectionTime) } / 1_000.0
        }.register(registry)
        Gauge.builder(JVM_THREADS_LIVE) { threadBean.threadCount.toDouble() }.register(registry)
        Gauge.builder(JVM_THREADS_DAEMON) { threadBean.daemonThreadCount.toDouble() }
            .register(registry)
        Gauge.builder(JVM_THREADS_PEAK) { threadBean.peakThreadCount.toDouble() }.register(registry)
        Gauge.builder(PROCESS_UPTIME_SECONDS) { runtimeBean.uptime / 1_000.0 }.register(registry)
        Gauge.builder(PROCESS_CPU_COUNT) { operatingSystemBean.availableProcessors.toDouble() }
            .register(registry)
        Gauge.builder(PROCESS_SYSTEM_LOAD_AVERAGE) { operatingSystemBean.systemLoadAverage }
            .register(registry)
    }

    private fun OperationalEvent.isExcludedOperationalRequest(): Boolean =
        operation in setOf(
            OperationalOperation.LEGACY_HEALTH,
            OperationalOperation.LIVENESS,
            OperationalOperation.READINESS,
            OperationalOperation.METRICS,
        )

    private fun OperationalEvent.shouldExcludeFromHttpStatistics(): Boolean =
        operation == OperationalOperation.METRICS ||
            (operation in HEALTH_OPERATIONS && statusCode in 200..299)

    private fun statusClass(statusCode: Int?): String =
        when (statusCode) {
            in 100..199 -> "1xx"
            in 200..299 -> "2xx"
            in 300..399 -> "3xx"
            in 400..499 -> "4xx"
            in 500..599 -> "5xx"
            else -> "other"
        }

    private companion object {
        const val OPERATION_LABEL = "operation"
        const val METHOD_LABEL = "method"
        const val STATUS_CLASS_LABEL = "status_class"
        const val DEPENDENCY_LABEL = "dependency"
        const val OUTCOME_LABEL = "outcome"

        const val HTTP_REQUESTS = "travel_assistant.http.requests"
        const val HTTP_ACTIVE_REQUESTS = "travel_assistant.http.active_requests"
        const val HTTP_REQUEST_DURATION = "travel_assistant.http.request.duration"
        const val ASSISTANT_TURNS = "travel_assistant.assistant.turns"
        const val HOTEL_SEARCHES = "travel_assistant.hotel.searches"
        const val HOTEL_SEARCH_DURATION = "travel_assistant.hotel.search.duration"
        const val HOTEL_DETAILS = "travel_assistant.hotel.details"
        const val HOTEL_DETAILS_DURATION = "travel_assistant.hotel.details.duration"
        const val DEPENDENCY_CALLS = "travel_assistant.dependency.calls"
        const val DEPENDENCY_CALL_DURATION = "travel_assistant.dependency.call.duration"
        const val UNEXPECTED_ERRORS = "travel_assistant.unexpected.errors"
        const val READINESS = "travel_assistant.readiness"
        const val BUILD_INFO = "travel_assistant.build.info"

        const val JVM_MEMORY_USED_BYTES = "jvm.memory.used.bytes"
        const val JVM_HEAP_USED_BYTES = "jvm.memory.heap.used.bytes"
        const val JVM_HEAP_MAX_BYTES = "jvm.memory.heap.max.bytes"
        const val JVM_NON_HEAP_USED_BYTES = "jvm.memory.non_heap.used.bytes"
        const val JVM_GC_COLLECTIONS = "jvm.gc.collections"
        const val JVM_GC_TIME_SECONDS = "jvm.gc.time.seconds"
        const val JVM_THREADS_LIVE = "jvm.threads.live"
        const val JVM_THREADS_DAEMON = "jvm.threads.daemon"
        const val JVM_THREADS_PEAK = "jvm.threads.peak"
        const val PROCESS_UPTIME_SECONDS = "process.uptime.seconds"
        const val PROCESS_CPU_COUNT = "process.cpu.count"
        const val PROCESS_SYSTEM_LOAD_AVERAGE = "process.system.load.average"

        val HEALTH_OPERATIONS = setOf(
            OperationalOperation.LEGACY_HEALTH,
            OperationalOperation.LIVENESS,
            OperationalOperation.READINESS,
        )
    }
}
