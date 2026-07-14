package com.travelassistant.backend.application.assistant

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BuildProceedWithCandidateConfirmationProposalUseCaseTest {

    private val useCase = BuildProceedWithCandidateConfirmationProposalUseCase()

    @Test
    fun buildsHumanReadableProposalFromAcceptedCriteria() {
        val proposal = useCase(acceptedCriteria())

        assertEquals(
            "Параметры hotel search: направление: Rome; заезд: 2026-07-01; " +
                "выезд: 2026-07-04; взрослые: 2; дети: 1; возраст детей: 7; номера: 1.",
            proposal.summary,
        )
        assertEquals("Проверить отели по этим параметрам?", proposal.confirmationQuestion)
        assertEquals(
            listOf(
                ProceedWithCandidateConfirmationField("destination", "направление", "Rome"),
                ProceedWithCandidateConfirmationField("check-in", "заезд", "2026-07-01"),
                ProceedWithCandidateConfirmationField("check-out", "выезд", "2026-07-04"),
                ProceedWithCandidateConfirmationField("adults", "взрослые", "2"),
                ProceedWithCandidateConfirmationField("children", "дети", "1"),
                ProceedWithCandidateConfirmationField("children-ages", "возраст детей", "7"),
                ProceedWithCandidateConfirmationField("rooms", "номера", "1"),
            ),
            proposal.displayFields,
        )
    }

    @Test
    fun remainsDeterministicForSameAcceptedCriteria() {
        val accepted = acceptedCriteria()

        val firstProposal = useCase(accepted)
        val secondProposal = useCase(accepted)

        assertEquals(firstProposal, secondProposal)
    }

    @Test
    fun includesDestinationDatesGuestsAndRooms() {
        val proposal = useCase(acceptedCriteria())

        val keys = proposal.displayFields.map { it.key }
        assertEquals(
            listOf(
                "destination",
                "check-in",
                "check-out",
                "adults",
                "children",
                "children-ages",
                "rooms",
            ),
            keys,
        )
        assertEquals(true, proposal.summary.contains("Rome"))
        assertEquals(true, proposal.summary.contains("2026-07-01"))
        assertEquals(true, proposal.summary.contains("2026-07-04"))
        assertEquals(true, proposal.summary.contains("взрослые: 2"))
        assertEquals(true, proposal.summary.contains("дети: 1"))
        assertEquals(true, proposal.summary.contains("возраст детей: 7"))
        assertEquals(true, proposal.summary.contains("номера: 1"))
    }

    @Test
    fun includesZeroChildrenAsSafeDisplayValue() {
        val proposal = useCase(
            acceptedCriteria(
                guests = ProceedWithCandidateCriteria.Guests(
                    adults = 2,
                    childrenAges = emptyList(),
                ),
            ),
        )

        assertEquals(
            ProceedWithCandidateConfirmationField("children", "дети", "0"),
            proposal.displayFields.first { it.key == "children" },
        )
        assertEquals(true, proposal.summary.contains("дети: 0"))
    }

    @Test
    fun omitsBlankDestinationFromDisplayFields() {
        val proposal = useCase(acceptedCriteria(destination = "   "))

        assertEquals(false, proposal.displayFields.any { it.key == "destination" })
        assertEquals(false, proposal.summary.contains("направление"))
    }

    @Test
    fun doesNotExposeRawCandidateInternalMetadataOrHotelSearchId() {
        val proposalText = useCase(acceptedCriteria()).toString()

        listOf(
            "LlmCandidate",
            "raw candidate",
            "candidatePayload",
            "modelResponse",
            "extractedConstraints",
            "missingRequiredFields",
            "conflicts",
            "warnings",
            "hotelSearchId",
            "show_hotel_results",
        ).forEach { forbidden ->
            assertFalse(
                proposalText.contains(forbidden),
                "Proposal must not expose $forbidden",
            )
        }
    }

    @Test
    fun buildsWithoutProviderNetworkOrCredentialDependency() {
        val localUseCase = BuildProceedWithCandidateConfirmationProposalUseCase()

        val proposal = localUseCase(acceptedCriteria())

        assertEquals("Проверить отели по этим параметрам?", proposal.confirmationQuestion)
    }

    private fun acceptedCriteria(
        destination: String = "Rome",
        guests: ProceedWithCandidateCriteria.Guests = ProceedWithCandidateCriteria.Guests(
            adults = 2,
            childrenAges = listOf(7),
        ),
    ): ProceedWithCandidateValidationResult.Accepted =
        ProceedWithCandidateValidationResult.Accepted(
            ProceedWithCandidateCriteria(
                destination = destination,
                checkInDate = LocalDate.parse("2026-07-01"),
                checkOutDate = LocalDate.parse("2026-07-04"),
                guests = guests,
                rooms = 1,
            ),
        )
}
