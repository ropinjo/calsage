package com.calorietracker.data.remote.ai

const val VENICE_BASE_URL = "https://api.venice.ai/api/v1/"

data class AiConfig(
    val apiKey: String,
    val model: String = "most_intelligent",
    val thinkingEnabled: Boolean = false
)
