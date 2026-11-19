package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DailyEarningPoint
import com.example.data.RiderEarningsCalculator
import com.example.ui.theme.AmberLight
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

data class SevenDayTrendSummary(
    val totalEarnings: Double,
    val totalDeliveries: Int,
    val totalDistanceKm: Double,
    val dailyAverage: Double,
    val peakDay: DailyEarningPoint?,
    val todayVsYesterdayDelta: Double,
    val hasAnyEarnings: Boolean
)

/**
 * Pure-Kotlin D3-inspired scale & curve mathematics (`scaleLinear().nice()` + `curveMonotoneX`
 * Fritsch-Carlson monotone cubic Hermite tangents) for crisp, non-overshooting line charts.
 */
object EarningsChartScale {

    fun computeNiceAxisMax(rawMax: Double): Double {
        if (!rawMax.isFinite() || rawMax <= 0.0) return 100.0
        val padded = rawMax * 1.15
        val magnitude = 10.0.pow(floor(log10(padded)))
        val normalized = padded / magnitude
        val niceFactor = when {
            normalized <= 1.0 -> 1.0
            normalized <= 1.5 -> 1.5
            normalized <= 2.0 -> 2.0
            normalized <= 2.5 -> 2.5
            normalized <= 3.0 -> 3.0
            normalized <= 4.0 -> 4.0
            normalized <= 5.0 -> 5.0
            normalized <= 6.0 -> 6.0
            normalized <= 8.0 -> 8.0
            else -> 10.0
        }
        val result = niceFactor * magnitude
        return ceil(result / 10.0) * 10.0
    }

    fun computeYAxisTicks(niceMax: Double): List<Double> {
        val safeMax = niceMax.coerceAtLeast(10.0)
        return listOf(safeMax, safeMax / 2.0, 0.0)
    }

    /**
     * Fritsch-Carlson monotone cubic Hermite slopes (identical algorithm to `d3.curveMonotoneX`)
     * for uniformly spaced X coordinates (`dx = 1`), guaranteeing zero overshoot below ₹0.
     */
    fun computeMonotoneTangents(values: List<Float>): List<Float> {
        val n = values.size
        if (n == 0) return emptyList()
        if (n == 1) return listOf(0f)

        val secants = FloatArray(n - 1) { i -> values[i + 1] - values[i] }
        val tangents = FloatArray(n)
        tangents[0] = secants[0]
        tangents[n - 1] = secants[n - 2]

        for (i in 1 until n - 1) {
            val prev = secants[i - 1]
            val next = secants[i]
            tangents[i] = if (prev * next <= 0f) {
                0f
            } else {
                (prev + next) * 0.5f
            }
        }

        for (i in 0 until n - 1) {
            val d = secants[i]
            if (d == 0f) {
                tangents[i] = 0f
                tangents[i + 1] = 0f
            } else {
                val a = tangents[i] / d
                val b = tangents[i + 1] / d
                val h = a * a + b * b
                if (h > 9f) {
                    val tau = 3f / kotlin.math.sqrt(h)
                    tangents[i] = tau * a * d
                    tangents[i + 1] = tau * b * d
                }
            }
        }
        return tangents.toList()
    }

    fun computeTrendSummary(series: List<DailyEarningPoint>): SevenDayTrendSummary {
        if (series.isEmpty()) {
            return SevenDayTrendSummary(
                totalEarnings = 0.0,
                totalDeliveries = 0,
                totalDistanceKm = 0.0,
                dailyAverage = 0.0,
                peakDay = null,
                todayVsYesterdayDelta = 0.0,
                hasAnyEarnings = false
            )
        }
        val totalEarnings = RiderEarningsCalculator.round2(series.sumOf { it.earnings })
        val totalDeliveries = series.sumOf { it.deliveries }
        val totalDistance = RiderEarningsCalculator.round2(series.sumOf { it.distanceKm })
        val dailyAvg = RiderEarningsCalculator.round2(totalEarnings / series.size.toDouble())
        val peak = series.maxByOrNull { it.earnings }?.takeIf { it.earnings > 0.0 }
        val todayEarning = series.last().earnings
        val yesterdayEarning = if (series.size >= 2) series[series.lastIndex - 1].earnings else 0.0
        val delta = RiderEarningsCalculator.round2(todayEarning - yesterdayEarning)
        return SevenDayTrendSummary(
            totalEarnings = totalEarnings,
            totalDeliveries = totalDeliveries,
            totalDistanceKm = totalDistance,
            dailyAverage = dailyAvg,
            peakDay = peak,
            todayVsYesterdayDelta = delta,
            hasAnyEarnings = totalEarnings > 0.0
        )
    }

    fun formatCompactRupees(value: Double): String {
        val rounded = value.roundToInt()
        return when {
            abs(rounded) >= 10_000 -> "₹%.1fk".format(value / 1000.0)
            else -> "₹$rounded"
        }
    }
}

