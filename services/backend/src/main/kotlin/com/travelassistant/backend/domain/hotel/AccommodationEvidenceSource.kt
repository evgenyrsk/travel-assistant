package com.travelassistant.backend.domain.hotel

enum class AccommodationEvidenceSource(
    val apiValue: String,
) {
    NAME("name"),
    DESCRIPTION("description"),
    AMENITIES("amenities"),
    IMAGE("image"),
}
