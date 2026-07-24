package com.travelassistant.backend.application.assistant

internal const val SINGLE_ROOM_ONLY_CLARIFICATION_MESSAGE =
    "Сейчас я могу искать только один номер за раз. " +
        "Укажите состав гостей для одного номера или выполните отдельный поиск для второго номера."

class AssistantHotelClarificationPolicy {

    fun questionFor(missingFields: Collection<String>): String? {
        val missing = missingFields.toSet()
        return when {
            AssistantHotelConstraintField.CHILDREN_AGES.key in missing ->
                "Укажите возраст каждого ребёнка (от 0 до 17 лет)."

            AssistantHotelConstraintField.CHILDREN.key in missing ->
                "Укажите количество детей."

            AssistantHotelConstraintField.STAY_LENGTH_NIGHTS.key in missing ->
                "Уточните длительность проживания в ночах."

            AssistantHotelConstraintField.CHECK_IN.key in missing &&
                AssistantHotelConstraintField.CHECK_OUT.key in missing ->
                "Уточните точные даты заезда и выезда."

            AssistantHotelConstraintField.CHECK_IN.key in missing ->
                "Уточните точную дату заезда."

            AssistantHotelConstraintField.CHECK_OUT.key in missing ->
                "Уточните точную дату выезда."

            AssistantHotelConstraintField.DESTINATION.key in missing ->
                "Уточните город, район или конкретный отель."

            AssistantHotelConstraintField.ADULTS.key in missing ->
                "Уточните количество взрослых."

            AssistantHotelConstraintField.ROOMS.key in missing ->
                SINGLE_ROOM_ONLY_CLARIFICATION_MESSAGE

            else -> null
        }
    }
}
