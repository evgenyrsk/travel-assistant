package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.hotel.HotelSearchPreferences
import java.math.BigDecimal
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
            """Проверьте параметры:
Куда: Rome
Даты: 1–4 июля 2026
Гости: 2 взрослых, 1 ребёнок (7 лет)
Номера: 1 номер""",
            proposal.summary,
        )
        assertEquals("Найти отели по этим параметрам?", proposal.confirmationQuestion)
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
        assertEquals(true, proposal.summary.contains("1–4 июля 2026"))
        assertEquals(true, proposal.summary.contains("2 взрослых"))
        assertEquals(true, proposal.summary.contains("1 ребёнок (7 лет)"))
        assertEquals(true, proposal.summary.contains("1 номер"))
        assertFalse(proposal.summary.contains("hotel search"))
        assertFalse(proposal.summary.contains("2026-07-01"))
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
        assertEquals(true, proposal.summary.contains("без детей"))
    }

    @Test
    fun includesOnlyActivePreferencesInDeterministicOrder() {
        val proposal = useCase(
            acceptedCriteria(
                preferences = HotelSearchPreferences(
                    maxTotalPrice = HotelSearchPreferences.MaxTotalPrice(
                        amount = BigDecimal("80000.00"),
                        currency = "RUB",
                    ),
                    stars = setOf(5, 4),
                    minimumGuestRating = HotelSearchPreferences.MinimumGuestRating.EIGHT,
                    freeCancellationRequired = true,
                ),
            ),
        )

        assertEquals(
            listOf(
                ProceedWithCandidateConfirmationField(
                    "max-total-price",
                    "максимальная стоимость за весь период",
                    "80000 RUB",
                ),
                ProceedWithCandidateConfirmationField("stars", "звёзды", "4, 5"),
                ProceedWithCandidateConfirmationField(
                    "min-guest-rating",
                    "минимальный гостевой рейтинг",
                    "8",
                ),
                ProceedWithCandidateConfirmationField(
                    "free-cancellation",
                    "бесплатная отмена",
                    "обязательна",
                ),
            ),
            proposal.displayFields.takeLast(4),
        )
        assertEquals(
            "Условия: до 80 000 ₽ за всё проживание; 4–5 звёзд; " +
                "рейтинг от 8; бесплатная отмена",
            proposal.summary.lineSequence().last(),
        )
    }

    @Test
    fun `formats date ranges across months and years in Russian`() {
        val acrossMonths = useCase(
            acceptedCriteria(
                checkInDate = LocalDate.parse("2026-07-30"),
                checkOutDate = LocalDate.parse("2026-08-02"),
            ),
        )
        val acrossYears = useCase(
            acceptedCriteria(
                checkInDate = LocalDate.parse("2026-12-30"),
                checkOutDate = LocalDate.parse("2027-01-02"),
            ),
        )

        assertEquals(true, acrossMonths.summary.contains("30 июля — 2 августа 2026"))
        assertEquals(true, acrossYears.summary.contains("30 декабря 2026 — 2 января 2027"))
    }

    @Test
    fun `formats multiple children and ages with Russian plurals`() {
        val proposal = useCase(
            acceptedCriteria(
                guests = ProceedWithCandidateCriteria.Guests(
                    adults = 1,
                    childrenAges = listOf(1, 2, 5),
                ),
                rooms = 2,
            ),
        )

        assertEquals(
            true,
            proposal.summary.contains(
                "Гости: 1 взрослый, 3 ребёнка (1 год, 2 года и 5 лет)",
            ),
        )
        assertEquals(true, proposal.summary.contains("Номера: 2 номера"))
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

        assertEquals("Найти отели по этим параметрам?", proposal.confirmationQuestion)
    }

    private fun acceptedCriteria(
        destination: String = "Rome",
        checkInDate: LocalDate = LocalDate.parse("2026-07-01"),
        checkOutDate: LocalDate = LocalDate.parse("2026-07-04"),
        guests: ProceedWithCandidateCriteria.Guests = ProceedWithCandidateCriteria.Guests(
            adults = 2,
            childrenAges = listOf(7),
        ),
        rooms: Int = 1,
        preferences: HotelSearchPreferences = HotelSearchPreferences(),
    ): ProceedWithCandidateValidationResult.Accepted =
        ProceedWithCandidateValidationResult.Accepted(
            ProceedWithCandidateCriteria(
                destination = destination,
                checkInDate = checkInDate,
                checkOutDate = checkOutDate,
                guests = guests,
                rooms = rooms,
                preferences = preferences,
            ),
        )
}
