package com.calorietracker.domain.repository

import com.calorietracker.domain.model.FavoriteMeal
import com.calorietracker.domain.model.MealType
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {

    fun getByMealType(mealType: MealType): Flow<List<FavoriteMeal>>

    suspend fun getById(id: Long): FavoriteMeal?

    suspend fun findByDescriptionAndMealType(
        description: String,
        mealType: MealType
    ): FavoriteMeal?

    fun searchByMealType(query: String, mealType: MealType): Flow<List<FavoriteMeal>>

    suspend fun insert(meal: FavoriteMeal)

    suspend fun update(meal: FavoriteMeal)

    suspend fun delete(id: Long)
}
