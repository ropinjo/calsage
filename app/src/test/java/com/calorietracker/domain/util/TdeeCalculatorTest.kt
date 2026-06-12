package com.calorietracker.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TdeeCalculatorTest {

    @Test
    fun `bmr matches Mifflin-St Jeor for males`() {
        // 10*80 + 6.25*180 - 5*30 + 5
        assertEquals(1780f, TdeeCalculator.calculateBmr("male", 80f, 180f, 30), 0.01f)
    }

    @Test
    fun `bmr matches Mifflin-St Jeor for females`() {
        // 10*65 + 6.25*165 - 5*25 - 161
        assertEquals(1395.25f, TdeeCalculator.calculateBmr("female", 65f, 165f, 25), 0.01f)
    }

    @Test
    fun `bmr uses male-female average offset for other sexes`() {
        // 10*70 + 6.25*170 - 5*40 - 78
        assertEquals(1484.5f, TdeeCalculator.calculateBmr("other", 70f, 170f, 40), 0.01f)
    }

    @Test
    fun `activity multipliers match standard values`() {
        assertEquals(1.2f, TdeeCalculator.getActivityMultiplier("sedentary"), 0f)
        assertEquals(1.375f, TdeeCalculator.getActivityMultiplier("lightly_active"), 0f)
        assertEquals(1.55f, TdeeCalculator.getActivityMultiplier("moderately_active"), 0f)
        assertEquals(1.725f, TdeeCalculator.getActivityMultiplier("very_active"), 0f)
        assertEquals(1.9f, TdeeCalculator.getActivityMultiplier("extra_active"), 0f)
        assertEquals(1.2f, TdeeCalculator.getActivityMultiplier("unknown"), 0f)
    }

    @Test
    fun `tdee is bmr scaled by activity multiplier`() {
        assertEquals(2759f, TdeeCalculator.calculateTdee(1780f, "moderately_active"), 0.01f)
    }

    @Test
    fun `goal adjustments are plus or minus 500 calories`() {
        assertEquals(-500, TdeeCalculator.getGoalAdjustment("lose"))
        assertEquals(0, TdeeCalculator.getGoalAdjustment("maintain"))
        assertEquals(500, TdeeCalculator.getGoalAdjustment("gain"))
        assertEquals(0, TdeeCalculator.getGoalAdjustment("unknown"))
    }
}
