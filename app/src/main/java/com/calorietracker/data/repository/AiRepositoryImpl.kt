package com.calorietracker.data.repository

import com.calorietracker.data.local.preferences.UserPreferencesDataStore
import com.calorietracker.data.local.security.SecureStorage
import com.calorietracker.data.remote.ai.AiConfig
import com.calorietracker.data.remote.ai.NutritionResponseParser
import com.calorietracker.data.remote.ai.cache.NutritionCache
import com.calorietracker.data.remote.ai.prompt.NutritionPromptBuilder
import com.calorietracker.data.remote.ai.stripCodeFences
import com.calorietracker.data.remote.ai.venice.ApiKeyHolder
import com.calorietracker.data.remote.ai.venice.VeniceApiService
import com.calorietracker.data.remote.ai.venice.VeniceRateLimitException
import com.calorietracker.data.remote.ai.venice.dto.ChatCompletionRequest
import com.calorietracker.data.remote.ai.venice.dto.ChatCompletionResponse
import com.calorietracker.data.remote.ai.venice.dto.ChatMessage
import com.calorietracker.data.remote.ai.venice.dto.JsonSchemaSpec
import com.calorietracker.data.remote.ai.venice.dto.ModelInfo
import com.calorietracker.data.remote.ai.venice.dto.ResponseFormat
import com.calorietracker.data.remote.ai.venice.dto.Usage
import com.calorietracker.data.remote.ai.venice.dto.VeniceParameters
import com.calorietracker.domain.model.NutritionInfo
import com.calorietracker.domain.repository.AiModel
import com.calorietracker.domain.repository.AiModelExtendedPricing
import com.calorietracker.domain.repository.AiModelPricing
import com.calorietracker.domain.repository.AiRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

private const val MODEL_PICKER_MODEL_LIMIT = 30

