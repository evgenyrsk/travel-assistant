package com.travelassistant.backend.domain.hotel

/**
 * Opaque process-local hotel search identity.
 *
 * This is not a provider identifier, persistent database key, or generated
 * client readiness claim.
 */
@JvmInline
value class HotelSearchId(val value: String)
