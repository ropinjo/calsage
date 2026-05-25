package com.calorietracker.domain.util

import com.calorietracker.domain.model.UserProfile
import kotlin.math.roundToInt

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

    fun calculateTdee(profile: UserProfile): Int? {
        val sex = profile.sex ?: return null
        val weightKg = profile.weightKg ?: return null
        val heightCm = profile.heightCm ?: return null
        val age = profile.age ?: return null
        val activityLevel = profile.activityLevel ?: return null
        val goalType = profile.goalType ?: return null

        val bmr = calculateBmr(sex, weightKg, heightCm, age)
        val tdee = calculateTdee(bmr, activityLevel)
        val adjustment = getGoalAdjustment(goalType)

        return tdee.roundToInt() + adjustment
    }

    fun calculateMacroTargets(calorieTarget: Int, weightKg: Float?): Triple<Float, Float, Float> {
        val proteinG = if (weightKg != null) {
            weightKg * 1.6f
        } else {
            150f
        }
        val carbsG = (calorieTarget * 0.50f) / 4f
        val fatG = (calorieTarget * 0.30f) / 9f

        return Triple(proteinG, carbsG, fatG)
    }
}
