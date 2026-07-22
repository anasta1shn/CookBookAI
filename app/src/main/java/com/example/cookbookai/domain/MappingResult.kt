package com.example.cookbookai.domain

data class MappingResult(

    val foodName: String = "",

    val needUserChoice: Boolean = false,

    val options: List<String> = emptyList(),

    val detectedClasses: List<String> = emptyList(),

    val step: SelectionStep? = null
)