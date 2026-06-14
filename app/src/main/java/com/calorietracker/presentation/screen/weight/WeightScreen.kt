package com.calorietracker.presentation.screen.weight

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.calorietracker.presentation.theme.extendedColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorietracker.domain.model.WeightEntry
import com.calorietracker.presentation.common.components.AddFab
import com.calorietracker.presentation.common.components.LogWeightSheet
import com.calorietracker.presentation.common.components.WeightHistoryChart
import com.calorietracker.presentation.theme.MotionDurations
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    viewModel: WeightViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val weightInput by viewModel.weightInput.collectAsStateWithLifecycle()
    val noteInput by viewModel.noteInput.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    var entriesExpanded by remember { mutableStateOf(false) }
    var showLogSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                WeightEvent.WeightLogged -> {
                    // Haptic is the instant feedback; let the toast land after the sheet has
                    // slid away so it doesn't overlap the closing sheet.
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    delay(MotionDurations.STANDARD.toLong())
                    Toast.makeText(context, "Weight logged", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Weight",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            AddFab(
                onClick = { showLogSheet = true },
                contentDescription = "Log weight"
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTapGestures {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                }
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Current Weight Display ---
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (uiState.latestWeight != null) {
                            Text(
                                formatWeight(uiState.latestWeight!!, uiState.unit),
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Current Weight",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                "No weight logged yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // --- Chart Range Selector ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChartRange.entries.forEach { range ->
                        FilterChip(
                            selected = uiState.chartRange == range,
                            onClick = { viewModel.selectChartRange(range) },
                            label = {
                                Text(
                                    range.label,
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
            }

            // --- Weight Chart Placeholder ---
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (uiState.entries.size >= 2) {
                        WeightHistoryChart(
                            entries = uiState.entries,
                            unit = uiState.unit,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Log at least 2 weights to see the chart",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // --- Change Stats ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ChangeStatCard(
                        label = "Weekly",
                        change = uiState.weeklyChange,
                        unit = uiState.unit,
                        modifier = Modifier.weight(1f)
                    )
                    ChangeStatCard(
                        label = "Monthly",
                        change = uiState.monthlyChange,
                        unit = uiState.unit,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- Recent Entries ---
            if (uiState.entries.isNotEmpty()) {
                val sortedEntries = uiState.entries.sortedWith(compareByDescending<WeightEntry> { it.date }.thenByDescending { it.timestamp })
                val collapsedLimit = 7
                val visibleEntries = if (entriesExpanded) sortedEntries else sortedEntries.take(collapsedLimit)
                val hasMore = sortedEntries.size > collapsedLimit

                item {
                    Text(
                        text = "Recent Entries".uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(visibleEntries, key = { it.id }) { entry ->
                    WeightEntryRow(
                        entry = entry,
                        unit = uiState.unit,
                        onDelete = { viewModel.deleteWeight(entry.id) }
                    )
                }

                if (hasMore) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(onClick = { entriesExpanded = !entriesExpanded }) {
                                Text(
                                    if (entriesExpanded) "Show less"
                                    else "Show all (${sortedEntries.size})",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            }

        }
    }

    if (showLogSheet) {
        LogWeightSheet(
            weightInput = weightInput,
            noteInput = noteInput,
            unit = uiState.unit,
            onWeightChange = viewModel::updateWeightInput,
            onNoteChange = viewModel::updateNoteInput,
            onConfirm = { viewModel.logWeight() },
            onDismissRequest = {
                showLogSheet = false
                viewModel.clearInputs()
            }
        )
    }
}

@Composable
private fun ChangeStatCard(label: String, change: Float?, unit: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            if (change != null) {
                val color = when {
                    change < -0.1f -> MaterialTheme.extendedColors.success
                    change > 0.1f -> MaterialTheme.extendedColors.warning
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val icon = when {
                    change < -0.1f -> Icons.Filled.TrendingDown
                    change > 0.1f -> Icons.Filled.TrendingUp
                    else -> Icons.Filled.TrendingFlat
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "%+.2f %s".format(if (unit == "lb") change * 2.20462f else change, unit),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = color
                    )
                }
            } else {
                Text("--", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WeightEntryRow(entry: WeightEntry, unit: String, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    formatDate(entry.date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!entry.note.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        entry.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                formatWeight(entry.weightKg, unit),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun formatWeight(weightKg: Float, unit: String): String {
    return if (unit == "lb") {
        "%.2f lb".format(weightKg * 2.20462f)
    } else {
        "%.2f kg".format(weightKg)
    }
}

private fun formatDate(isoDate: String): String {
    return try {
        val date = LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE)
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
    } catch (e: Exception) {
        isoDate
    }
}
