package com.calorietracker.presentation.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorietracker.data.local.preferences.UserPreferencesDataStore
import com.calorietracker.domain.model.MealType
import com.calorietracker.domain.repository.FoodRepository
import com.calorietracker.domain.repository.GoalsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val goalsRepository: GoalsRepository,
    private val userPreferences: UserPreferencesDataStore
) : ViewModel() {

    private val today: String
        get() = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    val selectedDate: StateFlow<String> = userPreferences.selectedDate
        .map { it ?: today }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = today
        )

    val uiState: StateFlow<DashboardUiState> = selectedDate.flatMapLatest { date ->
        combine(
            foodRepository.getDailyTotal(date),
            foodRepository.getMealSubtotals(date),
            goalsRepository.getGoals()
        ) { dailyTotal, mealSubtotals, goals ->
            val mealSummaries = MealType.entries.map { type ->
                val subtotal = mealSubtotals.find { it.mealType == type }
                MealSummary(
                    mealType = type.displayName,
                    totalCalories = subtotal?.calories ?: 0,
                    protein = subtotal?.protein ?: 0f,
                    carbs = subtotal?.carbs ?: 0f,
                    fat = subtotal?.fat ?: 0f,
                    itemCount = subtotal?.itemCount ?: 0
                )
            }

            DashboardUiState.Success(
                selectedDate = date,
                caloriesConsumed = dailyTotal?.calories ?: 0,
                calorieTarget = goals?.calorieTarget ?: 2000,
                proteinConsumed = dailyTotal?.proteinGrams ?: 0f,
                proteinTarget = goals?.proteinTargetGrams ?: 150f,
                carbsConsumed = dailyTotal?.carbsGrams ?: 0f,
                carbsTarget = goals?.carbsTargetGrams ?: 250f,
                fatConsumed = dailyTotal?.fatGrams ?: 0f,
                fatTarget = goals?.fatTargetGrams ?: 65f,
                mealSummaries = mealSummaries
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState.Loading
    )

    fun selectDate(date: String) {
        viewModelScope.launch {
            userPreferences.setSelectedDate(date)
        }
    }

    fun navigateDay(offset: Int) {
        val currentDate = LocalDate.parse(selectedDate.value, DateTimeFormatter.ISO_LOCAL_DATE)
        val newDate = currentDate.plusDays(offset.toLong())
        selectDate(newDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
    }
}
