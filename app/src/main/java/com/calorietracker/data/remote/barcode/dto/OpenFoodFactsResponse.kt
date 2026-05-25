package com.calorietracker.data.remote.barcode.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenFoodFactsResponse(
    val status: Int,
    val product: Product? = null
) {
    @Serializable
    data class Product(
        @SerialName("product_name") val name: String = "",
        @SerialName("serving_size") val servingSize: String? = null,
        val nutriments: Nutriments = Nutriments()
    )

    @Serializable
    data class Nutriments(
        @SerialName("energy-kcal_100g") val kcalPer100g: Float = 0f,
        @SerialName("energy_100g") val energyKjPer100g: Float = 0f,
        @SerialName("proteins_100g") val proteinPer100g: Float = 0f,
        @SerialName("carbohydrates_100g") val carbsPer100g: Float = 0f,
        @SerialName("fat_100g") val fatPer100g: Float = 0f
    )
}
