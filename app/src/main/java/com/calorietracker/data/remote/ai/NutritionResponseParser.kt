package com.calorietracker.data.remote.ai

import com.calorietracker.data.remote.ai.venice.dto.NutritionResult
import kotlinx.serialization.json.Json

object NutritionResponseParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val thinkingTagRegex = Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL)

    fun parse(responseContent: String): NutritionResult {
        var cleaned = responseContent

        // Strip <think>...</think> tags
        cleaned = thinkingTagRegex.replace(cleaned, "")

        // Strip markdown code fences
        cleaned = stripCodeFences(cleaned)

        cleaned = cleaned.trim()

        if (cleaned.isEmpty()) {
            throw IllegalArgumentException("AI returned empty response content")
        }

        return json.decodeFromString<NutritionResult>(cleaned)
    }
}

private val codeBlockRegex = Regex("```(?:[A-Za-z0-9_-]+)?\\s*\\n?(.*?)\\n?\\s*```", RegexOption.DOT_MATCHES_ALL)

fun stripCodeFences(value: String): String {
    val codeBlockMatch = codeBlockRegex.find(value)
    return codeBlockMatch?.groupValues?.get(1) ?: value
}
