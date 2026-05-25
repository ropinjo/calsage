package com.calorietracker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.calorietracker.data.local.db.entity.WeightEntryEntity
import kotlinx.coroutines.flow.Flow

data class WeeklyAvgTuple(
    val week: String,
    val avgWeight: Float
)

data class MonthlyAvgTuple(
    val month: String,
    val avgWeight: Float
)

@Dao
interface WeightEntryDao {

    @Insert
    suspend fun insert(entity: WeightEntryEntity): Long

    @Delete
    suspend fun delete(entity: WeightEntryEntity)

    @Query(
        """
        SELECT * FROM weight_entries
        WHERE date >= :startDate AND date <= :endDate
        ORDER BY date ASC, timestamp ASC
        """
    )
    fun getEntriesByDateRange(startDate: String, endDate: String): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries ORDER BY date ASC, timestamp ASC")
    fun getAllEntries(): Flow<List<WeightEntryEntity>>

    @Query(
        """
        SELECT * FROM weight_entries
        ORDER BY date DESC, timestamp DESC
        LIMIT 1
        """
    )
    fun getLatestEntry(): Flow<WeightEntryEntity?>

    @Query(
        """
        SELECT strftime('%Y-W%W', date) AS week,
               AVG(weight_kg) AS avgWeight
        FROM weight_entries
        WHERE date >= :startDate AND date <= :endDate
        GROUP BY strftime('%Y-W%W', date)
        ORDER BY week ASC
        """
    )
    fun getWeeklyAverages(startDate: String, endDate: String): Flow<List<WeeklyAvgTuple>>

    @Query(
        """
        SELECT substr(date, 1, 7) AS month,
               AVG(weight_kg) AS avgWeight
        FROM weight_entries
        WHERE date >= :startDate AND date <= :endDate
        GROUP BY substr(date, 1, 7)
        ORDER BY month ASC
        """
    )
    fun getMonthlyAverages(startDate: String, endDate: String): Flow<List<MonthlyAvgTuple>>

    @Query("DELETE FROM weight_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM weight_entries ORDER BY date ASC, timestamp ASC")
    suspend fun getAllEntriesOnce(): List<WeightEntryEntity>

    @Query("DELETE FROM weight_entries WHERE date IN (:dates)")
    suspend fun deleteByDates(dates: List<String>)
}
