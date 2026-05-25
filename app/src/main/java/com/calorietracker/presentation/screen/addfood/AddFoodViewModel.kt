package com.calorietracker.presentation.screen.addfood

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorietracker.data.connectivity.ConnectivityObserver
import com.calorietracker.data.local.preferences.UserPreferencesDataStore
import com.calorietracker.domain.model.FavoriteMeal
import com.calorietracker.domain.model.FoodEntry
import com.calorietracker.domain.model.FoodSource
import com.calorietracker.domain.model.MealType
import com.calorietracker.domain.model.NutritionInfo
import com.calorietracker.domain.model.NutritionItem
import com.calorietracker.domain.repository.AiRepository
import com.calorietracker.domain.repository.BarcodeLookupResult
import com.calorietracker.domain.repository.BarcodeRepository
import com.calorietracker.domain.repository.FavoriteRepository
import com.calorietracker.domain.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class AddFoodViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val barcodeRepository: BarcodeRepository,
    private val foodRepository: FoodRepository,
    private val favoriteRepository: FavoriteRepository,
    private val connectivityObserver: ConnectivityObserver,
    userPreferencesDataStore: UserPreferencesDataStore,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val routeArgs = MutableStateFlow(
        AddFoodRouteArgs(
            mealType = savedStateHandle.get<String>("mealType") ?: "BREAKFAST",
            date = savedStateHandle.get<String>("date") ?: "",
            favoriteId = savedStateHandle.get<Long>("favoriteId")
        )
    )

    private val _foodDescription = MutableStateFlow(
        (savedStateHandle.get<String>("foodDescription") ?: "").let { saved ->
            TextFieldValue(text = saved, selection = TextRange(saved.length))
        }
    )
    val foodDescription: StateFlow<TextFieldValue> = _foodDescription.asStateFlow()

    private val _uiState = MutableStateFlow<AddFoodUiState>(AddFoodUiState.Idle)
    val uiState: StateFlow<AddFoodUiState> = _uiState.asStateFlow()
    val thinkingEnabled = userPreferencesDataStore.thinkingEnabled

    private val _barcodeUiState = MutableStateFlow(restoreBarcodeState())
    val barcodeUiState: StateFlow<BarcodeUiState> = _barcodeUiState.asStateFlow()

    private fun setBarcodeState(state: BarcodeUiState) {
        _barcodeUiState.value = state
        savedStateHandle["bc_show_dialog"] = state.showServingDialog
        savedStateHandle["bc_serving_grams"] = state.servingGrams
        savedStateHandle["bc_product_name"] = state.scannedProductName
        savedStateHandle["bc_serving_suggestion"] = state.scannedServingSuggestion
        val nutrition = state.scannedNutritionPer100g
        savedStateHandle["bc_has_nutrition"] = nutrition != null
        if (nutrition != null) {
            savedStateHandle["bc_cal"] = nutrition.calories
            savedStateHandle["bc_protein"] = nutrition.proteinGrams
            savedStateHandle["bc_carbs"] = nutrition.carbsGrams
            savedStateHandle["bc_fat"] = nutrition.fatGrams
        }
    }

    private fun restoreBarcodeState(): BarcodeUiState {
        val showDialog = savedStateHandle.get<Boolean>("bc_show_dialog") ?: false
        if (!showDialog) return BarcodeUiState()
        val nutrition = if (savedStateHandle.get<Boolean>("bc_has_nutrition") == true) {
            NutritionInfo(
                calories = savedStateHandle.get<Int>("bc_cal") ?: 0,
                proteinGrams = savedStateHandle.get<Float>("bc_protein") ?: 0f,
                carbsGrams = savedStateHandle.get<Float>("bc_carbs") ?: 0f,
                fatGrams = savedStateHandle.get<Float>("bc_fat") ?: 0f
            )
        } else null
        return BarcodeUiState(
            showServingDialog = true,
            servingGrams = savedStateHandle.get<String>("bc_serving_grams") ?: "100",
            scannedProductName = savedStateHandle.get<String>("bc_product_name") ?: "",
            scannedServingSuggestion = savedStateHandle.get<String>("bc_serving_suggestion"),
            scannedNutritionPer100g = nutrition
        )
    }

    private val _events = MutableSharedFlow<AddFoodEvent>()
    val events: SharedFlow<AddFoodEvent> = _events.asSharedFlow()

    // Cache for AI results to detect re-analysis
    private var lastAnalyzedDescription: String? = null
    private var cachedResult: AddFoodUiState.Result? = null

    fun updateRouteArgs(mealType: String, date: String, favoriteId: Long? = null) {
        val normalizedMealType = mealType.uppercase()
        val newArgs = AddFoodRouteArgs(
            mealType = normalizedMealType,
            date = date,
            favoriteId = favoriteId
        )
        if (routeArgs.value == newArgs) return

        routeArgs.value = newArgs
        savedStateHandle["mealType"] = normalizedMealType
        savedStateHandle["date"] = date
        if (favoriteId != null) {
            savedStateHandle["favoriteId"] = favoriteId
        } else {
            savedStateHandle.remove<Long>("favoriteId")
        }

        lastAnalyzedDescription = null
        cachedResult = null
        savedStateHandle.remove<String>("foodDescription")
        _foodDescription.value = TextFieldValue()
        _uiState.value = AddFoodUiState.Idle
        setBarcodeState(BarcodeUiState())

        if (favoriteId != null) {
            loadFavoriteAsResult(favoriteId)
        }
    }

    private fun loadFavoriteAsResult(favoriteId: Long) {
        viewModelScope.launch {
            val favorite = favoriteRepository.getById(favoriteId) ?: return@launch
            val items = if (favorite.items.isNotEmpty()) {
                favorite.items.map { item ->
                    FoodItemResult(
                        name = item.name,
                        amount = item.amount,
                        calories = item.calories,
                        proteinGrams = item.proteinGrams,
                        carbsGrams = item.carbsGrams,
                        fatGrams = item.fatGrams
                    )
                }
            } else {
                listOf(
                    FoodItemResult(
                        name = favorite.name,
                        amount = "",
                        calories = favorite.totalCalories,
                        proteinGrams = favorite.totalProtein,
                        carbsGrams = favorite.totalCarbs,
                        fatGrams = favorite.totalFat
                    )
                )
            }
            val description = favorite.description.ifBlank { favorite.name }
            _foodDescription.value = TextFieldValue(
                text = description,
                selection = TextRange(description.length)
            )
            savedStateHandle["foodDescription"] = description
            lastAnalyzedDescription = description
            val resultState = AddFoodUiState.Result(
                description = description,
                totalCalories = favorite.totalCalories,
                totalProtein = favorite.totalProtein,
                totalCarbs = favorite.totalCarbs,
                totalFat = favorite.totalFat,
                items = items,
                isCached = false,
                favoriteName = favorite.name,
                isAlreadyFavorite = true,
                source = favorite.source
            )
            cachedResult = resultState
            _uiState.value = resultState
        }
    }

    fun updateDescription(value: TextFieldValue) {
        val prev = _foodDescription.value.text
        val next = value.text
        val insertedIndex = singleInsertedCharIndex(prev = prev, next = next)
        val selectionOverride: Int?
        val transformed = when {
            insertedIndex != null && next[insertedIndex] == '\n' -> {
                val insertAboveBullet =
                    (insertedIndex == 0 || next[insertedIndex - 1] == '\n') &&
                        next.startsWith("- ", insertedIndex + 1)
                if (insertAboveBullet) {
                    selectionOverride = insertedIndex + 2
                    next.replaceRange(insertedIndex, insertedIndex, "- ")
                } else {
                    selectionOverride = insertedIndex + 3
                    next.replaceRange(insertedIndex + 1, insertedIndex + 1, "- ")
                }
            }
            prev.isEmpty() && next.isNotEmpty() && !next.startsWith("- ") -> {
                selectionOverride = next.length + 2
                "- $next"
            }
            else -> {
                selectionOverride = null
                next
            }
        }
        val trimmed = transformed.take(700)
        val selection = if (selectionOverride != null || trimmed.length != transformed.length) {
            TextRange((selectionOverride ?: trimmed.length).coerceIn(0, trimmed.length))
        } else {
            TextRange(
                value.selection.start.coerceIn(0, trimmed.length),
                value.selection.end.coerceIn(0, trimmed.length)
            )
        }
        _foodDescription.value = value.copy(text = trimmed, selection = selection)
        savedStateHandle["foodDescription"] = trimmed
    }

    private fun singleInsertedCharIndex(prev: String, next: String): Int? {
        if (next.length != prev.length + 1) return null

        var index = 0
        while (index < prev.length && prev[index] == next[index]) {
            index++
        }

        return if (prev.regionMatches(index, next, index + 1, prev.length - index)) {
            index
        } else {
            null
        }
    }

    fun analyzeFood() {
        val description = _foodDescription.value.text
            .lines()
            .dropLastWhile { it.isBlank() || it.trimEnd() == "-" }
            .joinToString("\n")
            .trim()
        if (description.isEmpty()) return

        if (description != lastAnalyzedDescription && !connectivityObserver.isOnline()) {
            _uiState.value = AddFoodUiState.Error("You're offline — connect to analyze")
            return
        }

        // Check cache
        if (description == lastAnalyzedDescription && cachedResult != null) {
            viewModelScope.launch {
                val parsedMealType = MealType.valueOf(routeArgs.value.mealType)
                val existingFavorite = favoriteRepository
                    .findByDescriptionAndMealType(description, parsedMealType)
                _uiState.value = cachedResult!!.copy(
                    isCached = true,
                    isAlreadyFavorite = existingFavorite != null,
                    favoriteName = existingFavorite?.name ?: cachedResult!!.favoriteName
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = AddFoodUiState.Analyzing
            aiRepository.analyzeFood(description)
                .onSuccess { result ->
                    val items = result.items.map { item ->
                        FoodItemResult(
                            name = item.name,
                            amount = item.amount,
                            calories = item.calories,
                            proteinGrams = item.proteinGrams,
                            carbsGrams = item.carbsGrams,
                            fatGrams = item.fatGrams
                        )
                    }
                    val parsedMealType = MealType.valueOf(routeArgs.value.mealType)
                    val existingFavorite = favoriteRepository
                        .findByDescriptionAndMealType(description, parsedMealType)
                    val resultState = AddFoodUiState.Result(
                        description = description,
                        totalCalories = result.calories,
                        totalProtein = result.proteinGrams,
                        totalCarbs = result.carbsGrams,
                        totalFat = result.fatGrams,
                        items = items,
                        isCached = false,
                        favoriteName = existingFavorite?.name.orEmpty(),
                        isAlreadyFavorite = existingFavorite != null,
                        source = FoodSource.AI
                    )
                    lastAnalyzedDescription = description
                    cachedResult = resultState
                    _uiState.value = resultState
                }
                .onFailure { error ->
                    _uiState.value = AddFoodUiState.Error(
                        error.message ?: "Failed to analyze food"
                    )
                }
        }
    }

    fun reAnalyze() {
        lastAnalyzedDescription = null
        cachedResult = null
        analyzeFood()
    }

    fun logFood() {
        logFood(updateFavorite = true)
    }

    fun logFoodOnly() {
        logFood(updateFavorite = false)
    }

    private fun logFood(updateFavorite: Boolean) {
        val state = _uiState.value
        if (state !is AddFoodUiState.Result) return

        viewModelScope.launch {
            val args = routeArgs.value
            val parsedMealType = MealType.valueOf(args.mealType)

            val items = state.items.map { item ->
                NutritionItem(
                    name = item.name,
                    amount = item.amount,
                    calories = item.calories,
                    proteinGrams = item.proteinGrams,
                    carbsGrams = item.carbsGrams,
                    fatGrams = item.fatGrams
                )
            }

            if (updateFavorite && args.favoriteId != null && state.isAlreadyFavorite) {
                val existing = favoriteRepository.getById(args.favoriteId)
                if (existing != null) {
                    favoriteRepository.update(
                        existing.copy(
                            name = state.favoriteName.capitalizedFoodName(),
                            description = state.description,
                            totalCalories = state.totalCalories,
                            totalProtein = state.totalProtein,
                            totalCarbs = state.totalCarbs,
                            totalFat = state.totalFat,
                            items = items,
                            source = state.source
                        )
                    )
                }
                savedStateHandle.remove<String>("foodDescription")
                _foodDescription.value = TextFieldValue()
                _uiState.value = AddFoodUiState.Idle
                _events.emit(AddFoodEvent.FoodLogged)
                return@launch
            }

            val timestamp = System.currentTimeMillis()

            state.items.forEach { item ->
                foodRepository.insertEntry(
                    FoodEntry(
                        date = args.date,
                        mealType = parsedMealType,
                        description = item.loggedDescription(),
                        nutritionInfo = NutritionInfo(
                            calories = item.calories,
                            proteinGrams = item.proteinGrams,
                            carbsGrams = item.carbsGrams,
                            fatGrams = item.fatGrams
                        ),
                        timestamp = timestamp,
                        source = state.source
                    )
                )
            }

            if (updateFavorite && state.saveAsFavorite && state.favoriteName.isNotBlank()) {
                favoriteRepository.insert(
                    FavoriteMeal(
                        name = state.favoriteName.capitalizedFoodName(),
                        description = state.description,
                        totalCalories = state.totalCalories,
                        totalProtein = state.totalProtein,
                        totalCarbs = state.totalCarbs,
                        totalFat = state.totalFat,
                        items = items,
                        mealType = parsedMealType,
                        source = state.source
                    )
                )
            } else if (updateFavorite && state.isAlreadyFavorite) {
                val existing = args.favoriteId?.let { favoriteRepository.getById(it) }
                    ?: favoriteRepository.findByDescriptionAndMealType(state.description, parsedMealType)
                if (existing != null) {
                    favoriteRepository.update(
                        existing.copy(
                            name = state.favoriteName.capitalizedFoodName(),
                            description = state.description,
                            totalCalories = state.totalCalories,
                            totalProtein = state.totalProtein,
                            totalCarbs = state.totalCarbs,
                            totalFat = state.totalFat,
                            items = items,
                            source = state.source
                        )
                    )
                }
            }

            savedStateHandle.remove<String>("foodDescription")
            _foodDescription.value = TextFieldValue()
            _uiState.value = AddFoodUiState.Idle
            _events.emit(AddFoodEvent.FoodLogged)
        }
    }

    fun switchToManualEntry() {
        _uiState.value = AddFoodUiState.ManualEntry()
    }

    fun switchToAiEntry() {
        _uiState.value = AddFoodUiState.Idle
    }

    fun updateManualEntry(entry: AddFoodUiState.ManualEntry) {
        _uiState.value = entry
    }

    fun logManualEntry() {
        val state = _uiState.value
        if (state !is AddFoodUiState.ManualEntry) return
        if (!ManualEntryValidator.isValid(state)) return

        val name = state.name.ifBlank { "Custom entry" }
        val calories = state.calories.toIntOrNull() ?: 0
        val protein = state.protein.replace(',', '.').toFloatOrNull() ?: 0f
        val carbs = state.carbs.replace(',', '.').toFloatOrNull() ?: 0f
        val fat = state.fat.replace(',', '.').toFloatOrNull() ?: 0f

        viewModelScope.launch {
            val args = routeArgs.value
            val parsedMealType = MealType.valueOf(args.mealType)
            foodRepository.insertEntry(
                FoodEntry(
                    date = args.date,
                    mealType = parsedMealType,
                    description = name,
                    nutritionInfo = NutritionInfo(
                        calories = calories,
                        proteinGrams = protein,
                        carbsGrams = carbs,
                        fatGrams = fat
                    ),
                    timestamp = System.currentTimeMillis(),
                    source = FoodSource.MANUAL
                )
            )

            savedStateHandle.remove<String>("foodDescription")
            _foodDescription.value = TextFieldValue()
            _uiState.value = AddFoodUiState.Idle
            _events.emit(AddFoodEvent.FoodLogged)
        }
    }

    fun toggleFavorite() {
        val state = _uiState.value
        if (state is AddFoodUiState.Result) {
            _uiState.value = state.copy(
                saveAsFavorite = !state.saveAsFavorite,
                favoriteName = if (!state.saveAsFavorite) state.defaultFavoriteName() else state.favoriteName
            )
        }
    }

    fun updateFavoriteName(name: String) {
        val state = _uiState.value
        if (state is AddFoodUiState.Result) {
            _uiState.value = state.copy(favoriteName = name)
        }
    }

    fun editItem(index: Int) {
        val state = _uiState.value
        if (state is AddFoodUiState.Result) {
            val updatedItems = state.items.mapIndexed { i, item ->
                if (i == index) item.copy(isEditing = !item.isEditing)
                else item.copy(isEditing = false)
            }
            _uiState.value = state.copy(items = updatedItems)
        }
    }

    fun updateItem(index: Int, updated: FoodItemResult) {
        val state = _uiState.value
        if (state is AddFoodUiState.Result) {
            val updatedItems = state.items.toMutableList().apply {
                set(index, updated.copy(isEditing = false))
            }
            val totalCals = updatedItems.sumOf { it.calories }
            val totalProtein = updatedItems.map { it.proteinGrams }.sum()
            val totalCarbs = updatedItems.map { it.carbsGrams }.sum()
            val totalFat = updatedItems.map { it.fatGrams }.sum()

            _uiState.value = state.copy(
                items = updatedItems,
                totalCalories = totalCals,
                totalProtein = totalProtein,
                totalCarbs = totalCarbs,
                totalFat = totalFat
            )
        }
    }

    fun removeItem(index: Int) {
        val state = _uiState.value
        if (state is AddFoodUiState.Result) {
            val updatedItems = state.items.toMutableList().apply {
                removeAt(index)
            }
            if (updatedItems.isEmpty()) {
                _uiState.value = AddFoodUiState.Idle
                return
            }
            val totalCals = updatedItems.sumOf { it.calories }
            val totalProtein = updatedItems.map { it.proteinGrams }.sum()
            val totalCarbs = updatedItems.map { it.carbsGrams }.sum()
            val totalFat = updatedItems.map { it.fatGrams }.sum()

            _uiState.value = state.copy(
                items = updatedItems,
                totalCalories = totalCals,
                totalProtein = totalProtein,
                totalCarbs = totalCarbs,
                totalFat = totalFat
            )
        }
    }

    fun onScanClicked() {
        setBarcodeState(
            _barcodeUiState.value.copy(
                isScannerVisible = true,
                feedbackMessage = null
            )
        )
    }

    fun onScannerDismissed() {
        setBarcodeState(_barcodeUiState.value.copy(isScannerVisible = false))
    }

    fun onBarcodeDetected(barcode: String) {
        if (!connectivityObserver.isOnline()) {
            setBarcodeState(
                BarcodeUiState(feedbackMessage = "You're offline — connect to look up products")
            )
            return
        }
        setBarcodeState(
            _barcodeUiState.value.copy(
                isScannerVisible = false,
                isLookupLoading = true,
                showServingDialog = false,
                scannedProductName = "",
                scannedNutritionPer100g = null,
                scannedServingSuggestion = null
            )
        )

        viewModelScope.launch {
            val nextState = when (val result = barcodeRepository.getProduct(barcode)) {
                is BarcodeLookupResult.Found -> {
                    val parsedServing = parseServingAmount(result.servingSize)
                    _barcodeUiState.value.copy(
                        isLookupLoading = false,
                        showServingDialog = true,
                        scannedProductName = result.productName,
                        scannedNutritionPer100g = result.nutritionPer100g,
                        scannedServingSuggestion = result.servingSize,
                        servingGrams = parsedServing?.toString() ?: "100"
                    )
                }

                is BarcodeLookupResult.NotFound -> {
                    BarcodeUiState(
                        feedbackMessage = "Product not found - describe it manually or ask AI"
                    )
                }

                is BarcodeLookupResult.IncompleteData -> {
                    BarcodeUiState(
                        feedbackMessage = "\"${result.productName}\" found but has no nutrition data - describe it manually or ask AI"
                    )
                }

                is BarcodeLookupResult.Error -> {
                    BarcodeUiState(feedbackMessage = result.message)
                }
            }
            setBarcodeState(nextState)
        }
    }

    fun onServingGramsChanged(value: String) {
        val sanitized = value.filter { it.isDigit() || it == '.' || it == ',' }
        val parsed = sanitized.replace(',', '.').toFloatOrNull()
        val cleared = parsed != null && parsed > 0f
        setBarcodeState(
            _barcodeUiState.value.copy(
                servingGrams = sanitized,
                servingError = if (cleared) null else _barcodeUiState.value.servingError
            )
        )
    }

    fun onServingConfirmed() {
        val barcodeState = _barcodeUiState.value
        val nutritionPer100g = barcodeState.scannedNutritionPer100g ?: return
        val grams = barcodeState.servingGrams.replace(',', '.').toFloatOrNull()
        if (grams == null || grams <= 0f) {
            setBarcodeState(
                barcodeState.copy(servingError = "Enter a serving size greater than 0")
            )
            return
        }

        viewModelScope.launch {
            val args = routeArgs.value
            val parsedMealType = MealType.valueOf(args.mealType)
            val multiplier = grams / 100f
            val gramsLabel = if (grams % 1f == 0f) {
                grams.toInt().toString()
            } else {
                grams.toString()
            }

            val description = "${gramsLabel}g ${barcodeState.scannedProductName}"
            val existingFavorite = favoriteRepository.findByDescriptionAndMealType(
                description,
                parsedMealType
            )
            val resultState = AddFoodUiState.Result(
                description = description,
                totalCalories = (nutritionPer100g.calories * multiplier).roundToInt(),
                totalProtein = nutritionPer100g.proteinGrams * multiplier,
                totalCarbs = nutritionPer100g.carbsGrams * multiplier,
                totalFat = nutritionPer100g.fatGrams * multiplier,
                items = listOf(
                    FoodItemResult(
                        name = barcodeState.scannedProductName,
                        amount = "${gramsLabel}g",
                        calories = (nutritionPer100g.calories * multiplier).roundToInt(),
                        proteinGrams = nutritionPer100g.proteinGrams * multiplier,
                        carbsGrams = nutritionPer100g.carbsGrams * multiplier,
                        fatGrams = nutritionPer100g.fatGrams * multiplier
                    )
                ),
                favoriteName = existingFavorite?.name
                    ?: barcodeState.scannedProductName.capitalizedFoodName(),
                isAlreadyFavorite = existingFavorite != null,
                source = FoodSource.SCANNED
            )
            cachedResult = resultState
            _uiState.value = resultState
            setBarcodeState(BarcodeUiState())
        }
    }

    fun onServingDismissed() {
        setBarcodeState(BarcodeUiState())
    }

    fun consumeBarcodeFeedback() {
        setBarcodeState(_barcodeUiState.value.copy(feedbackMessage = null))
    }
}

private data class AddFoodRouteArgs(
    val mealType: String,
    val date: String,
    val favoriteId: Long? = null
)

private fun AddFoodUiState.Result.defaultFavoriteName(): String {
    val itemNames = items.mapNotNull { it.name.trim().takeIf(String::isNotBlank) }
    return when {
        itemNames.isEmpty() -> description
        itemNames.size == 1 -> itemNames.first()
        itemNames.size <= 3 -> itemNames.joinToString(", ")
        else -> "${itemNames.take(2).joinToString(", ")} + ${itemNames.size - 2}"
    }.capitalizedFoodName()
}

private fun FoodItemResult.loggedDescription(): String {
    val loggedAmount = amount.trim()
    val loggedName = name.trim()
    val correctedDescription = loggedAmount.correctedWith(loggedName)

    return when {
        loggedAmount.isEmpty() -> loggedName
        loggedName.isEmpty() -> loggedAmount
        loggedAmount.equals(loggedName, ignoreCase = true) -> loggedName
        loggedAmount.contains(loggedName, ignoreCase = true) -> loggedAmount
        loggedName.startsWith(loggedAmount, ignoreCase = true) -> loggedName
        correctedDescription != null -> correctedDescription
        else -> "$loggedAmount $loggedName"
    }.capitalizedFoodName()
}

private fun String.correctedWith(name: String): String? {
    val match = Regex("""^\s*(\d+(?:[.,]\d+)?\s*[a-zA-Z%]*)\s+(.+?)\s*$""").matchEntire(this)
        ?: return null
    val amountPrefix = match.groupValues[1].trim()
    val amountFood = match.groupValues[2]
    if (!amountFood.isNearMatch(name)) return null
    return "$amountPrefix $name"
}

private fun String.isNearMatch(other: String): Boolean {
    val left = lettersOnly()
    val right = other.lettersOnly()
    if (left.length < 4 || right.length < 4) return false
    if (left == right) return true

    val maxDistance = if (maxOf(left.length, right.length) >= 9) 2 else 1
    return levenshteinDistance(left, right) <= maxDistance
}

private fun String.lettersOnly(): String {
    return filter { it.isLetter() }.lowercase()
}

private fun String.capitalizedFoodName(): String {
    val trimmed = trim()
    val match = Regex("""^(\d+(?:[.,]\d+)?\s*[a-zA-Z%]*\s+)(\p{L})(.*)$""").matchEntire(trimmed)
    if (match != null) {
        return match.groupValues[1] + match.groupValues[2].uppercase() + match.groupValues[3]
    }

    val firstLetter = trimmed.indexOfFirst { it.isLetter() }
    if (firstLetter == -1) return trimmed
    return trimmed.replaceRange(firstLetter, firstLetter + 1, trimmed[firstLetter].titlecase())
}

private fun levenshteinDistance(left: String, right: String): Int {
    var previous = IntArray(right.length + 1) { it }
    var current = IntArray(right.length + 1)

    for (i in left.indices) {
        current[0] = i + 1
        for (j in right.indices) {
            val cost = if (left[i] == right[j]) 0 else 1
            current[j + 1] = minOf(
                current[j] + 1,
                previous[j + 1] + 1,
                previous[j] + cost
            )
        }
        val swap = previous
        previous = current
        current = swap
    }

    return previous[right.length]
}

private fun parseServingAmount(servingSize: String?): Int? {
    if (servingSize.isNullOrBlank()) return null

    return Regex("""(\d+(?:[.,]\d+)?)\s*(?:g|ml)\b""", RegexOption.IGNORE_CASE)
        .findAll(servingSize)
        .lastOrNull()
        ?.groupValues
        ?.getOrNull(1)
        ?.replace(',', '.')
        ?.toFloatOrNull()
        ?.roundToInt()
}

sealed interface AddFoodEvent {
    data object FoodLogged : AddFoodEvent
}
