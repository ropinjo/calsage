package com.calorietracker.domain.model

data class FoodEntry(
    val id: Long = 0,
    val date: String,
    val mealType: MealType,
    val description: String,
    val nutritionInfo: NutritionInfo,
    val timestamp: Long,
    val source: FoodSource = FoodSource.AI
)
