package com.travelassistant.backend.infrastructure.llm

import com.travelassistant.backend.application.llm.LlmHotelSearchPreferencesPatch
import com.travelassistant.backend.domain.hotel.AccommodationConcept
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal object OpenRouterHotelSearchPreferencesContract {
    const val promptAddition =
        "Also extract only explicit hotel-search preference changes into preferencePatch. " +
            "Use max-total-price for the total stay; when currency is omitted, leave currency null " +
            "because the application treats it as RUB. For example, 'до 80 тысяч' means amount " +
            "80000 with null currency. Use stars for exact categories from 0 to 5. " +
            "Russian phrases 'пятизвёздочный', 'пятизвездочный', and '5 звёзд' mean stars=[5]. " +
            "Use min-guest-rating only for exact thresholds 5, 6, 7, 8, or 9. " +
            "Values such as 8.5 or 10 and vague phrases such as a high rating require " +
            "NEEDS_CLARIFICATION and must not be rounded. Use free-cancellation=true only for an " +
            "explicit requirement. Use breakfast-included=true only when the user explicitly " +
            "requires included breakfast. Requests for another meal plan are unsupported and " +
            "require clarification. Use accommodation-concept=glamping only when the user " +
            "explicitly asks for glamping. No other accommodation concept is active; never " +
            "copy an unknown category or user wording into this field. Use clear for an " +
            "explicitly removed preference, including " +
            "phrases such as remove the rating restriction. Never set and clear the same field. " +
            "Existing preferences can be present in confirmedConstraints under the same canonical " +
            "keys; keep every preference that the user did not explicitly change or clear. " +
            "Preferences are optional and must never appear in missingRequiredFields. " +
            "Do not extract sorting preferences."

    fun schema(): JsonObject =
        buildJsonObject {
            put("type", "object")
            put(
                "properties",
                buildJsonObject {
                    put("max-total-price", nullableMaxTotalPriceSchema())
                    put("stars", nullableStarsSchema())
                    put("min-guest-rating", nullableMinimumGuestRatingSchema())
                    put(
                        "free-cancellation",
                        nullableBooleanSchema(FREE_CANCELLATION_DESCRIPTION),
                    )
                    put(
                        "breakfast-included",
                        nullableBooleanSchema(BREAKFAST_INCLUDED_DESCRIPTION),
                    )
                    put("accommodation-concept", nullableAccommodationConceptSchema())
                    put("clear", clearedPreferencesSchema())
                },
            )
            put(
                "required",
                stringArray(
                    "max-total-price",
                    "stars",
                    "min-guest-rating",
                    "free-cancellation",
                    "breakfast-included",
                    "accommodation-concept",
                    "clear",
                ),
            )
            put("additionalProperties", false)
        }

    @Serializable
    data class Dto(
        @SerialName("max-total-price")
        val maxTotalPrice: MaxTotalPriceDto?,
        val stars: List<Int>?,
        @SerialName("min-guest-rating")
        val minimumGuestRating: Int?,
        @SerialName("free-cancellation")
        val freeCancellationRequired: Boolean?,
        @SerialName("breakfast-included")
        val breakfastIncludedRequired: Boolean?,
        @SerialName("accommodation-concept")
        val accommodationConcept: String?,
        val clear: List<String>,
    ) {
        fun toDomainPatch(): LlmHotelSearchPreferencesPatch? {
            if (stars?.distinct()?.size != stars?.size || clear.distinct().size != clear.size) {
                return null
            }
            val clearedFields = linkedSetOf<LlmHotelSearchPreferencesPatch.Field>()
            clear.forEach { fieldName ->
                val field = LlmHotelSearchPreferencesPatch.Field.fromWireName(fieldName)
                    ?: return null
                clearedFields += field
            }
            val mappedAccommodationConcept = accommodationConcept?.let { value ->
                AccommodationConcept.fromCode(value) ?: return null
            }

            return LlmHotelSearchPreferencesPatch(
                maxTotalPrice = maxTotalPrice?.let { price ->
                    LlmHotelSearchPreferencesPatch.MaxTotalPrice(
                        amount = price.amount,
                        currency = price.currency,
                    )
                },
                stars = stars?.toCollection(linkedSetOf()),
                minimumGuestRating = minimumGuestRating,
                freeCancellationRequired = freeCancellationRequired,
                breakfastIncludedRequired = breakfastIncludedRequired,
                accommodationConcept = mappedAccommodationConcept,
                clear = clearedFields,
            )
        }
    }

    @Serializable
    data class MaxTotalPriceDto(
        val amount: String,
        val currency: String?,
    )

    private fun nullableMaxTotalPriceSchema(): JsonObject =
        buildJsonObject {
            put("type", stringArray("object", "null"))
            put("description", MAX_TOTAL_PRICE_DESCRIPTION)
            put(
                "properties",
                buildJsonObject {
                    put("amount", stringSchema(MAX_TOTAL_PRICE_AMOUNT_DESCRIPTION))
                    put("currency", nullableStringSchema(MAX_TOTAL_PRICE_CURRENCY_DESCRIPTION))
                },
            )
            put("required", stringArray("amount", "currency"))
            put("additionalProperties", false)
        }

    private fun nullableStarsSchema(): JsonObject =
        buildJsonObject {
            put("type", stringArray("array", "null"))
            put("description", STARS_DESCRIPTION)
            put("uniqueItems", true)
            put(
                "items",
                buildJsonObject {
                    put("type", "integer")
                    put(
                        "enum",
                        buildJsonArray {
                            (MIN_STARS..MAX_STARS).forEach(::add)
                        },
                    )
                },
            )
        }

    private fun nullableMinimumGuestRatingSchema(): JsonObject =
        buildJsonObject {
            put("type", stringArray("integer", "null"))
            put("description", MINIMUM_GUEST_RATING_DESCRIPTION)
            put(
                "enum",
                buildJsonArray {
                    SUPPORTED_GUEST_RATINGS.forEach(::add)
                    add(JsonNull)
                },
            )
        }

    private fun nullableBooleanSchema(description: String): JsonObject =
        buildJsonObject {
            put("type", stringArray("boolean", "null"))
            put("description", description)
        }

    private fun nullableAccommodationConceptSchema(): JsonObject =
        buildJsonObject {
            put("type", stringArray("string", "null"))
            put("description", ACCOMMODATION_CONCEPT_DESCRIPTION)
            put(
                "enum",
                buildJsonArray {
                    AccommodationConcept.entries.forEach { concept -> add(concept.code) }
                    add(JsonNull)
                },
            )
        }

    private fun clearedPreferencesSchema(): JsonObject =
        buildJsonObject {
            put("type", "array")
            put("description", CLEARED_PREFERENCES_DESCRIPTION)
            put("uniqueItems", true)
            put(
                "items",
                buildJsonObject {
                    put("type", "string")
                    put(
                        "enum",
                        buildJsonArray {
                            LlmHotelSearchPreferencesPatch.Field.entries.forEach { field ->
                                add(field.wireName)
                            }
                        },
                    )
                },
            )
        }

    private fun nullableStringSchema(description: String): JsonObject =
        buildJsonObject {
            put("type", stringArray("string", "null"))
            put("description", description)
        }

    private fun stringSchema(description: String): JsonObject =
        buildJsonObject {
            put("type", "string")
            put("description", description)
        }

    private fun stringArray(vararg values: String) =
        buildJsonArray {
            values.forEach(::add)
        }

    private const val MIN_STARS = 0
    private const val MAX_STARS = 5
    private val SUPPORTED_GUEST_RATINGS = 5..9
    private const val MAX_TOTAL_PRICE_DESCRIPTION =
        "Explicit maximum total price for the complete stay, or null when not changed."
    private const val MAX_TOTAL_PRICE_AMOUNT_DESCRIPTION =
        "Positive decimal amount exactly as understood from the user."
    private const val MAX_TOTAL_PRICE_CURRENCY_DESCRIPTION =
        "Currency code, or null when the user omitted currency; omitted currency means RUB."
    private const val STARS_DESCRIPTION =
        "Explicit non-empty set of hotel star categories from 0 to 5, or null when not changed."
    private const val MINIMUM_GUEST_RATING_DESCRIPTION =
        "Exact supported guest rating threshold: 5, 6, 7, 8, or 9; null when not changed."
    private const val FREE_CANCELLATION_DESCRIPTION =
        "True only when free cancellation is explicitly required; use clear to remove it."
    private const val BREAKFAST_INCLUDED_DESCRIPTION =
        "True only when included breakfast is explicitly required; use clear to remove it."
    private const val ACCOMMODATION_CONCEPT_DESCRIPTION =
        "Managed accommodation concept glamping, or null when not changed."
    private const val CLEARED_PREFERENCES_DESCRIPTION =
        "Preference keys that the user explicitly asked to remove; empty when none are removed."
}
