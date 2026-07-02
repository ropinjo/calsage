package com.calorietracker.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.calorietracker.data.local.db.AppDatabase
import com.calorietracker.data.local.db.dao.FavoriteMealDao
import com.calorietracker.data.local.db.dao.FoodEntryDao
import com.calorietracker.data.local.db.dao.UserGoalsDao
import com.calorietracker.data.local.db.dao.WeightEntryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "calsage_preferences"
)

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "calsage_database"
        )
            // Any database version bump must add a Migration here; user data must survive upgrades.
            .build()
    }

    @Provides
    fun provideFoodEntryDao(database: AppDatabase): FoodEntryDao {
        return database.foodEntryDao()
    }

    @Provides
    fun provideWeightEntryDao(database: AppDatabase): WeightEntryDao {
        return database.weightEntryDao()
    }

    @Provides
    fun provideFavoriteMealDao(database: AppDatabase): FavoriteMealDao {
        return database.favoriteMealDao()
    }

    @Provides
    fun provideUserGoalsDao(database: AppDatabase): UserGoalsDao {
        return database.userGoalsDao()
    }

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.dataStore
    }
}
