package com.calorietracker.presentation.screen.favorites

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorietracker.domain.model.FavoriteMeal
import com.calorietracker.domain.model.FoodEntry
import com.calorietracker.domain.model.MealType
import com.calorietracker.domain.model.NutritionInfo
import com.calorietracker.domain.repository.FavoriteRepository
import com.calorietracker.domain.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val favorites: List<FavoriteMeal> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
    private val foodRepository: FoodRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val mealTypeFlow: MutableStateFlow<MealType?> = MutableStateFlow(
        savedStateHandle.get<String>("mealType")?.uppercase()
            ?.let { runCatching { MealType.valueOf(it) }.getOrNull() }
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _events = MutableSharedFlow<FavoritesEvent>()
    val events: SharedFlow<FavoritesEvent> = _events.asSharedFlow()

    val uiState: StateFlow<FavoritesUiState> = combine(_searchQuery, mealTypeFlow) { query, type ->
        query to type
    }.flatMapLatest { (query, type) ->
        when {
            type == null -> flowOf(null)
            query.isBlank() -> favoriteRepository.getByMealType(type)
            else -> favoriteRepository.searchByMealType(query, type)
        }
    }.map { favorites ->
        FavoritesUiState(
            favorites = favorites.orEmpty(),
            searchQuery = _searchQuery.value,
            isLoading = favorites == null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FavoritesUiState()
    )

    fun setMealType(mealType: String) {
        val normalized = runCatching { MealType.valueOf(mealType.uppercase()) }.getOrNull()
            ?: return
        if (mealTypeFlow.value != normalized) {
            mealTypeFlow.value = normalized
            savedStateHandle["mealType"] = normalized.name
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun quickLog(favorite: FavoriteMeal, date: String) {
        val mealType = mealTypeFlow.value ?: return
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()

            if (favorite.items.isNotEmpty()) {
                favorite.items.forEach { item ->
                    foodRepository.insertEntry(
                        FoodEntry(
                            date = date,
                            mealType = mealType,
                            description = item.name,
                            nutritionInfo = NutritionInfo(
                                calories = item.calories,
                                proteinGrams = item.proteinGrams,
                                carbsGrams = item.carbsGrams,
                                fatGrams = item.fatGrams
                            ),
                            timestamp = timestamp,
                            source = favorite.source
                        )
                    )
                }
            } else {
                foodRepository.insertEntry(
                    FoodEntry(
                        date = date,
                        mealType = mealType,
                        description = favorite.name,
                        nutritionInfo = NutritionInfo(
                            calories = favorite.totalCalories,
                            proteinGrams = favorite.totalProtein,
                            carbsGrams = favorite.totalCarbs,
                            fatGrams = favorite.totalFat
                        ),
                        timestamp = timestamp,
                        source = favorite.source
                    )
                )
            }

            _events.emit(FavoritesEvent.FavoriteLogged(favorite.name, mealType))
        }
    }

    fun deleteFavorite(id: Long) {
        viewModelScope.launch {
            favoriteRepository.delete(id)
        }
    }
}

sealed interface FavoritesEvent {
    data class FavoriteLogged(val name: String, val mealType: MealType) : FavoritesEvent
}
