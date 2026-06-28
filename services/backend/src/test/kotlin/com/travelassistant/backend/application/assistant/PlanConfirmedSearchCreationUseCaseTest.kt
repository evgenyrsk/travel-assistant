package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class PlanConfirmedSearchCreationUseCaseTest {

    @Test
    fun returnsReadyPlanForConfirmedDecisionWithMappedHotelSearchCriteria() {
        val useCase = PlanConfirmedSearchCreationUseCase()

        val plan = useCase(confirmedDecision())

        val readyPlan = assertIs<ConfirmedSearchCreationPlan.ReadyToCreateSearch>(plan)
        assertEquals(
            HotelSearchCriteria(
                destination = "Rome",
                checkInDate = LocalDate.parse("2026-07-01"),
                checkOutDate = LocalDate.parse("2026-07-04"),
                guests = HotelSearchCriteria.Guests(
                    adults = 2,
                    children = 1,
                ),
                rooms = 1,
            ),
            readyPlan.criteria,
        )
    }

    @Test
    fun preservesAllCriteriaFieldsThroughPlan() {
        val useCase = PlanConfirmedSearchCreationUseCase()

        val plan = assertIs<ConfirmedSearchCreationPlan.ReadyToCreateSearch>(
            useCase(
                confirmedDecision(
                    criteria = criteria(
                        destination = "Rome Centro",
                        checkInDate = LocalDate.parse("2026-12-30"),
                        checkOutDate = LocalDate.parse("2027-01-03"),
                        guests = ProceedWithCandidateCriteria.Guests(
                            adults = 3,
                            children = 0,
                        ),
                        rooms = 2,
                    ),
                ),
            ),
        )

        assertEquals("Rome Centro", plan.criteria.destination)
        assertEquals(LocalDate.parse("2026-12-30"), plan.criteria.checkInDate)
        assertEquals(LocalDate.parse("2027-01-03"), plan.criteria.checkOutDate)
        assertEquals(3, plan.criteria.guests.adults)
        assertEquals(0, plan.criteria.guests.children)
        assertEquals(2, plan.criteria.rooms)
    }

    @Test
    fun exposesLifecyclePolicyForFutureSuccessfulSearchCreationOnly() {
        val useCase = PlanConfirmedSearchCreationUseCase()

        val plan = assertIs<ConfirmedSearchCreationPlan.ReadyToCreateSearch>(
            useCase(confirmedDecision()),
        )

        assertEquals(
            ConfirmedSearchCreationLifecyclePolicy.PendingConsumption.CONSUME_AFTER_SEARCH_SUCCESS,
            plan.lifecyclePolicy.pendingConsumption,
        )
        assertEquals(
            ConfirmedSearchCreationLifecyclePolicy.FailureHandling.DO_NOT_CONSUME_ON_SEARCH_FAILURE,
            plan.lifecyclePolicy.failureHandling,
        )
    }

    @Test
    fun marksDuplicateConfirmationAsFutureIdempotencyGuardRequirement() {
        val useCase = PlanConfirmedSearchCreationUseCase()

        val plan = assertIs<ConfirmedSearchCreationPlan.ReadyToCreateSearch>(
            useCase(confirmedDecision()),
        )

        assertEquals(
            ConfirmedSearchCreationLifecyclePolicy.DuplicateConfirmationHandling.REQUIRES_IDEMPOTENCY_GUARD,
            plan.lifecyclePolicy.duplicateConfirmationHandling,
        )
    }

    @Test
    fun usesInjectedMapperWithoutCreatingSearch() {
        val mappedCriteria = HotelSearchCriteria(
            destination = "Paris",
            checkInDate = LocalDate.parse("2026-08-10"),
            checkOutDate = LocalDate.parse("2026-08-15"),
            guests = HotelSearchCriteria.Guests(
                adults = 1,
                children = 0,
            ),
            rooms = 1,
        )
        val useCase = PlanConfirmedSearchCreationUseCase(
            mapCriteria = { inputCriteria ->
                assertEquals("Rome", inputCriteria.destination)
                mappedCriteria
            },
        )

        val plan = assertIs<ConfirmedSearchCreationPlan.ReadyToCreateSearch>(
            useCase(confirmedDecision()),
        )

        assertEquals(mappedCriteria, plan.criteria)
    }

    @Test
    fun planDoesNotExposeSearchCreationOrRuntimeSideEffects() {
        val useCase = PlanConfirmedSearchCreationUseCase()

        val plan = useCase(confirmedDecision())
        val planText = plan.toString()

        listOf(
            "hotelSearchId",
            "show_hotel_results",
            "CreateHotelSearchUseCase",
            "Hotel search created",
            "provider",
            "markConsumed",
            "LlmCandidate",
            "candidatePayload",
            "modelResponse",
        ).forEach { forbidden ->
            assertFalse(
                planText.contains(forbidden),
                "Confirmed search creation plan must not expose $forbidden",
            )
        }
    }

    @Test
    fun remainsDeterministicForSameConfirmedDecision() {
        val useCase = PlanConfirmedSearchCreationUseCase()
        val decision = confirmedDecision()

        val firstPlan = useCase(decision)
        val secondPlan = useCase(decision)

        assertEquals(firstPlan, secondPlan)
    }

    private fun confirmedDecision(
        criteria: ProceedWithCandidateCriteria = criteria(),
    ): PostConfirmationDecision.Confirmed =
        PostConfirmationDecision.Confirmed(criteria)

    private fun criteria(
        destination: String = "Rome",
        checkInDate: LocalDate = LocalDate.parse("2026-07-01"),
        checkOutDate: LocalDate = LocalDate.parse("2026-07-04"),
        guests: ProceedWithCandidateCriteria.Guests = ProceedWithCandidateCriteria.Guests(
            adults = 2,
            children = 1,
        ),
        rooms: Int = 1,
    ): ProceedWithCandidateCriteria =
        ProceedWithCandidateCriteria(
            destination = destination,
            checkInDate = checkInDate,
            checkOutDate = checkOutDate,
            guests = guests,
            rooms = rooms,
        )
}
