package com.travelassistant.backend.application.assistant

import java.text.Normalizer
import java.util.Locale

class BookingRequestBoundaryPolicy {

    fun isBookingRequested(message: String): Boolean =
        BOOKING_EXPRESSION.containsMatchIn(
            Normalizer.normalize(message, Normalizer.Form.NFKC)
                .lowercase(Locale.ROOT)
                .replace('ё', 'е'),
        )

    private companion object {
        val BOOKING_EXPRESSION = Regex(
            """(?<![\p{L}\p{N}])(?:заброниров[\p{L}]*|брониров[\p{L}]*|бронь|book)(?![\p{L}\p{N}])""",
        )
    }
}
