package com.travelassistant.backend.domain.hotel

data class AccommodationSemanticMatch(
    val concept: AccommodationConcept,
    val verdict: AccommodationMatchVerdict,
    val evidenceSources: Set<AccommodationEvidenceSource>,
)
