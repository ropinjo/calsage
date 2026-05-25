package com.calorietracker.data.remote.ai.venice

import java.io.IOException

class VeniceRateLimitException(
    val secondsUntilReset: Int?,
    message: String
) : IOException(message)
