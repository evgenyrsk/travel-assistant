package com.travelassistant.backend.application.llm

fun interface LlmClient {
    fun generateCandidate(request: LlmCandidateRequest): LlmClientResponse
}
