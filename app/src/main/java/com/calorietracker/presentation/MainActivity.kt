package com.calorietracker.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorietracker.data.local.preferences.UserPreferencesDataStore
import com.calorietracker.presentation.theme.CalSageTheme
import com.calorietracker.presentation.theme.ThemePreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesDataStore: UserPreferencesDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeKey by userPreferencesDataStore.themePreference
                .collectAsStateWithLifecycle(initialValue = "dark")
            val themePreference = ThemePreference.fromKey(themeKey)

            CalSageTheme(themePreference = themePreference) {
                val onboardingCompleted by userPreferencesDataStore
                    .hasCompletedOnboarding
                    .collectAsStateWithLifecycle(initialValue = null)

                when (val completed = onboardingCompleted) {
                    null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    else -> {
                        CalorieTrackerApp(onboardingCompleted = completed)
                    }
                }
            }
        }
    }
}
