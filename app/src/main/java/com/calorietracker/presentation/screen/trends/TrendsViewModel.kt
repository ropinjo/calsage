package com.calorietracker.presentation.screen.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorietracker.data.local.preferences.UserPreferencesDataStore
import com.calorietracker.domain.model.WeightEntry
import com.calorietracker.domain.repository.DailyCalorie
import com.calorietracker.domain.repository.FoodRepository
import com.calorietracker.domain.repository.GoalsRepository
import com.calorietracker.domain.repository.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class TrendsTab { Calories, Macros, Weight }
enum class TrendsRange(val label: String, val days: Int?) {
    ONE_WEEK("7d", 7),
    ONE_MONTH("30d", 30),
    THREE_MONTHS("90d", 90),
    ONE_YEAR("1y", 365),
    ALL("All", null)
}

data class DailyMacros(
    val date: String,
    val protein: Float,
    val carbs: Float,
    val fat: Float
)

data class DailyCaloriePoint(
    val date: String,
    val totalCalories: Int,
    val tracked: Boolean
)

data class TrendsUiState(
    val selectedTab: TrendsTab = TrendsTab.Calories,
    val selectedRange: TrendsRange = TrendsRange.ONE_WEEK,
    val calorieData: List<DailyCaloriePoint> = emptyList(),
    val macroData: List<DailyMacros> = emptyList(),
    val calorieGoal: Int = 2000,
    val weightData: List<WeightEntry> = emptyList(),
    val weightUnit: String = "kg",
    val isLoading: Boolean = true
)

private data class TrendsBaseState(
    val selectedTab: TrendsTab,
    val selectedRange: TrendsRange,
    val calorieGoal: Int,
    val weightUnit: String
)

@HiltViewModel
class TrendsViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val weightRepository: WeightRepository,
    private val goalsRepository: GoalsRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(TrendsTab.Calories)
    private val _selectedRange = MutableStateFlow(TrendsRange.ONE_WEEK)

    val uiState: StateFlow<TrendsUiState> = combine(
        _selectedTab,
        _selectedRange,
        goalsRepository.getGoals(),
        userPreferencesDataStore.selectedUnit
    ) { tab, range, goals, unit ->
        TrendsBaseState(
            selectedTab = tab,
            selectedRange = range,
            calorieGoal = goals.calorieTarget,
            weightUnit = unit
        )
    }.flatMapLatest { baseState ->
        val endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val startDate = baseState.selectedRange.days?.let { days ->
            LocalDate.now().minusDays((days - 1).toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
        } ?: "1970-01-01"
        // Short fixed windows (7d, 30d) show the full range so the chart matches
        // the selected chip; longer and open-ended ranges trim leading untracked
        // days so they don't open with empty space from before tracking began.
        val trimLeading = baseState.selectedRange.days?.let { it > 30 } ?: true

        combine(
            foodRepository.getCalorieTrend(startDate, endDate),
            foodRepository.getAllEntriesInRange(startDate, endDate),
            weightRepository.getWeightEntries(startDate, endDate)
        ) { calorieData, allEntries, weightData ->
            val macroData = allEntries
                .groupBy { it.date }
                .map { (date, entries) ->
                    DailyMacros(
                        date = date,
                        protein = entries.map { it.nutritionInfo.proteinGrams }.sum(),
                        carbs = entries.map { it.nutritionInfo.carbsGrams }.sum(),
                        fat = entries.map { it.nutritionInfo.fatGrams }.sum()
                    )
                }
                .sortedBy { it.date }

            TrendsUiState(
                selectedTab = baseState.selectedTab,
                selectedRange = baseState.selectedRange,
                calorieData = fillUntrackedDays(calorieData, startDate, endDate, trimLeading = trimLeading),
                macroData = macroData,
                calorieGoal = baseState.calorieGoal,
                weightData = weightData,
                weightUnit = baseState.weightUnit,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrendsUiState()
    )

    fun selectTab(tab: TrendsTab) {
        _selectedTab.value = tab
    }

    fun selectRange(range: TrendsRange) {
        _selectedRange.value = range
    }
}

/**
 * Expands sparse per-day totals into a continuous day series so untracked days
 * appear as gaps instead of being silently skipped. When [trimLeading] is set
 * the series starts at the first tracked day in range (not the range start) so
 * long charts don't lead with empty space from before the user began tracking;
 * otherwise it spans the full range so short fixed windows match their chip.
 */
internal fun fillUntrackedDays(
    data: List<DailyCalorie>,
    startDate: String,
    endDate: String,
    trimLeading: Boolean
): List<DailyCaloriePoint> {
    if (data.isEmpty()) return emptyList()
    val byDate = data.associateBy { it.date }
    val rangeStart = LocalDate.parse(startDate)
    var current = if (trimLeading) {
        maxOf(rangeStart, LocalDate.parse(data.minOf { it.date }))
    } else {
        rangeStart
    }
    val end = LocalDate.parse(endDate)
    val result = mutableListOf<DailyCaloriePoint>()
    while (current <= end) {
        val date = current.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val tracked = byDate[date]
        result += DailyCaloriePoint(date, tracked?.totalCalories ?: 0, tracked != null)
        current = current.plusDays(1)
    }
    return result
}
