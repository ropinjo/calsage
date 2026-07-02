package com.calorietracker.presentation.screen.addfood

import com.calorietracker.domain.repository.BarcodeNutritionPer100g

data class BarcodeUiState(
    val isScannerVisible: Boolean = false,
    val isLookupLoading: Boolean = false,
    val showServingDialog: Boolean = false,
    val scannedProductName: String = "",
    val scannedNutritionPer100g: BarcodeNutritionPer100g? = null,
    val scannedServingSuggestion: String? = null,
    val servingGrams: String = "100",
    val servingError: String? = null,
    val feedbackMessage: String? = null
)
