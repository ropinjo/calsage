package com.calorietracker.data.repository

import com.calorietracker.data.local.db.dao.UserGoalsDao
import com.calorietracker.data.local.db.entity.UserGoalsEntity
import com.calorietracker.domain.model.UserGoals
import com.calorietracker.domain.repository.GoalsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalsRepositoryImpl @Inject constructor(
    private val dao: UserGoalsDao
) : GoalsRepository {

    override fun getGoals(): Flow<UserGoals> {
        return dao.getGoals().map { entity ->
            entity?.toDomain() ?: UserGoals()
        }
    }

    override suspend fun saveGoals(goals: UserGoals) {
        dao.upsert(goals.toEntity())
    }
}

private fun UserGoalsEntity.toDomain(): UserGoals {
    return UserGoals(
        calorieTarget = calorieTarget,
        proteinTargetGrams = proteinTargetGrams,
        carbsTargetGrams = carbsTargetGrams,
        fatTargetGrams = fatTargetGrams,
        targetWeightKg = targetWeightKg
    )
}

private fun UserGoals.toEntity(): UserGoalsEntity {
    return UserGoalsEntity(
        id = 1,
        calorieTarget = calorieTarget,
        proteinTargetGrams = proteinTargetGrams,
        carbsTargetGrams = carbsTargetGrams,
        fatTargetGrams = fatTargetGrams,
        targetWeightKg = targetWeightKg
    )
}
