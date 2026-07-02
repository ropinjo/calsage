package com.calorietracker.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.calorietracker.data.local.db.dao.FavoriteMealDao
import com.calorietracker.data.local.db.dao.FoodEntryDao
import com.calorietracker.data.local.db.dao.WeightEntryDao
import com.calorietracker.data.local.db.entity.FavoriteMealEntity
import com.calorietracker.data.local.db.entity.FoodEntryEntity
import com.calorietracker.data.local.db.entity.WeightEntryEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ExportResult {
    data class Success(val uri: Uri) : ExportResult
    data object Empty : ExportResult
}

@Singleton
class CsvExportManager @Inject constructor(
    private val foodEntryDao: FoodEntryDao,
    private val weightEntryDao: WeightEntryDao,
    private val favoriteMealDao: FavoriteMealDao,
    @ApplicationContext private val context: Context
) {

    /**
     * Generates a ZIP archive containing food_entries.csv, weight_entries.csv,
     * daily_summaries.csv, and favorites.csv. Returns [ExportResult.Empty] when
     * there is no data to export, otherwise [ExportResult.Success] wrapping a
     * content URI for sharing via FileProvider.
     *
     * @param startDate Optional start date (ISO format) to filter food and weight entries.
     * @param endDate Optional end date (ISO format) to filter food and weight entries.
     */
    suspend fun exportData(
        startDate: String?,
        endDate: String?
    ): ExportResult = withContext(Dispatchers.IO) {
        val foodEntries = if (startDate != null && endDate != null) {
            foodEntryDao.getEntriesInRange(startDate, endDate).first()
        } else {
            foodEntryDao.getAllEntries().first()
        }

        val weightEntries = if (startDate != null && endDate != null) {
            weightEntryDao.getEntriesByDateRange(startDate, endDate).first()
        } else {
            weightEntryDao.getAllEntries().first()
        }

        val favorites = favoriteMealDao.getAllFavorites().first()

        if (foodEntries.isEmpty() && weightEntries.isEmpty() && favorites.isEmpty()) {
            return@withContext ExportResult.Empty
        }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        // Stale exports hold a full copy of the user's data; keep only the newest.
        exportDir.listFiles()?.forEach { it.delete() }
        val zipFile = File(exportDir, "calsage_export_${System.currentTimeMillis()}.zip")

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // Food entries CSV
            addCsvToZip(zos, "food_entries.csv") { writer ->
                writer.writeLine("ID,Date,Meal Type,Description,Calories,Protein (g),Carbs (g),Fat (g),Timestamp,Source")
                for (entry in foodEntries) {
                    writer.writeLine(
                        "${entry.id}," +
                        "${escapeCsv(entry.date)}," +
                        "${escapeCsv(entry.mealType)}," +
                        "${escapeFreeTextCsv(entry.description)}," +
                        "${entry.calories}," +
                        "${entry.proteinGrams}," +
                        "${entry.carbsGrams}," +
                        "${entry.fatGrams}," +
                        "${entry.timestamp}," +
                        "${escapeCsv(entry.source)}"
                    )
                }
            }

            // Weight entries CSV
            addCsvToZip(zos, "weight_entries.csv") { writer ->
                writer.writeLine("ID,Date,Weight (kg),Note,Timestamp")
                for (entry in weightEntries) {
                    writer.writeLine(
                        "${entry.id}," +
                        "${escapeCsv(entry.date)}," +
                        "${entry.weightKg}," +
                        "${escapeFreeTextCsv(entry.note ?: "")}," +
                        "${entry.timestamp}"
                    )
                }
            }

            // Daily summaries CSV
            addCsvToZip(zos, "daily_summaries.csv") { writer ->
                writer.writeLine("Date,Total Calories,Total Protein (g),Total Carbs (g),Total Fat (g)")
                val groupedByDate = foodEntries.groupBy { it.date }
                for ((date, entries) in groupedByDate.toSortedMap()) {
                    val totalCalories = entries.sumOf { it.calories }
                    val totalProtein = entries.sumOf { it.proteinGrams.toDouble() }.toFloat()
                    val totalCarbs = entries.sumOf { it.carbsGrams.toDouble() }.toFloat()
                    val totalFat = entries.sumOf { it.fatGrams.toDouble() }.toFloat()

                    writer.writeLine(
                        "${escapeCsv(date)}," +
                        "$totalCalories," +
                        "$totalProtein," +
                        "$totalCarbs," +
                        "$totalFat"
                    )
                }
            }

            // Favorites CSV
            addCsvToZip(zos, "favorites.csv") { writer ->
                writer.writeLine("ID,Name,Description,Total Calories,Total Protein (g),Total Carbs (g),Total Fat (g),Meal Type,Source,Items JSON")
                for (fav in favorites) {
                    writer.writeLine(
                        "${fav.id}," +
                        "${escapeFreeTextCsv(fav.name)}," +
                        "${escapeFreeTextCsv(fav.description)}," +
                        "${fav.totalCalories}," +
                        "${fav.totalProtein}," +
                        "${fav.totalCarbs}," +
                        "${fav.totalFat}," +
                        "${escapeCsv(fav.mealType)}," +
                        "${escapeCsv(fav.source)}," +
                        "${escapeCsv(fav.itemsJson ?: "")}"
                    )
                }
            }
        }

        ExportResult.Success(
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                zipFile
            )
        )
    }

    private inline fun addCsvToZip(
        zos: ZipOutputStream,
        fileName: String,
        writeContent: (BufferedWriter) -> Unit
    ) {
        zos.putNextEntry(ZipEntry(fileName))
        val writer = BufferedWriter(OutputStreamWriter(zos, Charsets.UTF_8))
        writeContent(writer)
        writer.flush()
        zos.closeEntry()
    }

    private fun BufferedWriter.writeLine(line: String) {
        write(line)
        newLine()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun escapeFreeTextCsv(value: String): String {
        return escapeCsv(value.neutralizeCsvFormulaCell())
    }
}

internal val CSV_FORMULA_PREFIXES = setOf('=', '+', '-', '@', '\t', '\r')

internal fun String.neutralizeCsvFormulaCell(): String {
    if (isEmpty()) return this
    val dangerous = this[0] in CSV_FORMULA_PREFIXES
    val guardedDangerous = length > 1 && this[0] == '\'' && this[1] in CSV_FORMULA_PREFIXES
    return if (dangerous || guardedDangerous) "'$this" else this
}

internal fun String.stripCsvFormulaGuard(): String {
    return when {
        length >= 2 && this[0] == '\'' && this[1] in CSV_FORMULA_PREFIXES -> drop(1)
        length >= 3 && this.startsWith("''") && this[2] in CSV_FORMULA_PREFIXES -> drop(1)
        else -> this
    }
}
