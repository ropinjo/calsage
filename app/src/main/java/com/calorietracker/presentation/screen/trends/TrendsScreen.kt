package com.calorietracker.presentation.screen.trends

import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import com.calorietracker.presentation.theme.extendedColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorietracker.domain.model.WeightEntry
import com.calorietracker.presentation.common.components.CHART_MAX_DAILY_BARS
import com.calorietracker.presentation.common.components.CHART_MAX_DOTS
import com.calorietracker.presentation.common.components.CHART_MAX_VALUE_LABELS
import com.calorietracker.presentation.common.components.ChartScrubHeader
import com.calorietracker.presentation.common.components.WeightHistoryChart
import com.calorietracker.presentation.common.components.chartScrub
import com.calorietracker.presentation.common.components.dateXFractions
import com.calorietracker.presentation.common.components.formatScrubDate
import com.calorietracker.presentation.common.components.formatScrubDay
import com.calorietracker.presentation.common.components.nearestPointIndex
import com.calorietracker.presentation.common.components.selectSpreadLabelIndices
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(
    viewModel: TrendsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Trends",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Tab Row ---
            TabRow(
                selectedTabIndex = TrendsTab.entries.indexOf(uiState.selectedTab),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                indicator = { tabPositions ->
                    if (tabPositions.isNotEmpty()) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[TrendsTab.entries.indexOf(uiState.selectedTab)]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                TrendsTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                tab.name,
                                fontWeight = if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- Time Range ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrendsRange.entries.forEach { range ->
                    FilterChip(
                        selected = uiState.selectedRange == range,
                        onClick = { viewModel.selectRange(range) },
                        label = {
                            Text(
                                range.label,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- Chart ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                when (uiState.selectedTab) {
                    TrendsTab.Calories -> {
                        if (uiState.calorieData.any { it.tracked }) {
                            CalorieBarChart(
                                data = uiState.calorieData,
                                goalLine = uiState.calorieGoal,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            EmptyChartPlaceholder("No calorie data for this period")
                        }
                    }
                    TrendsTab.Macros -> {
                        if (uiState.macroData.isNotEmpty()) {
                            MacroLineChart(
                                data = uiState.macroData,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            EmptyChartPlaceholder("No macro data for this period")
                        }
                    }
                    TrendsTab.Weight -> {
                        if (uiState.weightData.size >= 2) {
                            WeightHistoryChart(
                                entries = uiState.weightData,
                                unit = uiState.weightUnit,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            EmptyChartPlaceholder("Need at least 2 weight entries")
                        }
                    }
                }
            }

            // --- Summary Stats ---
            when (uiState.selectedTab) {
                TrendsTab.Calories -> {
                    if (uiState.calorieData.any { it.tracked }) {
                        CalorieSummary(uiState.calorieData, uiState.calorieGoal)
                    }
                }
                TrendsTab.Macros -> {
                    if (uiState.macroData.isNotEmpty()) {
                        MacroSummary(uiState.macroData)
                    }
                }
                TrendsTab.Weight -> {
                    if (uiState.weightData.size >= 2) {
                        WeightSummary(uiState.weightData, uiState.weightUnit)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private data class CalorieBucket(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val calories: Int,
    val trackedDays: Int
)

// Beyond a month of daily bars the labels and bars become unreadable, so
// aggregate into weekly averages per tracked day (comparable to the daily goal).
private fun buildCalorieBuckets(data: List<DailyCaloriePoint>): List<CalorieBucket> {
    val daily = data.map {
        val date = LocalDate.parse(it.date)
        CalorieBucket(date, date, it.totalCalories, if (it.tracked) 1 else 0)
    }
    if (daily.size <= CHART_MAX_DAILY_BARS) return daily
    return daily
        .groupBy { it.startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
        .toSortedMap()
        .map { (_, days) ->
            val tracked = days.filter { it.trackedDays > 0 }
            CalorieBucket(
                startDate = days.first().startDate,
                endDate = days.last().startDate,
                calories = if (tracked.isEmpty()) 0 else tracked.map { it.calories }.average().roundToInt(),
                trackedDays = tracked.size
            )
        }
}

@Composable
private fun CalorieBarChart(
    data: List<DailyCaloriePoint>,
    goalLine: Int,
    modifier: Modifier = Modifier
) {
    val buckets = remember(data) { buildCalorieBuckets(data) }
    val isWeekly = data.size > CHART_MAX_DAILY_BARS
    val maxCal = remember(buckets, goalLine) {
        (buckets.maxOfOrNull { it.calories } ?: goalLine).coerceAtLeast(goalLine).toFloat() * 1.15f
    }
    val showValueLabels = buckets.size <= CHART_MAX_VALUE_LABELS
    val xAxisLabelIndices = remember(buckets) { selectAxisLabelIndices(buckets.size, maxLabels = 4) }
    var selectedIndex by remember(buckets) { mutableStateOf<Int?>(null) }
    var canvasWidth by remember { mutableStateOf(0) }

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridLineColor = onSurfaceVariantColor.copy(alpha = 0.18f)
    val warningColor = MaterialTheme.extendedColors.warning
    val primaryColor = MaterialTheme.colorScheme.primary

    val headerText = selectedIndex?.let { index ->
        val bucket = buckets[index]
        if (isWeekly) {
            val range = "${formatScrubDate(bucket.startDate)} - ${formatScrubDate(bucket.endDate)}"
            if (bucket.trackedDays == 0) {
                "$range • not tracked"
            } else {
                "$range • avg ${bucket.calories} kcal/day (${bucket.trackedDays} days tracked)"
            }
        } else {
            val day = formatScrubDay(bucket.startDate)
            if (bucket.trackedDays == 0) "$day • not tracked" else "$day • ${bucket.calories} kcal"
        }
    } ?: "Weekly averages".takeIf { isWeekly }

    Column(modifier = modifier) {
        ChartScrubHeader(text = headerText)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .onSizeChanged { canvasWidth = it.width }
                .chartScrub(
                    key = buckets,
                    resolveIndex = { offset ->
                        if (canvasWidth == 0 || buckets.isEmpty()) {
                            null
                        } else {
                            (offset.x / (canvasWidth.toFloat() / buckets.size)).toInt()
                                .coerceIn(0, buckets.size - 1)
                        }
                    },
                    onTap = { index -> selectedIndex = if (selectedIndex == index) null else index },
                    onScrub = { selectedIndex = it }
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barCount = buckets.size
                if (barCount == 0) return@Canvas

                val chartTop = 26.dp.toPx()
                val chartBottom = size.height - 30.dp.toPx()
                val chartHeight = chartBottom - chartTop
                val slotWidth = size.width / barCount
                val barWidth = slotWidth * 0.62f

                val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = onSurfaceColor.toArgb()
                    textAlign = Paint.Align.CENTER
                    textSize = 11.sp.toPx()
                    isFakeBoldText = true
                }
                val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = onSurfaceVariantColor.toArgb()
                    textAlign = Paint.Align.CENTER
                    textSize = 11.sp.toPx()
                }

                repeat(4) { index ->
                    val y = chartTop + chartHeight * (index / 3f)
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                selectedIndex?.let { index ->
                    val centerX = slotWidth * index + slotWidth / 2f
                    drawLine(
                        color = onSurfaceVariantColor.copy(alpha = 0.35f),
                        start = Offset(centerX, chartTop),
                        end = Offset(centerX, chartBottom),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                // Goal line
                val goalY = chartBottom - chartHeight * (goalLine / maxCal)
                drawLine(
                    color = warningColor,
                    start = Offset(0f, goalY),
                    end = Offset(size.width, goalY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )

                // Bars; untracked buckets stay empty so gaps in tracking are visible
                buckets.forEachIndexed { index, bucket ->
                    val centerX = slotWidth * index + slotWidth / 2f

                    if (bucket.trackedDays > 0) {
                        val x = centerX - barWidth / 2f
                        val barHeight = chartHeight * (bucket.calories / maxCal)
                        val barTop = chartBottom - barHeight
                        val baseColor = if (bucket.calories > goalLine) warningColor else primaryColor
                        val barColor = if (index == selectedIndex) baseColor else baseColor.copy(alpha = 0.8f)

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, barTop),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                        )

                        if (showValueLabels) {
                            val textHeight = valuePaint.fontMetrics.run { descent - ascent }
                            val defaultLabelBaseline = (barTop - 8.dp.toPx()).coerceAtLeast(textHeight)
                            val labelTop = defaultLabelBaseline - textHeight
                            val lineOverlapPadding = 6.dp.toPx()
                            val labelBaseline = if (goalY in (labelTop - lineOverlapPadding)..(defaultLabelBaseline + lineOverlapPadding)) {
                                (goalY - lineOverlapPadding).coerceAtLeast(textHeight)
                            } else {
                                defaultLabelBaseline
                            }

                            drawContext.canvas.nativeCanvas.drawText(
                                bucket.calories.toString(),
                                centerX,
                                labelBaseline,
                                valuePaint
                            )
                        }
                    }

                    if (index in xAxisLabelIndices) {
                        drawContext.canvas.nativeCanvas.drawText(
                            formatScrubDate(bucket.startDate),
                            centerX,
                            size.height - 8.dp.toPx(),
                            axisPaint
                        )
                    }
                }
            }
        }

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(4.dp))
            Text("At or under goal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Box(modifier = Modifier.size(10.dp).background(MaterialTheme.extendedColors.warning.copy(alpha = 0.8f), RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(4.dp))
            Text("Over goal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Box(modifier = Modifier.size(12.dp, 2.dp).background(MaterialTheme.extendedColors.warning, RoundedCornerShape(1.dp)))
            Spacer(Modifier.width(4.dp))
            Text("Goal (${goalLine})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CalorieSummary(data: List<DailyCaloriePoint>, goal: Int) {
    val tracked = remember(data) { data.filter { it.tracked } }
    // Rounded, not truncated, so the summary matches the weekly bars
    val avg = remember(tracked) {
        if (tracked.isEmpty()) 0 else tracked.map { it.totalCalories }.average().roundToInt()
    }
    val daysOverGoal = remember(tracked, goal) { tracked.count { it.totalCalories > goal } }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Summary", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            SummaryRow("Average on tracked days", "$avg kcal")
            SummaryRow("Days tracked", "${tracked.size} / ${data.size}")
            SummaryRow("Days over goal", "$daysOverGoal / ${tracked.size}")
        }
    }
}

@Composable
private fun WeightSummary(data: List<WeightEntry>, unit: String) {
    val sorted = remember(data) { data.sortedWith(compareBy<WeightEntry> { it.date }.thenBy { it.timestamp }) }
    val first = sorted.firstOrNull()
    val last = sorted.lastOrNull()
    val change = if (first != null && last != null) last.weightKg - first.weightKg else null

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Summary", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            if (first != null) SummaryRow("Starting", formatSummaryWeight(first.weightKg, unit, includeSign = false))
            if (last != null) SummaryRow("Latest", formatSummaryWeight(last.weightKg, unit, includeSign = false))
            if (change != null) SummaryRow("Change", formatSummaryWeight(change, unit, includeSign = true))
            SummaryRow("Entries", "${sorted.size}")
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun MacroLineChart(
    data: List<DailyMacros>,
    modifier: Modifier = Modifier
) {
    val macroValues = remember(data) { data.flatMap { listOf(it.protein, it.carbs, it.fat) } }
    val yAxisTicks = remember(macroValues) { buildMacroAxisTicks(macroValues) }
    val minAxisValue = yAxisTicks.firstOrNull() ?: macroValues.minOrNull().orZero()
    val maxAxisValue = yAxisTicks.lastOrNull() ?: macroValues.maxOrNull().orZero()
    val range = (maxAxisValue - minAxisValue).coerceAtLeast(1f)
    val xFractions = remember(data) { dateXFractions(data.map { it.date }) }
    val xAxisLabelIndices = remember(xFractions) { selectSpreadLabelIndices(xFractions) }
    val showDots = data.size <= CHART_MAX_DOTS
    var selectedIndex by remember(data) { mutableStateOf<Int?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val axisColor = onSurfaceVariantColor.copy(alpha = 0.28f)
    val gridColor = onSurfaceVariantColor.copy(alpha = 0.18f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val proteinColor = MaterialTheme.extendedColors.protein
    val carbsColor = MaterialTheme.extendedColors.carbs
    val fatColor = MaterialTheme.extendedColors.fat

    val density = LocalDensity.current
    // The gesture handler needs the same left inset the canvas draws with, so
    // measure the axis labels here instead of inside the draw scope.
    val leftPaddingPx = remember(yAxisTicks, density) {
        with(density) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11.sp.toPx() }
            maxOf(
                42.dp.toPx(),
                (yAxisTicks.maxOfOrNull { paint.measureText(formatMacroAxisLabel(it)) } ?: 0f) + 16.dp.toPx()
            )
        }
    }
    val rightPaddingPx = with(density) { 12.dp.toPx() }

    val headerText = selectedIndex?.let { index ->
        val entry = data[index]
        "${formatScrubDay(LocalDate.parse(entry.date))} • P %.0fg · C %.0fg · F %.0fg"
            .format(entry.protein, entry.carbs, entry.fat)
    }

    Column(modifier = modifier) {
        ChartScrubHeader(text = headerText)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .onSizeChanged { canvasSize = it }
                .chartScrub(
                    key = data,
                    resolveIndex = { offset ->
                        nearestPointIndex(offset.x, xFractions, canvasSize.width, leftPaddingPx, rightPaddingPx)
                    },
                    onTap = { index -> selectedIndex = if (selectedIndex == index) null else index },
                    onScrub = { selectedIndex = it }
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pointCount = data.size
                if (pointCount == 0) return@Canvas

                val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = onSurfaceVariantColor.toArgb()
                    textSize = 11.sp.toPx()
                }
                val xAxisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = onSurfaceVariantColor.toArgb()
                    textAlign = Paint.Align.CENTER
                    textSize = 11.sp.toPx()
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
                    val y = topPadding + chartHeight * (1f - (tick - minAxisValue) / range)
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
                        formatMacroAxisLabel(tick),
                        axisX - 8.dp.toPx(),
                        y + 4.dp.toPx(),
                        axisPaint.apply { textAlign = Paint.Align.RIGHT }
                    )
                }

                fun pointFor(index: Int, value: Float): Offset {
                    val x = leftPadding + chartWidth * xFractions[index]
                    val y = topPadding + chartHeight * (1f - (value - minAxisValue) / range)
                    return Offset(x, y)
                }

                selectedIndex?.let { index ->
                    val x = leftPadding + chartWidth * xFractions[index]
                    drawLine(
                        color = onSurfaceVariantColor.copy(alpha = 0.35f),
                        start = Offset(x, topPadding),
                        end = Offset(x, chartBottom),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                fun drawSeries(values: List<Float>, color: androidx.compose.ui.graphics.Color) {
                    val points = values.mapIndexed { i, v -> pointFor(i, v) }
                    val path = Path()
                    points.forEachIndexed { index, point ->
                        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    if (showDots) {
                        points.forEach { p ->
                            drawCircle(color = color, radius = 5.dp.toPx(), center = p)
                            drawCircle(color = surfaceColor, radius = 2.5.dp.toPx(), center = p)
                        }
                    }
                    selectedIndex?.let { index ->
                        drawCircle(color = color, radius = 6.dp.toPx(), center = points[index])
                        drawCircle(color = surfaceColor, radius = 3.dp.toPx(), center = points[index])
                    }
                }

                drawSeries(data.map { it.protein }, proteinColor)
                drawSeries(data.map { it.carbs }, carbsColor)
                drawSeries(data.map { it.fat }, fatColor)

                data.forEachIndexed { index, entry ->
                    if (index in xAxisLabelIndices) {
                        val x = leftPadding + chartWidth * xFractions[index]
                        drawContext.canvas.nativeCanvas.drawText(
                            formatScrubDate(entry.date),
                            x,
                            size.height - 8.dp.toPx(),
                            xAxisPaint
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MacroLegendLine(proteinColor)
            Text("Protein", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            MacroLegendLine(carbsColor)
            Text("Carbs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            MacroLegendLine(fatColor)
            Text("Fat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MacroLegendLine(color: androidx.compose.ui.graphics.Color) {
    Box(modifier = Modifier.size(width = 12.dp, height = 3.dp).background(color, RoundedCornerShape(2.dp)))
    Spacer(Modifier.width(4.dp))
}

@Composable
private fun MacroSummary(data: List<DailyMacros>) {
    val avgProtein = remember(data) { data.map { it.protein }.average().toFloat() }
    val avgCarbs = remember(data) { data.map { it.carbs }.average().toFloat() }
    val avgFat = remember(data) { data.map { it.fat }.average().toFloat() }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Summary", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            SummaryRow("Avg protein", "%.0f g".format(avgProtein))
            SummaryRow("Avg carbs", "%.0f g".format(avgCarbs))
            SummaryRow("Avg fat", "%.0f g".format(avgFat))
            SummaryRow("Days tracked", "${data.size}")
        }
    }
}

@Composable
private fun EmptyChartPlaceholder(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(220.dp).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
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

private fun buildMacroAxisTicks(values: List<Float>): List<Float> {
    if (values.isEmpty()) return emptyList()

    val minValue = floor(values.minOrNull().orZero().toDouble() / 10.0).toFloat() * 10f
    val maxValue = ceil(values.maxOrNull().orZero().toDouble() / 10.0).toFloat() * 10f
    if (minValue == maxValue) {
        return listOf((minValue - 10f).coerceAtLeast(0f), minValue, minValue + 10f).distinct()
    }

    val span = (maxValue - minValue).coerceAtLeast(10f)
    val step = (ceil(span / 30.0).toFloat() * 10f).coerceAtLeast(10f)

    val ticks = mutableListOf<Float>()
    var current = minValue
    while (current <= maxValue) {
        ticks += current
        current += step
    }

    if (ticks.lastOrNull() != maxValue) {
        ticks += maxValue
    }

    return ticks.distinct()
}

private fun formatMacroAxisLabel(value: Float): String {
    return "${value.roundToInt()} g"
}

private fun formatSummaryWeight(weightKg: Float, unit: String, includeSign: Boolean): String {
    val displayValue = if (unit == "lb") weightKg * 2.20462f else weightKg
    val formattedValue = if (includeSign) "%+.2f".format(displayValue) else "%.2f".format(displayValue)
    return "$formattedValue $unit"
}

private fun Float?.orZero(): Float = this ?: 0f
