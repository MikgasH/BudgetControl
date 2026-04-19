package com.example.budgetcontrol.feature.analytics

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.core.os.ConfigurationCompat
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.GridLines
import co.yml.charts.ui.linechart.model.IntersectionPoint
import co.yml.charts.ui.linechart.model.Line
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.LineType
import co.yml.charts.ui.linechart.model.ShadowUnderLine
import com.example.budgetcontrol.core.data.remote.cerps.dto.RatePoint
import com.example.budgetcontrol.core.data.remote.cerps.dto.TrendsResponse
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

// LTTB (Largest-Triangle-Three-Buckets) downsampling.
// Preserves visual shape far better than uniform stride sampling because it selects
// the point in each bucket that forms the largest triangle with its neighbours,
// retaining peaks/troughs that matter visually.
// X and Y are normalised before area computation so the two axes have equal weight.
private fun lttbDownsample(points: List<RatePoint>, threshold: Int): List<RatePoint> {
    val n = points.size
    if (n <= threshold || threshold < 3) return points

    val yMin   = points.minOf { it.rate }
    val yMax   = points.maxOf { it.rate }
    val yScale = if (yMax > yMin) yMax - yMin else 1.0
    val xScale = (n - 1).toDouble()

    fun normX(i: Int)   = i / xScale
    fun normY(r: Double) = (r - yMin) / yScale

    val result = ArrayList<RatePoint>(threshold)
    result.add(points.first())

    val every = (n - 2).toDouble() / (threshold - 2)
    var a = 0                               // index of last selected point

    for (i in 0 until threshold - 2) {
        // Averaged "next" bucket centroid
        val nextStart = ((i + 1) * every + 1).toInt()
        val nextEnd   = ((i + 2) * every + 1).toInt().coerceAtMost(n)
        val nextLen   = nextEnd - nextStart
        var avgNX = 0.0; var avgNY = 0.0
        for (j in nextStart until nextEnd) { avgNX += normX(j); avgNY += normY(points[j].rate) }
        avgNX /= nextLen; avgNY /= nextLen

        // Current bucket: pick the point that maximises triangle area
        val rangeStart = (i * every + 1).toInt()
        val rangeEnd   = ((i + 1) * every + 1).toInt().coerceAtMost(n)
        val aNX = normX(a); val aNY = normY(points[a].rate)
        var maxArea = -1.0; var nextA = rangeStart
        for (j in rangeStart until rangeEnd) {
            val area = kotlin.math.abs(
                (aNX - avgNX) * (normY(points[j].rate) - aNY) -
                (aNX - normX(j)) * (avgNY - aNY)
            ) * 0.5
            if (area > maxArea) { maxArea = area; nextA = j }
        }
        result.add(points[nextA]); a = nextA
    }
    result.add(points.last())
    return result
}

// Catmull-Rom spline interpolation.
// YCharts' SmoothCurve places both cubic control points at the segment midpoint,
// producing an S-step shape that is visually jagged with sparse data.
// Pre-interpolating with Catmull-Rom gives ~100 densely-packed points so
// each S-step spans only a few pixels and the line appears smooth.
// `segments` = number of sub-segments per original pair (≥ 2).
private fun catmullRomInterpolate(
    points: List<RatePoint>,
    segments: Int
): List<RatePoint> {
    if (points.size < 2 || segments <= 1) return points
    val result = mutableListOf<RatePoint>()
    for (i in 0 until points.size - 1) {
        val r0 = if (i > 0) points[i - 1].rate else points[i].rate
        val r1 = points[i].rate
        val r2 = points[i + 1].rate
        val r3 = if (i + 2 < points.size) points[i + 2].rate else points[i + 1].rate
        result.add(points[i])
        for (j in 1 until segments) {
            val t  = j.toDouble() / segments
            val t2 = t * t
            val t3 = t2 * t
            // Standard uniform Catmull-Rom basis (α = 0.5)
            val rate = 0.5 * (
                (-t3 + 2.0 * t2 - t)       * r0 +
                (3.0 * t3 - 5.0 * t2 + 2.0) * r1 +
                (-3.0 * t3 + 4.0 * t2 + t)  * r2 +
                (t3 - t2)                    * r3
            )
            result.add(RatePoint(timestamp = points[i].timestamp, rate = rate))
        }
    }
    result.add(points.last())
    return result
}

