package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.domain.hotel.HotelDetails
import java.text.Normalizer
import java.util.Locale

internal object HotelsApiHotelDetailsDescriptionPolicy {

    fun filter(
        sections: List<HotelsApiHotelDetailsResponseDto.DescriptionSection>?,
    ): List<HotelDetails.DescriptionSection>? =
        sections
            ?.mapNotNull(::filterSection)
            ?.takeIf(List<HotelDetails.DescriptionSection>::isNotEmpty)

    private fun filterSection(
        section: HotelsApiHotelDetailsResponseDto.DescriptionSection,
    ): HotelDetails.DescriptionSection? {
        val title = section.title.normalizedOrNull()
        if (title != null && title.normalizedKey() !in ALLOWED_TITLES) {
            return null
        }

        val paragraphs = section.paragraphs
            .mapNotNull { paragraph -> paragraph.normalizedOrNull() }
            .filterNot(::containsRestrictedData)
        if (paragraphs.isEmpty()) {
            return null
        }

        return HotelDetails.DescriptionSection(
            title = title,
            paragraphs = paragraphs,
        )
    }

    private fun containsRestrictedData(value: String): Boolean {
        val normalized = value.normalizedKey()
        return RESTRICTED_MARKERS.any(normalized::contains) ||
            URL_PATTERN.containsMatchIn(value) ||
            EMAIL_PATTERN.containsMatchIn(value) ||
            PHONE_PATTERN.containsMatchIn(value)
    }

    private fun String?.normalizedOrNull(): String? =
        this?.trim()?.takeIf(String::isNotEmpty)

    private fun String.normalizedKey(): String =
        Normalizer.normalize(trim(), Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace('ё', 'е')
            .replace(Regex("\\s+"), " ")

    private val ALLOWED_TITLES = setOf(
        "описание",
        "об отеле",
        "важная информация",
        "description",
        "about hotel",
        "important information",
    )
    private val RESTRICTED_MARKERS = setOf(
        "сертификац",
        "реестров",
        "данные о владельце",
        "владелец",
        "инн",
        "огрн",
        "кпп",
        "контакт",
        "телефон",
        "certification",
        "registry",
        "register number",
        "owner data",
        "owner:",
        "contact",
        "phone",
        "tax id",
    )
    private val URL_PATTERN = Regex("(?i)\\b(?:https?://|www\\.)\\S+")
    private val EMAIL_PATTERN = Regex("(?i)\\b[\\w.%+-]+@[\\w.-]+\\.[a-z]{2,}\\b")
    private val PHONE_PATTERN = Regex("(?:\\+?\\d[\\d ()-]{6,}\\d)")
}
