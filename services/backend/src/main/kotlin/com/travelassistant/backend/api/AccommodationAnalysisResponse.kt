package com.travelassistant.backend.api

import com.travelassistant.backend.domain.hotel.AccommodationAnalysisMetadata
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AccommodationAnalysisResponse(
    val status: String,
    val analyzedCount: Int,
    val deepAnalyzedCount: Int,
    val matchCount: Int,
    val probableCount: Int,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val pollAfterMillis: Long? = null,
) {
    companion object {
        fun from(metadata: AccommodationAnalysisMetadata): AccommodationAnalysisResponse =
            AccommodationAnalysisResponse(
                status = metadata.status.apiValue,
                analyzedCount = metadata.analyzedCount,
                deepAnalyzedCount = metadata.deepAnalyzedCount,
                matchCount = metadata.matchCount,
                probableCount = metadata.probableCount,
                pollAfterMillis = metadata.pollAfterMillis,
            )
    }
}
