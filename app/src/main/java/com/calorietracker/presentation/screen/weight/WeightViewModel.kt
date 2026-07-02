package com.calorietracker.presentation.screen.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorietracker.data.local.preferences.UserPreferencesDataStore
import com.calorietracker.domain.model.WeightEntry
import com.calorietracker.domain.repository.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class ChartRange(val label: String, val days: Int?) {
    SEVEN_DAYS("7d", 7),
    THIRTY_DAYS("30d", 30),
    NINETY_DAYS("90d", 90),
    ONE_YEAR("1y", 365),
    ALL("All", null)
}

data class WeightUiState(
    val latestWeight: Float? = null,
    val entries: List<WeightEntry> = emptyList(),
    val chartRange: ChartRange = ChartRange.THIRTY_DAYS,
    val weeklyChange: Float? = null,
    val monthlyChange: Float? = null,
    val unit: String = "kg",
    val isLoading: Boolean = true
)

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val weightRepository: WeightRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _chartRange = MutableStateFlow(ChartRange.THIRTY_DAYS)

    private val _weightInput = MutableStateFlow("")
    val weightInput: StateFlow<String> = _weightInput.asStateFlow()

    private val _noteInput = MutableStateFlow("")
    val noteInput: StateFlow<String> = _noteInput.asStateFlow()

    private val _events = MutableSharedFlow<WeightEvent>()
    val events: SharedFlow<WeightEvent> = _events.asSharedFlow()

    val uiState: StateFlow<WeightUiState> = combine(
        _chartRange,
        weightRepository.getLatestWeight(),
        userPreferencesDataStore.selectedUnit
    ) { range, latest, unit ->
        Triple(range, latest, unit)
    }.flatMapLatest { (range, latest, unit) ->
        val endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val rangeEntries = range.days?.let { days ->
            val startDate = LocalDate.now().minusDays((days - 1).toLong())
                .format(DateTimeFormatter.ISO_LOCAL_DATE)
            weightRepository.getWeightEntries(startDate, endDate)
        } ?: weightRepository.getAllEntries()

        rangeEntries.combine(
            weightRepository.getAllEntries()
        ) { rangeEntries, allEntries ->
            val weeklyChange = calculateChange(allEntries, 7)
            val monthlyChange = calculateChange(allEntries, 30)

            WeightUiState(
                latestWeight = latest?.weightKg,
                entries = rangeEntries,
                chartRange = range,
                weeklyChange = weeklyChange,
                monthlyChange = monthlyChange,
                unit = unit,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WeightUiState()
    )

    private fun calculateChange(entries: List<WeightEntry>, days: Int): Float? {
        if (entries.size < 2) return null
        val today = LocalDate.now()
        val cutoff = today.minusDays(days.toLong())
        val sorted = entries.sortedWith(compareBy<WeightEntry> { it.date }.thenBy { it.timestamp })
        val recent = sorted.lastOrNull() ?: return null
        val older = sorted.filter {
            LocalDate.parse(it.date, DateTimeFormatter.ISO_LOCAL_DATE) <= cutoff
        }.lastOrNull() ?: return null
        if (recent.date == older.date) return null
        return recent.weightKg - older.weightKg
    }

    fun updateWeightInput(value: String) {
        _weightInput.value = value
    }

    fun updateNoteInput(value: String) {
        _noteInput.value = value
    }

    fun selectChartRange(range: ChartRange) {
        _chartRange.value = range
    }

    fun logWeight() {
        val rawWeight = _weightInput.value.replace(',', '.').toFloatOrNull()
            ?.takeIf { it > 0f } ?: return
        val note = _noteInput.value.ifBlank { null }
        // Inputs are cleared by clearInputs() once the sheet has finished animating out,
        // so the typed value stays visible during the optimistic submit's slide-down.
        viewModelScope.launch {
            val storedWeightKg = if (userPreferencesDataStore.selectedUnit.first() == "lb") {
                rawWeight / POUNDS_PER_KILOGRAM
            } else {
                rawWeight
            }
            weightRepository.insertWeight(
                WeightEntry(
                    date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    weightKg = storedWeightKg,
                    note = note,
                    timestamp = System.currentTimeMillis()
                )
            )
            _events.emit(WeightEvent.WeightLogged)
        }
    }

    fun clearInputs() {
        _weightInput.value = ""
        _noteInput.value = ""
    }

    fun deleteWeight(id: Long) {
        viewModelScope.launch {
            weightRepository.deleteWeight(id)
        }
    }

    private companion object {
        const val POUNDS_PER_KILOGRAM = 2.20462f
    }
}

sealed interface WeightEvent {
    data object WeightLogged : WeightEvent
}
