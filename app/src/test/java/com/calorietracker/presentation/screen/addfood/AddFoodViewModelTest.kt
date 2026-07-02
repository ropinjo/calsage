package com.calorietracker.presentation.screen.addfood

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import com.calorietracker.domain.model.FavoriteMeal
import com.calorietracker.domain.model.FoodEntry
import com.calorietracker.domain.model.FoodSource
import com.calorietracker.domain.model.MealType
import com.calorietracker.domain.model.NutritionInfo
import com.calorietracker.domain.model.NutritionItem
import com.calorietracker.domain.model.NutritionPer100g
import com.calorietracker.domain.repository.AiModel
import com.calorietracker.domain.repository.AiRepository
import com.calorietracker.domain.repository.BarcodeNutritionPer100g
import com.calorietracker.domain.repository.BarcodeLookupResult
import com.calorietracker.domain.repository.BarcodeRepository
import com.calorietracker.domain.repository.DailyCalorie
import com.calorietracker.domain.repository.FavoriteRepository
import com.calorietracker.domain.repository.FoodRepository
import com.calorietracker.domain.repository.MealSubtotal
import com.calorietracker.testutil.MainDispatcherRule
import com.calorietracker.data.local.preferences.UserPreferencesDataStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddFoodViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `scan handlers toggle scanner visibility`() = runTest {
        val viewModel = createViewModel()

        viewModel.onScanClicked()
        assertTrue(viewModel.barcodeUiState.value.isScannerVisible)

        viewModel.onScannerDismissed()
        assertFalse(viewModel.barcodeUiState.value.isScannerVisible)
    }

    @Test
    fun `description adds bullet when newline is inserted above an existing row`() = runTest {
        val viewModel = createViewModel()
        val previous = "- kobasica\n- 4 jaja\n- jagode"
        val insertionIndex = previous.indexOf("- jagode")

        viewModel.updateDescription(TextFieldValue(previous, selection = TextRange(insertionIndex)))
        viewModel.updateDescription(
            TextFieldValue(
                text = previous.replaceRange(insertionIndex, insertionIndex, "\n"),
                selection = TextRange(insertionIndex + 1)
            )
        )

        val description = viewModel.foodDescription.value
        assertEquals("- kobasica\n- 4 jaja\n- \n- jagode", description.text)
        assertEquals(TextRange(insertionIndex + 2), description.selection)
    }

    @Test
    fun `successful barcode flow shows dialog, preserves text, and reviews scanned entry`() = runTest {
        val barcodeRepository = FakeBarcodeRepository(
            result = BarcodeLookupResult.Found(
                productName = "Activia Yogurt",
                nutritionPer100g = BarcodeNutritionPer100g(
                    calories = 92f,
                    proteinGrams = 3.8f,
                    carbsGrams = 12.5f,
                    fatGrams = 3.1f
                ),
                servingSize = "1 serving (125g)"
            )
        )
        val foodRepository = FakeFoodRepository()
        val favoriteRepository = FakeFavoriteRepository()
        val viewModel = createViewModel(
            barcodeRepository = barcodeRepository,
            foodRepository = foodRepository,
            favoriteRepository = favoriteRepository
        )

        viewModel.updateDescription(TextFieldValue("2 eggs and toast"))
        viewModel.onBarcodeDetected("1234567890123")
        advanceUntilIdle()

        val barcodeStateAfterLookup = viewModel.barcodeUiState.value
        assertTrue(barcodeStateAfterLookup.showServingDialog)
        assertEquals("Activia Yogurt", barcodeStateAfterLookup.scannedProductName)
        assertEquals("125", barcodeStateAfterLookup.servingGrams)
        assertEquals("- 2 eggs and toast", viewModel.foodDescription.value.text)
        assertTrue(viewModel.uiState.value is AddFoodUiState.Idle)

        viewModel.onServingConfirmed()
        advanceUntilIdle()

        val result = viewModel.uiState.value as AddFoodUiState.Result
        assertEquals(FoodSource.SCANNED, result.source)
        assertEquals("125g Activia Yogurt", result.description)
        assertEquals("Activia Yogurt", result.favoriteName)
        assertEquals(115, result.totalCalories)
        assertEquals("Activia Yogurt", result.items.single().name)
        assertEquals("125g", result.items.single().amount)
        assertEquals("- 2 eggs and toast", viewModel.foodDescription.value.text)
        assertFalse(viewModel.barcodeUiState.value.showServingDialog)
        assertTrue(foodRepository.insertedEntries.isEmpty())

        viewModel.toggleFavorite()
        viewModel.logFood()
        advanceUntilIdle()

        val entry = foodRepository.insertedEntries.single()
        assertEquals("2026-04-09", entry.date)
        assertEquals(MealType.BREAKFAST, entry.mealType)
        assertEquals("125g Activia Yogurt", entry.description)
        assertEquals(115, entry.nutritionInfo.calories)
        assertEquals(4.75f, entry.nutritionInfo.proteinGrams, 0.001f)
        assertEquals(15.625f, entry.nutritionInfo.carbsGrams, 0.001f)
        assertEquals(3.875f, entry.nutritionInfo.fatGrams, 0.001f)
        assertEquals(FoodSource.SCANNED, entry.source)

        val favorite = favoriteRepository.insertedMeals.single()
        assertEquals("Activia Yogurt", favorite.name)
        assertEquals("125g Activia Yogurt", favorite.description)
        assertEquals(FoodSource.SCANNED, favorite.source)
    }

    @Test
    fun `scanned review uses existing favorite name when product was already saved`() = runTest {
        val existingFavorite = FavoriteMeal(
            id = 7,
            name = "Daily yogurt",
            description = "125g Activia Yogurt",
            totalCalories = 115,
            totalProtein = 4.75f,
            totalCarbs = 15.625f,
            totalFat = 3.875f,
            mealType = MealType.BREAKFAST,
            source = FoodSource.SCANNED
        )
        val barcodeRepository = FakeBarcodeRepository(
            result = BarcodeLookupResult.Found(
                productName = "Activia Yogurt",
                nutritionPer100g = BarcodeNutritionPer100g(
                    calories = 92f,
                    proteinGrams = 3.8f,
                    carbsGrams = 12.5f,
                    fatGrams = 3.1f
                ),
                servingSize = "125g"
            )
        )
        val favoriteRepository = FakeFavoriteRepository(existingMeal = existingFavorite)
        val viewModel = createViewModel(
            barcodeRepository = barcodeRepository,
            favoriteRepository = favoriteRepository
        )

        viewModel.onBarcodeDetected("1234567890123")
        advanceUntilIdle()
        viewModel.onServingConfirmed()
        advanceUntilIdle()

        val result = viewModel.uiState.value as AddFoodUiState.Result
        assertTrue(result.isAlreadyFavorite)
        assertEquals("Daily yogurt", result.favoriteName)

        viewModel.logFood()
        advanceUntilIdle()

        val updatedFavorite = favoriteRepository.updatedMeals.single()
        assertEquals("Daily yogurt", updatedFavorite.name)
        assertEquals(FoodSource.SCANNED, updatedFavorite.source)
    }

    @Test
    fun `barcode serving prefill handles multiplicative serving sizes`() = runTest {
        val viewModel = createViewModel(
            barcodeRepository = FakeBarcodeRepository(
                result = BarcodeLookupResult.Found(
                    productName = "Snack Pack",
                    nutritionPer100g = BarcodeNutritionPer100g(
                        calories = 120f,
                        proteinGrams = 2f,
                        carbsGrams = 20f,
                        fatGrams = 4f
                    ),
                    servingSize = "2 x 25 g"
                )
            )
        )

        viewModel.onBarcodeDetected("123")
        advanceUntilIdle()

        assertEquals("50", viewModel.barcodeUiState.value.servingGrams)
    }

    @Test
    fun `barcode serving prefill handles multiplication symbol`() = runTest {
        val viewModel = createViewModel(
            barcodeRepository = FakeBarcodeRepository(
                result = BarcodeLookupResult.Found(
                    productName = "Snack Pack",
                    nutritionPer100g = BarcodeNutritionPer100g(
                        calories = 120f,
                        proteinGrams = 2f,
                        carbsGrams = 20f,
                        fatGrams = 4f
                    ),
                    servingSize = "2×25g"
                )
            )
        )

        viewModel.onBarcodeDetected("123")
        advanceUntilIdle()

        assertEquals("50", viewModel.barcodeUiState.value.servingGrams)
    }

    @Test
    fun `barcode serving scales float per 100g calories before rounding`() = runTest {
        val viewModel = createViewModel(
            barcodeRepository = FakeBarcodeRepository(
                result = BarcodeLookupResult.Found(
                    productName = "Half Portion",
                    nutritionPer100g = BarcodeNutritionPer100g(
                        calories = 92.5f,
                        proteinGrams = 1f,
                        carbsGrams = 1f,
                        fatGrams = 1f
                    ),
                    servingSize = "50g"
                )
            )
        )

        viewModel.onBarcodeDetected("123")
        advanceUntilIdle()
        viewModel.onServingConfirmed()
        advanceUntilIdle()

        val result = viewModel.uiState.value as AddFoodUiState.Result
        assertEquals(46, result.totalCalories)
    }

    @Test
    fun `editing favorite updates favorite without logging meal for every meal type`() = runTest {
        MealType.entries.forEach { mealType ->
            val existingFavorite = FavoriteMeal(
                id = 7,
                name = "Chicken, potatoes",
                description = "200g chicken\n100g potatoes",
                totalCalories = 366,
                totalProtein = 46f,
                totalCarbs = 17f,
                totalFat = 12f,
                items = listOf(
                    NutritionItem(
                        name = "chicken",
                        amount = "200g chicken",
                        calories = 330,
                        proteinGrams = 44f,
                        carbsGrams = 0f,
                        fatGrams = 12f,
                        grams = 200f,
                        per100g = NutritionPer100g(
                            calories = 165f,
                            proteinGrams = 22f,
                            carbsGrams = 0f,
                            fatGrams = 6f
                        ),
                        caloriesRecomputed = true
                    ),
                    NutritionItem("potatoes", "100g potatoes", 36, 2f, 17f, 0f)
                ),
                mealType = mealType
            )
            val foodRepository = FakeFoodRepository()
            val favoriteRepository = FakeFavoriteRepository(existingMeal = existingFavorite)
            val viewModel = createViewModel(
                foodRepository = foodRepository,
                favoriteRepository = favoriteRepository
            )

            viewModel.updateRouteArgs(mealType = mealType.name, date = "2026-04-09", favoriteId = 7)
            advanceUntilIdle()
            viewModel.logFood()
            advanceUntilIdle()

            assertTrue(foodRepository.insertedEntries.isEmpty())
            val updatedFavorite = favoriteRepository.updatedMeals.single()
            assertEquals(7, updatedFavorite.id)
            assertEquals("Chicken, potatoes", updatedFavorite.name)
            assertEquals(mealType, updatedFavorite.mealType)
            assertEquals(200f, updatedFavorite.items.first().grams!!, 0.001f)
            assertEquals(165f, updatedFavorite.items.first().per100g!!.calories, 0.001f)
            assertEquals(true, updatedFavorite.items.first().caloriesRecomputed)
        }
    }

    @Test
    fun `reanalyzing edited favorite updates favorite without logging meal`() = runTest {
        val existingFavorite = FavoriteMeal(
            id = 7,
            name = "Chicken",
            description = "200g chicken",
            totalCalories = 330,
            totalProtein = 44f,
            totalCarbs = 0f,
            totalFat = 12f,
            items = listOf(
                NutritionItem("chicken", "200g", 330, 44f, 0f, 12f)
            ),
            mealType = MealType.BREAKFAST
        )
        val foodRepository = FakeFoodRepository()
        val favoriteRepository = FakeFavoriteRepository(existingMeal = existingFavorite)
        val viewModel = createViewModel(
            aiRepository = FakeAiRepository(
                result = NutritionInfo(
                    calories = 248,
                    proteinGrams = 31f,
                    carbsGrams = 0f,
                    fatGrams = 8f,
                    items = listOf(
                        NutritionItem("turkey", "150g", 248, 31f, 0f, 8f)
                    )
                )
            ),
            foodRepository = foodRepository,
            favoriteRepository = favoriteRepository
        )

        viewModel.updateRouteArgs(mealType = "BREAKFAST", date = "2026-04-09", favoriteId = 7)
        advanceUntilIdle()
        viewModel.updateDescription(TextFieldValue("150g turkey"))
        viewModel.reAnalyze()
        advanceUntilIdle()
        viewModel.logFood()
        advanceUntilIdle()

        assertTrue(foodRepository.insertedEntries.isEmpty())
        val updatedFavorite = favoriteRepository.updatedMeals.single()
        assertEquals(7, updatedFavorite.id)
        assertEquals("Chicken", updatedFavorite.name)
        assertEquals("150g turkey", updatedFavorite.description)
    }

    @Test
    fun `ai result with empty items shows error instead of saveable result`() = runTest {
        val viewModel = createViewModel(
            aiRepository = FakeAiRepository(
                result = NutritionInfo(
                    calories = 500,
                    proteinGrams = 20f,
                    carbsGrams = 50f,
                    fatGrams = 10f,
                    items = emptyList()
                )
            )
        )

        viewModel.updateDescription(TextFieldValue("mystery meal"))
        viewModel.analyzeFood()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AddFoodUiState.Error)
        assertEquals("AI couldn't identify any food items", (state as AddFoodUiState.Error).message)
    }

    @Test
    fun `log food uses corrected name when amount contains typo duplicate`() = runTest {
        val foodRepository = FakeFoodRepository()
        val viewModel = createViewModel(
            aiRepository = FakeAiRepository(
                result = NutritionInfo(
                    calories = 31,
                    proteinGrams = 1f,
                    carbsGrams = 2f,
                    fatGrams = 1f,
                    items = listOf(
                        NutritionItem(
                            name = "Yoghurt",
                            amount = "50g youghurt",
                            calories = 31,
                            proteinGrams = 1f,
                            carbsGrams = 2f,
                            fatGrams = 1f
                        )
                    )
                )
            ),
            foodRepository = foodRepository
        )

        viewModel.updateDescription(TextFieldValue("50g youghurt"))
        viewModel.analyzeFood()
        advanceUntilIdle()
        viewModel.logFood()
        advanceUntilIdle()

        assertEquals("50g Yoghurt", foodRepository.insertedEntries.single().description)
    }

    @Test
    fun `log food capitalizes food name after amount`() = runTest {
        val foodRepository = FakeFoodRepository()
        val viewModel = createViewModel(
            aiRepository = FakeAiRepository(
                result = NutritionInfo(
                    calories = 265,
                    proteinGrams = 9f,
                    carbsGrams = 49f,
                    fatGrams = 3f,
                    items = listOf(
                        NutritionItem(
                            name = "bread",
                            amount = "100g bread",
                            calories = 265,
                            proteinGrams = 9f,
                            carbsGrams = 49f,
                            fatGrams = 3f
                        )
                    )
                )
            ),
            foodRepository = foodRepository
        )

        viewModel.updateDescription(TextFieldValue("100g bread"))
        viewModel.analyzeFood()
        advanceUntilIdle()
        viewModel.logFood()
        advanceUntilIdle()

        assertEquals("100g Bread", foodRepository.insertedEntries.single().description)
    }

    @Test
    fun `log food places assumed marker at end when amount and name are separate`() = runTest {
        val foodRepository = FakeFoodRepository()
        val viewModel = createViewModel(
            aiRepository = FakeAiRepository(
                result = NutritionInfo(
                    calories = 156,
                    proteinGrams = 12f,
                    carbsGrams = 1f,
                    fatGrams = 10f,
                    items = listOf(
                        NutritionItem(
                            name = "eggs",
                            amount = "2 (assumed)",
                            calories = 156,
                            proteinGrams = 12f,
                            carbsGrams = 1f,
                            fatGrams = 10f
                        )
                    )
                )
            ),
            foodRepository = foodRepository
        )

        viewModel.updateDescription(TextFieldValue("eggs"))
        viewModel.analyzeFood()
        advanceUntilIdle()
        viewModel.logFood()
        advanceUntilIdle()

        assertEquals("2 Eggs (assumed)", foodRepository.insertedEntries.single().description)
    }

    @Test
    fun `save as favorite defaults to capitalized item summary name`() = runTest {
        val favoriteRepository = FakeFavoriteRepository()
        val viewModel = createViewModel(
            aiRepository = FakeAiRepository(
                result = NutritionInfo(
                    calories = 575,
                    proteinGrams = 32f,
                    carbsGrams = 60f,
                    fatGrams = 22f,
                    items = listOf(
                        NutritionItem("argeta cajna pasteta", "1 can (assumed)", 155, 7f, 1f, 13f),
                        NutritionItem("lepinja", "1 piece (assumed)", 300, 9f, 55f, 5f),
                        NutritionItem("kikiriki", "20g kikiriki", 120, 1f, 0f, 3f)
                    )
                )
            ),
            favoriteRepository = favoriteRepository
        )

        viewModel.updateDescription(TextFieldValue("- argeta cajna pasteta\n- lepinja\n- kikiriki"))
        viewModel.analyzeFood()
        advanceUntilIdle()
        viewModel.toggleFavorite()
        viewModel.logFood()
        advanceUntilIdle()

        assertEquals("Argeta cajna pasteta, lepinja, kikiriki", favoriteRepository.insertedMeals.single().name)
    }

    private fun createViewModel(
        aiRepository: AiRepository = FakeAiRepository(),
        barcodeRepository: BarcodeRepository = FakeBarcodeRepository(),
        foodRepository: FakeFoodRepository = FakeFoodRepository(),
        favoriteRepository: FavoriteRepository = FakeFavoriteRepository()
    ): AddFoodViewModel {
        return AddFoodViewModel(
            aiRepository = aiRepository,
            barcodeRepository = barcodeRepository,
            foodRepository = foodRepository,
            favoriteRepository = favoriteRepository,
            connectivityObserver = AlwaysOnlineObserver,
            userPreferencesDataStore = fakeUserPreferencesDataStore(),
            savedStateHandle = SavedStateHandle(
                mapOf(
                    "mealType" to "BREAKFAST",
                    "date" to "2026-04-09"
                )
            )
        )
    }

    private fun fakeUserPreferencesDataStore(): UserPreferencesDataStore {
        return mockk {
            every { thinkingEnabled } returns flowOf(false)
        }
    }

    private object AlwaysOnlineObserver : com.calorietracker.data.connectivity.ConnectivityObserver {
        override fun isOnline(): Boolean = true
        override fun observe(): Flow<Boolean> = flowOf(true)
    }
}

