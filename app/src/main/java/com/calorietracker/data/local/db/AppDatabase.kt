package com.calorietracker.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.calorietracker.data.local.db.dao.FavoriteMealDao
import com.calorietracker.data.local.db.dao.FoodEntryDao
import com.calorietracker.data.local.db.dao.UserGoalsDao
import com.calorietracker.data.local.db.dao.WeightEntryDao
import com.calorietracker.data.local.db.entity.FavoriteMealEntity
import com.calorietracker.data.local.db.entity.FoodEntryEntity
import com.calorietracker.data.local.db.entity.UserGoalsEntity
import com.calorietracker.data.local.db.entity.WeightEntryEntity

@Database(
    entities = [
        FoodEntryEntity::class,
        WeightEntryEntity::class,
        FavoriteMealEntity::class,
        UserGoalsEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun weightEntryDao(): WeightEntryDao
    abstract fun favoriteMealDao(): FavoriteMealDao
    abstract fun userGoalsDao(): UserGoalsDao
}