/**
 * Standalone Material 3 Dashboard Card featuring the interactive 7-day smooth monotone line chart,
 * live selected-day inspector, 7-day total & daily trend delta badge.
 */
@Composable
fun DashboardEarningsTrendCard(
    weeklySeries: List<DailyEarningPoint>,
    onOpenEarnings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val summary = remember(weeklySeries) { EarningsChartScale.computeTrendSummary(weeklySeries) }

    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colorScheme.outline, MaterialTheme.shapes.large)
            .testTag("dashboard_earnings_trend_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            colorScheme.surface
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val trendIcon = when {
                        summary.todayVsYesterdayDelta > 0.0 -> Icons.AutoMirrored.Filled.TrendingUp
                        summary.todayVsYesterdayDelta < 0.0 -> Icons.AutoMirrored.Filled.TrendingDown
                        else -> Icons.AutoMirrored.Filled.TrendingFlat
                    }
                    val trendAccent = when {
                        summary.todayVsYesterdayDelta > 0.0 -> EmeraldBright
                        summary.todayVsYesterdayDelta < 0.0 -> AmberWarning
                        else -> SkyInfo
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(trendAccent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = trendIcon,
                            contentDescription = null,
                            tint = trendAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.chart_7day_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.chart_7day_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    onClick = { onOpenEarnings?.invoke() },
                    enabled = onOpenEarnings != null,
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldPrimary.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f)),
                    modifier = Modifier.testTag("dashboard_trend_total_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(R.string.chart_7day_total_label).uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            Text(
                                text = RiderEarningsCalculator.formatRupees(summary.totalEarnings),
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldBright
                            )
                        }
                        if (onOpenEarnings != null) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.cd_open_earnings_details),
                                tint = EmeraldBright,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            DailyEarningsLineChart(
                weeklySeries = weeklySeries,
                summary = summary
            )
        }
    }
}

/**
 * Interactive 7-day smooth monotone spline line & area chart built natively in Jetpack Compose Canvas
 * using D3's `curveMonotoneX` Hermite interpolation and `scaleLinear().nice()` tick scaling.
 */
