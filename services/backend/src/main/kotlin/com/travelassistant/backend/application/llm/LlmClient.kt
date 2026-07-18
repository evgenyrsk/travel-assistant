package com.travelassistant.backend.application.llm

fun interface LlmClient {
    suspend fun generateCandidate(request: LlmCandidateRequest): LlmClientResponse
}
