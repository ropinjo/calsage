package com.calorietracker.presentation.screen.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.calorietracker.presentation.theme.extendedColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.calorietracker.presentation.common.components.AddFab
import com.calorietracker.presentation.common.components.CalorieRing
import com.calorietracker.presentation.common.components.MacroProgressBar
import com.calorietracker.presentation.common.components.MealCard
import com.calorietracker.presentation.common.components.MealTypePickerSheet
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToAddFood: (mealType: String, date: String) -> Unit,
    onNavigateToMealDetail: (mealType: String, date: String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    var showMealPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        // addObserver replays synthetic up-events on an already-started lifecycle
        // (e.g. when navigating back here), which must not reset the selected date;
        // only a real background -> foreground transition should.
        var ignoreStart = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (ignoreStart) ignoreStart = false else viewModel.selectToday()
                }
                Lifecycle.Event.ON_STOP -> ignoreStart = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(viewModel) {
        while (true) {
            val now = ZonedDateTime.now()
            val nextDay = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            delay(Duration.between(now, nextDay).toMillis().coerceAtLeast(1L))
            viewModel.selectToday()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CalSage",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = "Pick date",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            AddFab(
                onClick = { showMealPicker = true },
                contentDescription = "Add food"
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is DashboardUiState.Success -> {
                DashboardContent(
                    state = state,
                    selectedDate = selectedDate,
                    onDateSelected = { viewModel.selectDate(it) },
                    onMealClick = { mealType ->
                        onNavigateToMealDetail(mealType.uppercase(), selectedDate)
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    if (showMealPicker) {
        MealTypePickerSheet(
            onDismissRequest = { showMealPicker = false },
            onMealSelected = { meal ->
                showMealPicker = false
                onNavigateToAddFood(meal.name, selectedDate)
            }
        )
    }

    if (showDatePicker) {
        DateJumpPickerDialog(
            selectedDate = selectedDate,
            onDateSelected = {
                viewModel.selectDate(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun DateJumpPickerDialog(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val today = LocalDate.now()
    val selected = LocalDate.parse(selectedDate, DateTimeFormatter.ISO_LOCAL_DATE)
    var visibleMonth by remember(selected) { mutableStateOf(YearMonth.from(selected)) }
    val monthCells = remember(visibleMonth) {
        val firstDay = visibleMonth.atDay(1)
        val leadingEmptyDays = firstDay.dayOfWeek.value - 1
        val dates = List(leadingEmptyDays) { null } +
            (1..visibleMonth.lengthOfMonth()).map { visibleMonth.atDay(it) }
        dates + List((7 - dates.size % 7) % 7) { null }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .widthIn(max = 340.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous month"
                        )
                    }

                    Text(
                        text = "${visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${visibleMonth.year}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    IconButton(
                        onClick = { visibleMonth = visibleMonth.plusMonths(1) },
                        enabled = visibleMonth < YearMonth.from(today)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next month"
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    monthCells.chunked(7).forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            week.forEach { date ->
                                val isSelected = date == selected
                                val isToday = date == today
                                val isEnabled = date != null && !date.isAfter(today)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (date != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else androidx.compose.ui.graphics.Color.Transparent
                                                )
                                                .clickable(enabled = isEnabled) {
                                                    onDateSelected(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = date.dayOfMonth.toString(),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                },
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        onDateSelected(today.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    }) {
                        Text("Today")
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState.Success,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    onMealClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DateSelectorRibbon(
            selectedDate = selectedDate,
            onDateSelected = onDateSelected
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            CalorieRing(
                consumed = state.caloriesConsumed,
                target = state.calorieTarget
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MacroProgressBar(
                label = "Protein",
                current = state.proteinConsumed,
                target = state.proteinTarget,
                color = MaterialTheme.extendedColors.protein
            )
            MacroProgressBar(
                label = "Carbs",
                current = state.carbsConsumed,
                target = state.carbsTarget,
                color = MaterialTheme.extendedColors.carbs
            )
            MacroProgressBar(
                label = "Fat",
                current = state.fatConsumed,
                target = state.fatTarget,
                color = MaterialTheme.extendedColors.fat
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        state.mealSummaries.forEach { meal ->
            MealCard(
                mealType = meal.mealType,
                totalCalories = meal.totalCalories,
                protein = meal.protein,
                carbs = meal.carbs,
                fat = meal.fat,
                itemCount = meal.itemCount,
                onClick = { onMealClick(meal.mealType) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun DateSelectorRibbon(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    val today = LocalDate.now()
    val selected = LocalDate.parse(selectedDate, DateTimeFormatter.ISO_LOCAL_DATE)
    val weekDates = remember(selected) {
        val monday = selected.minusDays((selected.dayOfWeek.value - 1).toLong())
        (0L..6L).map { monday.plusDays(it) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        weekDates.forEach { date ->
            val isSelected = date == selected
            val isToday = date == today
            val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

            Box(
                modifier = Modifier
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 56.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else androidx.compose.ui.graphics.Color.Transparent
                        )
                        .clickable { onDateSelected(dateString) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            .uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${date.dayOfMonth}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isToday || isSelected) FontWeight.Bold
                            else FontWeight.Normal
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    if (isToday && !isSelected) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}
