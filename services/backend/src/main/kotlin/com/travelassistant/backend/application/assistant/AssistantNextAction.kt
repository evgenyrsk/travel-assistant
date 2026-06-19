package com.travelassistant.backend.application.assistant

enum class AssistantNextAction(val apiValue: String) {
    ASK_CLARIFICATION("ask_clarification"),
    SHOW_HOTEL_RESULTS("show_hotel_results"),
    SHOW_BOUNDARY_MESSAGE("show_boundary_message"),
}
