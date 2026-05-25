package com.calorietracker.di

import com.calorietracker.data.connectivity.AndroidConnectivityObserver
import com.calorietracker.data.connectivity.ConnectivityObserver
import com.calorietracker.data.repository.AiRepositoryImpl
import com.calorietracker.data.repository.FavoriteRepositoryImpl
import com.calorietracker.data.repository.FoodRepositoryImpl
import com.calorietracker.data.repository.GoalsRepositoryImpl
import com.calorietracker.data.repository.OpenFoodFactsBarcodeRepository
import com.calorietracker.data.repository.WeightRepositoryImpl
import com.calorietracker.domain.repository.AiRepository
import com.calorietracker.domain.repository.BarcodeRepository
import com.calorietracker.domain.repository.FavoriteRepository
import com.calorietracker.domain.repository.FoodRepository
import com.calorietracker.domain.repository.GoalsRepository
import com.calorietracker.domain.repository.WeightRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFoodRepository(impl: FoodRepositoryImpl): FoodRepository

    @Binds
    @Singleton
    abstract fun bindWeightRepository(impl: WeightRepositoryImpl): WeightRepository

    @Binds
    @Singleton
    abstract fun bindGoalsRepository(impl: GoalsRepositoryImpl): GoalsRepository

    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository

    @Binds
    @Singleton
    abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository

    @Binds
    @Singleton
    abstract fun bindBarcodeRepository(impl: OpenFoodFactsBarcodeRepository): BarcodeRepository

    @Binds
    @Singleton
    abstract fun bindConnectivityObserver(impl: AndroidConnectivityObserver): ConnectivityObserver
}
