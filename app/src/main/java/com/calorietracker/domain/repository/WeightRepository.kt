package com.calorietracker.domain.repository

import com.calorietracker.domain.model.WeightEntry
import kotlinx.coroutines.flow.Flow

interface WeightRepository {

    fun getWeightEntries(startDate: String, endDate: String): Flow<List<WeightEntry>>

    fun getAllEntries(): Flow<List<WeightEntry>>

    fun getLatestWeight(): Flow<WeightEntry?>

    suspend fun insertWeight(entry: WeightEntry)

    suspend fun deleteWeight(id: Long)
}
