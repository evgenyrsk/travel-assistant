package com.travelassistant.backend.infrastructure.provider

import kotlinx.serialization.Serializable

@Serializable
internal data class HotelsApiHotelRatesResponseDto(
    val payload: Payload,
) {
    @Serializable
    data class Payload(
        val rates: List<Rate>,
        val rooms: List<Room>,
    )

    @Serializable
    data class Rate(
        val availableRoomsCount: Int,
        val cancellationPolicyRules: CancellationPolicyRules? = null,
        val mealName: String? = null,
        val mealType: String? = null,
        val paymentPlace: String,
        val roomId: String,
        val shownPrice: Money,
    )

    @Serializable
    data class CancellationPolicyRules(
        val freeCancellationUntil: String? = null,
    )

    @Serializable
    data class Money(
        val amount: Double,
        val currency: String,
    )

    @Serializable
    data class Room(
        val roomId: String,
        val roomName: String,
        val images: List<Image>? = null,
    )

    @Serializable
    data class Image(
        val url: String,
    )
}
