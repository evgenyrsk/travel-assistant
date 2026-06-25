package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class InMemoryPendingConfirmationStoreTest {

    private val now = Instant.parse("2026-06-25T10:00:00Z")

    @Test
    fun savesAndLoadsActivePendingConfirmationBySession() {
        val store = InMemoryPendingConfirmationStore()
        val pendingConfirmation = pendingConfirmation()

        val saved = store.save(pendingConfirmation)
        val activeConfirmation = store.findActiveBySession(
            sessionId = pendingConfirmation.sessionId,
            now = now.plusSeconds(60),
        )

        assertEquals(pendingConfirmation, saved)
        assertEquals(pendingConfirmation, activeConfirmation)
        assertEquals(PendingConfirmationStatus.PENDING, activeConfirmation?.statusAt(now.plusSeconds(60)))
    }

    @Test
    fun expiredPendingConfirmationIsNotActive() {
        val store = InMemoryPendingConfirmationStore()
        val pendingConfirmation = pendingConfirmation(
            expiresAt = now.plusSeconds(60),
        )

        store.save(pendingConfirmation)

        assertNull(
            store.findActiveBySession(
                sessionId = pendingConfirmation.sessionId,
                now = now.plusSeconds(60),
            ),
        )
        assertEquals(
            PendingConfirmationStatus.EXPIRED,
            pendingConfirmation.statusAt(now.plusSeconds(60)),
        )
    }

    @Test
    fun consumedPendingConfirmationIsNotActive() {
        val store = InMemoryPendingConfirmationStore()
        val pendingConfirmation = store.save(pendingConfirmation())

        val consumedConfirmation = store.markConsumed(
            sessionId = pendingConfirmation.sessionId,
            consumedAt = now.plusSeconds(90),
        )

        assertEquals(PendingConfirmationStatus.CONSUMED, consumedConfirmation?.status)
        assertEquals(now.plusSeconds(90), consumedConfirmation?.updatedAt)
        assertNull(
            store.findActiveBySession(
                sessionId = pendingConfirmation.sessionId,
                now = now.plusSeconds(91),
            ),
        )
    }

    @Test
    fun markConsumedReturnsNullWhenSessionHasNoPendingConfirmation() {
        val store = InMemoryPendingConfirmationStore()

        assertNull(
            store.markConsumed(
                sessionId = AssistantSessionId("assistant-session-local-missing"),
                consumedAt = now.plusSeconds(1),
            ),
        )
    }

    @Test
    fun pendingConfirmationIsScopedBySession() {
        val store = InMemoryPendingConfirmationStore()
        val firstSessionConfirmation = pendingConfirmation(
            sessionId = AssistantSessionId("assistant-session-local-000001"),
            destination = "Rome",
        )
        val secondSessionConfirmation = pendingConfirmation(
            sessionId = AssistantSessionId("assistant-session-local-000002"),
            destination = "Paris",
        )

        store.save(firstSessionConfirmation)
        store.save(secondSessionConfirmation)

        assertEquals(
            "Rome",
            store.findActiveBySession(
                sessionId = firstSessionConfirmation.sessionId,
                now = now.plusSeconds(10),
            )?.criteria?.destination,
        )
        assertEquals(
            "Paris",
            store.findActiveBySession(
                sessionId = secondSessionConfirmation.sessionId,
                now = now.plusSeconds(10),
            )?.criteria?.destination,
        )
    }

    @Test
    fun pendingConfirmationDoesNotStoreRawCandidateOrHotelSearchId() {
        val store = InMemoryPendingConfirmationStore()
        val pendingConfirmation = store.save(pendingConfirmation())
        val storedText = pendingConfirmation.toString()

        listOf(
            "LlmCandidate",
            "raw candidate",
            "candidatePayload",
            "modelResponse",
            "extractedConstraints",
            "hotelSearchId",
            "show_hotel_results",
            "Hotel search created",
        ).forEach { forbidden ->
            assertFalse(
                storedText.contains(forbidden),
                "Pending confirmation must not store $forbidden",
            )
        }
    }

    @Test
    fun usesDeterministicTimeInputsWithoutProviderNetworkOrApiKeyDependency() {
        val store = InMemoryPendingConfirmationStore()
        val pendingConfirmation = pendingConfirmation(
            createdAt = now,
            updatedAt = now,
            expiresAt = now.plusSeconds(300),
        )

        store.save(pendingConfirmation)

        assertEquals(
            pendingConfirmation,
            store.findActiveBySession(
                sessionId = pendingConfirmation.sessionId,
                now = now.plusSeconds(299),
            ),
        )
        assertNull(
            store.findActiveBySession(
                sessionId = pendingConfirmation.sessionId,
                now = now.plusSeconds(300),
            ),
        )
    }

    private fun pendingConfirmation(
        sessionId: AssistantSessionId = AssistantSessionId("assistant-session-local-000001"),
        destination: String = "Rome",
        createdAt: Instant = now,
        updatedAt: Instant = now,
        expiresAt: Instant = now.plusSeconds(300),
    ): PendingProceedWithCandidateConfirmation {
        val criteria = ProceedWithCandidateCriteria(
            destination = destination,
            checkInDate = LocalDate.parse("2026-07-01"),
            checkOutDate = LocalDate.parse("2026-07-04"),
            guests = ProceedWithCandidateCriteria.Guests(
                adults = 2,
                children = 0,
            ),
            rooms = 1,
        )

        return PendingProceedWithCandidateConfirmation(
            sessionId = sessionId,
            criteria = criteria,
            proposal = ProceedWithCandidateConfirmationProposal(
                summary = "Параметры hotel search: направление: $destination; заезд: 2026-07-01; " +
                    "выезд: 2026-07-04; взрослые: 2; дети: 0; номера: 1.",
                confirmationQuestion = "Проверить отели по этим параметрам?",
                displayFields = listOf(
                    ProceedWithCandidateConfirmationField(
                        key = "destination",
                        label = "направление",
                        value = destination,
                    ),
                ),
            ),
            createdAt = createdAt,
            updatedAt = updatedAt,
            expiresAt = expiresAt,
        )
    }
}
