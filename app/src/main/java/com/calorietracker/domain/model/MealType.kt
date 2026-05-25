package com.calorietracker.domain.model

enum class MealType(
    val displayName: String,
    val examplePlaceholder: String
) {
    BREAKFAST(
        "Breakfast",
        "e.g.\n- 200g Greek yogurt\n- 50g granola\n- 100g mixed berries"
    ),
    LUNCH(
        "Lunch",
        "e.g.\n- 100g chicken breast\n- 200g mashed potatoes\n- 100g green salad"
    ),
    SNACK(
        "Snack",
        "e.g.\n- 1 apple\n- 1 tbsp peanut butter\n- 30g almonds"
    ),
    DINNER(
        "Dinner",
        "e.g.\n- 180g grilled salmon\n- 200g roasted potatoes\n- 150g steamed broccoli"
    )
}

fun mealTypeExamplePlaceholder(mealType: String): String {
    return runCatching { MealType.valueOf(mealType.uppercase()) }
        .getOrDefault(MealType.BREAKFAST)
        .examplePlaceholder
}
