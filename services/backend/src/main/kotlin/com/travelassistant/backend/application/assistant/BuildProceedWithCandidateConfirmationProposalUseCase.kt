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
        listOfNotNull(
            destination.trim().takeIf(String::isNotBlank)?.let { value ->
                ProceedWithCandidateConfirmationField(
                    key = "destination",
                    label = "направление",
                    value = value,
                )
            },
            ProceedWithCandidateConfirmationField(
                key = "check-in",
                label = "заезд",
                value = checkInDate.toString(),
            ),
            ProceedWithCandidateConfirmationField(
                key = "check-out",
                label = "выезд",
                value = checkOutDate.toString(),
            ),
            ProceedWithCandidateConfirmationField(
                key = "adults",
                label = "взрослые",
                value = guests.adults.toString(),
            ),
            ProceedWithCandidateConfirmationField(
                key = "children",
                label = "дети",
                value = guests.children.toString(),
            ),
            ProceedWithCandidateConfirmationField(
                key = "rooms",
                label = "номера",
                value = rooms.toString(),
            ),
        )
}
