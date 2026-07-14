package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.assistant.CreateAssistantSessionUseCase
import com.travelassistant.backend.application.assistant.InMemoryAssistantSessionStateStore
import com.travelassistant.backend.application.hotel.CreateHotelSearchCommand
import com.travelassistant.backend.application.hotel.CreateHotelSearchUseCase
import com.travelassistant.backend.application.hotel.InMemoryHotelSearchStateStore
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import com.travelassistant.backend.domain.provider.HotelOfferProviderBoundary
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class HotelProviderExceptionTest {

    @Test
    fun preservesCategoryAndMessage() {
        val exception = HotelProviderException(
            category = HotelProviderErrorCategory.UNAVAILABLE,
            message = "Provider is down",
        )

        assertEquals(HotelProviderErrorCategory.UNAVAILABLE, exception.category)
        assertEquals("Provider is down", exception.message)
        assertNull(exception.cause)
    }

    @Test
    fun preservesCategoryMessageAndCause() {
        val cause = RuntimeException("connection refused")
        val exception = HotelProviderException(
            category = HotelProviderErrorCategory.TIMEOUT,
            message = "Provider timed out",
            cause = cause,
        )

        assertEquals(HotelProviderErrorCategory.TIMEOUT, exception.category)
        assertEquals("Provider timed out", exception.message)
        assertSame(cause, exception.cause)
    }

    @Test
    fun isRuntimeException() {
        val exception = HotelProviderException(
            category = HotelProviderErrorCategory.UNKNOWN,
            message = "Unknown error",
        )

        assertIs<RuntimeException>(exception)
    }

    @Test
    fun providerExceptionPropagatesThroughCreateHotelSearchUseCase() {
        val sessionStore = InMemoryAssistantSessionStateStore()
        val session = CreateAssistantSessionUseCase(
            sessionStateStore = sessionStore,
        ).createSession()
        val providerException = HotelProviderException(
            category = HotelProviderErrorCategory.RATE_LIMITED,
            message = "Rate limit exceeded",
        )
        val throwingProvider = HotelOfferProviderBoundary {
            throw providerException
        }
        val useCase = CreateHotelSearchUseCase(
            assistantSessionStateStore = sessionStore,
            hotelOfferProvider = throwingProvider,
            hotelSearchStateStore = InMemoryHotelSearchStateStore(),
        )

        val thrown = assertFailsWith<HotelProviderException> {
            useCase.createSearch(
                CreateHotelSearchCommand(
                    sessionId = session.id,
                    criteria = HotelSearchCriteria(
                        destination = "Rome",
                        checkInDate = LocalDate.parse("2026-07-01"),
                        checkOutDate = LocalDate.parse("2026-07-04"),
                        guests = HotelSearchCriteria.Guests(adults = 2),
                        rooms = 1,
                    ),
                ),
            )
        }

        assertEquals(HotelProviderErrorCategory.RATE_LIMITED, thrown.category)
        assertEquals("Rate limit exceeded", thrown.message)
    }

    @Test
    fun allCategoriesCanBeUsedInException() {
        HotelProviderErrorCategory.entries.forEach { category ->
            val exception = HotelProviderException(
                category = category,
                message = "Test: ${category.name}",
            )
            assertEquals(category, exception.category)
        }
    }
}
