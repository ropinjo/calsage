package com.calorietracker.presentation.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorietracker.data.local.preferences.UserPreferencesDataStore
import com.calorietracker.domain.model.MealType
import com.calorietracker.domain.repository.FoodRepository
import com.calorietracker.domain.repository.GoalsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val goalsRepository: GoalsRepository,
    private val userPreferences: UserPreferencesDataStore
) : ViewModel() {

    private val today: String
        get() = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    init {
        viewModelScope.launch {
            val savedDate = userPreferences.selectedDate.first() ?: return@launch
            val currentDate = LocalDate.now()
            if (LocalDate.parse(savedDate, DateTimeFormatter.ISO_LOCAL_DATE).isBefore(currentDate)) {
                userPreferences.setSelectedDate(currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
            }
        }
    }

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
                caloriesConsumed = dailyTotal.calories,
                calorieTarget = goals.calorieTarget,
                proteinConsumed = dailyTotal.proteinGrams,
                proteinTarget = goals.proteinTargetGrams,
                carbsConsumed = dailyTotal.carbsGrams,
                carbsTarget = goals.carbsTargetGrams,
                fatConsumed = dailyTotal.fatGrams,
                fatTarget = goals.fatTargetGrams,
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

    fun selectToday() {
        selectDate(today)
    }

    fun navigateDay(offset: Int) {
        val currentDate = LocalDate.parse(selectedDate.value, DateTimeFormatter.ISO_LOCAL_DATE)
        val newDate = currentDate.plusDays(offset.toLong())
        selectDate(newDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
    }
}
