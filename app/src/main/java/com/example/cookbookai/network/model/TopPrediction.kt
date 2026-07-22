package com.example.cookbookai.network.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

data class TopPrediction(

    @SerializedName(
        value = "name",
        alternate = [
            "label",
            "class_name",
            "class",
            "dish",
            "food",
            "food_name",
            "prediction"
        ]
    )
    val name: String = "",

    @SerializedName(
        value = "class_name",
        alternate = [
            "className",
            "class",
            "label"
        ]
    )
    val className: String? = null,

    @SerializedName(
        value = "confidence",
        alternate = [
            "score",
            "probability",
            "prob"
        ]
    )
    val confidence: Float = 0f
)

class TopPredictionDeserializer : JsonDeserializer<TopPrediction> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): TopPrediction {

        if (json.isJsonPrimitive) {
            return TopPrediction(
                name = json.asString
            )
        }

        if (json.isJsonArray) {
            val array =
                json.asJsonArray

            return TopPrediction(
                name = if (
                    array.size() > 0 &&
                    array[0].isJsonPrimitive
                ) {
                    array[0].asString
                } else {
                    ""
                },
                confidence = if (
                    array.size() > 1 &&
                    array[1].isJsonPrimitive
                ) {
                    array[1].asFloat
                } else {
                    null
                }
                    ?: 0f
            )
        }

        val item =
            json.asJsonObject

        return TopPrediction(
            name = item.firstStringValue(
                "name",
                "label",
                "class_name",
                "class",
                "dish",
                "food",
                "food_name",
                "prediction"
            ),
            className = item.firstStringValueOrNull(
                "class_name",
                "className",
                "class",
                "label"
            ),
            confidence = item.firstFloat(
                "confidence",
                "score",
                "probability",
                "prob"
            )
        )
    }

    private fun JsonObject.firstStringValue(
        vararg keys: String
    ): String {

        return firstStringValueOrNull(
            *keys
        ) ?: ""
    }

    private fun JsonObject.firstStringValueOrNull(
        vararg keys: String
    ): String? {

        return firstNullableString(
            *keys
        )
    }

    private fun JsonObject.firstFloat(
        vararg keys: String
    ): Float {

        return firstFloatOrNull(
            *keys
        )
            ?: 0f
    }
}
