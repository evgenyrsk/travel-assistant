package com.travelassistant.backend.application.assistant

class BuildProceedWithCandidateConfirmationProposalUseCase {

    operator fun invoke(
        accepted: ProceedWithCandidateValidationResult.Accepted,
    ): ProceedWithCandidateConfirmationProposal {
        val fields = accepted.criteria.confirmationFields()

        return ProceedWithCandidateConfirmationProposal(
            summary = fields.joinToString(
                prefix = "Параметры hotel search: ",
                postfix = ".",
                separator = "; ",
            ) { field ->
                "${field.label}: ${field.value}"
            },
            confirmationQuestion = "Проверить отели по этим параметрам?",
            displayFields = fields,
        )
    }

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
            add(ProceedWithCandidateConfirmationField(
                key = "rooms",
                label = "номера",
                value = rooms.toString(),
            ))
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
        }
}
