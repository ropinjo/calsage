package com.calorietracker.data.remote.ai.venice.dto

import com.calorietracker.domain.model.NutritionInfo
import com.calorietracker.domain.model.NutritionItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NutritionResult(
    val calories: Int,
    @SerialName("protein_g")
    val proteinG: Float,
    @SerialName("carbs_g")
    val carbsG: Float,
    @SerialName("fat_g")
    val fatG: Float,
    val items: List<NutritionItemResult> = emptyList(),
    val error: String? = null
) {
    fun toDomain(): NutritionInfo {
        return NutritionInfo(
            calories = calories,
            proteinGrams = proteinG,
            carbsGrams = carbsG,
            fatGrams = fatG,
            items = items.map { it.toDomain() },
            error = error
        )
    }
}

@Serializable
data class NutritionItemResult(
    val name: String,
    val amount: String,
    val calories: Int,
    @SerialName("protein_g")
    val proteinG: Float,
    @SerialName("carbs_g")
    val carbsG: Float,
    @SerialName("fat_g")
    val fatG: Float
) {
    fun toDomain(): NutritionItem {
        return NutritionItem(
            name = name,
            amount = amount,
            calories = calories,
            proteinGrams = proteinG,
            carbsGrams = carbsG,
            fatGrams = fatG
        )
    }
}
