package com.calorietracker.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food_entries",
    indices = [Index(value = ["date"])]
)
data class FoodEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "meal_type")
    val mealType: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "calories")
    val calories: Int,

    @ColumnInfo(name = "protein_grams")
    val proteinGrams: Float,

    @ColumnInfo(name = "carbs_grams")
    val carbsGrams: Float,

    @ColumnInfo(name = "fat_grams")
    val fatGrams: Float,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "source")
    val source: String = "AI"
)
