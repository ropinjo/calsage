package com.calorietracker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.calorietracker.data.local.db.entity.UserGoalsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserGoalsDao {

    @Upsert
    suspend fun upsert(goals: UserGoalsEntity)

    @Query("SELECT * FROM user_goals WHERE id = 1")
    fun getGoals(): Flow<UserGoalsEntity?>
}
