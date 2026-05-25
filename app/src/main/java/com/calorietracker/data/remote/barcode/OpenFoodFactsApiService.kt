package com.calorietracker.data.remote.barcode

import com.calorietracker.data.remote.barcode.dto.OpenFoodFactsResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenFoodFactsApiService {

    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = "product_name,serving_size,nutriments"
    ): OpenFoodFactsResponse
}
