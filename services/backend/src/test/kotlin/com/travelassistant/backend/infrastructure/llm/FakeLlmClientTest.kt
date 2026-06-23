package com.travelassistant.backend.infrastructure.llm

import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmCandidateRequest
import com.travelassistant.backend.application.llm.LlmClientResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeLlmClientTest {

    @Test
    fun returnsConfiguredCandidateDeterministically() {
        val response = LlmClientResponse.Candidate(
            LlmCandidate(
                outcome = LlmCandidate.Outcome.INTERPRETED,
                intent = LlmCandidate.Intent.HOTEL_SEARCH,
                extractedConstraints = mapOf("destination" to "Rome"),
            ),
        )
        val client = FakeLlmClient(response)
        val request = LlmCandidateRequest(
            userMessage = "Find a hotel in Rome",
            confirmedConstraints = mapOf("guests" to "2"),
        )

        assertEquals(response, client.generateCandidate(request))
        assertEquals(response, client.generateCandidate(request))
    }

    @Test
    fun returnsConfiguredEmptyResponseDeterministically() {
        val client = FakeLlmClient(LlmClientResponse.Empty)
        val request = LlmCandidateRequest(
            userMessage = "Find a hotel",
            missingRequiredFields = listOf("destination", "stay_dates"),
        )

        assertEquals(LlmClientResponse.Empty, client.generateCandidate(request))
        assertEquals(LlmClientResponse.Empty, client.generateCandidate(request))
    }

    @Test
    fun returnsConfiguredAmbiguousCandidateDeterministically() {
        val response = LlmClientResponse.Candidate(
            LlmCandidate(
                outcome = LlmCandidate.Outcome.AMBIGUOUS,
                intent = LlmCandidate.Intent.UNKNOWN,
                conflicts = listOf("destination"),
                clarificationQuestion = "Which destination should I use?",
            ),
        )
        val client = FakeLlmClient(response)
        val request = LlmCandidateRequest(userMessage = "Somewhere warm")

        assertEquals(response, client.generateCandidate(request))
        assertEquals(response, client.generateCandidate(request))
    }
}
