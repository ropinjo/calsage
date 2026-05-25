package com.calorietracker.data.remote.ai.venice

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the mutable API key for the Venice AI service.
 * Updated at runtime when the user saves a key.
 */
@Singleton
class ApiKeyHolder @Inject constructor() {
    @Volatile
    var apiKey: String = ""
}

/**
 * OkHttp interceptor that adds the Venice AI Bearer token header.
 */
@Singleton
class VeniceAuthInterceptor @Inject constructor(
    private val apiKeyHolder: ApiKeyHolder
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = if (apiKeyHolder.apiKey.isNotBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${apiKeyHolder.apiKey}")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
