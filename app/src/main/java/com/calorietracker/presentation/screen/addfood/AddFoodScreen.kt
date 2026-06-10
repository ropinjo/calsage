package com.calorietracker.presentation.screen.addfood

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.camera.core.ExperimentalGetImage
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import com.calorietracker.presentation.common.components.BrandedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorietracker.domain.model.FoodSource
import com.calorietracker.domain.model.mealTypeExamplePlaceholder
import com.calorietracker.presentation.common.capitalizedFoodName
import com.calorietracker.presentation.common.displayAmountFor
import com.calorietracker.presentation.common.formatPastDateLabel
import com.calorietracker.presentation.common.isToday
import com.calorietracker.presentation.common.components.NutritionChip
import com.calorietracker.presentation.common.components.PrimaryButton
import com.calorietracker.presentation.common.components.ScannedChip
import com.calorietracker.presentation.common.components.SecondaryButton
import com.calorietracker.presentation.theme.MotionDurations
import com.calorietracker.presentation.theme.extendedColors
import com.calorietracker.presentation.theme.motionTween
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalGetImage
@Composable
fun AddFoodScreen(
    mealType: String,
    date: String,
    onBack: () -> Unit,
    onAfterLog: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFavorites: (mealType: String) -> Unit,
    favoriteId: Long? = null,
    viewModelKey: String? = null,
    viewModel: AddFoodViewModel = hiltViewModel(key = viewModelKey)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val barcodeUiState by viewModel.barcodeUiState.collectAsStateWithLifecycle()
    val description by viewModel.foodDescription.collectAsStateWithLifecycle()
    val thinkingEnabled by viewModel.thinkingEnabled.collectAsStateWithLifecycle(initialValue = false)
    val focusManager = LocalFocusManager.current
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var retryCameraPermission by remember { mutableStateOf(false) }

    LaunchedEffect(mealType, date, favoriteId) {
        viewModel.updateRouteArgs(mealType = mealType, date = date, favoriteId = favoriteId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddFoodEvent.FoodLogged -> {
                    focusManager.clearFocus()
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    val message = if (favoriteId != null) {
                        "Favorite updated"
                    } else {
                        "Meal logged"
                    }
                    Toast.makeText(
                        context,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                    onAfterLog()
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            focusManager.clearFocus()
            viewModel.onScanClicked()
        } else {
            val activity = context.findActivity()
            val permanentlyDenied = activity?.let {
                !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } == true

            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = if (permanentlyDenied) {
                        "Camera permission denied. Enable it in Settings to scan barcodes."
                    } else {
                        "Camera permission denied. Grant it to scan barcodes."
                    },
                    actionLabel = if (permanentlyDenied) "Settings" else "Retry",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    if (permanentlyDenied) {
                        context.openAppSettings()
                    } else {
                        retryCameraPermission = true
                    }
                }
            }
        }
    }

    LaunchedEffect(retryCameraPermission) {
        if (retryCameraPermission) {
            retryCameraPermission = false
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(barcodeUiState.feedbackMessage) {
        val message = barcodeUiState.feedbackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = message,
            duration = SnackbarDuration.Short
        )
        viewModel.consumeBarcodeFeedback()
    }

    LaunchedEffect(barcodeUiState.showServingDialog) {
        if (barcodeUiState.showServingDialog) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val showScannerFab = uiState is AddFoodUiState.Idle &&
        !barcodeUiState.isScannerVisible &&
        !barcodeUiState.isLookupLoading &&
        !barcodeUiState.showServingDialog

    // From the result/error step, back returns to the description entry
    // (text is kept in the ViewModel) instead of leaving the screen.
    val canReturnToEntry = favoriteId == null &&
        (uiState is AddFoodUiState.Result || uiState is AddFoodUiState.Error)
    val handleBack: () -> Unit = {
        when {
            barcodeUiState.isScannerVisible -> viewModel.onScannerDismissed()
            canReturnToEntry -> viewModel.switchToAiEntry()
            else -> onBack()
        }
    }

    BackHandler(enabled = barcodeUiState.isScannerVisible || canReturnToEntry) {
        handleBack()
    }

    val mealLabel = mealType.lowercase()
    val titleText = if (favoriteId != null) "Edit $mealLabel favorite" else "Add $mealLabel"

    Scaffold(
        topBar = {
            if (!barcodeUiState.isScannerVisible) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (!isToday(date)) {
                                Text(
                                    text = formatPastDateLabel(date),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = handleBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        if (favoriteId == null) {
                            IconButton(onClick = { onNavigateToFavorites(mealType) }) {
                                Icon(
                                    imageVector = Icons.Filled.FavoriteBorder,
                                    contentDescription = "Favorites",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val contentInTween = motionTween<Float>(MotionDurations.STANDARD)
            val contentOutTween = motionTween<Float>(MotionDurations.QUICK)
            AnimatedContent(
                targetState = uiState,
                contentKey = { it.contentType },
                transitionSpec = {
                    fadeIn(animationSpec = contentInTween) togetherWith
                        fadeOut(animationSpec = contentOutTween)
                },
                label = "addFoodContent"
            ) { state ->
                when (state) {
                    is AddFoodUiState.Idle -> {
                        IdleContent(
                            mealType = mealType,
                            description = description,
                            onDescriptionChange = viewModel::updateDescription,
                            onAnalyze = {
                                focusManager.clearFocus()
                                viewModel.analyzeFood()
                            },
                            onSwitchToManual = viewModel::switchToManualEntry
                        )
                    }

                    is AddFoodUiState.Analyzing -> {
                        AnalyzingContent(thinkingEnabled = thinkingEnabled)
                    }

                    is AddFoodUiState.Result -> {
                        ResultContent(
                            state = state,
                            onLog = viewModel::logFood,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onFavoriteNameChange = viewModel::updateFavoriteName,
                            onUpdateItem = viewModel::updateItem,
                            onRemoveItem = viewModel::removeItem,
                            onReAnalyze = viewModel::reAnalyze
                        )
                    }

                    is AddFoodUiState.ManualEntry -> {
                        ManualEntryContent(
                            state = state,
                            onUpdate = viewModel::updateManualEntry,
                            onLog = viewModel::logManualEntry,
                            onSwitchToAi = viewModel::switchToAiEntry
                        )
                    }

                    is AddFoodUiState.Error -> {
                        ErrorContent(
                            message = state.message,
                            onRetry = viewModel::analyzeFood,
                            onNavigateToSettings = onNavigateToSettings,
                            onSwitchToManual = viewModel::switchToManualEntry
                        )
                    }
                }
            }

            if (showScannerFab) {
                FloatingActionButton(
                    onClick = {
                        val permissionGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (permissionGranted) {
                            focusManager.clearFocus()
                            viewModel.onScanClicked()
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 96.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = "Scan barcode"
                    )
                }
            }

            if (barcodeUiState.isLookupLoading) {
                BarcodeLookupLoadingOverlay()
            }

            if (barcodeUiState.isScannerVisible) {
                BarcodeScannerScreen(
                    onBarcodeDetected = viewModel::onBarcodeDetected,
                    onDismiss = viewModel::onScannerDismissed
                )
            }
        }

        if (barcodeUiState.showServingDialog) {
            ServingSizeDialog(
                productName = barcodeUiState.scannedProductName,
                servingSuggestion = barcodeUiState.scannedServingSuggestion,
                servingGrams = barcodeUiState.servingGrams,
                servingError = barcodeUiState.servingError,
                onServingGramsChange = viewModel::onServingGramsChanged,
                onConfirm = viewModel::onServingConfirmed,
                onDismiss = viewModel::onServingDismissed
            )
        }
    }
}

@Composable
private fun IdleContent(
    mealType: String,
    description: TextFieldValue,
    onDescriptionChange: (TextFieldValue) -> Unit,
    onAnalyze: () -> Unit,
    onSwitchToManual: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Describe what you ate",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            placeholder = {
                Text(
                    text = mealTypeExamplePlaceholder(mealType),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "${description.text.length}/700",
                style = MaterialTheme.typography.labelSmall,
                color = if (description.text.length >= 680) MaterialTheme.extendedColors.warning else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        PrimaryButton(
            text = "Analyze with AI",
            onClick = onAnalyze,
            enabled = description.text.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        SecondaryButton(
            text = "Enter manually instead",
            onClick = onSwitchToManual,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AnalyzingContent(thinkingEnabled: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AiScanningIndicator(modifier = Modifier.padding(bottom = 20.dp))

        Text(
            text = "Analyzing your food...",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (thinkingEnabled) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Thinking mode is on - this can take a few minutes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AiScanningIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "aiScanning")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = MotionDurations.SHIMMER_CYCLE, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aiScanningRotation"
    )
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = MotionDurations.SLOW, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aiScanningPhase"
    )
    val activeColor = MaterialTheme.colorScheme.primary
    val trailColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.size(64.dp)) {
        val strokeWidth = 5.dp.toPx()
        val radius = size.minDimension / 2f - strokeWidth * 1.4f
        val waveDepth = 3.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

        fun wavyArcPath(startDegrees: Float, sweepDegrees: Float): Path {
            val path = Path()
            val steps = 96
            for (index in 0..steps) {
                val progress = index / steps.toFloat()
                val degrees = startDegrees + sweepDegrees * progress + rotation
                val radians = (degrees / 180f * PI).toFloat()
                val wave = sin(radians * 7f + phase) * waveDepth
                val pointRadius = radius + wave
                val point = Offset(
                    x = center.x + cos(radians) * pointRadius,
                    y = center.y + sin(radians) * pointRadius
                )
                if (index == 0) {
                    path.moveTo(point.x, point.y)
                } else {
                    path.lineTo(point.x, point.y)
                }
            }
            return path
        }

        drawPath(
            path = wavyArcPath(0f, 360f),
            color = trailColor.copy(alpha = 0.18f),
            style = stroke
        )
        drawPath(
            path = wavyArcPath(-80f, 235f),
            color = activeColor,
            style = stroke
        )
    }
}

@Composable
private fun ResultContent(
    state: AddFoodUiState.Result,
    onLog: () -> Unit,
    onToggleFavorite: () -> Unit,
    onFavoriteNameChange: (String) -> Unit,
    onUpdateItem: (Int, FoodItemResult) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onReAnalyze: () -> Unit
) {
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    val editingItem = editingIndex?.let { state.items.getOrNull(it) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 8.dp,
            bottom = 96.dp
        )
    ) {
        // Cached indicator
        if (state.isCached) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Cached",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(onClick = onReAnalyze) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Re-analyze",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        // Totals header card
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "${state.totalCalories}",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = "Calories total",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start
                        )
                        if (state.source == FoodSource.SCANNED) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ScannedChip()
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NutritionChip(label = "P", value = state.totalProtein, color = MaterialTheme.extendedColors.protein)
                            NutritionChip(label = "C", value = state.totalCarbs, color = MaterialTheme.extendedColors.carbs)
                            NutritionChip(label = "F", value = state.totalFat, color = MaterialTheme.extendedColors.fat)
                        }
                    }
                }
            }
        }

        // Per-item cards
        itemsIndexed(
            items = state.items,
            key = { index, item -> "${item.name}_$index" }
        ) { index, item ->
            FoodItemCard(
                item = item,
                onEdit = { editingIndex = index }
            )
        }

        if (state.isAlreadyFavorite) {
            item {
                OutlinedTextField(
                    value = state.favoriteName,
                    onValueChange = onFavoriteNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Favorite name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        } else {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onToggleFavorite)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.saveAsFavorite,
                        onCheckedChange = { onToggleFavorite() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = "Save as favorite",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                AnimatedVisibility(
                    visible = state.saveAsFavorite,
                    label = "saveAsFavoriteName"
                ) {
                    OutlinedTextField(
                        value = state.favoriteName,
                        onValueChange = onFavoriteNameChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        label = { Text("Favorite name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Log button
        item {
            Spacer(modifier = Modifier.height(4.dp))
            PrimaryButton(
                text = if (state.isAlreadyFavorite) "Update favorite" else "Save meal",
                onClick = onLog,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isAlreadyFavorite || state.favoriteName.isNotBlank()
            )
        }
    }

    if (editingItem != null) {
        EditItemSheet(
            item = editingItem,
            onDismissRequest = { editingIndex = null },
            onSave = { updated ->
                val index = editingIndex ?: return@EditItemSheet
                onUpdateItem(index, updated)
                editingIndex = null
            },
            onRemove = {
                val index = editingIndex ?: return@EditItemSheet
                onRemoveItem(index)
                editingIndex = null
            }
        )
    }
}

@Composable
private fun FoodItemCard(
    item: FoodItemResult,
    onEdit: () -> Unit
) {
    val displayAmount = displayAmountFor(item.name, item.amount)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name.capitalizedFoodName(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (displayAmount.isNotBlank()) {
                        Text(
                            text = displayAmount,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "${item.calories} kcal",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NutritionChip(label = "P", value = item.proteinGrams, color = MaterialTheme.extendedColors.protein)
                    NutritionChip(label = "C", value = item.carbsGrams, color = MaterialTheme.extendedColors.carbs)
                    NutritionChip(label = "F", value = item.fatGrams, color = MaterialTheme.extendedColors.fat)
                }
                FilledTonalIconButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditItemSheet(
    item: FoodItemResult,
    onDismissRequest: () -> Unit,
    onSave: (FoodItemResult) -> Unit,
    onRemove: () -> Unit
) {
    var name by remember(item) { mutableStateOf(item.name) }
    var calories by remember(item) { mutableStateOf(item.calories.toString()) }
    var protein by remember(item) { mutableStateOf(item.proteinGrams.toString()) }
    var carbs by remember(item) { mutableStateOf(item.carbsGrams.toString()) }
    var fat by remember(item) { mutableStateOf(item.fatGrams.toString()) }
    val calorieError = ManualEntryValidator.calorieError(calories)
    val proteinError = ManualEntryValidator.macroError(protein)
    val carbsError = ManualEntryValidator.macroError(carbs)
    val fatError = ManualEntryValidator.macroError(fat)
    val anyValid = calorieError == null && proteinError == null && carbsError == null && fatError == null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = Modifier.navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Edit item",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            CompactTextField(
                value = name,
                onValueChange = { name = it },
                label = "Name",
                keyboardType = KeyboardType.Text,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            CompactTextField(
                value = calories,
                onValueChange = { calories = it },
                label = "Calories",
                keyboardType = KeyboardType.Number,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactTextField(
                    value = protein,
                    onValueChange = { protein = it },
                    label = "P (g)",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                CompactTextField(
                    value = carbs,
                    onValueChange = { carbs = it },
                    label = "C (g)",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                CompactTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = "F (g)",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val updatedItem = item.updatedWith(
                        name = name,
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fat = fat
                    )
                    coroutineScope.launch {
                        sheetState.hide()
                        onSave(updatedItem)
                    }
                },
                enabled = anyValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        sheetState.hide()
                        onRemove()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Delete item",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

private val AddFoodUiState.contentType: String
    get() = when (this) {
        AddFoodUiState.Idle -> "Idle"
        AddFoodUiState.Analyzing -> "Analyzing"
        is AddFoodUiState.Result -> "Result"
        is AddFoodUiState.ManualEntry -> "ManualEntry"
        is AddFoodUiState.Error -> "Error"
    }

private fun FoodItemResult.updatedWith(
    name: String,
    calories: String,
    protein: String,
    carbs: String,
    fat: String
): FoodItemResult {
    return copy(
        name = name,
        calories = calories.toIntOrNull() ?: this.calories,
        proteinGrams = protein.replace(',', '.').toFloatOrNull() ?: proteinGrams,
        carbsGrams = carbs.replace(',', '.').toFloatOrNull() ?: carbsGrams,
        fatGrams = fat.replace(',', '.').toFloatOrNull() ?: fatGrams
    )
}

@Composable
private fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = {
            Text(text = label)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun ManualEntryContent(
    state: AddFoodUiState.ManualEntry,
    onUpdate: (AddFoodUiState.ManualEntry) -> Unit,
    onLog: () -> Unit,
    onSwitchToAi: () -> Unit
) {
    val calorieError = ManualEntryValidator.calorieError(state.calories)
    val proteinError = ManualEntryValidator.macroError(state.protein)
    val carbsError = ManualEntryValidator.macroError(state.carbs)
    val fatError = ManualEntryValidator.macroError(state.fat)
    val anyValid = ManualEntryValidator.isValid(state)
    val hasInput = state.name.isNotBlank() || state.calories.isNotBlank() ||
        state.protein.isNotBlank() || state.carbs.isNotBlank() || state.fat.isNotBlank()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Manual entry",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        OutlinedTextField(
            value = state.name,
            onValueChange = { onUpdate(state.copy(name = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Food name") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = state.calories,
            onValueChange = { onUpdate(state.copy(calories = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Calories") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = calorieError != null,
            supportingText = calorieError?.let { { Text(it) } },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = state.protein,
                    onValueChange = { onUpdate(state.copy(protein = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Protein") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = proteinError != null,
                    supportingText = proteinError?.let { { Text(it) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.extendedColors.protein,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        cursorColor = MaterialTheme.extendedColors.protein,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.extendedColors.protein,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = state.carbs,
                    onValueChange = { onUpdate(state.copy(carbs = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Carbs") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = carbsError != null,
                    supportingText = carbsError?.let { { Text(it) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.extendedColors.carbs,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        cursorColor = MaterialTheme.extendedColors.carbs,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.extendedColors.carbs,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = state.fat,
                    onValueChange = { onUpdate(state.copy(fat = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Fat") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = fatError != null,
                    supportingText = fatError?.let { { Text(it) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.extendedColors.fat,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        cursorColor = MaterialTheme.extendedColors.fat,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.extendedColors.fat,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        PrimaryButton(
            text = "Log it",
            onClick = onLog,
            enabled = hasInput && anyValid,
            modifier = Modifier.fillMaxWidth()
        )

        SecondaryButton(
            text = "Use AI analysis instead",
            onClick = onSwitchToAi,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSwitchToManual: () -> Unit
) {
    val isApiKeyError = message.contains("API key", ignoreCase = true) ||
            message.contains("unauthorized", ignoreCase = true) ||
            message.contains("401", ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isApiKeyError) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "API Key Required",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please configure your API key in Settings to use AI food analysis.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onNavigateToSettings,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Go to Settings")
                    }
                }
            }
        } else {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isApiKeyError) {
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Try again")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        SecondaryButton(
            text = "Enter manually instead",
            onClick = onSwitchToManual,
            modifier = Modifier.widthIn(min = 220.dp)
        )
    }
}

@Composable
private fun BarcodeLookupLoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BrandedLoadingIndicator()
                Text(
                    text = "Looking up product...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}
