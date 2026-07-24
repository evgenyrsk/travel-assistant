package com.travelassistant.backend.application.assistant

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ClassifyConfirmationReplyUseCaseTest {

    private val useCase = ClassifyConfirmationReplyUseCase()

    @Test
    fun classifiesExplicitPositiveReplies() {
        listOf(
            "да",
            "Да, ищи!",
            "подтверждаю",
            "всё верно",
            "yes",
            "confirm",
            "looks good",
            "ОК, ищи",
            "да, проверь отели",
        ).forEach { reply ->
            assertEquals(
                ConfirmationReplyClassification.ExplicitPositive,
                useCase(reply),
                "Expected explicit positive for: $reply",
            )
        }
    }

    @Test
    fun classifiesNegativeReplies() {
        listOf(
            "нет",
            "не надо",
            "cancel",
            "stop",
            "no",
        ).forEach { reply ->
            assertEquals(
                ConfirmationReplyClassification.Negative,
                useCase(reply),
                "Expected negative for: $reply",
            )
        }
    }

    @Test
    fun classifiesCorrectionReplies() {
        listOf(
            "лучше Париж",
            "нет, лучше Париж",
            "измени даты",
            "2 взрослых",
            "Давай два номера: в одном двое, во втором один",
            "Тогда один номер на троих",
            "с 10 по 15 июля",
            "change destination to Paris",
        ).forEach { reply ->
            assertEquals(
                ConfirmationReplyClassification.Correction,
                useCase(reply),
                "Expected correction for: $reply",
            )
        }
    }

    @Test
    fun classifiesAmbiguousReplies() {
        listOf(
            "ок",
            "угу",
            "maybe",
            "давай",
            "go",
        ).forEach { reply ->
            assertEquals(
                ConfirmationReplyClassification.Ambiguous,
                useCase(reply),
                "Expected ambiguous for: $reply",
            )
        }
    }

    @Test
    fun classifiesUnknownOrUnrelatedReplies() {
        listOf(
            "",
            "   ",
            "расскажи про музеи рядом",
            "I am still thinking about it",
        ).forEach { reply ->
            assertEquals(
                ConfirmationReplyClassification.Unknown,
                useCase(reply),
                "Expected unknown for: $reply",
            )
        }
    }

    @Test
    fun normalizesWhitespaceCaseAndPunctuationDeterministically() {
        assertEquals(ConfirmationReplyClassification.ExplicitPositive, useCase("  YES  "))
        assertEquals(ConfirmationReplyClassification.ExplicitPositive, useCase("Да,   ищи."))
        assertEquals(ConfirmationReplyClassification.ExplicitPositive, useCase("Всё верно!"))
        assertEquals(ConfirmationReplyClassification.Negative, useCase("  NO. "))
        assertEquals(ConfirmationReplyClassification.Correction, useCase("  2   взрослых "))
    }

    @Test
    fun classifiesReplyWithoutActivePendingStateInput() {
        assertEquals(
            ConfirmationReplyClassification.ExplicitPositive,
            ClassifyConfirmationReplyUseCase()("да"),
        )
    }

    @Test
    fun doesNotCreateHotelSearchOrHotelSearchId() {
        val classification = useCase("да")
        val classificationText = classification.toString()

        assertFalse(classificationText.contains("hotelSearchId"))
        assertFalse(classificationText.contains("show_hotel_results"))
        assertFalse(classificationText.contains("Hotel search created"))
    }

    @Test
    fun classifiesLocallyWithoutExternalDependencyInput() {
        val localUseCase = ClassifyConfirmationReplyUseCase()

        assertEquals(
            ConfirmationReplyClassification.ExplicitPositive,
            localUseCase("confirm"),
        )
    }
}
