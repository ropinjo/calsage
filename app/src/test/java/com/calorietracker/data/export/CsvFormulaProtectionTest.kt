package com.calorietracker.data.export

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvFormulaProtectionTest {

    @Test
    fun `formula guard round trips hyperlink description`() {
        val description = "=HYPERLINK(\"https://example.com\",\"meal\")"

        val exported = description.neutralizeCsvFormulaCell()
        val imported = exported.stripCsvFormulaGuard()

        assertEquals("'$description", exported)
        assertEquals(description, imported)
    }
}
