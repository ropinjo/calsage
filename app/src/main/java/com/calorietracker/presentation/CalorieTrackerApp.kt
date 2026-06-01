package com.calorietracker.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.calorietracker.presentation.navigation.AppDestination
import com.calorietracker.presentation.navigation.AddFoodDestination
import com.calorietracker.presentation.navigation.BottomNavItem
import com.calorietracker.presentation.navigation.DashboardDestination
import com.calorietracker.presentation.navigation.FavoritesDestination
import com.calorietracker.presentation.navigation.MealDetailDestination
import com.calorietracker.presentation.navigation.OnboardingDestination
import com.calorietracker.presentation.navigation.SettingsDestination
import com.calorietracker.presentation.navigation.TrendsDestination
import com.calorietracker.presentation.navigation.WeightDestination
import com.calorietracker.presentation.screen.addfood.AddFoodScreen
import com.calorietracker.presentation.screen.dashboard.DashboardScreen
import com.calorietracker.presentation.screen.favorites.FavoritesScreen
import com.calorietracker.presentation.screen.mealdetail.MealDetailScreen
import com.calorietracker.presentation.screen.onboarding.OnboardingScreen
import com.calorietracker.presentation.screen.settings.SettingsScreen
import com.calorietracker.presentation.screen.trends.TrendsScreen
import com.calorietracker.presentation.screen.weight.WeightScreen
import com.calorietracker.presentation.theme.MotionDurations
import com.calorietracker.presentation.theme.motionTween
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Composable
fun CalorieTrackerApp(
    onboardingCompleted: Boolean
) {
    val startDestination: AppDestination = if (onboardingCompleted) {
        DashboardDestination
    } else {
        OnboardingDestination
    }
    val backStack = rememberSaveable(
        onboardingCompleted,
        saver = appBackStackSaver
    ) {
        mutableStateListOf(startDestination)
    }

    val currentDestination by remember(backStack) {
        derivedStateOf { backStack.lastOrNull() ?: startDestination }
    }

    val showBottomBar by remember(backStack) {
        derivedStateOf {
            when (currentDestination) {
                is DashboardDestination,
                is TrendsDestination,
                is WeightDestination,
                is SettingsDestination -> true
                else -> false
            }
        }
    }

    val selectedNavItem by remember(backStack) {
        derivedStateOf {
            when (currentDestination) {
                is DashboardDestination -> BottomNavItem.Dashboard
                is TrendsDestination -> BottomNavItem.Trends
                is WeightDestination -> BottomNavItem.Weight
                is SettingsDestination -> BottomNavItem.Settings
                else -> null
            }
        }
    }

    BackHandler(enabled = backStack.size > 1) {
        backStack.removeLastOrNull()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    BottomNavItem.entries.forEach { item ->
                        val isSelected = selectedNavItem == item
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (selectedNavItem != item) {
                                    // Navigate to the target tab
                                    backStack.clear()
                                    backStack.add(DashboardDestination)
                                    if (item != BottomNavItem.Dashboard) {
                                        backStack.add(item.destination)
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            val screenTween = motionTween<Float>(MotionDurations.STANDARD)
            val screenIntTween = motionTween<androidx.compose.ui.unit.IntOffset>(MotionDurations.STANDARD)
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    (fadeIn(animationSpec = screenTween) +
                        slideInHorizontally(animationSpec = screenIntTween) { it / 4 })
                        .togetherWith(
                            fadeOut(animationSpec = screenTween) +
                                slideOutHorizontally(animationSpec = screenIntTween) { -it / 4 }
                        )
                },
                label = "screenTransition"
            ) { destination ->
                when (destination) {
                    is DashboardDestination -> {
                        DashboardScreen(
                            onNavigateToAddFood = { mealType, date ->
                                backStack.add(AddFoodDestination(mealType, date))
                            },
                            onNavigateToMealDetail = { mealType, date ->
                                backStack.add(MealDetailDestination(mealType, date))
                            }
                        )
                    }

                    is TrendsDestination -> {
                        TrendsScreen()
                    }

                    is WeightDestination -> {
                        WeightScreen()
                    }

                    is SettingsDestination -> {
                        SettingsScreen()
                    }

                    is AddFoodDestination -> {
                        AddFoodScreen(
                            mealType = destination.mealType,
                            date = destination.date,
                            favoriteId = destination.favoriteId,
                            viewModelKey = destination.entryKey,
                            onBack = { backStack.removeLastOrNull() },
                            onAfterLog = {
                                backStack.removeLastOrNull()
                            },
                            onNavigateToSettings = {
                                backStack.removeLastOrNull()
                                // Navigate to settings tab
                                backStack.clear()
                                backStack.add(DashboardDestination)
                                backStack.add(SettingsDestination)
                            },
                            onNavigateToFavorites = { mealType ->
                                backStack.add(FavoritesDestination(mealType = mealType, date = destination.date))
                            }
                        )
                    }

                    is MealDetailDestination -> {
                        MealDetailScreen(
                            mealType = destination.mealType,
                            date = destination.date,
                            onNavigateBack = { backStack.removeLastOrNull() },
                            onNavigateToAddFood = { mealType, date ->
                                backStack.add(AddFoodDestination(mealType, date))
                            }
                        )
                    }

                    is FavoritesDestination -> {
                        FavoritesScreen(
                            mealType = destination.mealType,
                            date = destination.date,
                            onNavigateBack = { backStack.removeLastOrNull() },
                            onEditFavorite = { favoriteId, date ->
                                backStack.add(
                                    AddFoodDestination(
                                        mealType = destination.mealType,
                                        date = date,
                                        favoriteId = favoriteId
                                    )
                                )
                            }
                        )
                    }

                    is OnboardingDestination -> {
                        OnboardingScreen(
                            onOnboardingComplete = {
                                backStack.clear()
                                backStack.add(DashboardDestination)
                            }
                        )
                    }
                }
            }
        }
    }
}

private val appBackStackSaver: Saver<androidx.compose.runtime.snapshots.SnapshotStateList<AppDestination>, String> = Saver(
    save = { backStack ->
        Json.encodeToString(ListSerializer(AppDestination.serializer()), backStack.toList())
    },
    restore = { serialized ->
        runCatching {
            Json.decodeFromString(ListSerializer(AppDestination.serializer()), serialized)
        }.getOrElse {
            listOf(DashboardDestination)
        }
            .let { restored -> mutableStateListOf(*restored.toTypedArray()) }
    }
)
