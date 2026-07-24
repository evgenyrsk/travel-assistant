package com.travelassistant.backend.domain.hotel

enum class AccommodationConcept(
    val code: String,
) {
    GLAMPING("glamping"),
    ;

    companion object {
        fun fromCode(value: String): AccommodationConcept? =
            entries.firstOrNull { concept -> concept.code == value }
    }
}
