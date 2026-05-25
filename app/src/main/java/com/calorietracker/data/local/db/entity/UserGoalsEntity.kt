package com.calorietracker.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_goals")
data class UserGoalsEntity(
    @PrimaryKey
    val id: Int = 1,

    @ColumnInfo(name = "calorie_target", defaultValue = "2000")
    val calorieTarget: Int = 2000,

    @ColumnInfo(name = "protein_target_grams", defaultValue = "150")
    val proteinTargetGrams: Float = 150f,

    @ColumnInfo(name = "carbs_target_grams", defaultValue = "250")
    val carbsTargetGrams: Float = 250f,

    @ColumnInfo(name = "fat_target_grams", defaultValue = "65")
    val fatTargetGrams: Float = 65f,

    @ColumnInfo(name = "target_weight_kg")
    val targetWeightKg: Float? = null
)
