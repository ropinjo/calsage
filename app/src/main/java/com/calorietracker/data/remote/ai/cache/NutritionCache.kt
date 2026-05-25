package com.calorietracker.data.remote.ai.cache

import com.calorietracker.domain.model.NutritionInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NutritionCache @Inject constructor() {

    private val maxSize = 100

    private val cache = object : LinkedHashMap<String, NutritionInfo>(
        maxSize,
        0.75f,
        true // accessOrder = true for LRU behavior
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, NutritionInfo>?): Boolean {
            return size > maxSize
        }
    }

    private fun normalizeKey(description: String): String {
        return description.lowercase().trim()
    }

    fun get(description: String): NutritionInfo? {
        return synchronized(cache) {
            cache[normalizeKey(description)]
        }
    }

    fun put(description: String, result: NutritionInfo) {
        synchronized(cache) {
            cache[normalizeKey(description)] = result
        }
    }

    fun clear() {
        synchronized(cache) {
            cache.clear()
        }
    }
}
