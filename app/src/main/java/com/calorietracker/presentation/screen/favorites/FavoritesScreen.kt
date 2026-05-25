package com.calorietracker.presentation.screen.favorites

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import com.calorietracker.presentation.theme.extendedColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorietracker.domain.model.FavoriteMeal
import com.calorietracker.domain.model.FoodSource
import com.calorietracker.domain.model.NutritionItem
import com.calorietracker.presentation.common.components.NutritionChip
import com.calorietracker.presentation.common.components.ScannedChip
import com.calorietracker.presentation.common.formatPastDateLabel
import com.calorietracker.presentation.common.isToday

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    mealType: String,
    onNavigateBack: () -> Unit,
    onEditFavorite: (favoriteId: Long, date: String) -> Unit = { _, _ -> },
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<FavoriteMeal?>(null) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val showPastDateBanner = !isToday(selectedDate)

    val mealLabel = mealType.lowercase().replaceFirstChar { it.uppercase() }

    LaunchedEffect(mealType) {
        viewModel.setMealType(mealType)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FavoritesEvent.FavoriteLogged -> {
                    val label = event.mealType.name.lowercase()
                        .replaceFirstChar { it.uppercase() }
                    Toast.makeText(
                        context,
                        "Added to $label",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "$mealLabel favorites",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
        ) {
            if (showPastDateBanner) {
                PastDateBanner(label = formatPastDateLabel(selectedDate))
            }

            if (uiState.favorites.size >= 5 || searchQuery.isNotBlank()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search favorites...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize())
            } else if (uiState.favorites.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            if (searchQuery.isNotBlank()) "No favorites match \"$searchQuery\""
                            else "No $mealLabel favorites yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Save meals as favorites when logging $mealLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "Tap a favorite to add it to this meal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }
                    items(uiState.favorites, key = { it.id }) { favorite ->
                        FavoriteCard(
                            favorite = favorite,
                            onQuickLog = { viewModel.quickLog(favorite) },
                            onEdit = { onEditFavorite(favorite.id, selectedDate) },
                            onDelete = { deleteTarget = favorite }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    deleteTarget?.let { fav ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Favorite?") },
            text = { Text("Remove \"${fav.name}\" from favorites?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFavorite(fav.id)
                    deleteTarget = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun PastDateBanner(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "Logging to $label",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteCard(
    favorite: FavoriteMeal,
    onQuickLog: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .combinedClickable(
                onClick = onQuickLog,
                onLongClick = onDelete
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        favorite.favoriteDisplayName(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (favorite.items.isEmpty()) {
                        Text(
                            favorite.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                    if (favorite.source == FoodSource.SCANNED) {
                        Spacer(Modifier.height(6.dp))
                        ScannedChip()
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutritionChip("P", favorite.totalProtein, MaterialTheme.extendedColors.protein)
                        NutritionChip("C", favorite.totalCarbs, MaterialTheme.extendedColors.carbs)
                        NutritionChip("F", favorite.totalFat, MaterialTheme.extendedColors.fat)
                    }

                    if (favorite.items.isNotEmpty()) {
                        TextButton(onClick = { expanded = !expanded }) {
                            Text(
                                "${favorite.items.size} items",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (expanded) "Hide items" else "Show items",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "${favorite.totalCalories}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalIconButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit and add",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (favorite.items.isNotEmpty() && expanded) {
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    favorite.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, top = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                item.favoriteDisplayLabel(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${item.calories} kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun FavoriteMeal.favoriteDisplayName(): String {
    val cleanName = name.trim()
    val looksLikeItemList = cleanName.contains('\n') || cleanName.startsWith("-")
    if (items.isNotEmpty() && looksLikeItemList) {
        return items.favoriteSummaryName().ifBlank { cleanName.capitalizedFavoriteText() }
    }
    return cleanName.capitalizedFavoriteText()
}

private fun List<NutritionItem>.favoriteSummaryName(): String {
    val itemNames = mapNotNull { it.name.trim().takeIf(String::isNotBlank) }
    return when {
        itemNames.isEmpty() -> ""
        itemNames.size == 1 -> itemNames.first()
        itemNames.size <= 3 -> itemNames.joinToString(", ")
        else -> "${itemNames.take(2).joinToString(", ")} + ${itemNames.size - 2}"
    }.capitalizedFavoriteText()
}

private fun NutritionItem.favoriteDisplayLabel(): String {
    val itemName = name.trim().capitalizedFavoriteText()
    val cleanAmount = amount.cleanAssumedAmount()
    if (cleanAmount.isBlank()) return itemName

    val amountWithoutName = cleanAmount.removeFoodName(name)
    return when {
        amountWithoutName.isBlank() -> itemName
        cleanAmount.equals(name, ignoreCase = true) -> itemName
        cleanAmount.contains(name, ignoreCase = true) -> cleanAmount.capitalizedFavoriteText()
        name.contains(cleanAmount, ignoreCase = true) -> itemName
        else -> "$itemName - $amountWithoutName"
    }
}

private fun String.cleanAssumedAmount(): String {
    if (contains("assumed", ignoreCase = true)) return ""
    return replace(Regex("""\s*\(?assumed\)?\s*""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
}

private fun String.removeFoodName(name: String): String {
    return replace(Regex("""\b${Regex.escape(name.trim())}\b""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""\s+"""), " ")
        .trim()
}

private fun String.capitalizedFavoriteText(): String {
    val trimmed = trim()
    val match = Regex("""^(\d+(?:[.,]\d+)?\s*[a-zA-Z%]*\s+)(\p{L})(.*)$""").matchEntire(trimmed)
    if (match != null) {
        return match.groupValues[1] + match.groupValues[2].uppercase() + match.groupValues[3]
    }

    val firstLetter = trimmed.indexOfFirst { it.isLetter() }
    if (firstLetter == -1) return trimmed
    return trimmed.replaceRange(firstLetter, firstLetter + 1, trimmed[firstLetter].titlecase())
}
