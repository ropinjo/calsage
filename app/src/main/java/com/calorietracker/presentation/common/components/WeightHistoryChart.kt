package com.calorietracker.presentation.common.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorietracker.domain.model.WeightEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun WeightHistoryChart(
    entries: List<WeightEntry>,
    unit: String,
    modifier: Modifier = Modifier
) {
    val sorted = remember(entries) { entries.sortedWith(compareBy<WeightEntry> { it.date }.thenBy { it.timestamp }) }
    val displayWeights = remember(sorted, unit) {
        sorted.map { entry -> convertWeight(entry.weightKg, unit) }
    }
    val movingAverage = remember(sorted, unit) {
        sorted.indices.map { index ->
            val start = (index - 6).coerceAtLeast(0)
            val averageKg = sorted.subList(start, index + 1)
                .map { it.weightKg }
                .average()
                .toFloat()
            convertWeight(averageKg, unit)
        }
    }
    val showTrend = sorted.size >= 7
    val yAxisTicks = remember(displayWeights) { buildWeightAxisTicks(displayWeights) }
    val minAxisWeight = yAxisTicks.firstOrNull() ?: displayWeights.minOrNull().orZero()
    val maxAxisWeight = yAxisTicks.lastOrNull() ?: displayWeights.maxOrNull().orZero()
    val range = (maxAxisWeight - minAxisWeight).coerceAtLeast(1f)
    val xAxisLabelIndices = remember(sorted) { selectAxisLabelIndices(sorted.size, maxLabels = 4) }
    val axisUnitLabel = remember(unit) { formatWeightUnitLabel(unit) }

    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val axisColor = onSurfaceVariantColor.copy(alpha = 0.28f)
    val gridColor = onSurfaceVariantColor.copy(alpha = 0.18f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val rawLineColor = primaryColor.copy(alpha = 0.38f)
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (sorted.size < 2) return@Canvas

                val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = onSurfaceVariantColor.toArgb()
                    textSize = 11.sp.toPx()
                }
                val xAxisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = onSurfaceVariantColor.toArgb()
                    textSize = 11.sp.toPx()
                    textAlign = Paint.Align.CENTER
                }
                val yAxisLabelValues = yAxisTicks.map { formatWeightAxisLabel(it, axisUnitLabel) }
                val leftPadding = maxOf(
                    42.dp.toPx(),
                    (yAxisLabelValues.maxOfOrNull { axisPaint.measureText(it) } ?: 0f) + 16.dp.toPx()
                )
                val rightPadding = 12.dp.toPx()
                val topPadding = 16.dp.toPx()
                val bottomPadding = 30.dp.toPx()
                val chartWidth = size.width - leftPadding - rightPadding
                val chartHeight = size.height - topPadding - bottomPadding
                val chartBottom = topPadding + chartHeight
                val stepX = chartWidth / (sorted.size - 1).coerceAtLeast(1)
                val axisX = leftPadding
                val axisOverhang = 5.dp.toPx()

                drawLine(
                    color = axisColor,
                    start = Offset(axisX, topPadding - axisOverhang),
                    end = Offset(axisX, chartBottom + axisOverhang),
                    strokeWidth = 1.dp.toPx()
                )

                yAxisTicks.forEach { tick ->
                    val y = topPadding + chartHeight * (1f - (tick - minAxisWeight) / range)
                    drawLine(
                        color = axisColor,
                        start = Offset(axisX - axisOverhang, y),
                        end = Offset(axisX, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(axisX, y),
                        end = Offset(size.width - rightPadding, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        formatWeightAxisLabel(tick, axisUnitLabel),
                        axisX - 8.dp.toPx(),
                        y + 4.dp.toPx(),
                        axisPaint.apply { textAlign = Paint.Align.RIGHT }
                    )
                }

                val rawPath = Path()
                displayWeights.forEachIndexed { index, weight ->
                    val x = leftPadding + index * stepX
                    val y = topPadding + chartHeight * (1f - (weight - minAxisWeight) / range)
                    if (index == 0) rawPath.moveTo(x, y) else rawPath.lineTo(x, y)
                }
                drawPath(
                    path = rawPath,
                    color = rawLineColor,
                    style = Stroke(width = 2.dp.toPx())
                )

                if (showTrend) {
                    val averagePath = Path()
                    movingAverage.forEachIndexed { index, average ->
                        val x = leftPadding + index * stepX
                        val y = topPadding + chartHeight * (1f - (average - minAxisWeight) / range)
                        if (index == 0) averagePath.moveTo(x, y) else averagePath.lineTo(x, y)
                    }
                    drawPath(
                        path = averagePath,
                        color = primaryColor,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                displayWeights.forEachIndexed { index, weight ->
                    val x = leftPadding + index * stepX
                    val y = topPadding + chartHeight * (1f - (weight - minAxisWeight) / range)
                    drawCircle(primaryColor, 5.dp.toPx(), Offset(x, y))
                    drawCircle(surfaceColor, 2.5f.dp.toPx(), Offset(x, y))

                    if (index in xAxisLabelIndices) {
                        drawContext.canvas.nativeCanvas.drawText(
                            formatAxisDate(sorted[index].date),
                            x,
                            size.height - 8.dp.toPx(),
                            xAxisPaint
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 3.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.38f), RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(4.dp))
            Text("Weight", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (showTrend) {
                Spacer(Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(width = 12.dp, height = 3.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(4.dp))
                Text("Trend", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun selectAxisLabelIndices(count: Int, maxLabels: Int): Set<Int> {
    if (count <= 0) return emptySet()
    if (count <= maxLabels) return (0 until count).toSet()

    val step = (count - 1).toFloat() / (maxLabels - 1).coerceAtLeast(1)
    return (0 until maxLabels)
        .map { index -> (index * step).roundToInt().coerceIn(0, count - 1) }
        .toSet()
}

private fun buildWeightAxisTicks(weights: List<Float>): List<Float> {
    if (weights.isEmpty()) return emptyList()

    val minWeight = floor(weights.minOrNull().orZero().toDouble()).toFloat()
    val maxWeight = ceil(weights.maxOrNull().orZero().toDouble()).toFloat()
    if (minWeight == maxWeight) {
        return listOf(minWeight - 1f, minWeight, minWeight + 1f)
    }

    val span = (maxWeight - minWeight).coerceAtLeast(1f)
    val step = ceil(span / 3f).toInt().coerceAtLeast(1)

    val ticks = mutableListOf<Float>()
    var current = minWeight.toInt()
    val end = maxWeight.toInt()

    while (current <= end) {
        ticks += current.toFloat()
        current += step
    }

    if (ticks.lastOrNull() != end.toFloat()) {
        ticks += end.toFloat()
    }

    return ticks.distinct()
}

private fun formatWeightAxisValue(value: Float): String {
    return if (value % 1f == 0f) {
        value.roundToInt().toString()
    } else {
        "%.1f".format(value)
    }
}

private fun formatWeightAxisLabel(value: Float, unitLabel: String): String {
    return "${formatWeightAxisValue(value)} $unitLabel"
}

private fun formatWeightUnitLabel(unit: String): String {
    return if (unit == "lb") "lbs" else "kg"
}

private fun formatAxisDate(isoDate: String): String {
    return try {
        val date = LocalDate.parse(isoDate)
        date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    } catch (_: Exception) {
        ""
    }
}

private fun convertWeight(weightKg: Float, unit: String): Float {
    return if (unit == "lb") weightKg * 2.20462f else weightKg
}

private fun Float?.orZero(): Float = this ?: 0f
