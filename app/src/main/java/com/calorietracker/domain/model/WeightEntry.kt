package com.calorietracker.domain.model

data class WeightEntry(
    val id: Long = 0,
    val date: String,
    val weightKg: Float,
    val note: String? = null,
    val timestamp: Long
)
