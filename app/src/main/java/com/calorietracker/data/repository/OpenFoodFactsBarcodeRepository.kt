package com.calorietracker.data.repository

import com.calorietracker.data.remote.barcode.OpenFoodFactsApiService
import com.calorietracker.domain.model.NutritionInfo
import com.calorietracker.domain.repository.BarcodeLookupResult
import com.calorietracker.domain.repository.BarcodeRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class OpenFoodFactsBarcodeRepository @Inject constructor(
    private val apiService: OpenFoodFactsApiService
) : BarcodeRepository {

    override suspend fun getProduct(barcode: String): BarcodeLookupResult {
        return try {
            val response = apiService.getProduct(barcode)
            val product = response.product
            if (response.status == 0 || product == null) {
                BarcodeLookupResult.NotFound
            } else {
                val nutriments = product.nutriments
                val caloriesPer100g = when {
                    nutriments.kcalPer100g != null && nutriments.kcalPer100g > 0f ->
                        nutriments.kcalPer100g
                    nutriments.energyKjPer100g != null && nutriments.energyKjPer100g > 0f ->
                        nutriments.energyKjPer100g / 4.184f
                    nutriments.kcalPer100g != null || nutriments.energyKjPer100g != null -> 0f
                    else -> null
                }

                // Incomplete only when the data is absent; explicit zeros (water,
                // diet drinks) are valid nutrition.
                if (
                    caloriesPer100g == null &&
                    nutriments.proteinPer100g == null &&
                    nutriments.carbsPer100g == null &&
                    nutriments.fatPer100g == null
                ) {
                    BarcodeLookupResult.IncompleteData(product.name.ifBlank { UNKNOWN_PRODUCT_NAME })
                } else {
                    BarcodeLookupResult.Found(
                        productName = product.name.ifBlank { UNKNOWN_PRODUCT_NAME },
                        nutritionPer100g = NutritionInfo(
                            calories = (caloriesPer100g ?: 0f).roundToInt(),
                            proteinGrams = nutriments.proteinPer100g ?: 0f,
                            carbsGrams = nutriments.carbsPer100g ?: 0f,
                            fatGrams = nutriments.fatPer100g ?: 0f
                        ),
                        servingSize = product.servingSize
                    )
                }
            }
        } catch (error: HttpException) {
            when (error.code()) {
                // The OFF v2 endpoint reports unknown barcodes as HTTP 404
                404 -> BarcodeLookupResult.NotFound
                429 -> BarcodeLookupResult.Error("Too many requests - try again shortly")
                else -> BarcodeLookupResult.Error("Failed to fetch product")
            }
        } catch (_: IOException) {
            BarcodeLookupResult.Error("No internet connection")
        } catch (_: Exception) {
            BarcodeLookupResult.Error("Failed to fetch product")
        }
    }

    private companion object {
        const val UNKNOWN_PRODUCT_NAME = "Unknown product"
    }
}
