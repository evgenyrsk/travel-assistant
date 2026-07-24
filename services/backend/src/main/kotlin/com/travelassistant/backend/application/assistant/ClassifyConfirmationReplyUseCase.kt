package com.travelassistant.backend.application.assistant

class ClassifyConfirmationReplyUseCase {
    operator fun invoke(replyText: String): ConfirmationReplyClassification {
        val normalizedReply = replyText.normalizedForConfirmationClassification()

        if (normalizedReply.isBlank()) {
            return ConfirmationReplyClassification.Unknown
        }

        if (normalizedReply.hasCorrectionSignal()) {
            return ConfirmationReplyClassification.Correction
        }

        return when (normalizedReply) {
            in explicitPositiveReplies ->
                ConfirmationReplyClassification.ExplicitPositive

            in negativeReplies ->
                ConfirmationReplyClassification.Negative

            in ambiguousReplies ->
                ConfirmationReplyClassification.Ambiguous

            else ->
                ConfirmationReplyClassification.Unknown
        }
    }

    private fun String.normalizedForConfirmationClassification(): String =
        trim()
            .lowercase()
            .replace('ё', 'е')
            .replace(Regex("[,.!?:;]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun String.hasCorrectionSignal(): Boolean =
        correctionMarkers.any { marker -> contains(marker) } ||
            guestOrRoomChangePattern.containsMatchIn(this) ||
            dateChangePattern.containsMatchIn(this)

    private companion object {
        val explicitPositiveReplies = setOf(
            "да",
            "да ищи",
            "да проверь отели",
            "подтверждаю",
            "все верно",
            "yes",
            "confirm",
            "looks good",
            "ок ищи",
        )

        val negativeReplies = setOf(
            "нет",
            "не надо",
            "нет не надо",
            "cancel",
            "stop",
            "no",
        )

        val ambiguousReplies = setOf(
            "ок",
            "угу",
            "давай",
            "go",
            "maybe",
            "ok",
        )

        val correctionMarkers = listOf(
            "лучше",
            "измени",
            "изменить",
            "поменяй",
            "замени",
            "другая",
            "другой",
            "другие",
            "instead",
            "change",
        )

        val guestOrRoomChangePattern =
            Regex(
                pattern = "(?U)\\b(" +
                    "(один|одна|одно|два|две|три|четыре|\\d+)\\s*" +
                    "(взросл\\S*|adult\\S*|гост\\S*|guest\\S*|дет\\S*|child\\S*|children|комнат\\S*|room\\S*|номер\\S*)|" +
                    "для\\s+(одного|двоих|троих|четверых)" +
                    ")\\b",
            )

        val dateChangePattern =
            Regex(
                pattern = "\\b(" +
                    "\\d{4}-\\d{2}-\\d{2}|" +
                    "\\d{1,2}\\s*(по|-|–)\\s*\\d{1,2}|" +
                    "январ\\S*|феврал\\S*|март\\S*|апрел\\S*|мая|май|июн\\S*|июл\\S*|" +
                    "август\\S*|сентябр\\S*|октябр\\S*|ноябр\\S*|декабр\\S*" +
                    ")\\b",
            )
    }
}
