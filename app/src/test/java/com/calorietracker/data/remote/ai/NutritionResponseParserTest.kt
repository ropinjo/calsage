package com.calorietracker.data.remote.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NutritionResponseParserTest {

    @Test
    fun `parse strips think tags and code fences`() {
        val response = """
            <think>Estimate portion sizes before answering.</think>
            ```json
            {
              "calories": 609,
              "protein_g": 52.3,
              "carbs_g": 0.0,
              "fat_g": 49.4,
              "items": [
                {
                  "name": "Chicken wings, with skin (raw)",
                  "amount": "300g",
                  "calories": 609,
                  "protein_g": 52.3,
                  "carbs_g": 0.0,
                  "fat_g": 49.4
                }
              ],
              "error": null
            }
            ```
        """.trimIndent()

        val parsed = NutritionResponseParser.parse(response)

        assertEquals(609, parsed.calories)
        assertEquals(52.3f, parsed.proteinG)
        assertEquals(1, parsed.items.size)
        assertEquals("Chicken wings, with skin (raw)", parsed.items.single().name)
        assertNull(parsed.error)
    }

    @Test
    fun `strip code fences handles text and markdown labels`() {
        assertEquals("New prompt", stripCodeFences("```text\nNew prompt\n```"))
        assertEquals("New prompt", stripCodeFences("```markdown\nNew prompt\n```"))
    }
}
