package com.calorietracker.domain.model

data class DailyProgress(
    val date: String,
    val totalCalories: Int,
    val totalProtein: Float,
    val totalCarbs: Float,
    val totalFat: Float,
    val calorieTarget: Int,
    val proteinTarget: Float,
    val carbsTarget: Float,
    val fatTarget: Float
)
