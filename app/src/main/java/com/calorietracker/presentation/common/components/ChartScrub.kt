package com.calorietracker.presentation.common.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

// Density limits shared by the charts: past these point counts the decoration
// becomes unreadable, so it is dropped or aggregated.
const val CHART_MAX_VALUE_LABELS = 10
const val CHART_MAX_DAILY_BARS = 31
const val CHART_MAX_DOTS = 30

/**
 * Tap selects the nearest data point (tap again to dismiss); long-press then
 * drag scrubs across points. Vertical scrolling above the chart keeps working
 * because plain drags are never consumed.
 */
fun Modifier.chartScrub(
    key: Any?,
    resolveIndex: (Offset) -> Int?,
    onTap: (Int) -> Unit,
    onScrub: (Int) -> Unit
): Modifier = this
    .pointerInput(key) {
        detectTapGestures { offset -> resolveIndex(offset)?.let(onTap) }
    }
    .pointerInput(key) {
        detectDragGesturesAfterLongPress(
            onDragStart = { offset -> resolveIndex(offset)?.let(onScrub) },
            onDrag = { change, _ ->
                change.consume()
                resolveIndex(change.position)?.let(onScrub)
            }
        )
    }

/** Fixed-height slot above a chart that shows details for the scrubbed point. */
@Composable
fun ChartScrubHeader(text: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (text != null) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
    }
}

/**
 * Maps ISO dates to horizontal fractions (0..1) proportional to elapsed time,
 * so points land where their dates fall rather than at evenly spaced indices.
 * Falls back to index spacing when all dates are equal.
 */
fun dateXFractions(isoDates: List<String>): List<Float> {
    if (isoDates.isEmpty()) return emptyList()
    if (isoDates.size == 1) return listOf(0.5f)
    val days = isoDates.map { LocalDate.parse(it).toEpochDay() }
    val min = days.min()
    val span = days.max() - min
    return if (span == 0L) {
        days.indices.map { it.toFloat() / (days.size - 1) }
    } else {
        days.map { (it - min).toFloat() / span }
    }
}

/**
 * Picks up to [maxLabels] indices for x-axis labels from points at the given
 * horizontal fractions (0..1), spreading them evenly and skipping candidates
 * that would land too close to an already chosen label.
 */
/** Index of the point whose x position is closest to the touch, or null before layout. */
fun nearestPointIndex(
    touchX: Float,
    fractions: List<Float>,
    canvasWidth: Int,
    leftPadding: Float,
    rightPadding: Float
): Int? {
    if (fractions.isEmpty() || canvasWidth == 0) return null
    val chartWidth = canvasWidth - leftPadding - rightPadding
    return fractions.indices.minByOrNull { abs(leftPadding + fractions[it] * chartWidth - touchX) }
}

fun formatScrubDay(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))

fun formatScrubDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))

fun formatScrubDate(isoDate: String): String = formatScrubDate(LocalDate.parse(isoDate))

fun selectSpreadLabelIndices(fractions: List<Float>, maxLabels: Int = 4): Set<Int> {
    if (fractions.isEmpty()) return emptySet()
    val targets = List(maxLabels) { it.toFloat() / (maxLabels - 1).coerceAtLeast(1) }
    val chosen = mutableListOf<Int>()
    targets.forEach { target ->
        val candidate = fractions.indices.minByOrNull { abs(fractions[it] - target) } ?: return@forEach
        if (chosen.none { abs(fractions[it] - fractions[candidate]) < 0.15f }) {
            chosen += candidate
        }
    }
    return chosen.toSet()
}
