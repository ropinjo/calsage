package com.calorietracker.data.repository

import com.calorietracker.data.remote.barcode.OpenFoodFactsApiService
import com.calorietracker.data.remote.barcode.dto.OpenFoodFactsResponse
import com.calorietracker.domain.repository.BarcodeLookupResult
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class OpenFoodFactsBarcodeRepositoryTest {

    @Test
    fun `returns found result when product has nutrition data`() = runTest {
        val repository = OpenFoodFactsBarcodeRepository(
            apiService = FakeOpenFoodFactsApiService {
                OpenFoodFactsResponse(
                    status = 1,
                    product = OpenFoodFactsResponse.Product(
                        name = "Activia Natural Yogurt",
                        servingSize = "125g",
                        nutriments = OpenFoodFactsResponse.Nutriments(
                            kcalPer100g = 92f,
                            proteinPer100g = 3.8f,
                            carbsPer100g = 12.5f,
                            fatPer100g = 3.1f
                        )
                    )
                )
            }
        )

        val result = repository.getProduct("1234567890123")

        assertTrue(result is BarcodeLookupResult.Found)
        result as BarcodeLookupResult.Found
        assertEquals("Activia Natural Yogurt", result.productName)
        assertEquals(92, result.nutritionPer100g.calories)
        assertEquals(3.8f, result.nutritionPer100g.proteinGrams, 0.001f)
        assertEquals("125g", result.servingSize)
    }

    @Test
    fun `returns not found when api status is zero`() = runTest {
        val repository = OpenFoodFactsBarcodeRepository(
            apiService = FakeOpenFoodFactsApiService {
                OpenFoodFactsResponse(status = 0, product = null)
            }
        )

        val result = repository.getProduct("000")

        assertTrue(result is BarcodeLookupResult.NotFound)
    }

    @Test
    fun `falls back to unknown product when name is blank`() = runTest {
        val repository = OpenFoodFactsBarcodeRepository(
            apiService = FakeOpenFoodFactsApiService {
                OpenFoodFactsResponse(
                    status = 1,
                    product = OpenFoodFactsResponse.Product(
                        name = "",
                        nutriments = OpenFoodFactsResponse.Nutriments(kcalPer100g = 45f)
                    )
                )
            }
        )

        val result = repository.getProduct("111")

        assertTrue(result is BarcodeLookupResult.Found)
        result as BarcodeLookupResult.Found
        assertEquals("Unknown product", result.productName)
    }

    @Test
    fun `converts kilojoules to calories when kcal field is missing`() = runTest {
        val repository = OpenFoodFactsBarcodeRepository(
            apiService = FakeOpenFoodFactsApiService {
                OpenFoodFactsResponse(
                    status = 1,
                    product = OpenFoodFactsResponse.Product(
                        name = "Sparkling drink",
                        nutriments = OpenFoodFactsResponse.Nutriments(
                            energyKjPer100g = 418.4f
                        )
                    )
                )
            }
        )

        val result = repository.getProduct("222")

        assertTrue(result is BarcodeLookupResult.Found)
        result as BarcodeLookupResult.Found
        assertEquals(100, result.nutritionPer100g.calories)
    }

    @Test
    fun `returns incomplete data when all nutrient values are absent`() = runTest {
        val repository = OpenFoodFactsBarcodeRepository(
            apiService = FakeOpenFoodFactsApiService {
                OpenFoodFactsResponse(
                    status = 1,
                    product = OpenFoodFactsResponse.Product(
                        name = "Mystery snack",
                        nutriments = OpenFoodFactsResponse.Nutriments()
                    )
                )
            }
        )

        val result = repository.getProduct("333")

        assertTrue(result is BarcodeLookupResult.IncompleteData)
        result as BarcodeLookupResult.IncompleteData
        assertEquals("Mystery snack", result.productName)
    }

    @Test
    fun `returns not found when api responds with http 404`() = runTest {
        val repository = OpenFoodFactsBarcodeRepository(
            apiService = FakeOpenFoodFactsApiService {
                throw HttpException(
                    Response.error<OpenFoodFactsResponse>(404, "".toResponseBody())
                )
            }
        )

        val result = repository.getProduct("666")

        assertTrue(result is BarcodeLookupResult.NotFound)
    }

    @Test
    fun `returns found for product with explicit zero nutrition`() = runTest {
        val repository = OpenFoodFactsBarcodeRepository(
            apiService = FakeOpenFoodFactsApiService {
                OpenFoodFactsResponse(
                    status = 1,
                    product = OpenFoodFactsResponse.Product(
                        name = "Mineral water",
                        nutriments = OpenFoodFactsResponse.Nutriments(
                            kcalPer100g = 0f,
                            proteinPer100g = 0f,
                            carbsPer100g = 0f,
                            fatPer100g = 0f
                        )
                    )
                )
            }
        )

        val result = repository.getProduct("777")

        assertTrue(result is BarcodeLookupResult.Found)
        result as BarcodeLookupResult.Found
        assertEquals(0, result.nutritionPer100g.calories)
    }

    @Test
    fun `returns no internet message for io exception`() = runTest {
        val repository = OpenFoodFactsBarcodeRepository(
            apiService = FakeOpenFoodFactsApiService {
                throw IOException("offline")
            }
        )

        val result = repository.getProduct("444")

        assertTrue(result is BarcodeLookupResult.Error)
        result as BarcodeLookupResult.Error
        assertEquals("No internet connection", result.message)
    }

    @Test
    fun `returns generic error message for unexpected exception`() = runTest {
        val repository = OpenFoodFactsBarcodeRepository(
            apiService = FakeOpenFoodFactsApiService {
                throw IllegalStateException("boom")
            }
        )

        val result = repository.getProduct("555")

        assertTrue(result is BarcodeLookupResult.Error)
        result as BarcodeLookupResult.Error
        assertEquals("Failed to fetch product", result.message)
    }
}

private class FakeOpenFoodFactsApiService(
    private val block: suspend (String) -> OpenFoodFactsResponse
) : OpenFoodFactsApiService {

    override suspend fun getProduct(
        barcode: String,
        fields: String
    ): OpenFoodFactsResponse = block(barcode)
}
