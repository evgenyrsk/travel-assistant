package com.travelassistant.backend.infrastructure.provider

import java.math.BigDecimal
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable(with = HotelsApiSearchFilterDto.Serializer::class)
internal sealed interface HotelsApiSearchFilterDto {
    val filterId: String

    data class Range(
        override val filterId: String,
        val min: BigDecimal,
        val max: BigDecimal,
    ) : HotelsApiSearchFilterDto

    data class StringArray(
        override val filterId: String,
        val values: List<String>,
    ) : HotelsApiSearchFilterDto

    data class Radio(
        override val filterId: String,
        val value: String,
    ) : HotelsApiSearchFilterDto

    data class BooleanValue(
        override val filterId: String,
        val value: Boolean,
    ) : HotelsApiSearchFilterDto

    @OptIn(ExperimentalSerializationApi::class)
    object Serializer : KSerializer<HotelsApiSearchFilterDto> {
        override val descriptor: SerialDescriptor =
            buildClassSerialDescriptor("HotelsApiSearchFilterDto")

        override fun serialize(encoder: Encoder, value: HotelsApiSearchFilterDto) {
            val jsonEncoder = encoder as? JsonEncoder
                ?: throw SerializationException("Hotels API filter requires JSON encoding")
            jsonEncoder.encodeJsonElement(value.toJsonObject())
        }

        override fun deserialize(decoder: Decoder): HotelsApiSearchFilterDto {
            val jsonDecoder = decoder as? JsonDecoder
                ?: throw SerializationException("Hotels API filter requires JSON decoding")
            val filter = jsonDecoder.decodeJsonElement().jsonObject
            val filterId = filter.requiredString(FILTER_ID)

            return when (filter.requiredString(OBJECT_TYPE)) {
                RANGE -> Range(
                    filterId = filterId,
                    min = filter.requiredDecimal(MIN),
                    max = filter.requiredDecimal(MAX),
                )

                ARRAY -> StringArray(
                    filterId = filterId,
                    values = filter.getValue(VALUES).jsonArray.map { value ->
                        value.jsonPrimitive.content
                    },
                )

                RADIO -> Radio(
                    filterId = filterId,
                    value = filter.requiredString(VALUE),
                )

                BOOLEAN -> BooleanValue(
                    filterId = filterId,
                    value = filter.getValue(VALUE).jsonPrimitive.boolean,
                )

                else -> throw SerializationException("Unsupported Hotels API filter type")
            }
        }

        private fun HotelsApiSearchFilterDto.toJsonObject(): JsonObject =
            buildJsonObject {
                put(OBJECT_TYPE, objectType())
                put(FILTER_ID, filterId)
                when (this@toJsonObject) {
                    is Range -> {
                        put(MIN, JsonUnquotedLiteral(min.toPlainString()))
                        put(MAX, JsonUnquotedLiteral(max.toPlainString()))
                    }

                    is StringArray -> put(
                        VALUES,
                        kotlinx.serialization.json.buildJsonArray {
                            values.forEach { value -> add(JsonPrimitive(value)) }
                        },
                    )

                    is Radio -> put(VALUE, value)
                    is BooleanValue -> put(VALUE, value)
                }
            }

        private fun HotelsApiSearchFilterDto.objectType(): String =
            when (this) {
                is Range -> RANGE
                is StringArray -> ARRAY
                is Radio -> RADIO
                is BooleanValue -> BOOLEAN
            }

        private fun JsonObject.requiredString(key: String): String =
            this[key]?.jsonPrimitive?.content
                ?: throw SerializationException("Hotels API filter field is missing")

        private fun JsonObject.requiredDecimal(key: String): BigDecimal =
            requiredString(key).toBigDecimalOrNull()
                ?: throw SerializationException("Hotels API filter number is invalid")

        private const val OBJECT_TYPE = "\$objectType"
        private const val FILTER_ID = "filterId"
        private const val MIN = "min"
        private const val MAX = "max"
        private const val VALUES = "values"
        private const val VALUE = "value"
        private const val RANGE = "range"
        private const val ARRAY = "array"
        private const val RADIO = "radio"
        private const val BOOLEAN = "boolean"
    }
}
