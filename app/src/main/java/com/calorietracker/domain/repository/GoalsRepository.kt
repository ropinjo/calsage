package com.calorietracker.domain.repository

import com.calorietracker.domain.model.UserGoals
import kotlinx.coroutines.flow.Flow

interface GoalsRepository {

    fun getGoals(): Flow<UserGoals>

    suspend fun saveGoals(goals: UserGoals)
}
