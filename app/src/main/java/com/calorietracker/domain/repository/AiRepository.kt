package com.calorietracker.domain.repository

import com.calorietracker.domain.model.NutritionInfo

data class AiModel(
    val id: String,
    val name: String,
    val createdAtEpochSeconds: Long,
    val privacy: String?,
    val traits: List<String>,
    val supportsResponseSchema: Boolean,
    val supportsReasoning: Boolean,
    val pricing: AiModelPricing?
) {
    val isPrivate: Boolean
        get() = privacy == "private"
}

data class AiModelPricing(
    val inputPerMillion: Float,
    val outputPerMillion: Float,
    val cacheInputPerMillion: Float = inputPerMillion,
    val cacheWritePerMillion: Float = inputPerMillion,
    val extended: AiModelExtendedPricing? = null
)

data class AiModelExtendedPricing(
    val contextTokenThreshold: Int,
    val inputPerMillion: Float,
    val outputPerMillion: Float,
    val cacheInputPerMillion: Float = inputPerMillion,
    val cacheWritePerMillion: Float = inputPerMillion
)

interface AiRepository {

    suspend fun analyzeFood(description: String): Result<NutritionInfo>

    suspend fun validateApiKey(apiKey: String): Result<Boolean>

    suspend fun fetchModels(): Result<List<AiModel>>

    suspend fun improvePrompt(currentPrompt: String): Result<String>
}
