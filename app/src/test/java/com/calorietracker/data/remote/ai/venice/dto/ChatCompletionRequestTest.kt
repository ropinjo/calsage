package com.calorietracker.data.remote.ai.venice.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatCompletionRequestTest {

    @Test
    fun `request serializes max completion tokens without deprecated max tokens`() {
        val json = Json {
            encodeDefaults = true
            explicitNulls = false
        }
        val request = ChatCompletionRequest(
            model = "qwen3-5-9b",
            messages = listOf(ChatMessage(role = "user", content = "ping")),
            maxCompletionTokens = 4096
        )

        val encoded = json.encodeToString(request)

        assertTrue(encoded.contains("\"max_completion_tokens\":4096"))
        assertFalse(encoded.contains("\"max_tokens\""))
    }

    @Test
    fun `request omits reasoning when unset`() {
        val json = Json {
            encodeDefaults = true
            explicitNulls = false
        }
        val request = ChatCompletionRequest(
            model = "qwen3-5-9b",
            messages = listOf(ChatMessage(role = "user", content = "ping"))
        )

        val encoded = json.encodeToString(request)

        assertFalse(encoded.contains("\"reasoning\""))
    }

    @Test
    fun `request serializes reasoning effort shape`() {
        val json = Json {
            encodeDefaults = true
            explicitNulls = false
        }
        val request = ChatCompletionRequest(
            model = "qwen3-5-9b",
            messages = listOf(ChatMessage(role = "user", content = "ping")),
            reasoning = Reasoning(effort = "low")
        )

        val encoded = json.encodeToString(request)

        assertTrue(encoded.contains("\"reasoning\":{\"effort\":\"low\"}"))
    }
}
