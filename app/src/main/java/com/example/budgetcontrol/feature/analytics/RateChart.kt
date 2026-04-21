package com.example.budgetcontrol.feature.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.data.remote.cerps.dto.RatePoint
import com.example.budgetcontrol.core.data.remote.cerps.dto.TrendsResponse
import com.example.budgetcontrol.core.util.FLAT_REL_THRESHOLD
import com.example.budgetcontrol.core.util.LTTB_THRESHOLD
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

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

// Monotone cubic Hermite interpolation (Fritsch-Carlson / PCHIP).
// Unlike uniform Catmull-Rom, this spline provably never overshoots: if the source
// data has no extremum between two knots, neither does the curve. That eliminates
// the artificial dips Catmull-Rom produced when LTTB picked samples with steep slope
// changes. Builds one cubic Bezier per segment with control points at ±Δx/3.
private fun buildMonotonePath(xs: FloatArray, ys: FloatArray): Path {
    val path = Path()
    val n = xs.size
    if (n == 0) return path
    path.moveTo(xs[0], ys[0])
    if (n == 1) return path
    if (n == 2) {
        path.lineTo(xs[1], ys[1])
        return path
    }

    val dx = FloatArray(n - 1)
    val d  = FloatArray(n - 1)
    for (k in 0 until n - 1) {
        dx[k] = xs[k + 1] - xs[k]
        d[k]  = if (dx[k] != 0f) (ys[k + 1] - ys[k]) / dx[k] else 0f
    }

    // Initial tangents: central differences with one-sided endpoints
    val m = FloatArray(n)
    m[0]     = d[0]
    m[n - 1] = d[n - 2]
    for (k in 1 until n - 1) m[k] = (d[k - 1] + d[k]) / 2f

    // Monotonicity clamp: flat segments zero their endpoint tangents; elsewhere,
    // if (α, β) escapes the unit circle of radius 3, rescale so the segment stays monotone.
    for (k in 0 until n - 1) {
        if (d[k] == 0f) {
            m[k]     = 0f
            m[k + 1] = 0f
        } else {
            val alpha = m[k]     / d[k]
            val beta  = m[k + 1] / d[k]
            val mag   = alpha * alpha + beta * beta
            if (mag > 9f) {
                val tau = 3f / sqrt(mag)
                m[k]     = tau * alpha * d[k]
                m[k + 1] = tau * beta  * d[k]
            }
        }
    }

    for (k in 0 until n - 1) {
        val h = dx[k]
        path.cubicTo(
            xs[k]     + h / 3f, ys[k]     + m[k]     * h / 3f,
            xs[k + 1] - h / 3f, ys[k + 1] - m[k + 1] * h / 3f,
            xs[k + 1],          ys[k + 1]
        )
    }
    return path
}

private val CHART_HEIGHT         = 310.dp
private val X_AXIS_LABEL_HEIGHT  = 20.dp
private val Y_AXIS_LABEL_END_PAD = 8.dp

