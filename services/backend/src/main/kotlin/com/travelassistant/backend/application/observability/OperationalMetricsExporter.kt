package com.travelassistant.backend.application.observability

interface OperationalMetricsExporter {
    val contentType: String

    fun scrapeOpenMetrics(): String

    companion object {
        val NONE = object : OperationalMetricsExporter {
            override val contentType: String = OPEN_METRICS_CONTENT_TYPE

            override fun scrapeOpenMetrics(): String = "# EOF\n"
        }
    }
}

const val OPEN_METRICS_CONTENT_TYPE =
    "application/openmetrics-text; version=1.0.0; charset=utf-8"
