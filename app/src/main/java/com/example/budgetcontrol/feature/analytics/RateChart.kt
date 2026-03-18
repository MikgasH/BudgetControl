package com.example.budgetcontrol.feature.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.GridLines
import co.yml.charts.ui.linechart.model.Line
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.LineType
import co.yml.charts.ui.linechart.model.ShadowUnderLine
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.data.remote.cerps.dto.RatePoint
import com.example.budgetcontrol.core.data.remote.cerps.dto.TrendsResponse
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

private fun downsamplePoints(
    points: List<RatePoint>,
    maxPoints: Int = 30
): List<RatePoint> {
    if (points.size <= maxPoints) return points
    val step = maxOf(1, points.size / maxPoints)
    val sampled = points.filterIndexed { i, _ -> i % step == 0 }.toMutableList()
    if (sampled.last() != points.last()) {
        sampled.add(points.last())
    }
    return sampled
}

// Chart layout constants (dp) — must match between axisStepSize, Canvas, and gesture
private val CHART_HEIGHT = 310.dp
private val START_DRAW_PAD = 12.dp
private val CONTAINER_PAD_END = 16.dp
private val PLOT_TOP = 30.dp          // LineChartData.paddingTop default
private val PLOT_BOTTOM_INSET = 20.dp // bottomPadding(10) + x-axis(~10)

