package com.calorietracker.domain.repository

sealed interface BarcodeLookupResult {
    data class Found(
        val productName: String,
        val nutritionPer100g: BarcodeNutritionPer100g,
        val servingSize: String?
    ) : BarcodeLookupResult

    data object NotFound : BarcodeLookupResult

    data class IncompleteData(val productName: String) : BarcodeLookupResult

    data class Error(val message: String) : BarcodeLookupResult
}

interface BarcodeRepository {

    suspend fun getProduct(barcode: String): BarcodeLookupResult
}

data class BarcodeNutritionPer100g(
    val calories: Float,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float
)
