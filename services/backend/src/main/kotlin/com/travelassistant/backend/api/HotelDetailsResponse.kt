package com.travelassistant.backend.api

import com.travelassistant.backend.domain.hotel.HotelDetails
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class HotelDetailsResponse(
    val hotelName: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val hotelChain: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val starRating: Int? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val location: Location? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val descriptionSections: List<DescriptionSection>? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val imageUrls: List<String>? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val amenityGroups: List<AmenityGroup>? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val checkInTime: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val checkOutTime: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val paymentMethods: List<String>? = null,
    val metadata: Metadata,
) {
    @Serializable
    data class Location(
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val address: String? = null,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val coordinates: Coordinates? = null,
    )

    @Serializable
    data class Coordinates(
        val latitude: Double,
        val longitude: Double,
    )

    @Serializable
    data class DescriptionSection(
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val title: String? = null,
        val paragraphs: List<String>,
    )

    @Serializable
    data class AmenityGroup(
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val name: String? = null,
        val amenities: List<String>,
    )

    @Serializable
    data class Metadata(
        val source: String,
        val freshness: String,
    )

    companion object {
        fun from(details: HotelDetails): HotelDetailsResponse =
            HotelDetailsResponse(
                hotelName = details.hotelName,
                hotelChain = details.hotelChain,
                starRating = details.starRating,
                location = details.location?.let { location ->
                    Location(
                        address = location.address,
                        coordinates = location.coordinates?.let { coordinates ->
                            Coordinates(
                                latitude = coordinates.latitude,
                                longitude = coordinates.longitude,
                            )
                        },
                    )
                },
                descriptionSections = details.descriptionSections?.map { section ->
                    DescriptionSection(
                        title = section.title,
                        paragraphs = section.paragraphs,
                    )
                },
                imageUrls = details.imageUrls,
                amenityGroups = details.amenityGroups?.map { group ->
                    AmenityGroup(
                        name = group.name,
                        amenities = group.amenities,
                    )
                },
                checkInTime = details.checkInTime?.toString(),
                checkOutTime = details.checkOutTime?.toString(),
                paymentMethods = details.paymentMethods?.map { it.apiValue },
                metadata = Metadata(
                    source = details.source.apiValue,
                    freshness = details.freshness.apiValue,
                ),
            )
    }
}