// Chart layout constants (dp) — must match between axisStepSize, Canvas, and gesture
private val CHART_HEIGHT = 310.dp
private val START_DRAW_PAD = 12.dp
private val CONTAINER_PAD_END = 16.dp
private val PLOT_TOP = 30.dp          // LineChartData.paddingTop default
// XAxis height: labelHeight(0) + axisLineThickness(2) + indicatorLineWidth(0) + labelAndAxisLinePadding(4) + bottomPadding(10)
private val X_AXIS_HEIGHT = 16.dp

@Composable
internal fun RateChart(
    trendsData: TrendsResponse,
    onPointSelected: (date: String?, rate: Double) -> Unit
) {
    val allPoints = trendsData.points
    Log.d("RateChart", "RateChart called: ${allPoints.size} points, " +
            "period=${trendsData.period}, change=${trendsData.changePercentage}%")
    if (allPoints.size < 2) {
        Log.d("RateChart", "Early return: < 2 points")
        return
    }
    // YCharts' SmoothCurve collapses to invisible degenerate bezier paths when all
    // Y values are identical (flat/weekend rate data). Fall back to a straight line
    // and render a visible yellow overlay so the chart doesn't appear blank.
    val allSame = allPoints.isNotEmpty() &&
            allPoints.all { it.rate == allPoints.first().rate }

    val points = lttbDownsample(allPoints, 100)
    // Target ~100 display points so YCharts' S-curve segments span only a few pixels
    val interpSegments = if (points.size > 1) maxOf(10, 100 / (points.size - 1)) else 1
    val displayPoints = remember(points) { catmullRomInterpolate(points, interpSegments) }
    Log.d("RateChart", "After LTTB: ${points.size} points → ${displayPoints.size} display points, " +
            "first=${points.first().timestamp}/${points.first().rate}, " +
            "last=${points.last().timestamp}/${points.last().rate}")
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(points) { selectedIndex = null }

    val lineColor = when {
        allSame -> Color(0xFFFFD700)
        trendsData.changePercentage >= 0 -> Color(0xFF4CAF50)
        else -> Color(0xFFF44336)
    }
    val axisLabelColor = MaterialTheme.colorScheme.onSurface
    val axisLineColor = MaterialTheme.colorScheme.outline
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val chartBackgroundColor = MaterialTheme.colorScheme.surface

    val rates = points.map { it.rate.toFloat() }
    val minRate = rates.min()
    val maxRate = rates.max()
    val currentRate = rates.last()
    // When the period-wide swing is < 0.1% of the current rate the chart line looks
    // dramatically sloped due to floating-point noise. Pin the Y axis to ±0.5% around
    // the current rate instead so the line renders flat/stable.
    val relativeRange = if (currentRate > 0f) (maxRate - minRate) / currentRate else 0f
    val (yMin, yMax) = if (relativeRange < 0.001f) {
        val halfBand = currentRate * 0.005f
        (currentRate - halfBand) to (currentRate + halfBand)
    } else {
        val rateRange = (maxRate - minRate) * 0.1f
        (minRate - rateRange) to (maxRate + rateRange)
    }

    // YCharts doesn't expose its computed Y-axis width, so we replicate the measurement
    // to correctly position the Canvas overlay and gesture detection on top of the chart
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

    val yRange = yMax - yMin
    // pointsData is built from displayPoints (Catmull-Rom interpolated) for smooth rendering;
    // original `points` are kept separately for gesture snapping and date labels.
    val pointsData = displayPoints.mapIndexed { index, ratePoint ->
        val normalizedY = if (yRange > 0f) {
            ((ratePoint.rate.toFloat() - yMin) / yRange) * ySteps
        } else {
            ySteps / 2f
        }
        Point(index.toFloat(), normalizedY)
    }

    val appLocale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    val inputFormatAlt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun parseTimestamp(timestamp: String): java.util.Date? {
        val clean = timestamp.trimEnd('Z')
        return try { inputFormat.parse(clean) }
        catch (_: Exception) {
            try { inputFormatAlt.parse(clean) }
            catch (_: Exception) { null }
        }
    }

    // Detect if all points fall on the same calendar day (typical for 1D period)
    // to show time-of-day labels instead of date labels
    val allSameDay = run {
        val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val days = points.mapNotNull { parseTimestamp(it.timestamp) }.map { dayFmt.format(it) }.toSet()
        days.size <= 1
    }

    val outputFormat = if (allSameDay) {
        SimpleDateFormat("HH:mm", appLocale)
    } else {
        SimpleDateFormat("d MMM", appLocale)
    }

    fun parseAndFormat(timestamp: String): String {
        return parseTimestamp(timestamp)?.let { outputFormat.format(it) } ?: timestamp.take(10)
    }

    val formattedDates = points.map { parseAndFormat(it.timestamp) }
    val firstLabel = formattedDates.first()
    val lastLabel = formattedDates.last()

    // Calculate axisStepSize to fill available width exactly.
    // Use displayPoints (interpolated) for the step count so YCharts fills the full width.
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val plotDataWidth = screenWidthDp - 32.dp - yAxisWidthDp - START_DRAW_PAD - CONTAINER_PAD_END
    val displaySteps = displayPoints.size - 1
    val xAxisStepSize = if (displaySteps > 0) maxOf(1.dp, plotDataWidth / displaySteps) else plotDataWidth

    // YCharts clips long x-axis labels; we disable them here and render our own Row below the chart
    val xAxisData = AxisData.Builder()
        .axisStepSize(xAxisStepSize)
        .steps(displaySteps)
        .labelData { "" }
        .labelAndAxisLinePadding(4.dp)
        .startDrawPadding(START_DRAW_PAD)
        .axisLabelColor(axisLabelColor)
        .axisLineColor(axisLineColor)
        .axisLabelFontSize(1.sp)
        .indicatorLineWidth(0.dp)
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
                        lineType = if (allSame) LineType.Straight(isDotted = false)
                                   else LineType.SmoothCurve(isDotted = false),
                        color = lineColor,
                        width = 3f
                    ),
                    // Hide YCharts' default intersection dot on flat data — our Canvas
                    // overlay renders the visible yellow dot instead.
                    intersectionPoint = if (allSame) {
                        IntersectionPoint(radius = 0.dp, color = Color.Transparent)
                    } else null,
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
        gridLines = GridLines(color = surfaceVariantColor, enableVerticalLines = false),
        backgroundColor = chartBackgroundColor,
        isZoomAllowed = false,
        paddingRight = 0.dp,
        containerPaddingEnd = CONTAINER_PAD_END
    )

    // Pre-compute pixel values for gesture & canvas overlay
    val yAxisWidthPx = with(density) { yAxisWidthDp.toPx() }
    val plotTopPx = with(density) { PLOT_TOP.toPx() }
    val xAxisHeightPx = with(density) { X_AXIS_HEIGHT.toPx() }
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

                // Layer 1b: Visible overlay when all rates identical — YCharts renders
                // nothing usable for flat data, so draw our own horizontal yellow line.
                // No center dot: the scrub dot is the only interactive dot on the line.
                if (allSame) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val yBottom = size.height - xAxisHeightPx
                        val centerY = plotTopPx + (yBottom - plotTopPx) / 2f
                        val plotStartX = yAxisWidthPx
                        val plotEndX = size.width
                        drawLine(
                            color = Color(0xFFFFD700),
                            start = Offset(plotStartX, centerY),
                            end = Offset(plotEndX, centerY),
                            strokeWidth = 6f
                        )
                    }
                }

                // Layer 2: Canvas overlay — vertical guide line + dot
                val currentSelectedIndex = selectedIndex
                if (currentSelectedIndex != null) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // YCharts: yBottom = canvasHeight - xAxisHeight
                        val yBottom = size.height - xAxisHeightPx
                        val plotHeightPx = yBottom - plotTopPx

                        // X: original point i lives at display index i*interpSegments
                        val xPx = yAxisWidthPx + currentSelectedIndex.toFloat() * interpSegments * xAxisStepSizePx

                        // Y: YCharts maps [yMinData, yMaxData] to [yBottom, paddingTop]
                        val rate = points[currentSelectedIndex].rate.toFloat()
                        val yMinData = pointsData.minOf { it.y }
                        val yMaxData = pointsData.maxOf { it.y }
                        val yDataRange = yMaxData - yMinData
                        val normalizedY = if (yRange > 0f) ((rate - yMin) / yRange) * ySteps else ySteps / 2f
                        val yPx = if (yDataRange > 0f) {
                            yBottom - ((normalizedY - yMinData) / yDataRange) * plotHeightPx
                        } else {
                            yBottom - plotHeightPx / 2f
                        }

                        // Vertical guide line (dashed)
                        drawLine(
                            color = lineColor.copy(alpha = 0.4f),
                            start = Offset(xPx, plotTopPx),
                            end = Offset(xPx, yBottom),
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

                        // Inner colored circle — match flat-line yellow when rates are identical
                        drawCircle(
                            color = if (allSame) Color(0xFFFFD700) else lineColor,
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
                                // Map touch X → display index, then scale back to original point index
                                val displayIdx = ((x - yAxisWidthPx) / xAxisStepSizePx)
                                    .roundToInt()
                                    .coerceIn(0, displayPoints.size - 1)
                                val idx = (displayIdx.toFloat() / interpSegments)
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
