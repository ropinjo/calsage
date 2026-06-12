package com.calorietracker.presentation.screen.onboarding

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorietracker.data.export.CsvImportManager
import com.calorietracker.data.export.ImportResult
import com.calorietracker.data.local.preferences.UserPreferencesDataStore
import com.calorietracker.data.local.security.SecureStorage
import com.calorietracker.domain.model.UserGoals
import com.calorietracker.domain.model.UserProfile
import com.calorietracker.domain.repository.AiModel
import com.calorietracker.domain.repository.AiRepository
import com.calorietracker.domain.repository.GoalsRepository
import com.calorietracker.domain.util.TdeeCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingState(
    val currentStep: Int = 0,
    val sex: String = "male",
    val age: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val activityLevel: String = "sedentary",
    val goalType: String = "maintain",
    val calculatedBmr: Float? = null,
    val calculatedTdee: Float? = null,
    val calorieTarget: String = "2000",
    val proteinTarget: String = "150",
    val carbsTarget: String = "200",
    val fatTarget: String = "67",
    val apiKey: String = "",
    val apiKeyValid: Boolean = false,
    val isValidatingApiKey: Boolean = false,
    val apiKeyError: String? = null,
    val selectedModel: String? = null,
    val thinkingEnabled: Boolean = false,
    val totalSteps: Int = 5
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val goalsRepository: GoalsRepository,
    private val secureStorage: SecureStorage,
    private val aiRepository: AiRepository,
    private val csvImportManager: CsvImportManager
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importEvents = MutableSharedFlow<OnboardingImportEvent>()
    val importEvents: SharedFlow<OnboardingImportEvent> = _importEvents.asSharedFlow()

    fun importBackup(uri: Uri) {
        if (_isImporting.value) return
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val result = csvImportManager.importData(uri)
                userPreferencesDataStore.setHasCompletedOnboarding(true)
                _importEvents.emit(OnboardingImportEvent.Success(result))
            } catch (e: Exception) {
                _importEvents.emit(
                    OnboardingImportEvent.Failure(e.message ?: "Failed to import backup")
                )
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun nextStep() {
        _state.update { it.copy(currentStep = (it.currentStep + 1).coerceAtMost(it.totalSteps - 1)) }
    }

    fun previousStep() {
        _state.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0)) }
    }

    fun goToStep(step: Int) {
        _state.update { it.copy(currentStep = step.coerceIn(0, it.totalSteps - 1)) }
    }

    fun updateSex(sex: String) {
        _state.update { it.copy(sex = sex) }
        recalculate()
    }

    fun updateAge(age: String) {
        _state.update { it.copy(age = age) }
        recalculate()
    }

    fun updateHeight(height: String) {
        _state.update { it.copy(heightCm = height) }
        recalculate()
    }

    fun updateWeight(weight: String) {
        _state.update { it.copy(weightKg = weight) }
        recalculate()
    }

    fun updateActivityLevel(level: String) {
        _state.update { it.copy(activityLevel = level) }
        recalculate()
    }

    fun updateGoalType(goal: String) {
        _state.update { it.copy(goalType = goal) }
        recalculate()
    }

    fun updateCalorieTarget(target: String) {
        _state.update { it.copy(calorieTarget = target) }
    }

    fun updateProteinTarget(target: String) {
        _state.update { it.copy(proteinTarget = target) }
    }

    fun updateApiKey(key: String) {
        _state.update {
            it.copy(
                apiKey = key,
                apiKeyValid = false,
                apiKeyError = null
            )
        }
    }

    fun validateAndSaveApiKey() {
        val key = _state.value.apiKey.trim()
        if (key.isNotBlank()) {
            viewModelScope.launch {
                _state.update {
                    it.copy(
                        isValidatingApiKey = true,
                        apiKeyError = null
                    )
                }

                aiRepository.validateApiKey(key)
                    .onSuccess { isValid ->
                        if (isValid) {
                            secureStorage.saveApiKey(key)
                            val defaultModel = aiRepository.fetchModels()
                                .getOrNull()
                                ?.defaultModelOrNull()
                            _state.update {
                                it.copy(
                                    apiKeyValid = true,
                                    isValidatingApiKey = false,
                                    apiKeyError = null,
                                    selectedModel = defaultModel?.id
                                )
                            }
                        } else {
                            _state.update {
                                it.copy(
                                    apiKeyValid = false,
                                    isValidatingApiKey = false,
                                    apiKeyError = "Invalid API key"
                                )
                            }
                        }
                    }
                    .onFailure { error ->
                        _state.update {
                            it.copy(
                                apiKeyValid = false,
                                isValidatingApiKey = false,
                                apiKeyError = error.message ?: "Failed to validate API key"
                            )
                        }
                    }
            }
        }
    }

    fun selectModel(model: String) {
        _state.update { it.copy(selectedModel = model) }
    }

    private fun List<AiModel>.defaultModelOrNull(): AiModel? {
        // Betas are listed for manual selection but never auto-assigned —
        // Venice marks them "not recommended for production use".
        val stable = filterNot { it.isBeta }.ifEmpty { this }
        return stable.filter { model ->
            model.id.contains("glm", ignoreCase = true) ||
                model.name.contains("glm", ignoreCase = true)
        }.maxByOrNull { it.createdAtEpochSeconds }
            ?: stable.maxByOrNull { it.createdAtEpochSeconds }
    }

    fun toggleThinking() {
        _state.update { it.copy(thinkingEnabled = !it.thinkingEnabled) }
    }

    private fun recalculate() {
        val s = _state.value
        val age = s.age.toIntOrNull() ?: return
        val height = s.heightCm.toFloatOrNull() ?: return
        val weight = s.weightKg.toFloatOrNull() ?: return

        val bmr = TdeeCalculator.calculateBmr(s.sex, weight, height, age)
        val tdee = TdeeCalculator.calculateTdee(bmr, s.activityLevel)
        val adjustment = TdeeCalculator.getGoalAdjustment(s.goalType)
        val target = (tdee + adjustment).toInt().coerceAtLeast(1200)

        val proteinGrams = (target * 0.30f / 4f).toInt()
        val carbsGrams = (target * 0.40f / 4f).toInt()
        val fatGrams = (target * 0.30f / 9f).toInt()

        _state.update {
            it.copy(
                calculatedBmr = bmr,
                calculatedTdee = tdee,
                calorieTarget = target.toString(),
                proteinTarget = proteinGrams.toString(),
                carbsTarget = carbsGrams.toString(),
                fatTarget = fatGrams.toString()
            )
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val s = _state.value

            // Save user profile
            val profile = UserProfile(
                sex = s.sex,
                age = s.age.toIntOrNull(),
                heightCm = s.heightCm.toFloatOrNull(),
                weightKg = s.weightKg.toFloatOrNull(),
                activityLevel = s.activityLevel,
                goalType = s.goalType
            )
            userPreferencesDataStore.saveUserProfile(profile)

            // Save goals; a 0 or unparsable calorie target falls back to the
            // default rather than saving a goal that breaks progress displays
            val goals = UserGoals(
                calorieTarget = s.calorieTarget.toIntOrNull()?.takeIf { it > 0 } ?: 2000,
                proteinTargetGrams = s.proteinTarget.toFloatOrNull()?.takeIf { it >= 0f } ?: 150f,
                carbsTargetGrams = s.carbsTarget.toFloatOrNull()?.takeIf { it >= 0f } ?: 200f,
                fatTargetGrams = s.fatTarget.toFloatOrNull()?.takeIf { it >= 0f } ?: 67f
            )
            goalsRepository.saveGoals(goals)

            // Save AI settings
            if (s.apiKeyValid) {
                s.selectedModel?.let { userPreferencesDataStore.setSelectedAiModel(it) }
                userPreferencesDataStore.setThinkingEnabled(s.thinkingEnabled)
            }

            // Mark onboarding complete
            userPreferencesDataStore.setHasCompletedOnboarding(true)
        }
    }
}

sealed interface OnboardingImportEvent {
    data class Success(val result: ImportResult) : OnboardingImportEvent
    data class Failure(val message: String) : OnboardingImportEvent
}
