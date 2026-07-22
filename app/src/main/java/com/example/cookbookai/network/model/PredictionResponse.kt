package com.example.cookbookai.network.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

data class PredictionResponse(

    val mode: String = "",

    @SerializedName(
        value = "dish",
        alternate = [
            "dish_name",
            "food",
            "food_name",
            "prediction",
            "predicted_class",
            "predicted_dish",
            "result",
            "label",
            "name"
        ]
    )
    val dish: String? = null,

    @SerializedName(
        value = "class_name",
        alternate = [
            "className",
            "class",
            "predicted_label",
            "predictedClass"
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
    val confidence: Float? = null,

    @SerializedName(
        value = "top_predictions",
        alternate = [
            "topPredictions",
            "top3",
            "top_3",
            "predictions",
            "top"
        ]
    )
    val topPredictions: List<TopPrediction> = emptyList(),

    val components: List<TopPrediction> = emptyList(),

    val detections: List<Detection> = emptyList()
)

class PredictionResponseDeserializer : JsonDeserializer<PredictionResponse> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): PredictionResponse {

        val item =
            json.asJsonObject

        val nestedPrediction =
            item.firstObject(
                "classification",
                "prediction",
                "result",
                "output",
                "classified"
            )

        val dish =
            item.firstNullableString(
                "dish",
                "dish_name",
                "food",
                "food_name",
                "prediction",
                "predicted_class",
                "predicted_dish",
                "result",
                "label",
                "name"
            ) ?: nestedPrediction?.firstNullableString(
                "dish",
                "dish_name",
                "food",
                "food_name",
                "prediction",
                "predicted_class",
                "predicted_dish",
                "label",
                "name"
            )

        val className =
            item.firstNullableString(
                "class_name",
                "className",
                "class",
                "predicted_label",
                "predictedClass"
            ) ?: nestedPrediction?.firstNullableString(
                "class_name",
                "className",
                "class",
                "predicted_label",
                "predictedClass",
                "label"
            )

        val topPredictions =
            item.firstArray(
                "top_predictions",
                "topPredictions",
                "top3",
                "top_3",
                "predictions",
                "top"
            )
                ?.map { prediction ->
                    context.deserialize<TopPrediction>(
                        prediction,
                        TopPrediction::class.java
                    )
                }
                ?: emptyList()

        val detections =
            item.firstArray(
                "detections",
                "objects"
            )
                ?.map { detection ->
                    context.deserialize<Detection>(
                        detection,
                        Detection::class.java
                    )
                }
                ?: emptyList()

        val components =
            item.firstArray(
                "components",
                "items",
                "foods",
                "parts"
            )
                ?.map { component ->
                    context.deserialize<TopPrediction>(
                        component,
                        TopPrediction::class.java
                    )
                }
                ?: emptyList()

        return PredictionResponse(
            mode = item.firstNullableString("mode").orEmpty(),
            dish = dish,
            className = className,
            confidence = item.firstFloatOrNull(
                "confidence",
                "score",
                "probability",
                "prob"
            ) ?: nestedPrediction?.firstFloatOrNull(
                "confidence",
                "score",
                "probability",
                "prob"
            ),
            topPredictions = topPredictions,
            components = components,
            detections = detections
        )
    }
}

internal fun JsonObject.firstNullableString(
    vararg keys: String
): String? {

    return keys
        .asSequence()
        .mapNotNull { key ->
            val value =
                get(key)
                    ?.takeIf { !it.isJsonNull }

            when {
                value == null ->
                    null

                value.isJsonPrimitive ->
                    value.asString
                        .trim()
                        .takeIf { it.isNotBlank() }

                value.isJsonObject ->
                    value.asJsonObject.firstNullableString(
                        "name",
                        "label",
                        "dish",
                        "food",
                        "food_name",
                        "class_name",
                        "class"
                    )

                else ->
                    null
            }
        }
        .firstOrNull()
}

internal fun JsonObject.firstFloatOrNull(
    vararg keys: String
): Float? {

    return keys
        .asSequence()
        .mapNotNull { key ->
            get(key)
                ?.takeIf { !it.isJsonNull && it.isJsonPrimitive }
                ?.asFloat
        }
        .firstOrNull()
}

internal fun JsonObject.firstObject(
    vararg keys: String
): JsonObject? {

    return keys
        .asSequence()
        .mapNotNull { key ->
            get(key)
                ?.takeIf { !it.isJsonNull && it.isJsonObject }
                ?.asJsonObject
        }
        .firstOrNull()
}

internal fun JsonObject.firstArray(
    vararg keys: String
) =
    keys
        .asSequence()
        .mapNotNull { key ->
            get(key)
                ?.takeIf { !it.isJsonNull && it.isJsonArray }
                ?.asJsonArray
        }
        .firstOrNull()
