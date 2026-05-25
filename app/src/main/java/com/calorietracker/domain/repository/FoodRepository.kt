package com.calorietracker.domain.repository

import com.calorietracker.domain.model.FoodEntry
import com.calorietracker.domain.model.MealType
import com.calorietracker.domain.model.NutritionInfo
import kotlinx.coroutines.flow.Flow

data class MealSubtotal(
    val mealType: MealType,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val itemCount: Int
)

data class DailyCalorie(
    val date: String,
    val totalCalories: Int
)

interface FoodRepository {

    fun getDailyTotal(date: String): Flow<NutritionInfo>

    fun getMealSubtotals(date: String): Flow<List<MealSubtotal>>

    fun getEntriesForMeal(date: String, mealType: MealType): Flow<List<FoodEntry>>

    fun getCalorieTrend(startDate: String, endDate: String): Flow<List<DailyCalorie>>

    fun getAllEntriesInRange(startDate: String, endDate: String): Flow<List<FoodEntry>>

    suspend fun insertEntry(entry: FoodEntry)

    suspend fun updateEntry(entry: FoodEntry)

    suspend fun deleteEntry(id: Long)
}
