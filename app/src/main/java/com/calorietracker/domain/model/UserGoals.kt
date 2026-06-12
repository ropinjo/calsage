package com.calorietracker.domain.model

// Defaults follow the onboarding 30/40/30 split of 2000 kcal so macro rings
// and the calorie ring agree for users who skip onboarding.
data class UserGoals(
    val calorieTarget: Int = 2000,
    val proteinTargetGrams: Float = 150f,
    val carbsTargetGrams: Float = 200f,
    val fatTargetGrams: Float = 67f,
    val targetWeightKg: Float? = null
)
