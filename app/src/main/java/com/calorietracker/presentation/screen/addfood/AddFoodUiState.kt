package com.calorietracker.presentation.screen.addfood

import com.calorietracker.domain.model.FoodSource

sealed interface AddFoodUiState {
    data object Idle : AddFoodUiState
    data object Analyzing : AddFoodUiState
    data class Result(
        val description: String,
        val totalCalories: Int,
        val totalProtein: Float,
        val totalCarbs: Float,
        val totalFat: Float,
        val items: List<FoodItemResult>,
        val isCached: Boolean = false,
        val saveAsFavorite: Boolean = false,
        val favoriteName: String = "",
        val isAlreadyFavorite: Boolean = false,
        val source: FoodSource = FoodSource.AI
    ) : AddFoodUiState
    data class ManualEntry(
        val name: String = "",
        val calories: String = "",
        val protein: String = "",
        val carbs: String = "",
        val fat: String = ""
    ) : AddFoodUiState
    data class Error(val message: String) : AddFoodUiState
}

object ManualEntryValidator {
    const val MAX_CALORIES = 5000
    const val MAX_MACRO_GRAMS = 500f

    fun calorieError(value: String): String? {
        if (value.isBlank()) return null
        val parsed = value.toIntOrNull() ?: return "Enter a whole number"
        if (parsed < 0) return "Cannot be negative"
        if (parsed > MAX_CALORIES) return "Max $MAX_CALORIES kcal"
        return null
    }

    fun macroError(value: String): String? {
        if (value.isBlank()) return null
        val parsed = value.replace(',', '.').toFloatOrNull() ?: return "Enter a number"
        if (parsed < 0f) return "Cannot be negative"
        if (parsed > MAX_MACRO_GRAMS) return "Max ${MAX_MACRO_GRAMS.toInt()}g"
        return null
    }

    fun isValid(state: AddFoodUiState.ManualEntry): Boolean {
        return calorieError(state.calories) == null &&
            macroError(state.protein) == null &&
            macroError(state.carbs) == null &&
            macroError(state.fat) == null
    }
}

data class FoodItemResult(
    val name: String,
    val amount: String,
    val calories: Int,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float,
    val isEditing: Boolean = false
)
