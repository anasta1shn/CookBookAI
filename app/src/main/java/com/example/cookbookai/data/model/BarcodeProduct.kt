package com.example.cookbookai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "barcode_products")
data class BarcodeProduct(

    @PrimaryKey
    val barcode: String,

    val name: String,

    val calories: Float,

    val proteins: Float,

    val fats: Float,

    val carbs: Float,

    val weight: Int = 100,

    @SerialName("user_id")
    val userId: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null
)
