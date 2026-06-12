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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorietracker.domain.model.WeightEntry
import java.time.LocalDate
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
    val xFractions = remember(sorted) { dateXFractions(sorted.map { it.date }) }
    val xAxisLabelIndices = remember(xFractions) { selectSpreadLabelIndices(xFractions) }
    val axisUnitLabel = remember(unit) { formatWeightUnitLabel(unit) }
    val showDots = sorted.size <= CHART_MAX_DOTS
    var selectedIndex by remember(sorted) { mutableStateOf<Int?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val axisColor = onSurfaceVariantColor.copy(alpha = 0.28f)
    val gridColor = onSurfaceVariantColor.copy(alpha = 0.18f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val rawLineColor = primaryColor.copy(alpha = 0.38f)
    val surfaceColor = MaterialTheme.colorScheme.surface

    val density = LocalDensity.current
    // The gesture handler needs the same left inset the canvas draws with, so
    // measure the axis labels here instead of inside the draw scope.
    val leftPaddingPx = remember(yAxisTicks, axisUnitLabel, density) {
        with(density) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11.sp.toPx() }
            maxOf(
                42.dp.toPx(),
                (yAxisTicks.maxOfOrNull { paint.measureText(formatWeightAxisLabel(it, axisUnitLabel)) } ?: 0f) + 16.dp.toPx()
            )
        }
    }
    val rightPaddingPx = with(density) { 12.dp.toPx() }

    val headerText = selectedIndex?.let { index ->
        val day = formatScrubDay(LocalDate.parse(sorted[index].date))
        val weight = "%.1f %s".format(displayWeights[index], axisUnitLabel)
        if (showTrend) {
            "$day • $weight · trend %.1f".format(movingAverage[index])
        } else {
            "$day • $weight"
        }
    }

    Column(modifier = modifier) {
        ChartScrubHeader(text = headerText)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .onSizeChanged { canvasSize = it }
                .chartScrub(
                    // leftPaddingPx is part of the key: it changes with the unit
                    // (label widths) and the captured resolveIndex must not go stale.
                    key = Pair(sorted, leftPaddingPx),
                    resolveIndex = { offset ->
                        nearestPointIndex(offset.x, xFractions, canvasSize.width, leftPaddingPx, rightPaddingPx)
                    },
                    onTap = { index -> selectedIndex = if (selectedIndex == index) null else index },
                    onScrub = { selectedIndex = it }
                )
        ) {
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
                val leftPadding = leftPaddingPx
                val rightPadding = rightPaddingPx
                val topPadding = 16.dp.toPx()
                val bottomPadding = 30.dp.toPx()
                val chartWidth = size.width - leftPadding - rightPadding
                val chartHeight = size.height - topPadding - bottomPadding
                val chartBottom = topPadding + chartHeight
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

                fun pointX(index: Int) = leftPadding + chartWidth * xFractions[index]

                selectedIndex?.let { index ->
                    drawLine(
                        color = onSurfaceVariantColor.copy(alpha = 0.35f),
                        start = Offset(pointX(index), topPadding),
                        end = Offset(pointX(index), chartBottom),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                val rawPath = Path()
                displayWeights.forEachIndexed { index, weight ->
                    val x = pointX(index)
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
                        val x = pointX(index)
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
                    val x = pointX(index)
                    val y = topPadding + chartHeight * (1f - (weight - minAxisWeight) / range)
                    if (showDots || index == selectedIndex) {
                        val emphasized = index == selectedIndex
                        drawCircle(primaryColor, if (emphasized) 6.dp.toPx() else 5.dp.toPx(), Offset(x, y))
                        drawCircle(surfaceColor, if (emphasized) 3.dp.toPx() else 2.5f.dp.toPx(), Offset(x, y))
                    }

                    if (index in xAxisLabelIndices) {
                        drawContext.canvas.nativeCanvas.drawText(
                            formatScrubDate(sorted[index].date),
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

private fun convertWeight(weightKg: Float, unit: String): Float {
    return if (unit == "lb") weightKg * 2.20462f else weightKg
}

private fun Float?.orZero(): Float = this ?: 0f
