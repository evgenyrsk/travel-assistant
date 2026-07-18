package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class AccumulateAssistantHotelConstraintsUseCaseTest {
    private val store = InMemoryAssistantHotelConstraintsStore()
    private val accumulate = AccumulateAssistantHotelConstraintsUseCase(store)
    private val sessionId = AssistantSessionId("assistant-session-context-000001")

    @Test
    fun accumulatesPartialConstraintsAndPreservesAbsentValues() {
        accumulate(
            command(
                "destination" to "  Казань  ",
                "raw-message" to "must-not-be-stored",
            ),
        )

        val result = accumulate(
            command(
                "check-in" to "2026-08-10",
                "check-out" to "2026-08-14",
            ),
        )

        assertEquals("Казань", result.constraints.destination)
        assertEquals(LocalDate.parse("2026-08-10"), result.constraints.checkInDate)
        assertEquals(LocalDate.parse("2026-08-14"), result.constraints.checkOutDate)
        assertEquals(
            listOf("adults", "rooms"),
            result.constraints.missingRequiredFields(),
        )
        assertFalse(result.constraints.toString().contains("must-not-be-stored"))
    }

    @Test
    fun validCorrectionReplacesOnlyExplicitValue() {
        accumulate(command(*completeConstraints(destination = "Rome")))

        val result = accumulate(command("destination" to "Paris"))

        assertEquals("Paris", result.constraints.destination)
        assertEquals(LocalDate.parse("2026-08-10"), result.constraints.checkInDate)
        assertEquals(2, result.constraints.adults)
        assertEquals(1, result.constraints.rooms)
        assertEquals(emptySet(), result.issues)
    }

    @Test
    fun invalidCorrectionClearsOldValueAndPreservesOtherValidUpdates() {
        accumulate(command(*completeConstraints(destination = "Rome")))

        val rejected = accumulate(
            command(
                "adults" to "0",
                "rooms" to "2",
            ),
        )

        assertNull(rejected.constraints.adults)
        assertEquals(2, rejected.constraints.rooms)
        assertEquals(
            setOf(AssistantHotelConstraintsAccumulationIssue.INVALID_ADULTS),
            rejected.issues,
        )
        assertEquals(listOf("adults"), rejected.constraints.missingRequiredFields())

        val corrected = accumulate(command("adults" to "3"))

        assertEquals(3, corrected.constraints.adults)
        assertEquals(emptySet(), corrected.constraints.unresolvedFields)
        assertEquals(emptyList(), corrected.constraints.missingRequiredFields())
    }

    @Test
    fun invalidDateCorrectionClearsOnlyChangedDate() {
        accumulate(command(*completeConstraints(destination = "Rome")))

        val result = accumulate(command("check-in" to "2026-08-20"))

        assertNull(result.constraints.checkInDate)
        assertEquals(LocalDate.parse("2026-08-14"), result.constraints.checkOutDate)
        assertEquals(
            setOf(AssistantHotelConstraintsAccumulationIssue.INVALID_DATE_RANGE),
            result.issues,
        )
        assertEquals(listOf("check-in"), result.constraints.missingRequiredFields())
    }

    @Test
    fun acceptsBoundaryChildAgesAndPreservesTheirOrder() {
        val result = accumulate(
            command(
                "children" to "2",
                "children-ages" to "17,0",
            ),
        )

        assertEquals(2, result.constraints.childrenCount)
        assertEquals(listOf(17, 0), result.constraints.childrenAges)
        assertEquals("17,0", result.constraints.toConfirmedConstraints()["children-ages"])
        assertEquals(emptySet(), result.issues)
    }

    @Test
    fun rejectsChildAgesOutsideSupportedRange() {
        val belowRange = accumulate(
            command(
                "children" to "1",
                "children-ages" to "-1",
            ),
        )

        assertNull(belowRange.constraints.childrenAges)
        assertEquals(
            setOf(AssistantHotelConstraintsAccumulationIssue.INVALID_CHILDREN_AGES),
            belowRange.issues,
        )
        assertEquals(
            listOf("destination", "check-in", "check-out", "adults", "children-ages", "rooms"),
            belowRange.constraints.missingRequiredFields(),
        )

        val aboveRange = accumulate(command("children-ages" to "18"))

        assertNull(aboveRange.constraints.childrenAges)
        assertEquals(
            setOf(AssistantHotelConstraintsAccumulationIssue.INVALID_CHILDREN_AGES),
            aboveRange.issues,
        )
    }

    @Test
    fun positiveChildCountWithoutAgesRemainsIncomplete() {
        val result = accumulate(command("children" to "1"))

        assertEquals(1, result.constraints.childrenCount)
        assertNull(result.constraints.childrenAges)
        assertEquals(
            listOf("destination", "check-in", "check-out", "adults", "children-ages", "rooms"),
            result.constraints.missingRequiredFields(),
        )
    }

    @Test
    fun changedChildCountClearsStaleAges() {
        accumulate(
            command(
                "children" to "1",
                "children-ages" to "7",
            ),
        )

        val result = accumulate(command("children" to "2"))

        assertEquals(2, result.constraints.childrenCount)
        assertNull(result.constraints.childrenAges)
        assertEquals(true, "children-ages" in result.constraints.missingRequiredFields())
    }

    @Test
    fun invalidChildCountDoesNotRemainConfirmedThroughOldAges() {
        accumulate(
            command(
                "children" to "1",
                "children-ages" to "7",
            ),
        )

        val invalid = accumulate(command("children" to "-1"))

        assertFalse(invalid.constraints.toConfirmedConstraints().containsKey("children"))
        assertEquals("7", invalid.constraints.toConfirmedConstraints()["children-ages"])
        assertEquals(true, "children" in invalid.constraints.missingRequiredFields())

        val corrected = accumulate(command("children" to "1"))

        assertEquals("1", corrected.constraints.toConfirmedConstraints()["children"])
        assertEquals("7", corrected.constraints.toConfirmedConstraints()["children-ages"])
        assertEquals(false, "children" in corrected.constraints.missingRequiredFields())
    }

    @Test
    fun agesWithoutCountDeriveCanonicalChildCount() {
        val result = accumulate(command("children-ages" to "4,12"))

        assertEquals(2, result.constraints.childrenCount)
        assertEquals(listOf(4, 12), result.constraints.childrenAges)
        assertEquals("2", result.constraints.toConfirmedConstraints()["children"])
    }

    @Test
    fun keepsSessionContextsIsolatedAndDeterministic() {
        val otherSessionId = AssistantSessionId("assistant-session-context-000002")

        val first = accumulate(command("destination" to "Rome"))
        val second = accumulate(
            AccumulateAssistantHotelConstraintsCommand(
                sessionId = otherSessionId,
                extractedConstraints = mapOf("destination" to "Paris"),
            ),
        )

        assertEquals("Rome", first.constraints.destination)
        assertEquals("Paris", second.constraints.destination)
        assertEquals("Rome", store.findBySession(sessionId)?.destination)
        assertEquals("Paris", store.findBySession(otherSessionId)?.destination)
    }

    private fun command(
        vararg constraints: Pair<String, String>,
    ): AccumulateAssistantHotelConstraintsCommand =
        AccumulateAssistantHotelConstraintsCommand(
            sessionId = sessionId,
            extractedConstraints = mapOf(*constraints),
        )

    private fun completeConstraints(
        destination: String,
    ): Array<Pair<String, String>> =
        arrayOf(
            "destination" to destination,
            "check-in" to "2026-08-10",
            "check-out" to "2026-08-14",
            "adults" to "2",
            "children" to "0",
            "rooms" to "1",
        )
}
