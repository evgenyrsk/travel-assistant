package com.travelassistant.backend.domain.hotel

enum class AccommodationMatchVerdict(
    val apiValue: String,
) {
    MATCH("match"),
    PROBABLE("probable"),
    NO_MATCH("no_match"),
    UNKNOWN("unknown"),
}
