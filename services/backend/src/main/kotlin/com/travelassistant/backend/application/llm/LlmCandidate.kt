package com.travelassistant.backend.application.llm

data class LlmCandidate(
    val outcome: Outcome,
    val intent: Intent,
    val extractedConstraints: Map<String, String> = emptyMap(),
    val preferencePatch: LlmHotelSearchPreferencesPatch = LlmHotelSearchPreferencesPatch(),
    val missingRequiredFields: List<String> = emptyList(),
    val conflicts: List<String> = emptyList(),
    val clarificationQuestion: String? = null,
    val warnings: List<String> = emptyList(),
) {
    enum class Outcome {
        INTERPRETED,
        NEEDS_CLARIFICATION,
        AMBIGUOUS,
        UNSUPPORTED,
    }

    enum class Intent {
        HOTEL_SEARCH,
        UNKNOWN,
        UNSUPPORTED,
    }
}
