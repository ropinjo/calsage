package com.calorietracker.domain.util

object TdeeCalculator {

    fun calculateBmr(sex: String, weightKg: Float, heightCm: Float, age: Int): Float {
        return when (sex.lowercase()) {
            "male" -> 10f * weightKg + 6.25f * heightCm - 5f * age + 5f
            "female" -> 10f * weightKg + 6.25f * heightCm - 5f * age - 161f
            else -> 10f * weightKg + 6.25f * heightCm - 5f * age - 78f // average of male/female
        }
    }

    fun getActivityMultiplier(activityLevel: String): Float {
        return when (activityLevel.lowercase()) {
            "sedentary" -> 1.2f
            "lightly_active" -> 1.375f
            "moderately_active" -> 1.55f
            "very_active" -> 1.725f
            "extra_active" -> 1.9f
            else -> 1.2f
        }
    }

    fun getGoalAdjustment(goalType: String): Int {
        return when (goalType.lowercase()) {
            "lose" -> -500
            "maintain" -> 0
            "gain" -> 500
            else -> 0
        }
    }

    fun calculateTdee(bmr: Float, activityLevel: String): Float {
        return bmr * getActivityMultiplier(activityLevel)
    }
}
