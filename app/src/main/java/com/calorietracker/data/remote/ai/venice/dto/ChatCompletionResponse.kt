package com.calorietracker.data.remote.ai.venice.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionResponse(
    val id: String,
    val choices: List<Choice>,
    val usage: Usage
)

@Serializable
data class Choice(
    val message: AssistantMessage,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class AssistantMessage(
    val content: String? = null,
    @SerialName("reasoning_content")
    val reasoningContent: String? = null,
    val role: String
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int,
    @SerialName("prompt_tokens_details")
    val promptTokensDetails: PromptTokensDetails? = null,
    @SerialName("completion_tokens_details")
    val completionTokensDetails: CompletionTokensDetails? = null
)

@Serializable
data class PromptTokensDetails(
    @SerialName("cached_tokens")
    val cachedTokens: Int = 0,
    @SerialName("cache_creation_input_tokens")
    val cacheCreationInputTokens: Int = 0
)

@Serializable
data class CompletionTokensDetails(
    @SerialName("reasoning_tokens")
    val reasoningTokens: Int = 0
)
