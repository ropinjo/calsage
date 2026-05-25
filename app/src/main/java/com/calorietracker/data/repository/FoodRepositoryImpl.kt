package com.calorietracker.data.repository

import com.calorietracker.data.local.db.dao.FoodEntryDao
import com.calorietracker.data.local.db.entity.FoodEntryEntity
import com.calorietracker.domain.model.FoodEntry
import com.calorietracker.domain.model.FoodSource
import com.calorietracker.domain.model.MealType
import com.calorietracker.domain.model.NutritionInfo
import com.calorietracker.domain.repository.DailyCalorie
import com.calorietracker.domain.repository.FoodRepository
import com.calorietracker.domain.repository.MealSubtotal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodRepositoryImpl @Inject constructor(
    private val dao: FoodEntryDao
) : FoodRepository {

    override fun getDailyTotal(date: String): Flow<NutritionInfo> {
        return dao.getDailyTotal(date).map { tuple ->
            NutritionInfo(
                calories = tuple?.calories ?: 0,
                proteinGrams = tuple?.protein ?: 0f,
                carbsGrams = tuple?.carbs ?: 0f,
                fatGrams = tuple?.fat ?: 0f
            )
        }
    }

    override fun getMealSubtotals(date: String): Flow<List<MealSubtotal>> {
        return dao.getMealSubtotals(date).map { tuples ->
            tuples.map { tuple ->
                MealSubtotal(
                    mealType = MealType.valueOf(tuple.mealType),
                    calories = tuple.calories,
                    protein = tuple.protein,
                    carbs = tuple.carbs,
                    fat = tuple.fat,
                    itemCount = tuple.itemCount
                )
            }
        }
    }

    override fun getEntriesForMeal(date: String, mealType: MealType): Flow<List<FoodEntry>> {
        return dao.getEntriesByDateAndMeal(date, mealType.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCalorieTrend(startDate: String, endDate: String): Flow<List<DailyCalorie>> {
        return dao.getCalorieTrend(startDate, endDate).map { tuples ->
            tuples.map { DailyCalorie(date = it.date, totalCalories = it.totalCalories) }
        }
    }

    override fun getAllEntriesInRange(startDate: String, endDate: String): Flow<List<FoodEntry>> {
        return dao.getEntriesInRange(startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertEntry(entry: FoodEntry) {
        dao.insert(entry.toEntity())
    }

    override suspend fun updateEntry(entry: FoodEntry) {
        dao.update(entry.toEntity())
    }

    override suspend fun deleteEntry(id: Long) {
        dao.deleteById(id)
    }
}

private fun FoodEntryEntity.toDomain(): FoodEntry {
    return FoodEntry(
        id = id,
        date = date,
        mealType = MealType.valueOf(mealType),
        description = description,
        nutritionInfo = NutritionInfo(
            calories = calories,
            proteinGrams = proteinGrams,
            carbsGrams = carbsGrams,
            fatGrams = fatGrams
        ),
        timestamp = timestamp,
        source = runCatching { FoodSource.valueOf(source) }.getOrDefault(FoodSource.AI)
    )
}

private fun FoodEntry.toEntity(): FoodEntryEntity {
    return FoodEntryEntity(
        id = id,
        date = date,
        mealType = mealType.name,
        description = description,
        calories = nutritionInfo.calories,
        proteinGrams = nutritionInfo.proteinGrams,
        carbsGrams = nutritionInfo.carbsGrams,
        fatGrams = nutritionInfo.fatGrams,
        timestamp = timestamp,
        source = source.name
    )
}
