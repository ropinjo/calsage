package com.calorietracker.domain.repository

import com.calorietracker.domain.model.NutritionInfo

sealed interface BarcodeLookupResult {
    data class Found(
        val productName: String,
        val nutritionPer100g: NutritionInfo,
        val servingSize: String?
    ) : BarcodeLookupResult

    data object NotFound : BarcodeLookupResult

    data class IncompleteData(val productName: String) : BarcodeLookupResult

    data class Error(val message: String) : BarcodeLookupResult
}

interface BarcodeRepository {

    suspend fun getProduct(barcode: String): BarcodeLookupResult
}
