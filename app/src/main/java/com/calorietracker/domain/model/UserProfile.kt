package com.calorietracker.domain.model

data class UserProfile(
    val sex: String? = null,
    val age: Int? = null,
    val heightCm: Float? = null,
    val weightKg: Float? = null,
    val activityLevel: String? = null,
    val goalType: String? = null
) {
    companion object {
        val ACTIVITY_LEVELS = listOf(
            "sedentary",
            "lightly_active",
            "moderately_active",
            "very_active",
            "extra_active"
        )

        val GOAL_TYPES = listOf(
            "lose",
            "maintain",
            "gain"
        )
    }
}
