package com.calorietracker.data.remote.ai.venice.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModelsResponse(
    val data: List<ModelInfo>,
    val type: String? = null,
    val `object`: String? = null
)

@Serializable
data class ModelInfo(
    val created: Long = 0L,
    val id: String,
    val type: String,
    val `object`: String? = null,
    @SerialName("owned_by")
    val ownedBy: String? = null,
    @SerialName("model_spec")
    val modelSpec: ModelSpec? = null
)

@Serializable
data class ModelSpec(
    val name: String? = null,
    val privacy: String? = null,
    val traits: List<String> = emptyList(),
    val deprecation: ModelDeprecation? = null,
    val capabilities: ModelCapabilities? = null,
    val pricing: ModelPricing? = null
)

@Serializable
data class ModelDeprecation(
    val date: String? = null
)

@Serializable
data class ModelCapabilities(
    val supportsResponseSchema: Boolean = false,
    val supportsFunctionCalling: Boolean = false,
    val supportsReasoning: Boolean = false
)

@Serializable
data class ModelPricing(
    val input: ModelPricePoint? = null,
    val output: ModelPricePoint? = null,
    @SerialName("cache_input")
    val cacheInput: ModelPricePoint? = null,
    @SerialName("cache_write")
    val cacheWrite: ModelPricePoint? = null,
    val extended: ExtendedPricing? = null
)

@Serializable
data class ModelPricePoint(
    val usd: Float = 0f,
    val diem: Float = 0f
)

@Serializable
data class ExtendedPricing(
    @SerialName("context_token_threshold")
    val contextTokenThreshold: Int,
    val input: ModelPricePoint,
    val output: ModelPricePoint,
    @SerialName("cache_input")
    val cacheInput: ModelPricePoint? = null,
    @SerialName("cache_write")
    val cacheWrite: ModelPricePoint? = null
)
