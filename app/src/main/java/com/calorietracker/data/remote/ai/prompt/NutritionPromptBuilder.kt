package com.calorietracker.data.remote.ai.prompt

import com.calorietracker.data.remote.ai.venice.dto.ChatCompletionRequest
import com.calorietracker.data.remote.ai.venice.dto.ChatMessage
import com.calorietracker.data.remote.ai.venice.dto.JsonSchemaSpec
import com.calorietracker.data.remote.ai.venice.dto.ResponseFormat
import com.calorietracker.data.remote.ai.venice.dto.VeniceParameters
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object NutritionPromptBuilder {

    private const val BASE_PROMPT =
        "You are a nutrition analysis assistant. Analyze the food description and return estimated nutritional values.\n" +
        "\n" +
        "Rules:\n" +
        "- The user typically provides each food item on its own line prefixed with \"- \". Treat each such line as a distinct item.\n" +
        "- For composite meals, break down into individual items and sum the totals.\n" +
        "- Use USDA / standard nutritional databases as reference.\n" +
        "- Return integer calories, float grams rounded to 1 decimal.\n" +
        "- If the input is not food, set the \"error\" field to explain why and use 0 for all numeric fields.\n" +
        "- Each item in \"items\" must have: name, amount, calories, protein_g, carbs_g, fat_g.\n" +
        "- The top-level calories/protein_g/carbs_g/fat_g must equal the sum of all items.\n" +
        "\n" +
        "Name and amount field formatting (important):\n" +
        "- The \"name\" field must contain ONLY the food name — never a quantity or unit.\n" +
        "- Write the name in the user's language, normalized to its base grammatical form (nominative case, as it would appear in a dictionary or on a menu). For example Croatian genitive \"pecenog krumpira\" becomes \"peceni krumpir\". Use a number (singular/plural) that reads naturally next to the amount.\n" +
        "- The \"amount\" field must contain ONLY the quantity and unit, using the same unit the user typed. Do NOT repeat the food name in \"amount\" and do NOT append a normalized gram conversion.\n" +
        "- If the user named an obvious single item without a quantity, use one natural serving and do not write \"(assumed)\".\n" +
        "- For vague bulk foods without a quantity, assume a typical serving for an average adult and append \"(assumed)\".\n" +
        "- Internally use grams for the calorie/macro math, but never surface those grams in \"amount\" unless the user typed grams.\n" +
        "Examples:\n" +
        "  Input \"5 eggs\" -> name: \"eggs\", amount: \"5\"\n" +
        "  Input \"200g milk\" -> name: \"milk\", amount: \"200g\"\n" +
        "  Input \"2dl whole milk\" -> name: \"whole milk\", amount: \"2dl\"\n" +
        "  Input \"a slice of bread\" -> name: \"bread\", amount: \"1 slice\"\n" +
        "  Input \"200g pecenog krumpira\" -> name: \"peceni krumpir\", amount: \"200g\"\n" +
        "  Input \"5 jagoda\" -> name: \"jagode\", amount: \"5\"\n" +
        "  Input \"lepinja\" -> name: \"lepinja\", amount: \"1\"\n" +
        "  Input \"cajna pasteta\" -> name: \"cajna pasteta\", amount: \"1\"\n" +
        "  Input \"eggs\" -> name: \"eggs\", amount: \"2 (assumed)\"\n" +
        "  Input \"milk\" -> name: \"milk\", amount: \"1 cup (assumed)\""

    fun buildSystemMessages(userPrompt: String): List<ChatMessage> {
        return listOf(
            ChatMessage(role = "system", content = userPrompt),
            ChatMessage(role = "system", content = BASE_PROMPT)
        )
    }

    fun buildNutritionRequest(
        description: String,
        userPrompt: String,
        model: String,
        thinkingEnabled: Boolean
    ): ChatCompletionRequest {
        val messages = buildSystemMessages(userPrompt) + ChatMessage(
            role = "user",
            content = description
        )

        return ChatCompletionRequest(
            model = model,
            messages = messages,
            temperature = 0.3f,
            maxCompletionTokens = if (thinkingEnabled) 4096 else 2048,
            responseFormat = ResponseFormat(
                type = "json_schema",
                jsonSchema = JsonSchemaSpec(
                    name = "nutrition_analysis",
                    strict = true,
                    schema = buildNutritionJsonSchema()
                )
            ),
            veniceParameters = VeniceParameters(
                disableThinking = !thinkingEnabled,
                stripThinkingResponse = true,
                includeVeniceSystemPrompt = false
            )
        )
    }

    fun buildValidationRequest(): ChatCompletionRequest {
        return ChatCompletionRequest(
            model = "default",
            messages = listOf(
                ChatMessage(role = "user", content = "ping")
            ),
            maxCompletionTokens = 1,
            veniceParameters = VeniceParameters(
                disableThinking = true,
                stripThinkingResponse = true,
                includeVeniceSystemPrompt = false
            )
        )
    }

    fun getNutritionJsonSchema(): JsonObject = buildNutritionJsonSchema()

    private fun buildNutritionJsonSchema(): JsonObject {
        val nutritionItemSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("name", buildJsonObject { put("type", "string") })
                put("amount", buildJsonObject { put("type", "string") })
                put("calories", buildJsonObject { put("type", "integer") })
                put("protein_g", buildJsonObject { put("type", "number") })
                put("carbs_g", buildJsonObject { put("type", "number") })
                put("fat_g", buildJsonObject { put("type", "number") })
            })
            put("required", buildJsonArray {
                add(JsonPrimitive("name"))
                add(JsonPrimitive("amount"))
                add(JsonPrimitive("calories"))
                add(JsonPrimitive("protein_g"))
                add(JsonPrimitive("carbs_g"))
                add(JsonPrimitive("fat_g"))
            })
            put("additionalProperties", JsonPrimitive(false))
        }

        return buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("calories", buildJsonObject { put("type", "integer") })
                put("protein_g", buildJsonObject { put("type", "number") })
                put("carbs_g", buildJsonObject { put("type", "number") })
                put("fat_g", buildJsonObject { put("type", "number") })
                put("items", buildJsonObject {
                    put("type", "array")
                    put("items", nutritionItemSchema)
                })
                put("error", buildJsonObject {
                    put("type", buildJsonArray {
                        add(JsonPrimitive("string"))
                        add(JsonPrimitive("null"))
                    })
                })
            })
            put("required", buildJsonArray {
                add(JsonPrimitive("calories"))
                add(JsonPrimitive("protein_g"))
                add(JsonPrimitive("carbs_g"))
                add(JsonPrimitive("fat_g"))
                add(JsonPrimitive("items"))
                add(JsonPrimitive("error"))
            })
            put("additionalProperties", JsonPrimitive(false))
        }
    }
}
