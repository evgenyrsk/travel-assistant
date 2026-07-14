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
        }
}
