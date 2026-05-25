package com.calorietracker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.calorietracker.data.local.db.entity.FavoriteMealEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteMealDao {

    @Insert
    suspend fun insert(entity: FavoriteMealEntity): Long

    @Update
    suspend fun update(entity: FavoriteMealEntity)

    @Delete
    suspend fun delete(entity: FavoriteMealEntity)

    @Query("SELECT * FROM favorite_meals ORDER BY name ASC")
    fun getAllFavorites(): Flow<List<FavoriteMealEntity>>

    @Query("SELECT * FROM favorite_meals WHERE meal_type = :mealType ORDER BY name ASC")
    fun getFavoritesByMealType(mealType: String): Flow<List<FavoriteMealEntity>>

    @Query("SELECT * FROM favorite_meals WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FavoriteMealEntity?

    @Query(
        """
        SELECT * FROM favorite_meals
        WHERE description = :description AND meal_type = :mealType
        LIMIT 1
        """
    )
    suspend fun findByDescriptionAndMealType(
        description: String,
        mealType: String
    ): FavoriteMealEntity?

    @Query(
        """
        SELECT * FROM favorite_meals
        WHERE (name LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%')
          AND meal_type = :mealType
        ORDER BY name ASC
        """
    )
    fun searchFavoritesByMealType(
        query: String,
        mealType: String
    ): Flow<List<FavoriteMealEntity>>

    @Query("DELETE FROM favorite_meals WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM favorite_meals ORDER BY name ASC")
    suspend fun getAllFavoritesOnce(): List<FavoriteMealEntity>
}