@Composable
internal fun RateChart(
    trendsData: TrendsResponse,
    onPointSelected: (date: String?, rate: Double) -> Unit
) {
    val allPoints = trendsData.points
    if (allPoints.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.rate_history_no_data),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val points = downsamplePoints(allPoints)
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(points) { selectedIndex = null }

    val lineColor = if (trendsData.changePercentage >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
    val axisLabelColor = MaterialTheme.colorScheme.onSurface
    val axisLineColor = MaterialTheme.colorScheme.outline
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val chartBackgroundColor = MaterialTheme.colorScheme.surface

    val rates = points.map { it.rate.toFloat() }
    val minRate = rates.min()
    val maxRate = rates.max()
    val rateRange = if (maxRate == minRate) 0.001f else (maxRate - minRate) * 0.1f
    val yMin = minRate - rateRange
    val yMax = maxRate + rateRange

    // Dynamically compute Y-axis width to match YCharts internal layout
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val ySteps = 4
    val yAxisLabelPadding = 24.dp   // matches labelAndAxisLinePadding in yAxisData
    val yAxisStartPad = 10.dp       // AxisData default axisStartPadding
    val yLabels = (0..ySteps).map { i ->
        val step = (yMax - yMin) / ySteps
        String.format(Locale.US, "%.4f", yMin + step * i)
    }
    val maxLabelWidthPx = yLabels.maxOf { label ->
        textMeasurer.measure(
            text = label,
            style = TextStyle(fontSize = 12.sp)
        ).size.width
    }
    val yAxisWidthDp = with(density) { maxLabelWidthPx.toDp() } + yAxisStartPad + yAxisLabelPadding

    val pointsData = points.mapIndexed { index, ratePoint ->
        Point(index.toFloat(), ratePoint.rate.toFloat())
    }

    val appLocale = LocalConfiguration.current.locales[0]
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    val inputFormatAlt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val outputFormat = SimpleDateFormat("d MMM", appLocale)

    fun parseAndFormat(timestamp: String): String {
        return try {
            val date = try { inputFormat.parse(timestamp) } catch (_: Exception) { inputFormatAlt.parse(timestamp) }
            date?.let { outputFormat.format(it) } ?: timestamp.take(10)
        } catch (_: Exception) {
            timestamp.take(10)
        }
    }

    val formattedDates = points.map { parseAndFormat(it.timestamp) }
    val firstLabel = formattedDates.first()
    val lastLabel = formattedDates.last()

    // Calculate axisStepSize to fill available width exactly
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val plotDataWidth = screenWidthDp - 32.dp - yAxisWidthDp - START_DRAW_PAD - CONTAINER_PAD_END
    val steps = points.size - 1
    val xAxisStepSize = if (steps > 0) maxOf(1.dp, plotDataWidth / steps) else plotDataWidth

    // X-axis: no labels rendered by chart — we draw our own below
    val xAxisData = AxisData.Builder()
        .axisStepSize(xAxisStepSize)
        .steps(steps)
        .labelData { "" }
        .labelAndAxisLinePadding(4.dp)
        .startDrawPadding(START_DRAW_PAD)
        .axisLabelColor(axisLabelColor)
        .axisLineColor(axisLineColor)
        .axisLabelFontSize(1.sp)
        .build()

    val yAxisData = AxisData.Builder()
        .steps(ySteps)
        .labelData { i ->
            val step = (yMax - yMin) / ySteps
            String.format(Locale.US, "%.4f", yMin + step * i)
        }
        .labelAndAxisLinePadding(24.dp)
        .axisLabelColor(axisLabelColor)
        .axisLineColor(axisLineColor)
        .axisLabelFontSize(12.sp)
        .build()

    val lineChartData = LineChartData(
        linePlotData = LinePlotData(
            lines = listOf(
                Line(
                    dataPoints = pointsData,
                    lineStyle = LineStyle(
                        lineType = LineType.SmoothCurve(isDotted = false),
                        color = lineColor,
                        width = 3f
                    ),
                    intersectionPoint = null,
                    shadowUnderLine = ShadowUnderLine(
                        alpha = 0.3f,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )
                )
            )
        ),
        xAxisData = xAxisData,
        yAxisData = yAxisData,
        gridLines = GridLines(color = surfaceVariantColor),
        backgroundColor = chartBackgroundColor,
        isZoomAllowed = false,
        paddingRight = 0.dp,
        containerPaddingEnd = CONTAINER_PAD_END
    )

    // Pre-compute pixel values for gesture & canvas overlay
    val yAxisWidthPx = with(density) { yAxisWidthDp.toPx() }
    val plotTopPx = with(density) { PLOT_TOP.toPx() }
    val plotBottomInsetPx = with(density) { PLOT_BOTTOM_INSET.toPx() }
    val xAxisStepSizePx = with(density) { xAxisStepSize.toPx() }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CHART_HEIGHT)
            ) {
                // Layer 1: LineChart
                LineChart(
                    modifier = Modifier.fillMaxSize(),
                    lineChartData = lineChartData
                )

                // Layer 2: Canvas overlay — vertical guide line + dot
                val currentSelectedIndex = selectedIndex
                if (currentSelectedIndex != null) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val plotBottomPx = size.height - plotBottomInsetPx
                        val plotHeightPx = plotBottomPx - plotTopPx

                        // X: YCharts places point i at columnWidth + i * axisStepSize
                        val xPx = yAxisWidthPx + currentSelectedIndex * xAxisStepSizePx

                        // Y: YCharts maps [minRate, maxRate] to [plotBottom, plotTop]
                        val rate = points[currentSelectedIndex].rate.toFloat()
                        val yFraction = if (maxRate > minRate) ((rate - minRate) / (maxRate - minRate)).coerceIn(0f, 1f) else 0.5f
                        val yPx = plotBottomPx - yFraction * plotHeightPx

                        // Vertical guide line (dashed)
                        drawLine(
                            color = lineColor.copy(alpha = 0.4f),
                            start = Offset(xPx, plotTopPx),
                            end = Offset(xPx, plotBottomPx),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(8.dp.toPx(), 4.dp.toPx())
                            )
                        )

                        // Outer white circle
                        drawCircle(
                            color = Color.White,
                            radius = 8.dp.toPx(),
                            center = Offset(xPx, yPx)
                        )

                        // Inner colored circle
                        drawCircle(
                            color = lineColor,
                            radius = 5.dp.toPx(),
                            center = Offset(xPx, yPx)
                        )
                    }
                }

                // Layer 3: Gesture overlay — tap & scrub
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(points) {
                            fun selectPoint(x: Float) {
                                if (xAxisStepSizePx <= 0f) return
                                val idx = ((x - yAxisWidthPx) / xAxisStepSizePx)
                                    .roundToInt()
                                    .coerceIn(0, points.size - 1)
                                selectedIndex = idx
                                val date = if (idx == points.size - 1) null else formattedDates[idx]
                                onPointSelected(date, points[idx].rate)
                            }

                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()
                                selectPoint(down.position.x)

                                var event = awaitPointerEvent()
                                while (event.changes.any { it.pressed }) {
                                    event.changes.forEach { change ->
                                        change.consume()
                                        selectPoint(change.position.x)
                                    }
                                    event = awaitPointerEvent()
                                }
                            }
                        }
                )
            }

            // Custom date labels below chart — always fully visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = yAxisWidthDp, end = CONTAINER_PAD_END, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = firstLabel,
                    color = axisLabelColor,
                    fontSize = 12.sp
                )
                Text(
                    text = lastLabel,
                    color = axisLabelColor,
                    fontSize = 12.sp
                )
            }
        }
    }
}
