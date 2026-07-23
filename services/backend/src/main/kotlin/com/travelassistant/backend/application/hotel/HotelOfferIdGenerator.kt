package com.travelassistant.backend.application.hotel

fun interface HotelOfferIdGenerator {
    fun nextId(): String
}
