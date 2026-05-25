package com.calorietracker.presentation.navigation

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
sealed interface AppDestination

@Serializable
data object DashboardDestination : AppDestination

@Serializable
data object TrendsDestination : AppDestination

@Serializable
data object WeightDestination : AppDestination

@Serializable
data object SettingsDestination : AppDestination

@Serializable
data class AddFoodDestination(
    val mealType: String,
    val date: String,
    val favoriteId: Long? = null,
    val entryKey: String = UUID.randomUUID().toString()
) : AppDestination

@Serializable
data class MealDetailDestination(val mealType: String, val date: String) : AppDestination

@Serializable
data class FavoritesDestination(val mealType: String) : AppDestination

@Serializable
data object OnboardingDestination : AppDestination
