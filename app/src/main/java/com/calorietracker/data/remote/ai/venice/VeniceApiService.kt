package com.calorietracker.data.remote.ai.venice

import com.calorietracker.data.remote.ai.venice.dto.ChatCompletionRequest
import com.calorietracker.data.remote.ai.venice.dto.ChatCompletionResponse
import com.calorietracker.data.remote.ai.venice.dto.ModelTraitsResponse
import com.calorietracker.data.remote.ai.venice.dto.ModelsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface VeniceApiService {

    @POST("chat/completions")
    suspend fun chatCompletion(@Body request: ChatCompletionRequest): Response<ChatCompletionResponse>

    @GET("models")
    suspend fun listModels(@Query("type") type: String = "text"): ModelsResponse

    @GET("models/traits")
    suspend fun listModelTraits(@Query("type") type: String = "text"): ModelTraitsResponse
}
