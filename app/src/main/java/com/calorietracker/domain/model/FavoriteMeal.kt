package com.calorietracker.domain.model

data class FavoriteMeal(
    val id: Long = 0,
    val name: String,
    val description: String,
    val totalCalories: Int,
    val totalProtein: Float,
    val totalCarbs: Float,
    val totalFat: Float,
    val items: List<NutritionItem> = emptyList(),
    val mealType: MealType,
    val source: FoodSource = FoodSource.AI
)
