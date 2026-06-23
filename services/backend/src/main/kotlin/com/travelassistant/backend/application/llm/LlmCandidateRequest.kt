package com.travelassistant.backend.application.llm

data class LlmCandidateRequest(
    val userMessage: String,
    val confirmedConstraints: Map<String, String> = emptyMap(),
    val missingRequiredFields: List<String> = emptyList(),
)
