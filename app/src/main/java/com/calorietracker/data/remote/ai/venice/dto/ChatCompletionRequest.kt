package com.calorietracker.data.remote.ai.venice.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.3f,
    @SerialName("max_completion_tokens")
    val maxCompletionTokens: Int? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    @SerialName("response_format")
    val responseFormat: ResponseFormat? = null,
    val reasoning: Reasoning? = null,
    @SerialName("venice_parameters")
    val veniceParameters: VeniceParameters? = null
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ResponseFormat(
    val type: String,
    @SerialName("json_schema")
    val jsonSchema: JsonSchemaSpec? = null
)

@Serializable
data class JsonSchemaSpec(
    val name: String,
    val strict: Boolean = true,
    val schema: JsonObject
)

@Serializable
data class Reasoning(
    val effort: String? = null,
    val summary: String? = null
)

@Serializable
data class VeniceParameters(
    @SerialName("disable_thinking")
    val disableThinking: Boolean = true,
    @SerialName("strip_thinking_response")
    val stripThinkingResponse: Boolean = true,
    @SerialName("include_venice_system_prompt")
    val includeVeniceSystemPrompt: Boolean = false
)
