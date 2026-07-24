package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.hotel.HotelDetails

interface HotelDetailsCache {
    fun find(providerReference: String): HotelDetails?

    fun save(providerReference: String, details: HotelDetails)

    companion object {
        val NONE = object : HotelDetailsCache {
            override fun find(providerReference: String): HotelDetails? = null

            override fun save(providerReference: String, details: HotelDetails) = Unit
        }
    }
}
