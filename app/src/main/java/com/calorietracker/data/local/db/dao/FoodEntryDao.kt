package com.calorietracker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.calorietracker.data.local.db.entity.FoodEntryEntity
import kotlinx.coroutines.flow.Flow

data class NutritionTuple(
    val calories: Int?,
    val protein: Float?,
    val carbs: Float?,
    val fat: Float?
)

data class MealSubtotalTuple(
    val mealType: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val itemCount: Int
)

data class DailyCalorieTuple(
    val date: String,
    val totalCalories: Int
)

@Dao
interface FoodEntryDao {

    @Insert
    suspend fun insert(entity: FoodEntryEntity): Long

    @Update
    suspend fun update(entity: FoodEntryEntity)

    @Delete
    suspend fun delete(entity: FoodEntryEntity)

    @Query("SELECT * FROM food_entries WHERE date = :date ORDER BY timestamp ASC")
    fun getEntriesByDate(date: String): Flow<List<FoodEntryEntity>>

    @Query(
        """
        SELECT SUM(calories) AS calories,
               SUM(protein_grams) AS protein,
               SUM(carbs_grams) AS carbs,
               SUM(fat_grams) AS fat
        FROM food_entries
        WHERE date = :date
        """
    )
    fun getDailyTotal(date: String): Flow<NutritionTuple?>

    @Query(
        """
        SELECT meal_type AS mealType,
               SUM(calories) AS calories,
               SUM(protein_grams) AS protein,
               SUM(carbs_grams) AS carbs,
               SUM(fat_grams) AS fat,
               COUNT(*) AS itemCount
        FROM food_entries
        WHERE date = :date
        GROUP BY meal_type
        """
    )
    fun getMealSubtotals(date: String): Flow<List<MealSubtotalTuple>>

    @Query(
        """
        SELECT * FROM food_entries
        WHERE date = :date AND meal_type = :mealType
        ORDER BY timestamp ASC
        """
    )
    fun getEntriesByDateAndMeal(date: String, mealType: String): Flow<List<FoodEntryEntity>>

    @Query(
        """
        SELECT date, SUM(calories) AS totalCalories
        FROM food_entries
        WHERE date >= :startDate AND date <= :endDate
        GROUP BY date
        ORDER BY date ASC
        """
    )
    fun getCalorieTrend(startDate: String, endDate: String): Flow<List<DailyCalorieTuple>>

    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
        SELECT * FROM food_entries
        WHERE date >= :startDate AND date <= :endDate
        ORDER BY date ASC, timestamp ASC
        """
    )
    fun getEntriesInRange(startDate: String, endDate: String): Flow<List<FoodEntryEntity>>

    @Query("SELECT * FROM food_entries ORDER BY date ASC, timestamp ASC")
    fun getAllEntries(): Flow<List<FoodEntryEntity>>

    @Query("SELECT * FROM food_entries ORDER BY date ASC, timestamp ASC")
    suspend fun getAllEntriesOnce(): List<FoodEntryEntity>

    @Query("DELETE FROM food_entries WHERE date IN (:dates)")
    suspend fun deleteByDates(dates: List<String>)
}
