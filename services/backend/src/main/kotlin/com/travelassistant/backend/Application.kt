package com.travelassistant.backend

import com.travelassistant.backend.api.configureApiRoutes
import com.travelassistant.backend.api.configureErrorHandling
import com.travelassistant.backend.api.configureSerialization
import com.travelassistant.backend.application.assistant.AssistantHotelSearchHandoffUseCase
import com.travelassistant.backend.application.assistant.CreateAssistantSessionUseCase
import com.travelassistant.backend.application.assistant.InMemoryAssistantSessionStateStore
import com.travelassistant.backend.application.hotel.CreateHotelSearchUseCase
import com.travelassistant.backend.application.hotel.InMemoryHotelSearchStateStore
import com.travelassistant.backend.infrastructure.provider.FakeHotelOfferProvider
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    embeddedServer(
        factory = Netty,
        host = "0.0.0.0",
        port = System.getenv("PORT")?.toIntOrNull() ?: 8080,
        module = Application::module,
    ).start(wait = true)
}

fun Application.module() {
    val assistantSessionStateStore = InMemoryAssistantSessionStateStore()
    val hotelSearchBoundary = CreateHotelSearchUseCase(
        assistantSessionStateStore = assistantSessionStateStore,
        hotelOfferProvider = FakeHotelOfferProvider(),
        hotelSearchStateStore = InMemoryHotelSearchStateStore(),
    )
    val assistantSessionBoundary = AssistantHotelSearchHandoffUseCase(
        assistantSessionBoundary = CreateAssistantSessionUseCase(
            sessionStateStore = assistantSessionStateStore,
        ),
        hotelSearchBoundary = hotelSearchBoundary,
    )

    configureSerialization()
    configureErrorHandling()
    configureApiRoutes(
        assistantSessionBoundary = assistantSessionBoundary,
        hotelSearchBoundary = hotelSearchBoundary,
    )
}
