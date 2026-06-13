package com.calorietracker.presentation.screen.trends

import com.calorietracker.domain.repository.DailyCalorie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FillUntrackedDaysTest {

    @Test
    fun `fills gaps between tracked days as untracked`() {
        val result = fillUntrackedDays(
            data = listOf(
                DailyCalorie("2026-06-01", 2000),
                DailyCalorie("2026-06-04", 1800)
            ),
            startDate = "2026-06-01",
            endDate = "2026-06-04",
            trimLeading = true
        )

        assertEquals(4, result.size)
        assertEquals(DailyCaloriePoint("2026-06-01", 2000, true), result[0])
        assertEquals(DailyCaloriePoint("2026-06-02", 0, false), result[1])
        assertEquals(DailyCaloriePoint("2026-06-03", 0, false), result[2])
        assertEquals(DailyCaloriePoint("2026-06-04", 1800, true), result[3])
    }

    @Test
    fun `trims to first tracked day when trimLeading is set`() {
        val result = fillUntrackedDays(
            data = listOf(DailyCalorie("2026-06-10", 1500)),
            startDate = "2026-05-14",
            endDate = "2026-06-12",
            trimLeading = true
        )

        assertEquals("2026-06-10", result.first().date)
        assertEquals(3, result.size)
    }

    @Test
    fun `keeps full window from range start when trimLeading is off`() {
        val result = fillUntrackedDays(
            data = listOf(DailyCalorie("2026-06-10", 1500)),
            startDate = "2026-06-08",
            endDate = "2026-06-12",
            trimLeading = false
        )

        assertEquals(
            listOf("2026-06-08", "2026-06-09", "2026-06-10", "2026-06-11", "2026-06-12"),
            result.map { it.date }
        )
        assertTrue(result[2].tracked)
        assertEquals(1, result.count { it.tracked })
    }

    @Test
    fun `fills trailing untracked days up to end date`() {
        val result = fillUntrackedDays(
            data = listOf(DailyCalorie("2026-06-10", 1500)),
            startDate = "2026-06-08",
            endDate = "2026-06-12",
            trimLeading = true
        )

        assertEquals(listOf("2026-06-10", "2026-06-11", "2026-06-12"), result.map { it.date })
        assertTrue(result[0].tracked)
        assertTrue(result.drop(1).none { it.tracked })
    }

    @Test
    fun `returns empty list for no data`() {
        assertEquals(
            emptyList<DailyCaloriePoint>(),
            fillUntrackedDays(emptyList(), "2026-06-01", "2026-06-12", trimLeading = true)
        )
    }
}
