package com.calorietracker.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_meals")
data class FavoriteMealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "total_calories")
    val totalCalories: Int,

    @ColumnInfo(name = "total_protein")
    val totalProtein: Float,

    @ColumnInfo(name = "total_carbs")
    val totalCarbs: Float,

    @ColumnInfo(name = "total_fat")
    val totalFat: Float,

    @ColumnInfo(name = "items_json")
    val itemsJson: String?,

    @ColumnInfo(name = "meal_type")
    val mealType: String,

    @ColumnInfo(name = "source")
    val source: String = "AI"
)