private class FakeBarcodeRepository(
    private val result: BarcodeLookupResult = BarcodeLookupResult.NotFound
) : BarcodeRepository {

    override suspend fun getProduct(barcode: String): BarcodeLookupResult = result
}

private class FakeAiRepository(
    private val result: NutritionInfo = NutritionInfo()
) : AiRepository {
    override suspend fun analyzeFood(description: String): Result<NutritionInfo> {
        return Result.success(result)
    }

    override suspend fun validateApiKey(apiKey: String): Result<Boolean> {
        return Result.success(true)
    }

    override suspend fun fetchModels(): Result<List<AiModel>> {
        return Result.success(emptyList())
    }

    override suspend fun improvePrompt(currentPrompt: String): Result<String> {
        return Result.success(currentPrompt)
    }
}

private class FakeFoodRepository : FoodRepository {
    val insertedEntries = mutableListOf<FoodEntry>()

    override fun getDailyTotal(date: String): Flow<NutritionInfo> = flowOf(NutritionInfo())

    override fun getMealSubtotals(date: String): Flow<List<MealSubtotal>> = flowOf(emptyList())

    override fun getEntriesForMeal(date: String, mealType: MealType): Flow<List<FoodEntry>> = flowOf(emptyList())

    override fun getCalorieTrend(startDate: String, endDate: String): Flow<List<DailyCalorie>> = flowOf(emptyList())

