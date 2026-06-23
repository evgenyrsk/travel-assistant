package com.travelassistant.backend.application.llm

sealed interface LlmClientResponse {
    data class Candidate(
        val value: LlmCandidate,
    ) : LlmClientResponse

    data object Empty : LlmClientResponse

    data object Failure : LlmClientResponse
}
