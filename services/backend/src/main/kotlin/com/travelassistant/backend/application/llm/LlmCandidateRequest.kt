package com.travelassistant.backend.application.llm

import java.time.LocalDate

data class LlmCandidateRequest(
    val userMessage: String,
    val confirmedConstraints: Map<String, String> = emptyMap(),
    val missingRequiredFields: List<String> = emptyList(),
    val referenceDate: LocalDate? = null,
)
