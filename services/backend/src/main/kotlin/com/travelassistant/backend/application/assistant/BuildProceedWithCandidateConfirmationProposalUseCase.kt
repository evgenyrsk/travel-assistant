package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.hotel.AccommodationConcept
import com.travelassistant.backend.domain.hotel.HotelSearchPreferences
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

class BuildProceedWithCandidateConfirmationProposalUseCase {

    operator fun invoke(
        accepted: ProceedWithCandidateValidationResult.Accepted,
    ): ProceedWithCandidateConfirmationProposal {
        val fields = accepted.criteria.confirmationFields()

        return ProceedWithCandidateConfirmationProposal(
            summary = accepted.criteria.humanReadableSummary(),
            confirmationQuestion = "Найти отели по этим параметрам?",
            displayFields = fields,
        )
    }

    private fun ProceedWithCandidateCriteria.humanReadableSummary(): String =
        buildList {
            add("Проверьте параметры:")
            destination.trim().takeIf(String::isNotBlank)?.let { value ->
                add("Куда: $value")
            }
            add("Даты: ${formatDateRange(checkInDate, checkOutDate)}")
            add("Гости: ${guests.humanReadableGuests()}")
            preferences.humanReadableConditions().takeIf(List<String>::isNotEmpty)?.let { conditions ->
                add("Условия: ${conditions.joinToString(separator = "; ")}")
            }
        }.joinToString(separator = "\n")

    private fun ProceedWithCandidateCriteria.confirmationFields(): List<ProceedWithCandidateConfirmationField> =
        buildList {
            destination.trim().takeIf(String::isNotBlank)?.let { value ->
                add(
                    ProceedWithCandidateConfirmationField(
                        key = "destination",
                        label = "направление",
                        value = value,
                    ),
                )
            }
            add(ProceedWithCandidateConfirmationField(
                key = "check-in",
                label = "заезд",
                value = checkInDate.toString(),
            ))
            add(ProceedWithCandidateConfirmationField(
                key = "check-out",
                label = "выезд",
                value = checkOutDate.toString(),
            ))
            add(ProceedWithCandidateConfirmationField(
                key = "adults",
                label = "взрослые",
                value = guests.adults.toString(),
            ))
            add(ProceedWithCandidateConfirmationField(
                key = "children",
                label = "дети",
                value = guests.children.toString(),
            ))
            if (guests.childrenAges.isNotEmpty()) {
                add(
                    ProceedWithCandidateConfirmationField(
                        key = "children-ages",
                        label = "возраст детей",
                        value = guests.childrenAges.joinToString(separator = ", "),
                    ),
                )
            }
            preferences.maxTotalPrice?.let { price ->
                add(
                    ProceedWithCandidateConfirmationField(
                        key = "max-total-price",
                        label = "максимальная стоимость за весь период",
                        value = "${price.amount.stripTrailingZeros().toPlainString()} ${price.currency}",
                    ),
                )
            }
            preferences.stars.takeIf(Set<Int>::isNotEmpty)?.let { stars ->
                add(
                    ProceedWithCandidateConfirmationField(
                        key = "stars",
                        label = "звёзды",
                        value = stars.sorted().joinToString(separator = ", "),
                    ),
                )
            }
            preferences.minimumGuestRating?.let { rating ->
                add(
                    ProceedWithCandidateConfirmationField(
                        key = "min-guest-rating",
                        label = "минимальный гостевой рейтинг",
                        value = rating.value.toString(),
                    ),
                )
            }
            if (preferences.freeCancellationRequired) {
                add(
                    ProceedWithCandidateConfirmationField(
                        key = "free-cancellation",
                        label = "бесплатная отмена",
                        value = "обязательна",
                    ),
                )
            }
            if (preferences.breakfastIncludedRequired) {
                add(
                    ProceedWithCandidateConfirmationField(
                        key = "breakfast-included",
                        label = "завтрак",
                        value = "включён",
                    ),
                )
            }
            preferences.accommodationConcept?.let { concept ->
                add(
                    ProceedWithCandidateConfirmationField(
                        key = "accommodation-concept",
                        label = "тип размещения",
                        value = concept.humanReadableName(),
                    ),
                )
            }
        }

