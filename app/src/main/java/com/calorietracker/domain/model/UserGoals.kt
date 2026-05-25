package com.calorietracker.domain.model

data class UserGoals(
    val calorieTarget: Int = 2000,
    val proteinTargetGrams: Float = 150f,
    val carbsTargetGrams: Float = 250f,
    val fatTargetGrams: Float = 65f,
    val targetWeightKg: Float? = null
)
