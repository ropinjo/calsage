package com.calorietracker.presentation.screen.mealdetail

import com.calorietracker.domain.model.FoodSource

sealed interface MealDetailUiState {
    data object Loading : MealDetailUiState
    data class Success(
        val mealType: String,
        val date: String,
        val totalCalories: Int,
        val totalProtein: Float,
        val totalCarbs: Float,
        val totalFat: Float,
        val entries: List<MealDetailEntry>
    ) : MealDetailUiState
    data object Empty : MealDetailUiState
}

data class MealDetailEntry(
    val id: Long,
    val description: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val isEditing: Boolean = false,
    val source: FoodSource = FoodSource.AI
)