    private fun ProceedWithCandidateCriteria.Guests.humanReadableGuests(): String {
        val adultsText = adults.withPlural("взрослый", "взрослых", "взрослых")
        if (childrenAges.isEmpty()) {
            return "$adultsText, без детей"
        }

        val childrenText = children.withPlural("ребёнок", "ребёнка", "детей")
        val ages = childrenAges.map { age ->
            age.withPlural("год", "года", "лет")
        }
        return "$adultsText, $childrenText (${ages.joinNaturally()})"
    }

    private fun HotelSearchPreferences.humanReadableConditions(): List<String> =
        buildList {
            maxTotalPrice?.let { price ->
                add("до ${price.amount.formattedAmount()} ${price.currency.currencySymbol()} за всё проживание")
            }
            stars.takeIf(Set<Int>::isNotEmpty)?.let { values ->
                add(values.sorted().formattedStars())
            }
            minimumGuestRating?.let { rating ->
                add("рейтинг от ${rating.value}")
            }
            if (freeCancellationRequired) {
                add("бесплатная отмена")
            }
            if (breakfastIncludedRequired) {
                add("завтрак включён")
            }
            accommodationConcept?.let { concept ->
                add("тип размещения — ${concept.humanReadableName()}")
            }
        }

    private fun AccommodationConcept.humanReadableName(): String =
        when (this) {
            AccommodationConcept.GLAMPING -> "глемпинг"
        }

    private fun formatDateRange(checkIn: LocalDate, checkOut: LocalDate): String =
        when {
            checkIn.year == checkOut.year && checkIn.month == checkOut.month ->
                "${checkIn.dayOfMonth}–${checkOut.dayOfMonth} " +
                    "${checkIn.month.russianName()} ${checkIn.year}"

            checkIn.year == checkOut.year ->
                "${checkIn.dayOfMonth} ${checkIn.month.russianName()} — " +
                    "${checkOut.dayOfMonth} ${checkOut.month.russianName()} ${checkIn.year}"

            else ->
                "${checkIn.dayOfMonth} ${checkIn.month.russianName()} ${checkIn.year} — " +
                    "${checkOut.dayOfMonth} ${checkOut.month.russianName()} ${checkOut.year}"
        }

    private fun Month.russianName(): String =
        getDisplayName(TextStyle.FULL, RUSSIAN_LOCALE)

    private fun Int.withPlural(one: String, few: String, many: String): String {
        val value = kotlin.math.abs(this)
        val word = when {
            value % 100 in 11..14 -> many
            value % 10 == 1 -> one
            value % 10 in 2..4 -> few
            else -> many
        }
        return "$this $word"
    }

    private fun List<String>.joinNaturally(): String =
        when (size) {
            0 -> ""
            1 -> first()
            2 -> "${first()} и ${last()}"
            else -> "${dropLast(1).joinToString(separator = ", ")} и ${last()}"
        }

    private fun List<Int>.formattedStars(): String {
        val isRange = size > 1 && zipWithNext().all { (left, right) -> right == left + 1 }
        return when {
            size == 1 -> first().withPlural("звезда", "звезды", "звёзд")
            isRange -> "${first()}–${last()} звёзд"
            else -> "${map(Int::toString).joinNaturally()} звёзд"
        }
    }

    private fun BigDecimal.formattedAmount(): String {
        val symbols = DecimalFormatSymbols(RUSSIAN_LOCALE).apply {
            groupingSeparator = ' '
        }
        return DecimalFormat("#,##0.##", symbols).format(this)
    }

    private fun String.currencySymbol(): String =
        when (uppercase(Locale.ROOT)) {
            "RUB" -> "₽"
            "EUR" -> "€"
            "USD" -> "$"
            else -> uppercase(Locale.ROOT)
        }

    private companion object {
        val RUSSIAN_LOCALE: Locale = Locale.forLanguageTag("ru")
    }
}
