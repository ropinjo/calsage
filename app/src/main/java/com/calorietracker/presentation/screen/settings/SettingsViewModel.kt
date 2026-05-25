package com.calorietracker.presentation.screen.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorietracker.data.export.CsvExportManager
import com.calorietracker.data.export.CsvImportManager
import com.calorietracker.data.export.ExportResult
import com.calorietracker.data.export.ImportMode
import com.calorietracker.data.export.ImportResult
import com.calorietracker.data.local.db.AppDatabase
import com.calorietracker.data.local.preferences.UserPreferencesDataStore
import com.calorietracker.data.local.security.SecureStorage
import com.calorietracker.domain.repository.AiModel
import com.calorietracker.domain.repository.AiRepository
import com.calorietracker.domain.repository.GoalsRepository
import com.calorietracker.domain.model.UserGoals
import com.calorietracker.presentation.theme.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsUiState(
    val hasApiKey: Boolean = false,
    val maskedApiKey: String = "",
    val selectedModel: String? = null,
    val availableModels: List<AiModel> = emptyList(),
    val selectedModelSupportsThinking: Boolean = false,
    val isLoadingModels: Boolean = false,
    val modelLoadError: String? = null,
    val thinkingEnabled: Boolean = false,
    val customPrompt: String? = null,
    val calorieTarget: Int = 2000,
    val proteinTarget: Float = 150f,
    val carbsTarget: Float = 250f,
    val fatTarget: Float = 65f,
    val selectedUnit: String = "kg",
    val inputTokens: Long = 0L,
    val outputTokens: Long = 0L,
    val cumulativeCost: Float = 0f,
    val veniceBalanceUsd: String? = null,
    val veniceBalanceDiem: String? = null,
    val modelDeprecationWarning: String? = null,
    val isSavingApiKey: Boolean = false,
    val isLoading: Boolean = true,
    val themePreference: ThemePreference = ThemePreference.Dark
)

private data class SettingsBaseState(
    val selectedModel: String?,
    val thinkingEnabled: Boolean,
    val customPrompt: String,
    val selectedUnit: String,
    val inputTokens: Long,
    val outputTokens: Long,
    val cumulativeCost: Float,
    val veniceBalanceUsd: String?,
    val veniceBalanceDiem: String?,
    val modelDeprecationWarning: String?,
    val modelDeprecationWarningModel: String?
)

