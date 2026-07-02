package com.calorietracker.data.repository

import com.calorietracker.data.local.db.dao.FavoriteMealDao
import com.calorietracker.data.local.db.entity.FavoriteMealEntity
import com.calorietracker.domain.model.FavoriteMeal
import com.calorietracker.domain.model.FoodSource
import com.calorietracker.domain.model.MealType
import com.calorietracker.domain.model.NutritionItem
import com.calorietracker.domain.model.NutritionPer100g
import com.calorietracker.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val dao: FavoriteMealDao
) : FavoriteRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getByMealType(mealType: MealType): Flow<List<FavoriteMeal>> {
        return dao.getFavoritesByMealType(mealType.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getById(id: Long): FavoriteMeal? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun findByDescriptionAndMealType(
        description: String,
        mealType: MealType
    ): FavoriteMeal? {
        return dao.findByDescriptionAndMealType(description, mealType.name)?.toDomain()
    }

    override suspend fun insert(meal: FavoriteMeal) {
        dao.insert(meal.toEntity())
    }

    override suspend fun update(meal: FavoriteMeal) {
        dao.update(meal.toEntity())
    }

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    override fun searchByMealType(
        query: String,
        mealType: MealType
    ): Flow<List<FavoriteMeal>> {
        return dao.searchFavoritesByMealType(query, mealType.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private fun FavoriteMealEntity.toDomain(): FavoriteMeal {
        val items = itemsJson?.let { jsonStr ->
            try {
                json.decodeFromString<List<SerializableItem>>(jsonStr).map { it.toDomain() }
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()

        return FavoriteMeal(
            id = id,
            name = name,
            description = description,
            totalCalories = totalCalories,
            totalProtein = totalProtein,
            totalCarbs = totalCarbs,
            totalFat = totalFat,
            items = items,
            mealType = MealType.valueOf(mealType),
            source = runCatching { FoodSource.valueOf(source) }.getOrDefault(FoodSource.AI)
        )
    }

    private fun FavoriteMeal.toEntity(): FavoriteMealEntity {
        val itemsJsonStr = if (items.isNotEmpty()) {
            json.encodeToString(items.map { it.toSerializable() })
        } else null

        return FavoriteMealEntity(
            id = id,
            name = name,
            description = description,
            totalCalories = totalCalories,
            totalProtein = totalProtein,
            totalCarbs = totalCarbs,
            totalFat = totalFat,
            itemsJson = itemsJsonStr,
            mealType = mealType.name,
            source = source.name
        )
    }
}

@Serializable
private data class SerializableItem(
    val name: String,
    val amount: String,
    val calories: Int,
    val protein_g: Float,
    val carbs_g: Float,
    val fat_g: Float,
    val grams: Float? = null,
    val per_100g: SerializablePer100g? = null,
    val calories_recomputed: Boolean = false
)

@Serializable
private data class SerializablePer100g(
    val calories: Float,
    val protein_g: Float,
    val carbs_g: Float,
    val fat_g: Float
)

private fun SerializableItem.toDomain(): NutritionItem {
    return NutritionItem(
        name = name,
        amount = amount,
        calories = calories,
        proteinGrams = protein_g,
        carbsGrams = carbs_g,
        fatGrams = fat_g,
        grams = grams,
        per100g = per_100g?.let {
            NutritionPer100g(
                calories = it.calories,
                proteinGrams = it.protein_g,
                carbsGrams = it.carbs_g,
                fatGrams = it.fat_g
            )
        },
        caloriesRecomputed = calories_recomputed
    )
}

private fun NutritionItem.toSerializable(): SerializableItem {
    return SerializableItem(
        name = name,
        amount = amount,
        calories = calories,
        protein_g = proteinGrams,
        carbs_g = carbsGrams,
        fat_g = fatGrams,
        grams = grams,
        per_100g = per100g?.let {
            SerializablePer100g(
                calories = it.calories,
                protein_g = it.proteinGrams,
                carbs_g = it.carbsGrams,
                fat_g = it.fatGrams
            )
        },
        calories_recomputed = caloriesRecomputed
    )
}
