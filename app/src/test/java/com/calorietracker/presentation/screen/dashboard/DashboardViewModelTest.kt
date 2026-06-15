package com.calorietracker.presentation.screen.dashboard

import com.calorietracker.data.local.preferences.UserPreferencesDataStore
import com.calorietracker.domain.model.FoodEntry
import com.calorietracker.domain.model.MealType
import com.calorietracker.domain.model.NutritionInfo
import com.calorietracker.domain.model.UserGoals
import com.calorietracker.domain.repository.DailyCalorie
import com.calorietracker.domain.repository.FoodRepository
import com.calorietracker.domain.repository.GoalsRepository
import com.calorietracker.domain.repository.MealSubtotal
import com.calorietracker.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `stale persisted selected date resets to today on creation`() = runTest {
        val today = LocalDate.now()
        val selectedDates = MutableStateFlow<String?>(
            today.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        )

        createViewModel(fakeUserPreferencesDataStore(selectedDates))
        advanceUntilIdle()

        assertEquals(today.format(DateTimeFormatter.ISO_LOCAL_DATE), selectedDates.value)
    }

    @Test
    fun `current persisted selected date is preserved on creation`() = runTest {
        val selectedDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val selectedDates = MutableStateFlow<String?>(selectedDate)

        createViewModel(fakeUserPreferencesDataStore(selectedDates))
        advanceUntilIdle()

        assertEquals(selectedDate, selectedDates.value)
    }

    @Test
    fun `future persisted selected date is preserved on creation`() = runTest {
        val selectedDate = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val selectedDates = MutableStateFlow<String?>(selectedDate)

        createViewModel(fakeUserPreferencesDataStore(selectedDates))
        advanceUntilIdle()

        assertEquals(selectedDate, selectedDates.value)
    }

    private fun createViewModel(userPreferences: UserPreferencesDataStore): DashboardViewModel {
        return DashboardViewModel(
            foodRepository = EmptyFoodRepository,
            goalsRepository = DefaultGoalsRepository,
            userPreferences = userPreferences
        )
    }

    private fun fakeUserPreferencesDataStore(
        selectedDates: MutableStateFlow<String?>
    ): UserPreferencesDataStore {
        return mockk {
            every { selectedDate } returns selectedDates
            coEvery { setSelectedDate(any()) } coAnswers {
                selectedDates.value = invocation.args[0] as String
            }
        }
    }

    private object EmptyFoodRepository : FoodRepository {
        override fun getDailyTotal(date: String): Flow<NutritionInfo> = flowOf(NutritionInfo())

        override fun getMealSubtotals(date: String): Flow<List<MealSubtotal>> = flowOf(emptyList())

        override fun getEntriesForMeal(date: String, mealType: MealType): Flow<List<FoodEntry>> =
            flowOf(emptyList())

        override fun getCalorieTrend(startDate: String, endDate: String): Flow<List<DailyCalorie>> =
            flowOf(emptyList())

        override fun getAllEntriesInRange(startDate: String, endDate: String): Flow<List<FoodEntry>> =
            flowOf(emptyList())

        override suspend fun insertEntry(entry: FoodEntry) = Unit

        override suspend fun updateEntry(entry: FoodEntry) = Unit

        override suspend fun deleteEntry(id: Long) = Unit
    }

    private object DefaultGoalsRepository : GoalsRepository {
        override fun getGoals(): Flow<UserGoals> = flowOf(UserGoals())

        override suspend fun saveGoals(goals: UserGoals) = Unit
    }
}
