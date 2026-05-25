package com.calorietracker.di

import com.calorietracker.BuildConfig
import com.calorietracker.data.remote.ai.VENICE_BASE_URL
import com.calorietracker.data.remote.ai.venice.VeniceAuthInterceptor
import com.calorietracker.data.remote.ai.venice.VeniceApiService
import com.calorietracker.data.remote.barcode.OpenFoodFactsApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            explicitNulls = false
        }
    }

    @Provides
    @Singleton
    @VeniceApi
    fun provideOkHttpClient(
        veniceAuthInterceptor: VeniceAuthInterceptor
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(veniceAuthInterceptor)
            .addInterceptor(veniceRetryInterceptor())
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @VeniceApi
    fun provideRetrofit(
        @VeniceApi okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(VENICE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideVeniceApiService(@VeniceApi retrofit: Retrofit): VeniceApiService {
        return retrofit.create(VeniceApiService::class.java)
    }

    @Provides
    @Singleton
    @OpenFoodFactsApi
    fun provideOpenFoodFactsOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "CalSage/1.0 (Android)")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    @OpenFoodFactsApi
    fun provideOpenFoodFactsRetrofit(
        @OpenFoodFactsApi okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenFoodFactsApiService(
        @OpenFoodFactsApi retrofit: Retrofit
    ): OpenFoodFactsApiService {
        return retrofit.create(OpenFoodFactsApiService::class.java)
    }

    private fun veniceRetryInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            var attempt = 1
            var nextDelayMs = 500L
            var retryDelayBudgetMs = 9_500L

            while (true) {
                try {
                    val response = chain.proceed(request)
                    if (attempt >= 3 || !response.code.isRetryableStatus()) {
                        return@Interceptor response
                    }

                    response.close()
                    retryDelayBudgetMs -= sleepBeforeRetry(response.header("retry-after"), nextDelayMs, retryDelayBudgetMs)
                } catch (e: IOException) {
                    if (e is SocketTimeoutException || attempt >= 3 || request.body?.isOneShot() == true) {
                        throw e
                    }
                    retryDelayBudgetMs -= sleepBeforeRetry(
                        retryAfter = null,
                        fallbackDelayMs = nextDelayMs,
                        remainingBudgetMs = retryDelayBudgetMs
                    )
                }

                attempt += 1
                nextDelayMs = min(nextDelayMs * 2, 8_000L)
            }
            throw IOException("Venice retry failed")
        }
    }

    private fun Int.isRetryableStatus(): Boolean {
        return this == 429 || this == 500 || this == 503
    }

    private fun sleepBeforeRetry(retryAfter: String?, fallbackDelayMs: Long, remainingBudgetMs: Long): Long {
        val retryAfterMs = retryAfter
            ?.toLongOrNull()
            ?.let { min(it * 1_000L, 8_000L) }
        val jitteredFallbackMs = fallbackDelayMs + Random.nextLong(0L, fallbackDelayMs / 2L + 1L)
        val delayMs = min(min(max(retryAfterMs ?: 0L, jitteredFallbackMs), 8_000L), remainingBudgetMs)
        Thread.sleep(delayMs)
        return delayMs
    }
}
