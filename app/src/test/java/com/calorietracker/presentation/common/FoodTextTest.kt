package com.calorietracker.presentation.common

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodTextTest {

    @Test
    fun `displayAmountFor strips name from legacy combined amount`() {
        assertEquals(
            "300g",
            displayAmountFor("juneca juha s rezancima i mrkvom", "300g juneca juha s rezancima i mrkvom")
        )
    }

    @Test
    fun `displayAmountFor hides amount equal to name`() {
        assertEquals("", displayAmountFor("malo kruha i zelene salate", "malo kruha i zelene salate"))
    }

    @Test
    fun `displayAmountFor keeps separated amount as is`() {
        assertEquals("200g", displayAmountFor("peceni krumpir", "200g"))
    }

    @Test
    fun `displayAmountFor hides assumed amounts`() {
        assertEquals("", displayAmountFor("eggs", "2 (assumed)"))
    }

    @Test
    fun `displayAmountFor returns empty for blank amount`() {
        assertEquals("", displayAmountFor("bread", "  "))
    }

    @Test
    fun `displayAmountFor returns amount unchanged for blank name`() {
        assertEquals("200g", displayAmountFor("", "200g"))
    }

    @Test
    fun `capitalizedFoodName capitalizes first letter`() {
        assertEquals("Jagode", "jagode".capitalizedFoodName())
    }

    @Test
    fun `capitalizedFoodName capitalizes letter after amount prefix`() {
        assertEquals("200g Peceni krumpir", "200g peceni krumpir".capitalizedFoodName())
    }
}
