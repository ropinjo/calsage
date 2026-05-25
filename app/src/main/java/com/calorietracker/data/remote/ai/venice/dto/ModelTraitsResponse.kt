package com.calorietracker.data.remote.ai.venice.dto

import kotlinx.serialization.Serializable

@Serializable
data class ModelTraitsResponse(
    val data: Map<String, String>,
    val type: String? = null,
    val `object`: String? = null
)
