package com.calorietracker.presentation.screen.mealdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorietracker.domain.model.FoodEntry
import com.calorietracker.domain.model.MealType
import com.calorietracker.domain.model.NutritionInfo
import com.calorietracker.domain.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class MealDetailViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val routeArgs = MutableStateFlow(
        MealDetailRouteArgs(
            mealType = savedStateHandle.get<String>("mealType") ?: "BREAKFAST",
            date = savedStateHandle.get<String>("date") ?: ""
        )
    )

    private val _events = MutableSharedFlow<MealDetailEvent>()
    val events: SharedFlow<MealDetailEvent> = _events.asSharedFlow()

    // Track which entries are in editing mode locally
    private val _editingIds = MutableStateFlow<Set<Long>>(emptySet())

    private var lastDeletedEntry: FoodEntry? = null
    private var currentEntries: List<FoodEntry> = emptyList()

    val uiState: StateFlow<MealDetailUiState> = routeArgs
        .flatMapLatest { args ->
            foodRepository
                .getEntriesForMeal(args.date, MealType.valueOf(args.mealType.uppercase()))
                .combine(_editingIds) { entries, editingIds ->
                    currentEntries = entries
                    if (entries.isEmpty()) {
                        MealDetailUiState.Empty
                    } else {
                        val detailEntries = entries.map { entry ->
                            MealDetailEntry(
                                id = entry.id,
                                description = entry.description,
                                calories = entry.nutritionInfo.calories,
                                protein = entry.nutritionInfo.proteinGrams,
                                carbs = entry.nutritionInfo.carbsGrams,
                                fat = entry.nutritionInfo.fatGrams,
                                isEditing = entry.id in editingIds,
                                source = entry.source
                            )
                        }
                        MealDetailUiState.Success(
                            mealType = args.mealType.lowercase().replaceFirstChar { it.uppercase() },
                            date = args.date,
                            totalCalories = detailEntries.sumOf { it.calories },
                            totalProtein = detailEntries.map { it.protein }.sum(),
                            totalCarbs = detailEntries.map { it.carbs }.sum(),
                            totalFat = detailEntries.map { it.fat }.sum(),
                            entries = detailEntries
                        )
                    }
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MealDetailUiState.Loading
        )

    fun updateRouteArgs(mealType: String, date: String) {
        val normalizedMealType = mealType.uppercase()
        val newArgs = MealDetailRouteArgs(mealType = normalizedMealType, date = date)
        if (routeArgs.value == newArgs) return

        routeArgs.value = newArgs
        savedStateHandle["mealType"] = normalizedMealType
        savedStateHandle["date"] = date
        _editingIds.value = emptySet()
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            val args = routeArgs.value
            // Store for undo before deleting
            lastDeletedEntry = currentEntries.find { it.id == id }?.copy(
                date = args.date,
                mealType = MealType.valueOf(args.mealType)
            )
            foodRepository.deleteEntry(id)
            _events.emit(MealDetailEvent.EntryDeleted)
        }
    }

    fun undoDelete() {
        val entry = lastDeletedEntry ?: return
        viewModelScope.launch {
            foodRepository.insertEntry(entry)
            lastDeletedEntry = null
        }
    }

    fun toggleEditEntry(id: Long) {
        val current = _editingIds.value.toMutableSet()
        if (id in current) {
            current.remove(id)
        } else {
            current.clear()
            current.add(id)
        }
        _editingIds.value = current
    }

    fun updateEntry(entry: MealDetailEntry) {
        viewModelScope.launch {
            val args = routeArgs.value
            val existing = currentEntries.find { it.id == entry.id } ?: return@launch
            foodRepository.updateEntry(
                FoodEntry(
                    id = entry.id,
                    date = args.date,
                    mealType = MealType.valueOf(args.mealType),
                    description = entry.description,
                    nutritionInfo = NutritionInfo(
                        calories = entry.calories,
                        proteinGrams = entry.protein,
                        carbsGrams = entry.carbs,
                        fatGrams = entry.fat
                    ),
                    timestamp = existing.timestamp,
                    source = entry.source
                )
            )
            _editingIds.value = _editingIds.value - entry.id
        }
    }
}

private data class MealDetailRouteArgs(
    val mealType: String,
    val date: String
)

sealed interface MealDetailEvent {
    data object EntryDeleted : MealDetailEvent
}
