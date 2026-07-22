package com.example.cookbookai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "recipes")
data class Recipe(

    @PrimaryKey
    val id: String,   // главный ключ
    val title: String,
    val description: String,
    val ingredients: String = "",
    val calories: Int = 0,
    val proteins: Int = 0,
    val fats: Int = 0,
    val carbs: Int = 0,
    val imageUri: String? = null,
    val isFavorite: Boolean = false,
    val category: String = "Без категории",
    val cookingTime: Int = 0,

    @SerialName("created_at")
    val createdAt: String? = null,

    val updatedAt: Long = System.currentTimeMillis()
)
