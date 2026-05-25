package com.calorietracker.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class VeniceApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OpenFoodFactsApi
