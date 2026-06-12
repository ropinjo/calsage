package com.calorietracker.presentation.common

/**
 * Shared display formatting for food item names and amounts.
 *
 * Legacy AI responses and saved favorites may carry the full user phrase in both
 * the name and the amount fields, so display code must dedup them defensively.
 */

fun String.capitalizedFoodName(): String {
    val trimmed = trim()
    val match = Regex("""^(\d+(?:[.,]\d+)?\s*[a-zA-Z%]*\s+)(\p{L})(.*)$""").matchEntire(trimmed)
    if (match != null) {
        return match.groupValues[1] + match.groupValues[2].uppercase() + match.groupValues[3]
    }

    val firstLetter = trimmed.indexOfFirst { it.isLetter() }
    if (firstLetter == -1) return trimmed
    return trimmed.replaceRange(firstLetter, firstLetter + 1, trimmed[firstLetter].titlecase())
}

// AI-assumed amounts are guesses, so they are hidden entirely rather than
// shown with the marker stripped.
fun String.cleanAssumedAmount(): String {
    if (contains("assumed", ignoreCase = true)) return ""
    return replace(Regex("""\s+"""), " ").trim()
}

fun String.removeFoodName(name: String): String {
    return replace(Regex("""\b${Regex.escape(name.trim())}\b""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""\s+"""), " ")
        .trim()
}

/**
 * Amount text to show under a food item title, with the name deduplicated out.
 * Returns "" when there is nothing meaningful to show.
 */
fun displayAmountFor(name: String, amount: String): String {
    val cleanAmount = amount.cleanAssumedAmount()
    if (cleanAmount.isBlank()) return ""

    val trimmedName = name.trim()
    if (trimmedName.isBlank()) return cleanAmount
    if (cleanAmount.equals(trimmedName, ignoreCase = true)) return ""
    if (cleanAmount.contains(trimmedName, ignoreCase = true)) {
        return cleanAmount.removeFoodName(trimmedName)
    }
    return cleanAmount
}
