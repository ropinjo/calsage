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
        "Estimation method (apply to every item):\n" +
        "- First recall the food's standard density per 100 g (per 100 ml for liquids) as eaten, using cooked values for cooked food: calories, protein, carbs and fat.\n" +
        "- The user weighs their food. When a weight or measure is given, use that exact amount — never round it, scale it, or second-guess it.\n" +
        "- When no quantity is given, assume a typical adult serving.\n" +
        "- Compute each value as density_per_100 * amount / 100.\n" +
        "- Check that each item is internally consistent: calories should be close to 4*protein_g + 4*carbs_g + 9*fat_g. If it is not, recheck the density before answering.\n" +
        "- When a food's species, cut or preparation is not specified, assume the most common everyday version (for example \"breast\" -> chicken breast, \"soup\" -> a broth-based soup).\n" +
        "\n" +
        "Reference densities per 100 g cooked / as eaten (per 100 ml for liquids) as calories, protein g, carbs g, fat g. Interpolate for similar foods; for anything not listed use standard nutritional databases:\n" +
        "  White bread / flatbread: 270, 9, 50, 3\n" +
        "  Cooked white rice: 130, 2.7, 28, 0.3\n" +
        "  Cooked pasta: 158, 6, 31, 1\n" +
        "  Boiled potato: 87, 2, 20, 0.1\n" +
        "  Roasted/grilled chicken breast, skinless: 165, 31, 0, 3.6\n" +
        "  Cooked lean meat (beef/pork): 210, 28, 0, 11\n" +
        "  Whole egg: 145, 13, 1, 10\n" +
        "  Whole milk: 62, 3.2, 4.8, 3.3\n" +
        "  Hard cheese: 400, 26, 1, 33\n" +
        "  Clear broth soup with noodles and vegetables: 35, 2, 5, 1\n" +
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
            temperature = 0.0f,
            maxCompletionTokens = if (thinkingEnabled) 4096 else 2048,
            responseFormat = ResponseFormat(
                type = "json_schema",
                jsonSchema = JsonSchemaSpec(
                    name = "nutrition_result",
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