@Singleton
class AiRepositoryImpl @Inject constructor(
    private val veniceApiService: VeniceApiService,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val secureStorage: SecureStorage,
    private val nutritionCache: NutritionCache,
    private val apiKeyHolder: ApiKeyHolder,
    private val json: Json
) : AiRepository {

    override suspend fun analyzeFood(description: String): Result<NutritionInfo> {
        return try {
            // 1. Check cache
            val cached = nutritionCache.get(description)
            if (cached != null) return Result.success(cached)

            // 2. Load config
            val config = loadAiConfig()
            if (config.apiKey.isBlank()) {
                return Result.failure(IllegalStateException("API key not configured. Set your Venice AI key in Settings."))
            }
            apiKeyHolder.apiKey = config.apiKey

            // 3. Build request
            val customPrompt = userPreferencesDataStore.customPrompt.firstOrNull()
                ?: UserPreferencesDataStore.DEFAULT_CUSTOM_PROMPT
            val systemMessages = NutritionPromptBuilder.buildSystemMessages(customPrompt)
            val allMessages = systemMessages + ChatMessage(role = "user", content = description)

            val veniceParams = VeniceParameters(
                disableThinking = !config.thinkingEnabled,
                stripThinkingResponse = true,
                includeVeniceSystemPrompt = false
            )

            val request = ChatCompletionRequest(
                model = config.model,
                messages = allMessages,
                temperature = 0.3f,
                maxCompletionTokens = if (config.thinkingEnabled) 4096 else 2048,
                responseFormat = ResponseFormat(
                    type = "json_schema",
                    jsonSchema = JsonSchemaSpec(
                        name = "nutrition_result",
                        strict = true,
                        schema = NutritionPromptBuilder.getNutritionJsonSchema()
                    )
                ),
                veniceParameters = veniceParams
            )

            // 4. Call API
            val httpResponse = veniceApiService.chatCompletion(request)
            persistVeniceBalanceIfPresent(httpResponse)
            val response = unwrapChatCompletion(httpResponse)
            persistModelDeprecationWarningIfPresent(httpResponse, config.model)
            val choice = response.choices.firstOrNull()
                ?: return Result.failure(IllegalStateException("AI returned no choices"))
            val rawContent = choice.message.content
                ?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalStateException("Empty response from AI"))

            // 5. Parse
            val dto = try {
                NutritionResponseParser.parse(rawContent)
            } catch (e: Exception) {
                if (choice.finishReason == "length") {
                    val message = if (config.thinkingEnabled) {
                        "AI response was cut off before the JSON finished. Try again or turn off thinking mode."
                    } else {
                        "AI response was cut off before the JSON finished. Try again."
                    }
                    return Result.failure(
                        IllegalStateException(message)
                    )
                }
                throw e
            }
            if (!dto.error.isNullOrBlank()) {
                return Result.failure(IllegalArgumentException(dto.error))
            }

            val nutritionInfo = dto.toDomain()

            // 6. Cache
            nutritionCache.put(description, nutritionInfo)

            // 7. Track usage and cost
            val modelPricing = getSelectedModelPricing(config.model)
            val requestCost = calculateRequestCost(
                response.usage,
                modelPricing
            )
            userPreferencesDataStore.addTokenUsage(
                inputTokens = response.usage.promptTokens.toLong(),
                outputTokens = response.usage.completionTokens.toLong(),
                cost = requestCost
            )

            Result.success(nutritionInfo)

        } catch (e: VeniceRateLimitException) {
            Result.failure(e)
        } catch (e: HttpException) {
            when (e.code()) {
                401 -> Result.failure(SecurityException(parseVeniceErrorMessage(e, "Authentication failed")))
                402 -> Result.failure(IllegalStateException("Insufficient Venice balance. Add credits and try again."))
                else -> Result.failure(IOException(parseVeniceErrorMessage(e, "Server error")))
            }
        } catch (e: IOException) {
            Result.failure(IOException("Network error. Check your internet connection.", e))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Failed to analyze food: ${e.message}", e))
        }
    }

    override suspend fun validateApiKey(apiKey: String): Result<Boolean> {
        return try {
            val previousKey = apiKeyHolder.apiKey
            apiKeyHolder.apiKey = apiKey
            try {
                val request = NutritionPromptBuilder.buildValidationRequest()
                val httpResponse = veniceApiService.chatCompletion(request)
                unwrapChatCompletion(httpResponse)
                Result.success(true)
            } finally {
                apiKeyHolder.apiKey = previousKey
            }
        } catch (e: HttpException) {
            if (e.code() == 401) {
                val error = parseVeniceError(e, "Authentication failed")
                when (error.code) {
                    "INVALID_API_KEY", "AUTHENTICATION_FAILED" -> Result.success(false)
                    "AUTHENTICATION_FAILED_INACTIVE_KEY" -> {
                        Result.failure(SecurityException("This API key is inactive. Reactivate it in your Venice dashboard."))
                    }
                    else -> Result.failure(SecurityException(error.message))
                }
            } else {
                Result.failure(IOException(parseVeniceErrorMessage(e, "Server error")))
            }
        } catch (e: IOException) {
            Result.failure(IOException("Network error. Check your internet connection.", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchModels(): Result<List<AiModel>> {
        return try {
            val config = loadAiConfig()
            if (config.apiKey.isBlank()) return Result.failure(IllegalStateException("API key not configured."))
            apiKeyHolder.apiKey = config.apiKey

            val response = veniceApiService.listModels(type = "text")

            val allSupportedModels = response.data
                .asSequence()
                .filter { it.type == "text" }
                .filter { model ->
                    val spec = model.modelSpec
                    spec != null &&
                        spec.privacy == "private" &&
                        !model.hasBetaTag() &&
                        spec.deprecation == null &&
                        spec.capabilities?.supportsResponseSchema == true
                }
                .toList()
            val latestGlmModel = allSupportedModels
                .filter { it.isGlmModel() }
                .maxByOrNull { it.created }

            val latestPrivateModels = allSupportedModels
                .sortedByDescending { it.created }
                .take(MODEL_PICKER_MODEL_LIMIT)

            val pickerModels = if (
                latestGlmModel != null &&
                latestPrivateModels.none { it.id == latestGlmModel.id }
            ) {
                latestPrivateModels.dropLast(1) + latestGlmModel
            } else {
                latestPrivateModels
            }

            val result = pickerModels
                .distinctBy { it.id }
                .sortedByDescending { it.created }
                .map { modelInfo ->
                    modelInfo.toAiModel()
                }

            // Cache pricing for cost calculation
            cachedModelPricing = result
                .filter { it.pricing != null }
                .associate { it.id to it.pricing!! }

            Result.success(result)
        } catch (e: HttpException) {
            if (e.code() == 401) Result.failure(SecurityException(parseVeniceErrorMessage(e, "Authentication failed")))
            else Result.failure(IOException(parseVeniceErrorMessage(e, "Server error")))
        } catch (e: IOException) {
            Result.failure(IOException("Network error.", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun improvePrompt(currentPrompt: String): Result<String> {
        return try {
            val config = loadAiConfig()
            if (config.apiKey.isBlank()) return Result.failure(IllegalStateException("API key not configured."))
            apiKeyHolder.apiKey = config.apiKey

            val request = ChatCompletionRequest(
                model = config.model,
                messages = listOf(
                    ChatMessage(role = "system", content = "You improve prompts. Return only the improved prompt text."),
                    ChatMessage(role = "user", content = """Improve this nutrition AI system prompt for clarity and effectiveness. Keep the same intent. Return ONLY the improved prompt text.

Current prompt:
$currentPrompt""")
                ),
                temperature = 0.5f,
                maxCompletionTokens = 512,
                veniceParameters = VeniceParameters(
                    disableThinking = true,
                    stripThinkingResponse = true,
                    includeVeniceSystemPrompt = false
                )
            )

            val httpResponse = veniceApiService.chatCompletion(request)
            persistVeniceBalanceIfPresent(httpResponse)
            val response = unwrapChatCompletion(httpResponse)
            persistModelDeprecationWarningIfPresent(httpResponse, config.model)
            val improved = response.choices.firstOrNull()?.message?.content
                ?.let(::stripCodeFences)
                ?.trim()
                ?: return Result.failure(IllegalStateException("Empty response"))

            val modelPricing = getSelectedModelPricing(config.model)
            val requestCost = calculateRequestCost(
                response.usage,
                modelPricing
            )
            userPreferencesDataStore.addTokenUsage(
                inputTokens = response.usage.promptTokens.toLong(),
                outputTokens = response.usage.completionTokens.toLong(),
                cost = requestCost
            )

            Result.success(improved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun unwrapChatCompletion(
        response: Response<ChatCompletionResponse>
    ): ChatCompletionResponse {
        if (response.isSuccessful) {
            return response.body()
                ?: throw IllegalStateException("Empty response body from AI")
        }
        if (response.code() == 429) {
            val nowMillis = System.currentTimeMillis()
            val resetSeconds = response.headers()["x-ratelimit-reset-requests"]
                ?.toLongOrNull()
                ?.let { resetAt ->
                    val resetMillis = if (resetAt > 9_999_999_999L) resetAt else resetAt * 1_000L
                    ((max(0L, resetMillis - nowMillis) + 999L) / 1_000L).toInt()
                }
                ?: response.headers()["retry-after"]?.toIntOrNull()
            throw VeniceRateLimitException(
                secondsUntilReset = resetSeconds,
                message = if (resetSeconds != null) {
                    "Rate limit reached — try again in ${resetSeconds}s"
                } else {
                    "Rate limit reached — try again later"
                }
            )
        }
        throw HttpException(response)
    }

    private suspend fun persistVeniceBalanceIfPresent(
        response: Response<ChatCompletionResponse>
    ) {
        val headers = response.headers()
        val usd = headers["x-venice-balance-usd"]?.trim()?.takeIf { it.isNotEmpty() }
        val diem = headers["x-venice-balance-diem"]?.trim()?.takeIf { it.isNotEmpty() }
        if (usd != null || diem != null) {
            userPreferencesDataStore.setVeniceBalance(usd = usd, diem = diem)
        }
    }

    private suspend fun persistModelDeprecationWarningIfPresent(
        response: Response<ChatCompletionResponse>,
        requestedModel: String
    ) {
        val warning = response.headers()["x-venice-model-deprecation-warning"]
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return
        val modelId = response.headers()["x-venice-model-id"]
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: requestedModel
        userPreferencesDataStore.setModelDeprecationWarning(modelId, warning)
    }

    @Volatile
    private var cachedModelPricing: Map<String, AiModelPricing> = emptyMap()

    private fun getSelectedModelPricing(modelId: String): AiModelPricing? {
        return cachedModelPricing[modelId]
    }

    private fun calculateRequestCost(
        usage: Usage,
        pricing: AiModelPricing?
    ): Float {
        if (pricing == null) return 0f
        val promptTokens = usage.promptTokens
        val cachedTokens = usage.promptTokensDetails?.cachedTokens ?: 0
        val cacheCreationTokens = usage.promptTokensDetails?.cacheCreationInputTokens ?: 0
        val uncachedInputTokens = (promptTokens - cachedTokens - cacheCreationTokens).coerceAtLeast(0)
        val rates = pricing.extended
            ?.takeIf { promptTokens > it.contextTokenThreshold }
            ?.let {
                AiModelPricing(
                    inputPerMillion = it.inputPerMillion,
                    outputPerMillion = it.outputPerMillion,
                    cacheInputPerMillion = it.cacheInputPerMillion,
                    cacheWritePerMillion = it.cacheWritePerMillion
                )
            }
            ?: pricing
        val inputCost = uncachedInputTokens * rates.inputPerMillion +
            cachedTokens * rates.cacheInputPerMillion +
            cacheCreationTokens * rates.cacheWritePerMillion
        val outputCost = usage.completionTokens * rates.outputPerMillion
        return (inputCost + outputCost) / 1_000_000f
    }

    private suspend fun loadAiConfig(): AiConfig {
        val apiKey = secureStorage.getApiKey() ?: ""
        val model = userPreferencesDataStore.selectedModel.firstOrNull() ?: UserPreferencesDataStore.DEFAULT_MODEL
        val thinking = userPreferencesDataStore.thinkingEnabled.firstOrNull() ?: false
        return AiConfig(apiKey = apiKey, model = model, thinkingEnabled = thinking)
    }

    private fun parseVeniceErrorMessage(
        exception: HttpException,
        fallbackLabel: String
    ): String = parseVeniceError(exception, fallbackLabel).message

    private fun parseVeniceError(
        exception: HttpException,
        fallbackLabel: String
    ): VeniceError {
        return parseVeniceError(
            body = exception.response()?.errorBody()?.string()?.trim().orEmpty(),
            fallback = "$fallbackLabel (${exception.code()}): ${exception.message()}"
        )
    }

    private fun parseVeniceError(
        body: String,
        fallback: String
    ): VeniceError {
        if (body.isBlank()) return VeniceError(message = fallback)

        return try {
            val root = json.parseToJsonElement(body).jsonObject
            val error = root["error"]?.jsonPrimitive?.contentOrNull
            val code = root["code"]?.jsonPrimitive?.contentOrNull
            val details = root["details"]?.let(::summarizeErrorDetails)

            VeniceError(message = buildString {
                append(error ?: fallback)
                if (!details.isNullOrBlank()) {
                    append(": ")
                    append(details)
                }
            }, code = code)
        } catch (_: Exception) {
            VeniceError(message = body)
        }
    }

    private fun summarizeErrorDetails(element: JsonElement): String? {
        return when (element) {
            is JsonObject -> {
                val rootErrors = element["_errors"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                    .orEmpty()
                if (rootErrors.isNotEmpty()) {
                    rootErrors.joinToString("; ")
                } else {
                    element.entries
                        .asSequence()
                        .filter { it.key != "_errors" }
                        .mapNotNull { (key, value) ->
                            summarizeErrorDetails(value)?.let { "$key: $it" }
                        }
                        .joinToString("; ")
                        .ifBlank { null }
                }
            }

            is JsonArray -> element
                .mapNotNull(::summarizeErrorDetails)
                .joinToString("; ")
                .ifBlank { null }

            else -> element.jsonPrimitive.contentOrNull
        }
    }

    private fun ModelInfo.isGlmModel(): Boolean {
        return id.contains("glm", ignoreCase = true) ||
            modelSpec?.name?.contains("glm", ignoreCase = true) == true
    }

    private fun ModelInfo.hasBetaTag(): Boolean {
        return modelSpec?.traits?.any { it.contains("beta", ignoreCase = true) } == true
    }

    private fun ModelInfo.toAiModel(): AiModel {
        return AiModel(
            id = id,
            name = modelSpec?.name ?: id,
            createdAtEpochSeconds = created,
            traits = modelSpec?.traits ?: emptyList(),
            supportsResponseSchema = modelSpec?.capabilities?.supportsResponseSchema ?: false,
            supportsReasoning = modelSpec?.capabilities?.supportsReasoning ?: false,
            pricing = modelSpec?.pricing?.let {
                AiModelPricing(
                    inputPerMillion = it.input?.usd ?: 0f,
                    outputPerMillion = it.output?.usd ?: 0f,
                    cacheInputPerMillion = it.cacheInput?.usd ?: it.input?.usd ?: 0f,
                    cacheWritePerMillion = it.cacheWrite?.usd ?: it.input?.usd ?: 0f,
                    extended = it.extended?.let { extended ->
                        AiModelExtendedPricing(
                            contextTokenThreshold = extended.contextTokenThreshold,
                            inputPerMillion = extended.input.usd,
                            outputPerMillion = extended.output.usd,
                            cacheInputPerMillion = extended.cacheInput?.usd ?: extended.input.usd,
                            cacheWritePerMillion = extended.cacheWrite?.usd ?: extended.input.usd
                        )
                    }
                )
            }
        )
    }

    private data class VeniceError(
        val message: String,
        val code: String? = null
    )
}
