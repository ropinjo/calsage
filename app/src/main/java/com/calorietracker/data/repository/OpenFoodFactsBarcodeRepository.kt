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
                val caloriesPer100g = if (nutriments.kcalPer100g > 0f) {
                    nutriments.kcalPer100g
                } else {
                    nutriments.energyKjPer100g / 4.184f
                }

                if (
                    caloriesPer100g == 0f &&
                    nutriments.proteinPer100g == 0f &&
                    nutriments.carbsPer100g == 0f &&
                    nutriments.fatPer100g == 0f
                ) {
                    BarcodeLookupResult.IncompleteData(product.name.ifBlank { UNKNOWN_PRODUCT_NAME })
                } else {
                    BarcodeLookupResult.Found(
                        productName = product.name.ifBlank { UNKNOWN_PRODUCT_NAME },
                        nutritionPer100g = NutritionInfo(
                            calories = caloriesPer100g.roundToInt(),
                            proteinGrams = nutriments.proteinPer100g,
                            carbsGrams = nutriments.carbsPer100g,
                            fatGrams = nutriments.fatPer100g
                        ),
                        servingSize = product.servingSize
                    )
                }
            }
        } catch (error: HttpException) {
            val message = if (error.code() == 429) {
                "Too many requests - try again shortly"
            } else {
                "Failed to fetch product"
            }
            BarcodeLookupResult.Error(message)
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
