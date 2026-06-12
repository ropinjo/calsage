package com.calorietracker.data.export

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.calorietracker.data.local.db.AppDatabase
import com.calorietracker.data.local.db.dao.FavoriteMealDao
import com.calorietracker.data.local.db.dao.FoodEntryDao
import com.calorietracker.data.local.db.dao.WeightEntryDao
import com.calorietracker.data.local.db.entity.FavoriteMealEntity
import com.calorietracker.data.local.db.entity.FoodEntryEntity
import com.calorietracker.data.local.db.entity.WeightEntryEntity
import com.calorietracker.domain.model.MealType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

data class ImportResult(
    val foodEntries: Int,
    val weightEntries: Int,
    val favorites: Int,
    val skipped: Int = 0
) {
    val total: Int get() = foodEntries + weightEntries + favorites
}

enum class ImportMode {
    /** Skip rows whose fingerprint matches an existing entry. */
    Merge,

    /** Delete existing entries for any date present in the import, then insert all incoming rows. */
    Replace
}

@Singleton
class CsvImportManager @Inject constructor(
    private val database: AppDatabase,
    private val foodEntryDao: FoodEntryDao,
    private val weightEntryDao: WeightEntryDao,
    private val favoriteMealDao: FavoriteMealDao,
    @ApplicationContext private val context: Context
) {

    /**
     * Reads a ZIP archive produced by [CsvExportManager] and applies its rows
     * to the current database according to [mode]. Imported rows get
     * fresh auto-generated IDs. The derived `daily_summaries.csv` is ignored
     * because it is recomputed from food entries.
     *
     * @throws IllegalArgumentException if the file cannot be read or contains
     *   none of the expected CSV entries.
     */
    suspend fun importData(
        uri: Uri,
        mode: ImportMode = ImportMode.Merge
    ): ImportResult = withContext(Dispatchers.IO) {
        val foodEntries = mutableListOf<FoodEntryEntity>()
        val weightEntries = mutableListOf<WeightEntryEntity>()
        val favorites = mutableListOf<FavoriteMealEntity>()
        var sawKnownEntry = false

        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Unable to open the selected file")

        input.use { stream ->
            ZipInputStream(stream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name.substringAfterLast('/')
                    val bytes = zis.readAllAvailable()
                    val text = String(bytes.toByteArray(), Charsets.UTF_8)
                    when (name) {
                        "food_entries.csv" -> {
                            sawKnownEntry = true
                            foodEntries += parseFoodEntries(text)
                        }
                        "weight_entries.csv" -> {
                            sawKnownEntry = true
                            weightEntries += parseWeightEntries(text)
                        }
                        "favorites.csv" -> {
                            sawKnownEntry = true
                            favorites += parseFavorites(text)
                        }
                        "daily_summaries.csv" -> sawKnownEntry = true
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        if (!sawKnownEntry) {
            throw IllegalArgumentException("The selected file does not look like a CalSage export")
        }

        when (mode) {
            ImportMode.Replace -> applyReplace(foodEntries, weightEntries, favorites)
            ImportMode.Merge -> applyMerge(foodEntries, weightEntries, favorites)
        }
    }

    private suspend fun applyReplace(
        foodEntries: List<FoodEntryEntity>,
        weightEntries: List<WeightEntryEntity>,
        favorites: List<FavoriteMealEntity>
    ): ImportResult = database.withTransaction {
        // Chunked to stay under SQLite's bind-variable limit on long histories.
        val foodDates = foodEntries.map { it.date }.distinct()
        foodDates.chunked(500).forEach { foodEntryDao.deleteByDates(it) }
        val weightDates = weightEntries.map { it.date }.distinct()
        weightDates.chunked(500).forEach { weightEntryDao.deleteByDates(it) }

        val existingFavorites = favoriteMealDao.getAllFavoritesOnce()
        val existingByKey = existingFavorites.associateBy { it.name to it.mealType }
        for (incoming in favorites) {
            existingByKey[incoming.name to incoming.mealType]?.let {
                favoriteMealDao.deleteById(it.id)
            }
        }

        foodEntries.forEach { foodEntryDao.insert(it) }
        weightEntries.forEach { weightEntryDao.insert(it) }
        favorites.forEach { favoriteMealDao.insert(it) }

        ImportResult(
            foodEntries = foodEntries.size,
            weightEntries = weightEntries.size,
            favorites = favorites.size,
            skipped = 0
        )
    }

    private suspend fun applyMerge(
        foodEntries: List<FoodEntryEntity>,
        weightEntries: List<WeightEntryEntity>,
        favorites: List<FavoriteMealEntity>
    ): ImportResult {
        val existingFoodKeys = foodEntryDao.getAllEntriesOnce().mapTo(mutableSetOf()) { foodKey(it) }
        var foodInserted = 0
        var skipped = 0
        for (entry in foodEntries) {
            val key = foodKey(entry)
            if (existingFoodKeys.add(key)) {
                foodEntryDao.insert(entry)
                foodInserted++
            } else {
                skipped++
            }
        }

        val existingWeightKeys = weightEntryDao.getAllEntriesOnce().mapTo(mutableSetOf()) { weightKey(it) }
        var weightInserted = 0
        for (entry in weightEntries) {
            val key = weightKey(entry)
            if (existingWeightKeys.add(key)) {
                weightEntryDao.insert(entry)
                weightInserted++
            } else {
                skipped++
            }
        }

        val existingFavKeys = favoriteMealDao.getAllFavoritesOnce().mapTo(mutableSetOf()) { favKey(it) }
        var favInserted = 0
        for (fav in favorites) {
            val key = favKey(fav)
            if (existingFavKeys.add(key)) {
                favoriteMealDao.insert(fav)
                favInserted++
            } else {
                skipped++
            }
        }

        return ImportResult(
            foodEntries = foodInserted,
            weightEntries = weightInserted,
            favorites = favInserted,
            skipped = skipped
        )
    }

    // Timestamp is part of the fingerprint so two identical foods logged at
    // different times (e.g. two coffees in one meal) both survive a restore.
    private fun foodKey(entry: FoodEntryEntity): String =
        "${entry.date}|${entry.mealType}|${entry.description}|${entry.calories}|${entry.timestamp}"

    private fun weightKey(entry: WeightEntryEntity): String =
        "${entry.date}|${entry.weightKg}|${entry.timestamp}"

    private fun favKey(entry: FavoriteMealEntity): String =
        "${entry.name}|${entry.mealType}"

    private fun parseFoodEntries(text: String): List<FoodEntryEntity> {
        return parseCsvRows(text).drop(1).mapNotNull { row ->
            if (row.size < 9) return@mapNotNull null
            runCatching {
                FoodEntryEntity(
                    id = 0,
                    // Reject rows with non-ISO dates here so downstream date
                    // parsing can trust everything stored in the database.
                    date = LocalDate.parse(row[1]).toString(),
                    mealType = parseMealType(row[2]),
                    description = row[3],
                    calories = row[4].toInt(),
                    proteinGrams = row[5].toFloat(),
                    carbsGrams = row[6].toFloat(),
                    fatGrams = row[7].toFloat(),
                    timestamp = row[8].toLong(),
                    source = row.getOrNull(9).orEmpty().ifBlank { "AI" }
                )
            }.getOrNull()
        }
    }

    private fun parseWeightEntries(text: String): List<WeightEntryEntity> {
        return parseCsvRows(text).drop(1).mapNotNull { row ->
            if (row.size < 5) return@mapNotNull null
            runCatching {
                WeightEntryEntity(
                    id = 0,
                    date = LocalDate.parse(row[1]).toString(),
                    weightKg = row[2].toFloat(),
                    note = row[3].ifBlank { null },
                    timestamp = row[4].toLong()
                )
            }.getOrNull()
        }
    }

    private fun parseFavorites(text: String): List<FavoriteMealEntity> {
        return parseCsvRows(text).drop(1).mapNotNull { row ->
            if (row.size < 8) return@mapNotNull null
            runCatching {
                FavoriteMealEntity(
                    id = 0,
                    name = row[1],
                    description = row[2],
                    totalCalories = row[3].toInt(),
                    totalProtein = row[4].toFloat(),
                    totalCarbs = row[5].toFloat(),
                    totalFat = row[6].toFloat(),
                    itemsJson = row.getOrNull(9)?.takeIf { it.isNotBlank() },
                    mealType = parseMealType(row[7]),
                    source = row.getOrNull(8).orEmpty().ifBlank { "AI" }
                )
            }.getOrNull()
        }
    }

    // Reject rows with unknown meal types (throws inside the row's runCatching)
    // so unguarded MealType.valueOf() calls on read paths can't crash later;
    // hand-edited CSVs often carry "Breakfast" instead of "BREAKFAST".
    private fun parseMealType(raw: String): String =
        MealType.valueOf(raw.trim().uppercase()).name

    // RFC 4180-style parser matching CsvExportManager.escapeCsv: fields may be
    // quoted, and literal quotes inside a quoted field are doubled.
    private fun parseCsvRows(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    ',' -> {
                        row.add(field.toString())
                        field.clear()
                    }
                    '\r' -> { /* swallow */ }
                    '\n' -> {
                        row.add(field.toString())
                        field.clear()
                        rows.add(row.toList())
                        row.clear()
                    }
                    else -> field.append(ch)
                }
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row.toList())
        }
        return rows
    }

    private fun ZipInputStream.readAllAvailable(): ByteArrayOutputStream {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val read = read(buffer)
            if (read <= 0) break
            out.write(buffer, 0, read)
        }
        return out
    }
}
