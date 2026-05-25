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
        "Amount field formatting (important):\n" +
        "- The \"amount\" field must echo the user's portion wording verbatim, with the same unit they used. Do NOT append a normalized gram conversion in parentheses or any other form.\n" +
        "- If the user named an obvious single item without a quantity, use one natural serving and do not write \"(assumed)\".\n" +
        "- For vague bulk foods without a quantity, assume a typical serving for an average adult and write it in natural language followed by \"(assumed)\".\n" +
        "- Internally use grams for the calorie/macro math, but never surface those grams in \"amount\" unless the user typed grams.\n" +
        "Examples:\n" +
        "  Input \"5 eggs\" -> amount: \"5 eggs\"\n" +
        "  Input \"200g milk\" -> amount: \"200g milk\"\n" +
        "  Input \"2dl whole milk\" -> amount: \"2dl whole milk\"\n" +
        "  Input \"a slice of bread\" -> amount: \"1 slice of bread\"\n" +
        "  Input \"lepinja\" -> amount: \"1 lepinja\"\n" +
        "  Input \"cajna pasteta\" -> amount: \"1 cajna pasteta\"\n" +
        "  Input \"eggs\" -> amount: \"2 eggs (assumed)\"\n" +
        "  Input \"milk\" -> amount: \"1 cup milk (assumed)\""

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
