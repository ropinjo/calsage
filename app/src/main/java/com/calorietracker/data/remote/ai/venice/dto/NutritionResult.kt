package com.calorietracker.data.remote.ai.venice.dto

import com.calorietracker.domain.model.NutritionInfo
import com.calorietracker.domain.model.NutritionItem
import com.calorietracker.domain.model.NutritionPer100g
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

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
        val domainItems = items.map { it.toDomain() }
        if (domainItems.isNotEmpty()) {
            return NutritionInfo(
                calories = domainItems.sumOf { it.calories },
                proteinGrams = domainItems.sumOf { it.proteinGrams.toDouble() }.toFloat(),
                carbsGrams = domainItems.sumOf { it.carbsGrams.toDouble() }.toFloat(),
                fatGrams = domainItems.sumOf { it.fatGrams.toDouble() }.toFloat(),
                items = domainItems,
                error = error
            )
        }
        return NutritionInfo(
            calories = calories,
            proteinGrams = proteinG,
            carbsGrams = carbsG,
            fatGrams = fatG,
            items = emptyList(),
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
    val fatG: Float,
    val grams: Float? = null,
    @SerialName("per_100g")
    val per100g: NutritionPer100gResult? = null
) {
    fun toDomain(): NutritionItem {
        val gramsValue = grams?.takeIf { it > 0f }
        val per100gValue = per100g?.takeIf { it.isPositive() }

        if (gramsValue != null && per100gValue != null) {
            val multiplier = gramsValue / 100f
            return NutritionItem(
                name = name,
                amount = amount,
                calories = (per100gValue.calories * multiplier).roundToInt(),
                proteinGrams = per100gValue.proteinG * multiplier,
                carbsGrams = per100gValue.carbsG * multiplier,
                fatGrams = per100gValue.fatG * multiplier,
                grams = gramsValue,
                per100g = NutritionPer100g(
                    calories = per100gValue.calories,
                    proteinGrams = per100gValue.proteinG,
                    carbsGrams = per100gValue.carbsG,
                    fatGrams = per100gValue.fatG
                )
            )
        }

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

@Serializable
data class NutritionPer100gResult(
    val calories: Float,
    @SerialName("protein_g")
    val proteinG: Float,
    @SerialName("carbs_g")
    val carbsG: Float,
    @SerialName("fat_g")
    val fatG: Float
) {
    fun isPositive(): Boolean {
        return calories > 0f && proteinG >= 0f && carbsG >= 0f && fatG >= 0f
    }
}
