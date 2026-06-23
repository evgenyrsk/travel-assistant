package com.travelassistant.backend.infrastructure.llm

import com.travelassistant.backend.application.llm.LlmCandidateRequest
import com.travelassistant.backend.application.llm.LlmClient
import com.travelassistant.backend.application.llm.LlmClientResponse

class FakeLlmClient(
    private val response: LlmClientResponse,
) : LlmClient {

    override fun generateCandidate(request: LlmCandidateRequest): LlmClientResponse = response
}