@Composable
fun DailyEarningsLineChart(
    weeklySeries: List<DailyEarningPoint>,
    summary: SevenDayTrendSummary = remember(weeklySeries) {
        EarningsChartScale.computeTrendSummary(weeklySeries)
    },
    modifier: Modifier = Modifier
) {
    val safeSeries = remember(weeklySeries) {
        if (weeklySeries.isNotEmpty()) weeklySeries else emptyList()
    }
    var selectedIndex by remember(safeSeries.size) {
        mutableIntStateOf((safeSeries.lastIndex).coerceAtLeast(0))
    }
    var chartVisible by remember { mutableStateOf(false) }
    LaunchedEffect(safeSeries) {
        chartVisible = true
    }

    val revealProgress by animateFloatAsState(
        targetValue = if (chartVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "earnings_line_reveal"
    )

    val niceMax = remember(safeSeries) {
        EarningsChartScale.computeNiceAxisMax(safeSeries.maxOfOrNull { it.earnings } ?: 0.0)
    }
    val yTicks = remember(niceMax) { EarningsChartScale.computeYAxisTicks(niceMax) }
    val selectedPoint = safeSeries.getOrNull(selectedIndex)
    val chartDesc = stringResource(R.string.cd_7day_line_chart)
    val avgLabelText = stringResource(R.string.chart_7day_avg_label)
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_earnings_line_chart"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Active Selected-Day Inspector Strip
        if (selectedPoint != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TerminalBg.copy(alpha = 0.78f))
                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp)
                    .testTag("chart_selected_day_banner"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldBright)
                    )
                    Column {
                        Text(
                            text = if (selectedIndex == safeSeries.lastIndex) {
                                "Today"
                            } else {
                                selectedPoint.dayLabel
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${selectedPoint.deliveries} drop${if (selectedPoint.deliveries == 1) "" else "s"} • ${RiderEarningsCalculator.formatKm(selectedPoint.distanceKm)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = RiderEarningsCalculator.formatRupees(selectedPoint.earnings),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldBright,
                        modifier = Modifier.testTag("chart_selected_day_value")
                    )
                    val avgDiff = selectedPoint.earnings - summary.dailyAverage
                    val diffColor = when {
                        !summary.hasAnyEarnings -> TextMuted
                        avgDiff >= 0.0 -> SkyInfo
                        else -> AmberLight
                    }
                    val diffText = when {
                        !summary.hasAnyEarnings -> "$avgLabelText: ₹0"
                        avgDiff >= 0.0 -> "+₹${avgDiff.roundToInt()} vs avg"
                        else -> "-₹${abs(avgDiff).roundToInt()} vs avg"
                    }
                    Text(
                        text = diffText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = diffColor
                    )
                }
            }
        }

        // Native Compose Canvas Line & Area Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(TerminalBg.copy(alpha = 0.72f))
                .border(1.dp, TerminalBorder.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
                .padding(top = 12.dp, bottom = 8.dp, start = 8.dp, end = 12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val axisTextStyle = TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
                val avgTextStyle = TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = SkyInfo
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(152.dp)
                        .semantics { contentDescription = chartDesc }
                        .pointerInput(safeSeries) {
                            if (safeSeries.size <= 1) return@pointerInput
                            val leftGutterPx = 42.dp.toPx()
                            val rightPadPx = 10.dp.toPx()
                            val chartWidth = (size.width - leftGutterPx - rightPadPx).coerceAtLeast(1f)

                            fun snapToX(touchX: Float) {
                                val rel = ((touchX - leftGutterPx) / chartWidth).coerceIn(0f, 1f)
                                val idx = (rel * (safeSeries.size - 1)).roundToInt()
                                    .coerceIn(0, safeSeries.lastIndex)
                                selectedIndex = idx
                            }

                            detectTapGestures { offset -> snapToX(offset.x) }
                        }
                        .pointerInput(safeSeries) {
                            if (safeSeries.size <= 1) return@pointerInput
                            val leftGutterPx = 42.dp.toPx()
                            val rightPadPx = 10.dp.toPx()
                            val chartWidth = (size.width - leftGutterPx - rightPadPx).coerceAtLeast(1f)

                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                val rel = ((change.position.x - leftGutterPx) / chartWidth).coerceIn(0f, 1f)
                                val idx = (rel * (safeSeries.size - 1)).roundToInt()
                                    .coerceIn(0, safeSeries.lastIndex)
                                selectedIndex = idx
                            }
                        }
                        .testTag("earnings_line_chart_canvas")
                ) {
                    if (safeSeries.isEmpty()) return@Canvas

                    val leftGutter = 42.dp.toPx()
                    val rightPad = 10.dp.toPx()
                    val topPad = 12.dp.toPx()
                    val bottomPad = 10.dp.toPx()

                    val plotLeft = leftGutter
                    val plotRight = (size.width - rightPad).coerceAtLeast(plotLeft + 1f)
                    val plotTop = topPad
                    val plotBottom = (size.height - bottomPad).coerceAtLeast(plotTop + 1f)
                    val plotWidth = plotRight - plotLeft
                    val plotHeight = plotBottom - plotTop

                    fun yForValue(v: Double): Float {
                        val frac = (v / niceMax).coerceIn(0.0, 1.0).toFloat()
                        return plotBottom - (frac * plotHeight)
                    }

                    // 1. Horizontal Gridlines + Y-axis rupee labels
                    yTicks.forEach { tick ->
                        val y = yForValue(tick)
                        drawLine(
                            color = TerminalBorder.copy(alpha = if (tick == 0.0) 0.75f else 0.4f),
                            start = Offset(plotLeft, y),
                            end = Offset(plotRight, y),
                            strokeWidth = if (tick == 0.0) 1.2.dp.toPx() else 0.8.dp.toPx()
                        )
                        val label = EarningsChartScale.formatCompactRupees(tick)
                        val measured = textMeasurer.measure(label, style = axisTextStyle)
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(
                                x = (plotLeft - measured.size.width - 6.dp.toPx()).coerceAtLeast(0f),
                                y = (y - measured.size.height / 2f).coerceIn(0f, size.height - measured.size.height)
                            )
                        )
                    }

                    // 2. Dashed 7-day average baseline
                    if (summary.dailyAverage > 0.0) {
                        val avgY = yForValue(summary.dailyAverage)
                        drawLine(
                            color = SkyInfo.copy(alpha = 0.65f),
                            start = Offset(plotLeft, avgY),
                            end = Offset(plotRight, avgY),
                            strokeWidth = 1.1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        )
                        val avgMeasured = textMeasurer.measure(
                            text = "avg ${EarningsChartScale.formatCompactRupees(summary.dailyAverage)}",
                            style = avgTextStyle
                        )
                        drawText(
                            textLayoutResult = avgMeasured,
                            topLeft = Offset(
                                x = (plotRight - avgMeasured.size.width).coerceAtLeast(plotLeft),
                                y = (avgY - avgMeasured.size.height - 2.dp.toPx()).coerceAtLeast(0f)
                            )
                        )
                    }

                    // 3. Compute node coordinates
                    val count = safeSeries.size
                    val stepX = if (count > 1) plotWidth / (count - 1).toFloat() else 0f
                    val points = safeSeries.mapIndexed { idx, p ->
                        val x = if (count > 1) plotLeft + idx * stepX else plotLeft + plotWidth / 2f
                        val y = yForValue(p.earnings)
                        Offset(x, y)
                    }

                    // 4. Build D3-style Monotone Cubic Spline (`curveMonotoneX`)
                    val linePath = Path()
                    val areaPath = Path()

                    if (points.size == 1) {
                        linePath.moveTo(points[0].x, points[0].y)
                        areaPath.moveTo(points[0].x, plotBottom)
                        areaPath.lineTo(points[0].x, points[0].y)
                        areaPath.lineTo(points[0].x, plotBottom)
                        areaPath.close()
                    } else {
                        val yValues = points.map { it.y }
                        val tangents = EarningsChartScale.computeMonotoneTangents(yValues)

                        linePath.moveTo(points[0].x, points[0].y)
                        areaPath.moveTo(points[0].x, plotBottom)
                        areaPath.lineTo(points[0].x, points[0].y)

                        for (i in 0 until points.lastIndex) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val dx = (p1.x - p0.x) / 3f
                            val c1x = p0.x + dx
                            val c1y = (p0.y + tangents[i] / 3f).coerceIn(plotTop, plotBottom)
                            val c2x = p1.x - dx
                            val c2y = (p1.y - tangents[i + 1] / 3f).coerceIn(plotTop, plotBottom)
                            linePath.cubicTo(c1x, c1y, c2x, c2y, p1.x, p1.y)
                            areaPath.cubicTo(c1x, c1y, c2x, c2y, p1.x, p1.y)
                        }
                        areaPath.lineTo(points.last().x, plotBottom)
                        areaPath.close()
                    }

                    // 5. Animated left-to-right clip reveal of area fill + glowing line stroke
                    val revealRight = plotLeft + (plotWidth + 16.dp.toPx()) * revealProgress
                    clipRect(left = plotLeft - 4.dp.toPx(), top = 0f, right = revealRight, bottom = size.height) {
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    EmeraldBright.copy(alpha = 0.30f),
                                    EmeraldPrimary.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                startY = plotTop,
                                endY = plotBottom
                            )
                        )
                        // Subtle outer glow
                        drawPath(
                            path = linePath,
                            color = EmeraldBright.copy(alpha = 0.25f),
                            style = Stroke(
                                width = 6.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                        // Crisp primary line
                        drawPath(
                            path = linePath,
                            brush = Brush.horizontalGradient(
                                colors = listOf(SkyInfo, EmeraldPrimary, EmeraldBright)
                            ),
                            style = Stroke(
                                width = 2.6.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    // 6. Selected-day vertical crosshair & glowing node + passive dots
                    val selIdx = selectedIndex.coerceIn(0, points.lastIndex)
                    val selOffset = points[selIdx]
                    drawLine(
                        color = EmeraldBright.copy(alpha = 0.45f),
                        start = Offset(selOffset.x, plotTop),
                        end = Offset(selOffset.x, plotBottom),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )

                    points.forEachIndexed { idx, pt ->
                        val isSelected = idx == selIdx
                        if (isSelected) {
                            drawCircle(
                                color = EmeraldBright.copy(alpha = 0.25f),
                                radius = 9.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = TerminalBg,
                                radius = 5.2.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = EmeraldBright,
                                radius = 3.8.dp.toPx(),
                                center = pt
                            )
                        } else {
                            val hasValue = safeSeries[idx].earnings > 0.0
                            drawCircle(
                                color = TerminalBg,
                                radius = 3.6.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = if (hasValue) SkyInfo else TerminalBorder,
                                radius = 2.5.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }

                // 7. Interactive X-Axis Day Labels Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 34.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    safeSeries.forEachIndexed { idx, point ->
                        val isSelected = idx == selectedIndex
                        val isToday = idx == safeSeries.lastIndex
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) EmeraldPrimary.copy(alpha = 0.22f) else Color.Transparent
                                )
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) EmeraldPrimary.copy(alpha = 0.65f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedIndex = idx }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("chart_day_tab_$idx"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = point.dayLabel,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                color = when {
                                    isSelected -> EmeraldBright
                                    isToday -> SkyInfo
                                    else -> TextMuted
                                },
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Compact Footer Metrics / Empty-State Tip
        if (!summary.hasAnyEarnings) {
            Text(
                text = stringResource(R.string.chart_7day_empty_hint),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$avgLabelText: ${RiderEarningsCalculator.formatRupees(summary.dailyAverage)}/day",
                    style = MaterialTheme.typography.labelSmall,
                    color = SkyInfo
                )
                val peak = summary.peakDay
                if (peak != null) {
                    Text(
                        text = "Peak: ${peak.dayLabel} (${RiderEarningsCalculator.formatRupees(peak.earnings)})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldBright
                    )
                }
            }
        }
    }
}