@Composable
internal fun RateChart(
    trendsData: TrendsResponse,
    onPointSelected: (date: String?, rate: Double) -> Unit
) {
    val allPoints = trendsData.points
    if (allPoints.size < 2) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CHART_HEIGHT),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.rate_history_not_enough_data),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // Flat data (weekends/holidays) only drives the colour now — PCHIP renders a
    // true horizontal line for identical y-values without any overlay hack.
    val allSame = allPoints.all { it.rate == allPoints.first().rate }

    val points = remember(allPoints) { lttbDownsample(allPoints, LTTB_THRESHOLD) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(points) { selectedIndex = null }

    val lineColor = when {
        allSame -> Color(0xFFFFD700)
        trendsData.changePercentage >= 0 -> Color(0xFF4CAF50)
        else -> Color(0xFFF44336)
    }
    val axisLabelColor = MaterialTheme.colorScheme.onSurface
    val gridColor = MaterialTheme.colorScheme.surfaceVariant

    val rates = points.map { it.rate.toFloat() }
    val minRate = rates.min()
    val maxRate = rates.max()
    val currentRate = rates.last()
    // When the period-wide swing is < 0.1% of the current rate the line looks
    // dramatically sloped due to floating-point noise. Pin the Y axis to ±0.5% around
    // the current rate instead so the line renders flat/stable.
    val relativeRange = if (currentRate > 0f) (maxRate - minRate) / currentRate else 0f
    val (yMin, yMax) = if (relativeRange < FLAT_REL_THRESHOLD) {
        val halfBand = currentRate * 0.005f
        (currentRate - halfBand) to (currentRate + halfBand)
    } else {
        val pad = (maxRate - minRate) * 0.1f
        (minRate - pad) to (maxRate + pad)
    }
    val yRange = yMax - yMin

    val ySteps = 4
    val yLabels = (0..ySteps).map { i ->
        // i = 0 is the top label (yMax); i = ySteps is the bottom label (yMin)
        String.format(Locale.US, "%.4f", yMax - (yMax - yMin) * i / ySteps)
    }

    val appLocale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    val inputFormat = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US) }
    val inputFormatAlt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

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
    val allSameDay = remember(points) {
        val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val days = points.mapNotNull { parseTimestamp(it.timestamp) }.map { dayFmt.format(it) }.toSet()
        days.size <= 1
    }
    val outputFormat = remember(allSameDay, appLocale) {
        if (allSameDay) {
            SimpleDateFormat("HH:mm", appLocale)
        } else {
            SimpleDateFormat("d MMM", appLocale)
        }
    }

    fun parseAndFormat(timestamp: String): String {
        return parseTimestamp(timestamp)?.let { outputFormat.format(it) } ?: timestamp.take(10)
    }

    val formattedDates = points.map { parseAndFormat(it.timestamp) }
    val firstLabel = formattedDates.first()
    val lastLabel = formattedDates.last()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT)
                .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 4.dp)
        ) {
            // Y-axis labels: the Column wraps to its widest label (intrinsic width) and
            // Arrangement.SpaceBetween pins the first/last label to the plot's top/bottom
            // edges — no manual text measurement needed.
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(bottom = X_AXIS_LABEL_HEIGHT, end = Y_AXIS_LABEL_END_PAD),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                yLabels.forEach { label ->
                    Text(
                        text = label,
                        color = axisLabelColor,
                        fontSize = 12.sp
                    )
                }
            }

            // Plot area + X-axis labels share the weight(1f) column so the labels
            // line up with the Canvas' left/right edges without extra math.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(points) {
                        val n = points.size
                        fun select(x: Float) {
                            val w = size.width.toFloat()
                            if (w <= 0f) return
                            val idx = ((x / w).coerceIn(0f, 1f) * (n - 1))
                                .roundToInt()
                                .coerceIn(0, n - 1)
                            selectedIndex = idx
                            val date = if (idx == n - 1) null else formattedDates[idx]
                            onPointSelected(date, points[idx].rate)
                        }
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            select(down.position.x)
                            var event = awaitPointerEvent()
                            while (event.changes.any { it.pressed }) {
                                event.changes.forEach { change ->
                                    change.consume()
                                    select(change.position.x)
                                }
                                event = awaitPointerEvent()
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val xAxisPx    = X_AXIS_LABEL_HEIGHT.toPx()
                    val plotTop    = 0f
                    val plotBottom = size.height - xAxisPx
                    val plotWidth  = size.width
                    val plotHeight = plotBottom - plotTop
                    if (plotHeight <= 0f || plotWidth <= 0f) return@Canvas

                    val n = points.size
                    val xs = FloatArray(n)
                    val ys = FloatArray(n)
                    for (i in 0 until n) {
                        xs[i] = if (n == 1) plotWidth / 2f else i.toFloat() / (n - 1) * plotWidth
                        val r = points[i].rate.toFloat()
                        ys[i] = if (yRange > 0f) {
                            plotBottom - ((r - yMin) / yRange) * plotHeight
                        } else {
                            plotTop + plotHeight / 2f
                        }
                    }

                    // Grid: ySteps + 1 horizontal lines matching the Y-axis labels.
                    val gridStroke = 1.dp.toPx()
                    for (i in 0..ySteps) {
                        val y = plotTop + plotHeight * i / ySteps
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end   = Offset(plotWidth, y),
                            strokeWidth = gridStroke
                        )
                    }

                    val linePath = buildMonotonePath(xs, ys)

                    // Area fill under the line: clone the line and close down to plotBottom.
                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(xs[n - 1], plotBottom)
                        lineTo(xs[0],     plotBottom)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent),
                            startY = plotTop,
                            endY   = plotBottom
                        )
                    )

                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    val idx = selectedIndex
                    if (idx != null && idx in 0 until n) {
                        val xPx = xs[idx]
                        val yPx = ys[idx]
                        drawLine(
                            color = lineColor.copy(alpha = 0.4f),
                            start = Offset(xPx, plotTop),
                            end   = Offset(xPx, plotBottom),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(8.dp.toPx(), 4.dp.toPx())
                            )
                        )
                        drawCircle(
                            color  = Color.White,
                            radius = 8.dp.toPx(),
                            center = Offset(xPx, yPx)
                        )
                        drawCircle(
                            color  = lineColor,
                            radius = 5.dp.toPx(),
                            center = Offset(xPx, yPx)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(X_AXIS_LABEL_HEIGHT)
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
}
