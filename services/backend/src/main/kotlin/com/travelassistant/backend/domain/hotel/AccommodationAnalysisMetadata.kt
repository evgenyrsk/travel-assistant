package com.travelassistant.backend.domain.hotel

data class AccommodationAnalysisMetadata(
    val status: Status,
    val analyzedCount: Int,
    val deepAnalyzedCount: Int,
    val matchCount: Int,
    val probableCount: Int,
    val pollAfterMillis: Long? = null,
) {
    enum class Status(
        val apiValue: String,
    ) {
        SEARCHING("searching"),
        COMPLETED("completed"),
        PARTIAL("partial"),
        FAILED("failed"),
    }

    companion object {
        fun searching(pollAfterMillis: Long): AccommodationAnalysisMetadata =
            AccommodationAnalysisMetadata(
                status = Status.SEARCHING,
                analyzedCount = 0,
                deepAnalyzedCount = 0,
                matchCount = 0,
                probableCount = 0,
                pollAfterMillis = pollAfterMillis,
            )

        fun failed(): AccommodationAnalysisMetadata =
            AccommodationAnalysisMetadata(
                status = Status.FAILED,
                analyzedCount = 0,
                deepAnalyzedCount = 0,
                matchCount = 0,
                probableCount = 0,
            )
    }
}
