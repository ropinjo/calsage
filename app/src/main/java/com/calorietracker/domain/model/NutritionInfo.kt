package com.calorietracker.domain.model

data class NutritionInfo(
    val calories: Int = 0,
    val proteinGrams: Float = 0f,
    val carbsGrams: Float = 0f,
    val fatGrams: Float = 0f,
    val items: List<NutritionItem> = emptyList(),
    val error: String? = null
)

data class NutritionItem(
    val name: String,
    val amount: String,
    val calories: Int,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float,
    val grams: Float? = null,
    val per100g: NutritionPer100g? = null,
    val caloriesRecomputed: Boolean = false
)

data class NutritionPer100g(
    val calories: Float,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float
)
