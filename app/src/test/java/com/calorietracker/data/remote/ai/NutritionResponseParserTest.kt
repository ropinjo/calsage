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

    @Test
    fun `toDomain computes item arithmetic from per 100g data`() {
        val parsed = NutritionResponseParser.parse(
            """
            {
              "calories": 999,
              "protein_g": 999.0,
              "carbs_g": 999.0,
              "fat_g": 999.0,
              "items": [
                {
                  "name": "rice",
                  "amount": "150g",
                  "calories": 1,
                  "protein_g": 1.0,
                  "carbs_g": 1.0,
                  "fat_g": 1.0,
                  "grams": 150.0,
                  "per_100g": {
                    "calories": 130.0,
                    "protein_g": 2.7,
                    "carbs_g": 28.0,
                    "fat_g": 0.3
                  }
                }
              ],
              "error": null
            }
            """.trimIndent()
        ).toDomain()

        val item = parsed.items.single()
        assertEquals(195, item.calories)
        assertEquals(4.05f, item.proteinGrams, 0.001f)
        assertEquals(42f, item.carbsGrams, 0.001f)
        assertEquals(0.45f, item.fatGrams, 0.001f)
        assertEquals(195, parsed.calories)
    }

    @Test
    fun `toDomain falls back to item values when per 100g data is absent`() {
        val parsed = NutritionResponseParser.parse(
            """
            {
              "calories": 10,
              "protein_g": 1.0,
              "carbs_g": 2.0,
              "fat_g": 3.0,
              "items": [
                {
                  "name": "soup",
                  "amount": "1 bowl",
                  "calories": 120,
                  "protein_g": 5.0,
                  "carbs_g": 15.0,
                  "fat_g": 4.0
                }
              ],
              "error": null
            }
            """.trimIndent()
        ).toDomain()

        assertEquals(120, parsed.items.single().calories)
        assertEquals(120, parsed.calories)
    }

    @Test
    fun `toDomain recomputes totals from multiple items`() {
        val parsed = NutritionResponseParser.parse(
            """
            {
              "calories": 1,
              "protein_g": 1.0,
              "carbs_g": 1.0,
              "fat_g": 1.0,
              "items": [
                {
                  "name": "bread",
                  "amount": "100g",
                  "calories": 270,
                  "protein_g": 9.0,
                  "carbs_g": 50.0,
                  "fat_g": 3.0
                },
                {
                  "name": "cheese",
                  "amount": "50g",
                  "calories": 200,
                  "protein_g": 13.0,
                  "carbs_g": 0.5,
                  "fat_g": 16.5
                }
              ],
              "error": null
            }
            """.trimIndent()
        ).toDomain()

        assertEquals(470, parsed.calories)
        assertEquals(22f, parsed.proteinGrams, 0.001f)
        assertEquals(50.5f, parsed.carbsGrams, 0.001f)
        assertEquals(19.5f, parsed.fatGrams, 0.001f)
    }

    @Test
    fun `toDomain keeps item calories when macros do not explain all calories`() {
        val parsed = NutritionResponseParser.parse(
            """
            {
              "calories": 100,
              "protein_g": 0.0,
              "carbs_g": 0.0,
              "fat_g": 0.0,
              "items": [
                {
                  "name": "bad item",
                  "amount": "100g",
                  "calories": 100,
                  "protein_g": 10.0,
                  "carbs_g": 10.0,
                  "fat_g": 10.0
                }
              ],
              "error": null
            }
            """.trimIndent()
        ).toDomain()

        val item = parsed.items.single()
        assertEquals(100, item.calories)
        assertEquals(false, item.caloriesRecomputed)
    }

    @Test
    fun `toDomain scales per 100g calories without Atwater rewrite`() {
        val parsed = NutritionResponseParser.parse(
            """
            {
              "calories": 100,
              "protein_g": 0.0,
              "carbs_g": 0.0,
              "fat_g": 0.0,
              "items": [
                {
                  "name": "bad density",
                  "amount": "50g",
                  "calories": 50,
                  "protein_g": 5.0,
                  "carbs_g": 5.0,
                  "fat_g": 5.0,
                  "grams": 50.0,
                  "per_100g": {
                    "calories": 100.0,
                    "protein_g": 10.0,
                    "carbs_g": 10.0,
                    "fat_g": 10.0
                  }
                }
              ],
              "error": null
            }
            """.trimIndent()
        ).toDomain()

        val item = parsed.items.single()
        assertEquals(50, item.calories)
        assertEquals(false, item.caloriesRecomputed)
        assertEquals(100f, item.per100g!!.calories, 0.001f)
    }

    @Test
    fun `toDomain preserves calories from alcohol or other non macro contributors`() {
        val parsed = NutritionResponseParser.parse(
            """
            {
              "calories": 128,
              "protein_g": 0.0,
              "carbs_g": 3.9,
              "fat_g": 0.0,
              "items": [
                {
                  "name": "wine",
                  "amount": "150ml",
                  "calories": 128,
                  "protein_g": 0.0,
                  "carbs_g": 3.9,
                  "fat_g": 0.0,
                  "grams": 150.0,
                  "per_100g": {
                    "calories": 85.0,
                    "protein_g": 0.0,
                    "carbs_g": 2.6,
                    "fat_g": 0.0
                  }
                }
              ],
              "error": null
            }
            """.trimIndent()
        ).toDomain()

        val item = parsed.items.single()
        assertEquals(128, item.calories)
        assertEquals(85f, item.per100g!!.calories, 0.001f)
        assertEquals(false, item.caloriesRecomputed)
    }

    @Test
    fun `toDomain keeps per 100g calories at Atwater tolerance edge`() {
        val parsed = NutritionResponseParser.parse(
            """
            {
              "calories": 240,
              "protein_g": 25.0,
              "carbs_g": 25.0,
              "fat_g": 0.0,
              "items": [
                {
                  "name": "edge density",
                  "amount": "100g",
                  "calories": 240,
                  "protein_g": 25.0,
                  "carbs_g": 25.0,
                  "fat_g": 0.0,
                  "grams": 100.0,
                  "per_100g": {
                    "calories": 240.0,
                    "protein_g": 25.0,
                    "carbs_g": 25.0,
                    "fat_g": 0.0
                  }
                }
              ],
              "error": null
            }
            """.trimIndent()
        ).toDomain()

        val item = parsed.items.single()
        assertEquals(240, item.calories)
        assertEquals(false, item.caloriesRecomputed)
    }

    @Test
    fun `toDomain keeps calories at Atwater tolerance edge`() {
        val parsed = NutritionResponseParser.parse(
            """
            {
              "calories": 240,
              "protein_g": 25.0,
              "carbs_g": 25.0,
              "fat_g": 0.0,
              "items": [
                {
                  "name": "edge item",
                  "amount": "100g",
                  "calories": 240,
                  "protein_g": 25.0,
                  "carbs_g": 25.0,
                  "fat_g": 0.0
                }
              ],
              "error": null
            }
            """.trimIndent()
        ).toDomain()

        val item = parsed.items.single()
        assertEquals(240, item.calories)
        assertEquals(false, item.caloriesRecomputed)
    }
}
