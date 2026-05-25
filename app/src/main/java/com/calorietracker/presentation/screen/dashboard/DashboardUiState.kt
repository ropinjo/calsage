package com.calorietracker.presentation.screen.dashboard

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(
        val selectedDate: String,
        val caloriesConsumed: Int,
        val calorieTarget: Int,
        val proteinConsumed: Float,
        val proteinTarget: Float,
        val carbsConsumed: Float,
        val carbsTarget: Float,
        val fatConsumed: Float,
        val fatTarget: Float,
        val mealSummaries: List<MealSummary>
    ) : DashboardUiState
}

data class MealSummary(
    val mealType: String,
    val totalCalories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val itemCount: Int
)
