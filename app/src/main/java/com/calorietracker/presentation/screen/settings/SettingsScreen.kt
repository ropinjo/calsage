package com.calorietracker.presentation.screen.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorietracker.data.local.preferences.UserPreferencesDataStore
import com.calorietracker.domain.repository.AiModel
import com.calorietracker.presentation.common.components.PrimaryButton
import com.calorietracker.presentation.common.components.SecondaryButton
import com.calorietracker.presentation.theme.ThemePreference
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private const val VENICE_MODELS_URL = "https://docs.venice.ai/models/text"
private val MODEL_ADDED_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val apiKeyInput by viewModel.apiKeyInput.collectAsStateWithLifecycle()
    val showClearDialog by viewModel.showClearDataDialog.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showApiKey by remember { mutableStateOf(false) }
    var showAdvancedGoals by remember { mutableStateOf(false) }
    var exportRange by remember { mutableStateOf("All") }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pendingImportUri = uri
    }
    var customPromptDraft by rememberSaveable(uiState.customPrompt) {
        mutableStateOf(uiState.customPrompt.orEmpty())
    }
    var calorieDraft by rememberSaveable(uiState.calorieTarget) {
        mutableStateOf(uiState.calorieTarget.toString())
    }
    var proteinDraft by rememberSaveable(uiState.proteinTarget) {
        mutableStateOf(uiState.proteinTarget.toInt().toString())
    }
    var carbsDraft by rememberSaveable(uiState.carbsTarget) {
        mutableStateOf(uiState.carbsTarget.toInt().toString())
    }
    var fatDraft by rememberSaveable(uiState.fatTarget) {
        mutableStateOf(uiState.fatTarget.toInt().toString())
    }
    val canSaveGoals = calorieDraft.toIntOrNull() != null &&
        proteinDraft.toFloatOrNull() != null &&
        carbsDraft.toFloatOrNull() != null &&
        fatDraft.toFloatOrNull() != null

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ExportReady -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Export CalSage Data"))
                }
                SettingsEvent.ExportEmpty -> {
                    snackbarHostState.showSnackbar("No entries to export")
                }
                is SettingsEvent.ExportError -> {
                    snackbarHostState.showSnackbar("Export failed: ${event.message}")
                }
                is SettingsEvent.ApiKeySaved -> {
                    snackbarHostState.showSnackbar("API key saved")
                }
                is SettingsEvent.ApiKeyError -> {
                    snackbarHostState.showSnackbar("API key error: ${event.message}")
                }
                is SettingsEvent.DataCleared -> {
                    snackbarHostState.showSnackbar("All data cleared")
                }
                is SettingsEvent.ImportComplete -> {
                    val r = event.result
                    val message = if (r.total == 0 && r.skipped == 0) {
                        "Import finished but no entries were found"
                    } else {
                        val parts = mutableListOf(
                            "${r.foodEntries} food",
                            "${r.weightEntries} weight",
                            "${r.favorites} favorites"
                        )
                        if (r.skipped > 0) parts.add("skipped ${r.skipped}")
                        "Imported ${parts.joinToString(", ")}"
                    }
                    snackbarHostState.showSnackbar(message)
                }
                is SettingsEvent.ImportError -> {
                    snackbarHostState.showSnackbar("Import failed: ${event.message}")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTapGestures {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- AI Provider Section ---
            SectionHeader("AI provider")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Venice AI", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)

                    if (uiState.hasApiKey) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                uiState.maskedApiKey,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { viewModel.clearApiKey() }) {
                                Text("Clear", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = viewModel::updateApiKeyInput,
                            label = { Text("API key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Icon(
                                        if (showApiKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = "Toggle visibility"
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        PrimaryButton(
                            text = if (uiState.isSavingApiKey) "Validating..." else "Validate & save API key",
                            onClick = { viewModel.saveApiKey() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = apiKeyInput.isNotBlank() && !uiState.isSavingApiKey,
                            leadingIcon = Icons.Filled.Save
                        )
                    }

                    if (uiState.hasApiKey) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        ModelSelectionSection(
                            selectedModelId = uiState.selectedModel,
                            availableModels = uiState.availableModels,
                            isLoading = uiState.isLoadingModels,
                            errorMessage = uiState.modelLoadError,
                            onRetry = viewModel::refreshModels,
                            onModelSelected = viewModel::selectModel
                        )
                        uiState.modelDeprecationWarning?.let { warning ->
                            Text(
                                text = warning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 16.dp)
                            ) {
                                Text("Thinking mode", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    if (uiState.selectedModelSupportsThinking) {
                                        "Better accuracy on complex meals. Increases response time."
                                    } else {
                                        "Available only for models that support reasoning."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val thinkingOn = uiState.thinkingEnabled && uiState.selectedModelSupportsThinking
                            ThinkingModeSwitch(
                                checked = thinkingOn,
                                enabled = uiState.selectedModelSupportsThinking,
                                onClick = { viewModel.toggleThinking() }
                            )
                        }
                    }
                }
            }

            // --- Usage Stats Section ---
            if (uiState.hasApiKey) {
                SectionHeader("Usage stats")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatRow("Input tokens", "%,d".format(uiState.inputTokens))
                        StatRow("Output tokens", "%,d".format(uiState.outputTokens))
                        StatRow("Estimated cost", "$%.4f".format(uiState.cumulativeCost))
                        val balanceDisplay = uiState.veniceBalanceUsd
                            ?.toDoubleOrNull()
                            ?.let { "$%.4f".format(Locale.US, it) }
                            ?: uiState.veniceBalanceDiem
                                ?.toDoubleOrNull()
                                ?.let { "%.4f DIEM".format(Locale.US, it) }
                        if (balanceDisplay != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                            StatRow("Venice balance", balanceDisplay)
                        }
                    }
                }
            }

            // --- Custom Prompt Section ---
            SectionHeader("Custom prompt")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = customPromptDraft,
                        onValueChange = { customPromptDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Personal context for AI") },
                        minLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    PrimaryButton(
                        text = "Save prompt",
                        onClick = { viewModel.updateCustomPrompt(customPromptDraft) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = customPromptDraft != (uiState.customPrompt ?: "")
                    )
                    SecondaryButton(
                        text = "Reset prompt to default",
                        onClick = { viewModel.resetCustomPrompt() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = customPromptDraft != UserPreferencesDataStore.DEFAULT_CUSTOM_PROMPT
                    )
                }
            }

            // --- Daily Goals Section ---
            SectionHeader("Daily goals")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GoalInput("Calories", calorieDraft, "kcal") { calorieDraft = it }
                    GoalInput("Protein", proteinDraft, "g") { proteinDraft = it }

                    TextButton(onClick = { showAdvancedGoals = !showAdvancedGoals }) {
                        Text(
                            if (showAdvancedGoals) "Hide advanced" else "Show advanced (carbs & fat)",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(
                        visible = showAdvancedGoals,
                        label = "advancedGoalsExpansion"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            GoalInput("Carbs", carbsDraft, "g") { carbsDraft = it }
                            GoalInput("Fat", fatDraft, "g") { fatDraft = it }
                        }
                    }

                    PrimaryButton(
                        text = "Save goals",
                        onClick = {
                            viewModel.saveGoals(
                                calorieTarget = calorieDraft,
                                proteinTarget = proteinDraft,
                                carbsTarget = carbsDraft,
                                fatTarget = fatDraft
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canSaveGoals && (
                            calorieDraft != uiState.calorieTarget.toString() ||
                                proteinDraft != uiState.proteinTarget.toInt().toString() ||
                                carbsDraft != uiState.carbsTarget.toInt().toString() ||
                                fatDraft != uiState.fatTarget.toInt().toString()
                            )
                    )
                }
            }

            // --- Units Section ---
            SectionHeader("Units")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedUnit == "kg",
                        onClick = { viewModel.setUnit("kg") },
                        label = { Text("Kilograms (kg)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    )
                    FilterChip(
                        selected = uiState.selectedUnit == "lb",
                        onClick = { viewModel.setUnit("lb") },
                        label = { Text("Pounds (lb)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    )
                }
            }

            // --- Appearance Section ---
            SectionHeader("Appearance")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Theme",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionChip(
                            label = "Dark",
                            icon = Icons.Filled.DarkMode,
                            selected = uiState.themePreference == ThemePreference.Dark,
                            onClick = { viewModel.setTheme(ThemePreference.Dark) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            label = "Light",
                            icon = Icons.Filled.LightMode,
                            selected = uiState.themePreference == ThemePreference.Light,
                            onClick = { viewModel.setTheme(ThemePreference.Light) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            label = "System",
                            icon = Icons.Filled.Brightness6,
                            selected = uiState.themePreference == ThemePreference.System,
                            onClick = { viewModel.setTheme(ThemePreference.System) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // --- Data Section ---
            SectionHeader("Data")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Import data", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Restore from a CalSage export (.zip). Imported entries are added to your existing data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PrimaryButton(
                        text = if (isImporting) "Importing..." else "Import from file",
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isImporting,
                        leadingIcon = Icons.Filled.FileDownload
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                    Text("Export CSV", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("7d", "30d", "90d", "All").forEach { range ->
                            FilterChip(
                                selected = exportRange == range,
                                onClick = { exportRange = range },
                                label = {
                                    Text(
                                        range,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    PrimaryButton(
                        text = "Export and share",
                        onClick = {
                            val (start, end) = getDateRange(exportRange)
                            viewModel.exportData(start, end)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Filled.Share
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                    OutlinedButton(
                        onClick = { viewModel.showClearDataConfirmation() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Clear all data",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Import data") },
            text = {
                Column {
                    Text("Choose how to handle entries that already exist:")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "• Merge — skip rows already in your data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "• Replace — overwrite entries on matching dates",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importData(uri, com.calorietracker.data.export.ImportMode.Merge)
                    pendingImportUri = null
                }) {
                    Text("Merge", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        viewModel.importData(uri, com.calorietracker.data.export.ImportMode.Replace)
                        pendingImportUri = null
                    }) {
                        Text("Replace", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { pendingImportUri = null }) {
                        Text("Cancel")
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearDataDialog() },
            title = { Text("Clear all data?") },
            text = { Text("This will permanently delete all food entries, weight entries, favorites, and settings. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAllData() }) {
                    Text("Delete everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClearDataDialog() }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun ThinkingModeSwitch(
    checked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val trackColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f)
            checked -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f)
        },
        label = "Thinking mode track"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 23.dp else 3.dp,
        label = "Thinking mode thumb"
    )
    val thumbColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
            checked -> MaterialTheme.colorScheme.surface
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "Thinking mode thumb color"
    )

    Box(
        modifier = Modifier
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(trackColor)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

@Composable
private fun ModelSelectionSection(
    selectedModelId: String?,
    availableModels: List<AiModel>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onModelSelected: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var showModelPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("AI model", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (availableModels.isNotEmpty()) {
                IconButton(
                    onClick = onRetry,
                    enabled = !isLoading,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh models",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { uriHandler.openUri(VENICE_MODELS_URL) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Guide",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open Venice model guide",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        when {
            isLoading -> {
                Text(
                    "Loading latest private Venice text models...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            availableModels.isNotEmpty() -> {
                val selectedModel = availableModels.firstOrNull { it.id == selectedModelId } ?: availableModels.first()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showModelPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = selectedModel.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    val badges = traitBadges(selectedModel.traits)
                                    if (badges.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = badges,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = selectedModel.id,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (selectedModel.supportsReasoning) {
                                    Text(
                                        text = "Thinking available",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Open model list",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (showModelPicker) {
                    AiModelPickerDialog(
                        availableModels = availableModels,
                        selectedModelId = selectedModel.id,
                        onDismissRequest = { showModelPicker = false },
                        onModelSelected = onModelSelected
                    )
                }
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        errorMessage ?: "No compatible private text models available.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun AiModelPickerDialog(
    availableModels: List<AiModel>,
    selectedModelId: String,
    onDismissRequest: () -> Unit,
    onModelSelected: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val configuration = LocalConfiguration.current
    val maxDialogHeight = (configuration.screenHeightDp * 0.9f).dp
    var searchFocused by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val filteredModels = remember(availableModels, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            availableModels
        } else {
            availableModels.filter { model ->
                model.name.contains(query, ignoreCase = true) ||
                    model.id.contains(query, ignoreCase = true) ||
                    model.traits.any { it.contains(query, ignoreCase = true) }
            }
        }
    }
    val searchBorderColor by animateColorAsState(
        targetValue = if (searchFocused) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f)
        },
        label = "AI model search border"
    )

    // Dismiss the keyboard as soon as the user starts scrolling the list.
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    // Reveal the currently selected model when the dialog opens.
    LaunchedEffect(Unit) {
        val index = availableModels.indexOfFirst { it.id == selectedModelId }
        if (index > 0) {
            listState.scrollToItem((index - 1).coerceAtLeast(0))
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        // Full-window scrim: a tap anywhere outside the card dismisses the dialog.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                )
                // Inset the card for system bars, display cutout and the keyboard,
                // while the scrim's click area (declared above) still covers the full screen.
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .heightIn(max = maxDialogHeight)
                    // Swallow taps on the card so they don't reach the scrim and close it.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Column(modifier = Modifier.padding(vertical = 18.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI model",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close model list",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                BorderStroke(1.dp, searchBorderColor),
                                RoundedCornerShape(12.dp)
                            )
                            .onFocusChanged { searchFocused = it.isFocused },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        ),
                        decorationBox = { innerTextField ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "GLM & Kimi recommended",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        }
                    )
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        if (filteredModels.isEmpty()) {
                            item {
                                Text(
                                    text = "No matching models",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = filteredModels,
                                key = { _, model -> model.id }
                            ) { index, model ->
                                AiModelPickerRow(
                                    model = model,
                                    selected = model.id == selectedModelId,
                                    onClick = {
                                        onModelSelected(model.id)
                                        onDismissRequest()
                                    }
                                )
                                if (index < filteredModels.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 24.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun traitBadges(traits: List<String>): String {
    val seen = mutableSetOf<String>()
    val builder = StringBuilder()
    for (trait in traits) {
        val lower = trait.lowercase()
        val badge = when {
            lower.contains("fast") -> "⚡"
            lower.contains("vision") -> "👁"
            lower.contains("code") -> "⌨"
            lower.contains("reasoning") -> "🧠"
            else -> null
        }
        if (badge != null && seen.add(badge)) {
            if (builder.isNotEmpty()) builder.append(' ')
            builder.append(badge)
        }
    }
    return builder.toString()
}

@Composable
private fun AiModelPickerRow(
    model: AiModel,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        )
        {
            Text(
                text = "AI",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = model.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = model.id,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Added ${modelAddedDate(model.createdAtEpochSeconds)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (model.supportsReasoning) {
                Text(
                    text = "Thinking available",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Selected",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun modelAddedDate(createdAtEpochSeconds: Long): String {
    return Instant.ofEpochSecond(createdAtEpochSeconds)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .format(MODEL_ADDED_DATE_FORMATTER)
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun GoalInput(label: String, value: String, unit: String, onValueChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(80.dp))
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
            },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            suffix = { Text(unit, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun ThemeOptionChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    else MaterialTheme.colorScheme.surface
                )
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = contentColor
        )
    }
}

private fun getDateRange(range: String): Pair<String?, String?> {
    if (range == "All") return Pair(null, null)
    val days = when (range) {
        "7d" -> 7L
        "30d" -> 30L
        "90d" -> 90L
        else -> return Pair(null, null)
    }
    val end = java.time.LocalDate.now()
    val start = end.minusDays(days)
    return Pair(start.toString(), end.toString())
}