private data class BalanceAndWarningState(
    val balanceUsd: String?,
    val balanceDiem: String?,
    val warning: String?,
    val warningModel: String?,
    val goals: UserGoals
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val secureStorage: SecureStorage,
    private val aiRepository: AiRepository,
    private val goalsRepository: GoalsRepository,
    private val csvExportManager: CsvExportManager,
    private val csvImportManager: CsvImportManager
) : ViewModel() {

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _apiKeyInput = MutableStateFlow("")
    val apiKeyInput: StateFlow<String> = _apiKeyInput.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    private val _showClearDataDialog = MutableStateFlow(false)
    val showClearDataDialog: StateFlow<Boolean> = _showClearDataDialog.asStateFlow()

    private val _storedApiKey = MutableStateFlow<String?>(null)
    private val _availableModels = MutableStateFlow<List<AiModel>>(emptyList())
    private val _isLoadingModels = MutableStateFlow(false)
    private val _modelLoadError = MutableStateFlow<String?>(null)
    private val _isSavingApiKey = MutableStateFlow(false)
    private val apiKeyState = combine(_storedApiKey, _isSavingApiKey) { storedApiKey, isSavingApiKey ->
        storedApiKey to isSavingApiKey
    }

    private val preferenceState = combine(
        userPreferencesDataStore.selectedAiModel,
        userPreferencesDataStore.thinkingEnabled,
        userPreferencesDataStore.customPrompt,
        userPreferencesDataStore.selectedUnit
    ) { model, thinking, prompt, unit ->
        SettingsBaseState(
            selectedModel = model,
            thinkingEnabled = thinking,
            customPrompt = prompt,
            selectedUnit = unit,
            inputTokens = 0L,
            outputTokens = 0L,
            cumulativeCost = 0f,
            veniceBalanceUsd = null,
            veniceBalanceDiem = null,
            modelDeprecationWarning = null,
            modelDeprecationWarningModel = null
        )
    }

    private val usageState = combine(
        userPreferencesDataStore.cumulativeInputTokens,
        userPreferencesDataStore.cumulativeOutputTokens,
        userPreferencesDataStore.cumulativeCost,
        combine(
            userPreferencesDataStore.veniceBalanceUsd,
            userPreferencesDataStore.veniceBalanceDiem,
            userPreferencesDataStore.modelDeprecationWarning,
            userPreferencesDataStore.modelDeprecationWarningModel,
            goalsRepository.getGoals()
        ) { balanceUsd, balanceDiem, warning, warningModel, goals ->
            BalanceAndWarningState(balanceUsd, balanceDiem, warning, warningModel, goals)
        }
    ) { inputTokens, outputTokens, cost, balanceAndWarning ->
        SettingsBaseState(
            selectedModel = null,
            thinkingEnabled = false,
            customPrompt = "",
            selectedUnit = "kg",
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            cumulativeCost = cost,
            veniceBalanceUsd = balanceAndWarning.balanceUsd,
            veniceBalanceDiem = balanceAndWarning.balanceDiem,
            modelDeprecationWarning = balanceAndWarning.warning,
            modelDeprecationWarningModel = balanceAndWarning.warningModel
        ) to balanceAndWarning.goals
    }

    private val combinedSettingsState = combine(
        preferenceState,
        usageState,
        apiKeyState
    ) { preferences, usage, apiKey ->
        Triple(preferences, usage, apiKey)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        combinedSettingsState,
        _availableModels,
        _isLoadingModels,
        _modelLoadError,
        userPreferencesDataStore.themePreference
    ) { combinedState, availableModels, isLoadingModels, modelLoadError, themeKey ->
        val preferences = combinedState.first
        val usage = combinedState.second
        val apiKey = combinedState.third
        val goals = usage.second
        val storedApiKey = apiKey.first
        val isSavingApiKey = apiKey.second

        val hasKey = !storedApiKey.isNullOrBlank()
        val masked = if (!storedApiKey.isNullOrBlank() && storedApiKey.length > 8) {
            storedApiKey.take(4) + "****" + storedApiKey.takeLast(4)
        } else if (hasKey) {
            "****"
        } else {
            ""
        }

        SettingsUiState(
            hasApiKey = hasKey,
            maskedApiKey = masked,
            selectedModel = preferences.selectedModel,
            availableModels = availableModels,
            selectedModelSupportsThinking = supportsThinking(
                selectedModelId = preferences.selectedModel,
                availableModels = availableModels
            ),
            isLoadingModels = isLoadingModels,
            modelLoadError = modelLoadError,
            thinkingEnabled = preferences.thinkingEnabled,
            customPrompt = preferences.customPrompt,
            calorieTarget = goals.calorieTarget,
            proteinTarget = goals.proteinTargetGrams,
            carbsTarget = goals.carbsTargetGrams,
            fatTarget = goals.fatTargetGrams,
            selectedUnit = preferences.selectedUnit,
            inputTokens = usage.first.inputTokens,
            outputTokens = usage.first.outputTokens,
            cumulativeCost = usage.first.cumulativeCost,
            veniceBalanceUsd = usage.first.veniceBalanceUsd,
            veniceBalanceDiem = usage.first.veniceBalanceDiem,
            modelDeprecationWarning = usage.first.modelDeprecationWarning
                ?.takeIf { usage.first.modelDeprecationWarningModel == preferences.selectedModel },
            isSavingApiKey = isSavingApiKey,
            isLoading = false,
            themePreference = ThemePreference.fromKey(themeKey)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    init {
        viewModelScope.launch {
            val apiKey = secureStorage.getApiKey()
            _storedApiKey.value = apiKey
            if (!apiKey.isNullOrBlank()) {
                refreshModels()
            }
        }
    }

    fun updateApiKeyInput(value: String) {
        _apiKeyInput.value = value
    }

    fun saveApiKey() {
        val key = _apiKeyInput.value.trim()
        if (key.isBlank() || _isSavingApiKey.value) return

        viewModelScope.launch {
            _isSavingApiKey.value = true
            aiRepository.validateApiKey(key)
                .onSuccess { isValid ->
                    if (isValid) {
                        secureStorage.saveApiKey(key)
                        _storedApiKey.value = key
                        _apiKeyInput.value = ""
                        _events.emit(SettingsEvent.ApiKeySaved)
                        refreshModels()
                    } else {
                        _events.emit(SettingsEvent.ApiKeyError("Invalid API key"))
                    }
                }
                .onFailure { error ->
                    _events.emit(
                        SettingsEvent.ApiKeyError(
                            error.message ?: "Failed to validate API key"
                        )
                    )
                }
            _isSavingApiKey.value = false
        }
    }

    fun clearApiKey() {
        viewModelScope.launch {
            secureStorage.clearApiKey()
            _storedApiKey.value = null
            _apiKeyInput.value = ""
            _availableModels.value = emptyList()
            _isLoadingModels.value = false
            _modelLoadError.value = null
        }
    }

    fun selectModel(model: String) {
        viewModelScope.launch {
            userPreferencesDataStore.setSelectedModel(model)
            if (!supportsThinking(model)) {
                userPreferencesDataStore.setThinkingEnabled(false)
            }
        }
    }

    fun refreshModels() {
        viewModelScope.launch {
            _isLoadingModels.value = true
            _modelLoadError.value = null

            aiRepository.fetchModels()
                .onSuccess { models ->
                    _availableModels.value = models

                    val currentSelection = userPreferencesDataStore.selectedAiModel.first()
                    val resolvedSelection = when {
                        models.isEmpty() -> null
                        currentSelection.isNullOrBlank() -> models.defaultModel().id
                        models.any { it.id == currentSelection } -> currentSelection
                        else -> models.defaultModel().id
                    }

                    if (!resolvedSelection.isNullOrBlank() && resolvedSelection != currentSelection) {
                        userPreferencesDataStore.setSelectedModel(resolvedSelection)
                    }

                    if (!supportsThinking(resolvedSelection, models)) {
                        userPreferencesDataStore.setThinkingEnabled(false)
                    }
                }
                .onFailure { error ->
                    _availableModels.value = emptyList()
                    _modelLoadError.value = error.message ?: "Failed to load models"
                }

            _isLoadingModels.value = false
        }
    }

    fun toggleThinking() {
        viewModelScope.launch {
            if (!uiState.value.selectedModelSupportsThinking) {
                userPreferencesDataStore.setThinkingEnabled(false)
                return@launch
            }
            val current = uiState.value.thinkingEnabled
            userPreferencesDataStore.setThinkingEnabled(!current)
        }
    }

    private fun supportsThinking(
        selectedModelId: String?,
        availableModels: List<AiModel> = _availableModels.value
    ): Boolean {
        return availableModels
            .firstOrNull { it.id == selectedModelId }
            ?.supportsReasoning == true
    }

    private fun List<AiModel>.defaultModel(): AiModel {
        return filter { model ->
            model.id.contains("glm", ignoreCase = true) ||
                model.name.contains("glm", ignoreCase = true)
        }.maxByOrNull { it.createdAtEpochSeconds } ?: maxByOrNull { it.createdAtEpochSeconds } ?: first()
    }

    fun updateCustomPrompt(prompt: String) {
        viewModelScope.launch {
            userPreferencesDataStore.setCustomPrompt(prompt)
        }
    }

    fun resetCustomPrompt() {
        viewModelScope.launch {
            userPreferencesDataStore.setCustomPrompt(null)
        }
    }

    fun saveGoals(
        calorieTarget: String,
        proteinTarget: String,
        carbsTarget: String,
        fatTarget: String
    ) {
        val calorieValue = calorieTarget.toIntOrNull() ?: return
        val proteinValue = proteinTarget.toFloatOrNull() ?: return
        val carbsValue = carbsTarget.toFloatOrNull() ?: return
        val fatValue = fatTarget.toFloatOrNull() ?: return

        viewModelScope.launch {
            val goals = goalsRepository.getGoals().first()
            goalsRepository.saveGoals(
                goals.copy(
                    calorieTarget = calorieValue,
                    proteinTargetGrams = proteinValue,
                    carbsTargetGrams = carbsValue,
                    fatTargetGrams = fatValue
                )
            )
        }
    }

    fun setUnit(unit: String) {
        viewModelScope.launch {
            userPreferencesDataStore.setUseMetric(unit == "kg")
        }
    }

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch {
            userPreferencesDataStore.setThemePreference(theme.key)
        }
    }

    fun showClearDataConfirmation() {
        _showClearDataDialog.value = true
    }

    fun dismissClearDataDialog() {
        _showClearDataDialog.value = false
    }

    fun clearAllData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                appDatabase.clearAllTables()
                userPreferencesDataStore.clearAll()
                secureStorage.clearAll()
            }
            _storedApiKey.value = null
            _availableModels.value = emptyList()
            _isLoadingModels.value = false
            _modelLoadError.value = null
            _showClearDataDialog.value = false
            _events.emit(SettingsEvent.DataCleared)
        }
    }

    fun exportData(startDate: String?, endDate: String?) {
        viewModelScope.launch {
            try {
                when (val result = csvExportManager.exportData(startDate, endDate)) {
                    is ExportResult.Success -> _events.emit(SettingsEvent.ExportReady(result.uri))
                    ExportResult.Empty -> _events.emit(SettingsEvent.ExportEmpty)
                }
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ExportError(e.message ?: "Unknown error"))
            }
        }
    }

    fun importData(uri: Uri, mode: ImportMode) {
        if (_isImporting.value) return
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val result = csvImportManager.importData(uri, mode)
                _events.emit(SettingsEvent.ImportComplete(result))
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ImportError(e.message ?: "Unknown error"))
            } finally {
                _isImporting.value = false
            }
        }
    }
}

sealed interface SettingsEvent {
    data class ExportReady(val uri: Uri) : SettingsEvent
    data object ExportEmpty : SettingsEvent
    data class ExportError(val message: String) : SettingsEvent
    data object ApiKeySaved : SettingsEvent
    data class ApiKeyError(val message: String) : SettingsEvent
    data object DataCleared : SettingsEvent
    data class ImportComplete(val result: ImportResult) : SettingsEvent
    data class ImportError(val message: String) : SettingsEvent
}
