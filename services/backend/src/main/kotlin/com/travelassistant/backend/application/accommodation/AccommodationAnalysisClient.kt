package com.travelassistant.backend.application.accommodation

fun interface AccommodationAnalysisClient {
    suspend fun analyze(request: AccommodationAnalysisRequest): AccommodationAnalysisResult
}
