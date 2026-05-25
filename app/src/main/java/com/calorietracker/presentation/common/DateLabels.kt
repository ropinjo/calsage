package com.calorietracker.presentation.common

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val MONTH_DAY = DateTimeFormatter.ofPattern("MMM d")
private val MONTH_DAY_YEAR = DateTimeFormatter.ofPattern("MMM d, yyyy")

fun isToday(isoDate: String): Boolean {
    return runCatching { LocalDate.parse(isoDate) }.getOrNull() == LocalDate.now()
}

fun formatPastDateLabel(isoDate: String): String {
    val date = runCatching { LocalDate.parse(isoDate) }.getOrNull() ?: return isoDate
    val today = LocalDate.now()
    return when {
        date == today.minusDays(1) -> "Yesterday"
        date.year == today.year -> date.format(MONTH_DAY)
        else -> date.format(MONTH_DAY_YEAR)
    }
}
