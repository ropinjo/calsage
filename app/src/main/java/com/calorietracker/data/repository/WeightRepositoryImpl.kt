package com.calorietracker.data.repository

import com.calorietracker.data.local.db.dao.WeightEntryDao
import com.calorietracker.data.local.db.entity.WeightEntryEntity
import com.calorietracker.domain.model.WeightEntry
import com.calorietracker.domain.repository.WeightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeightRepositoryImpl @Inject constructor(
    private val dao: WeightEntryDao
) : WeightRepository {

    override fun getWeightEntries(startDate: String, endDate: String): Flow<List<WeightEntry>> {
        return dao.getEntriesByDateRange(startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getLatestWeight(): Flow<WeightEntry?> {
        return dao.getLatestEntry().map { it?.toDomain() }
    }

    override suspend fun insertWeight(entry: WeightEntry) {
        dao.insert(entry.toEntity())
    }

    override suspend fun deleteWeight(id: Long) {
        dao.deleteById(id)
    }

    override fun getAllEntries(): Flow<List<WeightEntry>> {
        return dao.getAllEntries().map { entities ->
            entities.map { it.toDomain() }
        }
    }
}

private fun WeightEntryEntity.toDomain(): WeightEntry {
    return WeightEntry(
        id = id,
        date = date,
        weightKg = weightKg,
        note = note,
        timestamp = timestamp
    )
}

private fun WeightEntry.toEntity(): WeightEntryEntity {
    return WeightEntryEntity(
        id = id,
        date = date,
        weightKg = weightKg,
        note = note,
        timestamp = timestamp
    )
}
