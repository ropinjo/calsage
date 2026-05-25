package com.calorietracker.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import com.calorietracker.presentation.common.icons.CustomIcons
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val destination: AppDestination
) {
    Dashboard(
        label = "Dashboard",
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.Filled.Home,
        destination = DashboardDestination
    ),
    Trends(
        label = "Trends",
        icon = Icons.Outlined.BarChart,
        selectedIcon = Icons.Filled.BarChart,
        destination = TrendsDestination
    ),
    Weight(
        label = "Weight",
        icon = CustomIcons.ScaleOutlined,
        selectedIcon = CustomIcons.ScaleFilled,
        destination = WeightDestination
    ),
    Settings(
        label = "Settings",
        icon = Icons.Outlined.Settings,
        selectedIcon = Icons.Filled.Settings,
        destination = SettingsDestination
    )
}
