package com.calorietracker.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class VeniceApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OpenFoodFactsApi

/** Application-lifetime [kotlinx.coroutines.CoroutineScope] for fire-and-forget work
 *  that must outlive the call that triggered it (e.g. cost tracking after a result is returned). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
