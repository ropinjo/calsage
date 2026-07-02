package com.calorietracker.data.remote.ai.prompt

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NutritionPromptBuilderTest {

    @Test
    fun `nutrition request uses larger completion budget when thinking is enabled`() {
        val request = NutritionPromptBuilder.buildNutritionRequest(
            description = "chicken salad",
            userPrompt = "Estimate accurately.",
            model = "qwen3-5-9b",
            thinkingEnabled = true
        )

        assertEquals(4096, request.maxCompletionTokens)
    }

    @Test
    fun `nutrition request uses 2048 completion budget when thinking is disabled`() {
        val request = NutritionPromptBuilder.buildNutritionRequest(
            description = "chicken salad",
            userPrompt = "Estimate accurately.",
            model = "qwen3-5-9b",
            thinkingEnabled = false
        )

        assertEquals(2048, request.maxCompletionTokens)
    }

    @Test
    fun `nutrition request omits reasoning and disables thinking when thinking is disabled`() {
        val request = NutritionPromptBuilder.buildNutritionRequest(
            description = "chicken salad",
            userPrompt = "Estimate accurately.",
            model = "qwen3-5-9b",
            thinkingEnabled = false
        )

        assertEquals(null, request.reasoning)
        assertEquals(true, request.veniceParameters?.disableThinking)
    }

    @Test
    fun `validation request uses default trait and omits reasoning`() {
        val request = NutritionPromptBuilder.buildValidationRequest()

        assertEquals("default", request.model)
        assertEquals(null, request.reasoning)
        assertEquals(true, request.veniceParameters?.disableThinking)
        assertEquals(null, request.responseFormat)
    }

    @Test
    fun `nullable error field uses type array in response schema`() {
        val schema = NutritionPromptBuilder.getNutritionJsonSchema()
        val errorTypes = schema["properties"]!!
            .jsonObject["error"]!!
            .jsonObject["type"]!!
            .jsonArray
            .map { it.jsonPrimitive.content }

        assertEquals(listOf("string", "null"), errorTypes)
    }

    @Test
    fun `item schema includes portion basis fields`() {
        val schema = NutritionPromptBuilder.getNutritionJsonSchema()
        val itemSchema = schema["properties"]!!
            .jsonObject["items"]!!
            .jsonObject["items"]!!
            .jsonObject
        val itemProperties = itemSchema["properties"]!!
            .jsonObject
        val required = itemSchema["required"]!!
            .jsonArray
            .map { it.jsonPrimitive.content }

        assertNotNull(itemProperties["grams"])
        assertNotNull(itemProperties["per_100g"])
        assertEquals(true, required.contains("grams"))
        assertEquals(true, required.contains("per_100g"))
    }
}
