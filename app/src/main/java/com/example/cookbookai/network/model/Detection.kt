package com.example.cookbookai.network.model

import com.google.gson.annotations.SerializedName

data class Detection(
    @SerializedName(
        value = "class",
        alternate = [
            "class_name",
            "label",
            "name"
        ]
    )
    val `class`: String = "",

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