    override fun getAllEntriesInRange(startDate: String, endDate: String): Flow<List<FoodEntry>> = flowOf(emptyList())

    override suspend fun insertEntry(entry: FoodEntry) {
        insertedEntries += entry
    }

    override suspend fun updateEntry(entry: FoodEntry) = Unit

    override suspend fun deleteEntry(id: Long) = Unit
}

private class FakeFavoriteRepository(
    private val existingMeal: FavoriteMeal? = null
) : FavoriteRepository {
    val insertedMeals = mutableListOf<FavoriteMeal>()
    val updatedMeals = mutableListOf<FavoriteMeal>()

    override fun getByMealType(mealType: MealType): Flow<List<FavoriteMeal>> =
        flowOf(emptyList())

    override suspend fun getById(id: Long): FavoriteMeal? =
        existingMeal?.takeIf { it.id == id }

    override suspend fun findByDescriptionAndMealType(
        description: String,
        mealType: MealType
    ): FavoriteMeal? = existingMeal?.takeIf {
        it.description == description && it.mealType == mealType
    }

    override fun searchByMealType(query: String, mealType: MealType): Flow<List<FavoriteMeal>> =
        flowOf(emptyList())

    override suspend fun insert(meal: FavoriteMeal) {
        insertedMeals += meal
    }

    override suspend fun update(meal: FavoriteMeal) {
        updatedMeals += meal
    }

    override suspend fun delete(id: Long) = Unit
}
