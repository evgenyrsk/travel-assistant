package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmCandidateRequest
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AssistantDateInterpretationPolicyTest {
    private val policy = AssistantDateInterpretationPolicy()

    @Test
    fun `keeps resolved dates when client reference date is present`() {
        val decision = interpretedDecision()

        val result = policy(
            decision,
            request(
                message = "Хочу в Москву сегодня до завтра",
                referenceDate = LocalDate.parse("2026-07-23"),
            ),
        )

        assertEquals(decision, result)
    }

    @Test
    fun `requires explicit dates for relative wording without client timezone`() {
        val result = assertIs<AssistantCandidateDecision.AskClarification>(
            policy(
                interpretedDecision(),
                request(message = "Хочу в Москву сегодня до завтра"),
            ),
        )

        assertEquals(
            AssistantDateInterpretationPolicy.DATE_WITH_YEAR_CLARIFICATION_MESSAGE,
            result.question,
        )
        assertEquals(
            mapOf("destination" to "Москва", "adults" to "2"),
            result.candidate?.extractedConstraints,
        )
        assertEquals(
            listOf("check-in", "check-out"),
            result.candidate?.missingRequiredFields,
        )
    }

    @Test
    fun `requires explicit year for month wording without client timezone`() {
        val result = policy(
            interpretedDecision(),
            request(message = "Москва с 10 по 14 августа"),
        )

        assertIs<AssistantCandidateDecision.AskClarification>(result)
    }

    @Test
    fun `keeps explicit four digit year without client timezone`() {
        val decision = interpretedDecision()

        val result = policy(
            decision,
            request(message = "Москва с 10 по 14 августа 2026 года"),
        )

        assertEquals(decision, result)
    }

    @Test
    fun `does not hide an unsafe candidate behind date clarification`() {
        val decision = AssistantCandidateDecision.ProceedWithCandidate(
            interpretedCandidate().copy(warnings = listOf("unsupported-preference")),
        )

        val result = policy(
            decision,
            request(message = "Хочу в Москву сегодня до завтра"),
        )

        assertEquals(decision, result)
    }

    private fun interpretedDecision(): AssistantCandidateDecision.ProceedWithCandidate =
        AssistantCandidateDecision.ProceedWithCandidate(interpretedCandidate())

    private fun interpretedCandidate(): LlmCandidate =
        LlmCandidate(
            outcome = LlmCandidate.Outcome.INTERPRETED,
            intent = LlmCandidate.Intent.HOTEL_SEARCH,
            extractedConstraints = mapOf(
                "destination" to "Москва",
                "check-in" to "2026-07-23",
                "check-out" to "2026-07-24",
                "adults" to "2",
            ),
        )

    private fun request(
        message: String,
        referenceDate: LocalDate? = null,
    ): LlmCandidateRequest =
        LlmCandidateRequest(
            userMessage = message,
            referenceDate = referenceDate,
        )
}
