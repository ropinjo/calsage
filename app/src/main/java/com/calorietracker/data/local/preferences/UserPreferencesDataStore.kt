package com.calorietracker.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.calorietracker.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SEX = stringPreferencesKey("user_sex")
        val AGE = intPreferencesKey("user_age")
        val HEIGHT_CM = floatPreferencesKey("user_height_cm")
        val WEIGHT_KG = floatPreferencesKey("user_weight_kg")
        val ACTIVITY_LEVEL = stringPreferencesKey("user_activity_level")
        val GOAL_TYPE = stringPreferencesKey("user_goal_type")
        val VENICE_API_KEY = stringPreferencesKey("venice_api_key_ref")
        val SELECTED_MODEL = stringPreferencesKey("selected_ai_model")
        val THINKING_ENABLED = booleanPreferencesKey("thinking_enabled")
        val CUSTOM_PROMPT = stringPreferencesKey("custom_prompt")
        val USE_METRIC = booleanPreferencesKey("use_metric")
        val TOTAL_INPUT_TOKENS = longPreferencesKey("total_input_tokens")
        val TOTAL_OUTPUT_TOKENS = longPreferencesKey("total_output_tokens")
        val VENICE_BALANCE_USD = stringPreferencesKey("venice_balance_usd")
        val VENICE_BALANCE_DIEM = stringPreferencesKey("venice_balance_diem")
        val CUMULATIVE_COST = floatPreferencesKey("cumulative_cost")
        val MODEL_DEPRECATION_WARNING = stringPreferencesKey("model_deprecation_warning")
        val MODEL_DEPRECATION_WARNING_MODEL = stringPreferencesKey("model_deprecation_warning_model")
        val SELECTED_DATE = stringPreferencesKey("selected_date")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
    }

    companion object {
        const val DEFAULT_MODEL = "most_intelligent"
        const val DEFAULT_CUSTOM_PROMPT =
            "I'm tracking my daily nutrition and usually eat home-cooked meals. When amounts are missing, estimate realistic adult portions. Prefer slightly higher calorie estimates over lower ones."
    }

    // ---- Flow getters ----

    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETED] ?: false
    }

    /** Alias for [onboardingCompleted] used by MainActivity. */
    val hasCompletedOnboarding: Flow<Boolean> get() = onboardingCompleted

    val sex: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.SEX]
    }

    val age: Flow<Int?> = dataStore.data.map { prefs ->
        prefs[Keys.AGE]
    }

    val heightCm: Flow<Float?> = dataStore.data.map { prefs ->
        prefs[Keys.HEIGHT_CM]
    }

    val weightKg: Flow<Float?> = dataStore.data.map { prefs ->
        prefs[Keys.WEIGHT_KG]
    }

    val activityLevel: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.ACTIVITY_LEVEL]
    }

    val goalType: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.GOAL_TYPE]
    }

    val veniceApiKey: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.VENICE_API_KEY]
    }

    val selectedModel: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.SELECTED_MODEL] ?: DEFAULT_MODEL
    }

    val thinkingEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.THINKING_ENABLED] ?: false
    }

    val customPrompt: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.CUSTOM_PROMPT] ?: DEFAULT_CUSTOM_PROMPT
    }

    val useMetric: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.USE_METRIC] ?: true
    }

    val totalInputTokens: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.TOTAL_INPUT_TOKENS] ?: 0L
    }

    val totalOutputTokens: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.TOTAL_OUTPUT_TOKENS] ?: 0L
    }

    val cumulativeCost: Flow<Float> = dataStore.data.map { prefs ->
        prefs[Keys.CUMULATIVE_COST] ?: 0f
    }

    val veniceBalanceUsd: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.VENICE_BALANCE_USD]
    }

    val veniceBalanceDiem: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.VENICE_BALANCE_DIEM]
    }

    val modelDeprecationWarning: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.MODEL_DEPRECATION_WARNING]
    }

    val modelDeprecationWarningModel: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.MODEL_DEPRECATION_WARNING_MODEL]
    }

    val selectedDate: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.SELECTED_DATE]
    }

    /** Theme preference key: "dark" (default), "light", or "system". */
    val themePreference: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.THEME_PREFERENCE] ?: "dark"
    }

    /** Alias used by SettingsViewModel. */
    val selectedAiModel: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.SELECTED_MODEL]
    }

    /** Derived unit preference (kg/lb) from useMetric. */
    val selectedUnit: Flow<String> = useMetric.map { if (it) "kg" else "lb" }

    /** Alias for [totalInputTokens] used by SettingsViewModel. */
    val cumulativeInputTokens: Flow<Long> get() = totalInputTokens

    /** Alias for [totalOutputTokens] used by SettingsViewModel. */
    val cumulativeOutputTokens: Flow<Long> get() = totalOutputTokens

    /** Convenience composite flow for the user profile. */
    val userProfile: Flow<UserProfile> = dataStore.data.map { prefs ->
        UserProfile(
            sex = prefs[Keys.SEX],
            age = prefs[Keys.AGE],
            heightCm = prefs[Keys.HEIGHT_CM],
            weightKg = prefs[Keys.WEIGHT_KG],
            activityLevel = prefs[Keys.ACTIVITY_LEVEL],
            goalType = prefs[Keys.GOAL_TYPE]
        )
    }

    // ---- Suspend setters ----

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setSex(sex: String?) {
        dataStore.edit { prefs ->
            if (sex != null) prefs[Keys.SEX] = sex else prefs.remove(Keys.SEX)
        }
    }

    suspend fun setAge(age: Int?) {
        dataStore.edit { prefs ->
            if (age != null) prefs[Keys.AGE] = age else prefs.remove(Keys.AGE)
        }
    }

    suspend fun setHeightCm(heightCm: Float?) {
        dataStore.edit { prefs ->
            if (heightCm != null) prefs[Keys.HEIGHT_CM] = heightCm else prefs.remove(Keys.HEIGHT_CM)
        }
    }

    suspend fun setWeightKg(weightKg: Float?) {
        dataStore.edit { prefs ->
            if (weightKg != null) prefs[Keys.WEIGHT_KG] = weightKg else prefs.remove(Keys.WEIGHT_KG)
        }
    }

    suspend fun setActivityLevel(activityLevel: String?) {
        dataStore.edit { prefs ->
            if (activityLevel != null) prefs[Keys.ACTIVITY_LEVEL] = activityLevel else prefs.remove(Keys.ACTIVITY_LEVEL)
        }
    }

    suspend fun setGoalType(goalType: String?) {
        dataStore.edit { prefs ->
            if (goalType != null) prefs[Keys.GOAL_TYPE] = goalType else prefs.remove(Keys.GOAL_TYPE)
        }
    }

    suspend fun setVeniceApiKey(keyRef: String?) {
        dataStore.edit { prefs ->
            if (keyRef != null) prefs[Keys.VENICE_API_KEY] = keyRef else prefs.remove(Keys.VENICE_API_KEY)
        }
    }

    suspend fun setSelectedModel(model: String) {
        dataStore.edit { prefs ->
            val previousModel = prefs[Keys.SELECTED_MODEL] ?: DEFAULT_MODEL
            prefs[Keys.SELECTED_MODEL] = model
            if (previousModel != model) {
                prefs.remove(Keys.MODEL_DEPRECATION_WARNING)
                prefs.remove(Keys.MODEL_DEPRECATION_WARNING_MODEL)
            }
        }
    }

    suspend fun setThinkingEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.THINKING_ENABLED] = enabled
        }
    }

    suspend fun setCustomPrompt(prompt: String?) {
        dataStore.edit { prefs ->
            if (prompt != null) prefs[Keys.CUSTOM_PROMPT] = prompt else prefs.remove(Keys.CUSTOM_PROMPT)
        }
    }

    suspend fun setUseMetric(metric: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.USE_METRIC] = metric
        }
    }

    suspend fun setTotalInputTokens(tokens: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.TOTAL_INPUT_TOKENS] = tokens
        }
    }

    suspend fun setTotalOutputTokens(tokens: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.TOTAL_OUTPUT_TOKENS] = tokens
        }
    }

    suspend fun addTokenUsage(inputTokens: Long, outputTokens: Long, cost: Float = 0f) {
        dataStore.edit { prefs ->
            val currentInput = prefs[Keys.TOTAL_INPUT_TOKENS] ?: 0L
            val currentOutput = prefs[Keys.TOTAL_OUTPUT_TOKENS] ?: 0L
            prefs[Keys.TOTAL_INPUT_TOKENS] = currentInput + inputTokens
            prefs[Keys.TOTAL_OUTPUT_TOKENS] = currentOutput + outputTokens
            if (cost > 0f) {
                val currentCost = prefs[Keys.CUMULATIVE_COST] ?: 0f
                prefs[Keys.CUMULATIVE_COST] = currentCost + cost
            }
        }
    }

    suspend fun setVeniceBalance(usd: String?, diem: String?) {
        dataStore.edit { prefs ->
            if (usd != null) prefs[Keys.VENICE_BALANCE_USD] = usd else prefs.remove(Keys.VENICE_BALANCE_USD)
            if (diem != null) prefs[Keys.VENICE_BALANCE_DIEM] = diem else prefs.remove(Keys.VENICE_BALANCE_DIEM)
        }
    }

    suspend fun setModelDeprecationWarning(modelId: String?, warning: String?) {
        dataStore.edit { prefs ->
            if (!modelId.isNullOrBlank() && !warning.isNullOrBlank()) {
                prefs[Keys.MODEL_DEPRECATION_WARNING_MODEL] = modelId
                prefs[Keys.MODEL_DEPRECATION_WARNING] = warning
            } else {
                prefs.remove(Keys.MODEL_DEPRECATION_WARNING_MODEL)
                prefs.remove(Keys.MODEL_DEPRECATION_WARNING)
            }
        }
    }

    suspend fun setSelectedDate(date: String) {
        dataStore.edit { prefs ->
            prefs[Keys.SELECTED_DATE] = date
        }
    }

    suspend fun setThemePreference(theme: String) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_PREFERENCE] = theme
        }
    }

    /** Alias for [setSelectedModel] used by SettingsViewModel. */
    suspend fun setSelectedAiModel(model: String) = setSelectedModel(model)

    /** Derived unit setter from string "kg"/"lb". */
    suspend fun setSelectedUnit(unit: String) = setUseMetric(unit == "kg")

    /** Alias for [setOnboardingCompleted]. */
    suspend fun setHasCompletedOnboarding(completed: Boolean) = setOnboardingCompleted(completed)

    /** Batch-save the entire user profile in a single transaction. */
    suspend fun saveUserProfile(profile: UserProfile) {
        dataStore.edit { prefs ->
            profile.sex?.let { prefs[Keys.SEX] = it } ?: prefs.remove(Keys.SEX)
            profile.age?.let { prefs[Keys.AGE] = it } ?: prefs.remove(Keys.AGE)
            profile.heightCm?.let { prefs[Keys.HEIGHT_CM] = it } ?: prefs.remove(Keys.HEIGHT_CM)
            profile.weightKg?.let { prefs[Keys.WEIGHT_KG] = it } ?: prefs.remove(Keys.WEIGHT_KG)
            profile.activityLevel?.let { prefs[Keys.ACTIVITY_LEVEL] = it } ?: prefs.remove(Keys.ACTIVITY_LEVEL)
            profile.goalType?.let { prefs[Keys.GOAL_TYPE] = it } ?: prefs.remove(Keys.GOAL_TYPE)
        }
    }

    suspend fun resetTokenUsage() {
        dataStore.edit { prefs ->
            prefs[Keys.TOTAL_INPUT_TOKENS] = 0L
            prefs[Keys.TOTAL_OUTPUT_TOKENS] = 0L
        }
    }

    suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
