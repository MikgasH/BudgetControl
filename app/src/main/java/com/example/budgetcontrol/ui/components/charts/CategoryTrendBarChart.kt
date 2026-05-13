package com.example.budgetcontrol.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgetcontrol.core.domain.model.PairedStackedTrendBucket
import com.example.budgetcontrol.core.domain.model.StackedTrendBucket
import com.example.budgetcontrol.core.domain.model.TrendBucket
import com.example.budgetcontrol.core.domain.model.TrendChartData
import com.example.budgetcontrol.core.domain.model.TrendSegment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LIMIT_AMBER = Color(0xFFFFC107)
private val OVER_LIMIT_RED = Color(0xFFF44336)

enum class TrendBarPeriod { DAY, WEEK, MONTH, YEAR }

fun trendLabelFor(periodType: TrendBarPeriod, start: Long): String {
    val locale = Locale.getDefault()
    val format = when (periodType) {
        TrendBarPeriod.DAY -> SimpleDateFormat("d MMM", locale)
        TrendBarPeriod.WEEK -> SimpleDateFormat("d MMM", locale)
        TrendBarPeriod.MONTH -> SimpleDateFormat("MMM", locale)
        TrendBarPeriod.YEAR -> SimpleDateFormat("yyyy", locale)
    }
    return format.format(Date(start))
}

/**
 * Top-level chart entry point. Dispatches to the right renderer based on the [data] variant.
 *
 * Tap behaviour is uniform across variants: a tap inside a bucket's slot reports
 * `(start, end, total)` to [onBucketTapped] so the caller can apply a date-range filter.
 * A tap that lands outside any bar — or on a slot whose total is 0 — invokes
 * [onBackgroundTapped] so the caller can clear that filter (matches the "tap empty area to
 * reset" affordance).
 */
@Composable
fun TrendChart(
    data: TrendChartData,
    onBucketTapped: (start: Long, end: Long, total: Double) -> Unit,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 180.dp,
    onBackgroundTapped: () -> Unit = {}
) {
    when (data) {
        is TrendChartData.Stacked -> StackedBarChart(
            buckets = data.buckets,
            onBucketTapped = onBucketTapped,
            onBackgroundTapped = onBackgroundTapped,
            modifier = modifier,
            chartHeight = chartHeight
        )
        is TrendChartData.PairedStacked -> PairedStackedBarChart(
            buckets = data.buckets,
            onBucketTapped = onBucketTapped,
            onBackgroundTapped = onBackgroundTapped,
            modifier = modifier,
            chartHeight = chartHeight
        )
        is TrendChartData.SingleCategory -> SingleCategoryBarChart(
            buckets = data.buckets,
            categoryColor = data.color,
            limitAmount = data.limitAmount,
            onBucketTapped = onBucketTapped,
            onBackgroundTapped = onBackgroundTapped,
            modifier = modifier,
            chartHeight = chartHeight
        )
        TrendChartData.Empty -> {
            // Caller decides whether to render a placeholder; nothing to draw here.
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────────────
// Shared layout / drawing primitives
// ────────────────────────────────────────────────────────────────────────────────────────

private data class ChartLayout(
    val plotLeft: Float,
    val plotTop: Float,
    val plotRight: Float,
    val plotBottom: Float,
    val plotWidth: Float,
    val plotHeight: Float
)

private fun DrawScope.computeLayout(
    yAxisLabelWidth: Float,
    chartTopPadding: Float,
    xAxisLabelHeight: Float
): ChartLayout {
    val plotRight = size.width
    val plotBottom = size.height - xAxisLabelHeight
    return ChartLayout(
        plotLeft = yAxisLabelWidth,
        plotTop = chartTopPadding,
        plotRight = plotRight,
        plotBottom = plotBottom,
        plotWidth = (plotRight - yAxisLabelWidth).coerceAtLeast(0f),
        plotHeight = (plotBottom - chartTopPadding).coerceAtLeast(0f)
    )
}

private fun DrawScope.drawAxes(
    layout: ChartLayout,
    maxValue: Double,
    paint: android.graphics.Paint,
    labelTextSizePx: Float,
    gridColor: Color
) {
    val maxLabel = String.format(Locale.US, "%.2f", maxValue)
    val minLabel = String.format(Locale.US, "%.2f", 0.0)
    drawContext.canvas.nativeCanvas.drawText(maxLabel, 0f, layout.plotTop + labelTextSizePx, paint)
    drawContext.canvas.nativeCanvas.drawText(minLabel, 0f, layout.plotBottom, paint)
    drawLine(
        color = gridColor,
        start = Offset(layout.plotLeft, layout.plotBottom),
        end = Offset(layout.plotRight, layout.plotBottom),
        strokeWidth = 1f
    )
}

private fun DrawScope.drawXLabel(
    label: String,
    slotLeft: Float,
    slotWidth: Float,
    plotBottom: Float,
    xAxisLabelHeight: Float,
    paint: android.graphics.Paint
) {
    val labelWidth = paint.measureText(label)
    val labelX = slotLeft + (slotWidth - labelWidth) / 2f
    drawContext.canvas.nativeCanvas.drawText(
        label,
        labelX,
        plotBottom + xAxisLabelHeight - 2f,
        paint
    )
}

/**
 * Stacks [segments] from bottom to top within a single bar. Empty stacks render as a
 * 2dp placeholder so an empty bucket reads as "no data" rather than a missing slot.
 */
private fun DrawScope.drawStackedBar(
    segments: List<TrendSegment>,
    barLeft: Float,
    barWidth: Float,
    plotBottom: Float,
    plotHeight: Float,
    maxValue: Double,
    placeholderColor: Color,
    placeholderHeightPx: Float,
    dividerColor: Color,
    dividerHeightPx: Float
) {
    if (segments.isEmpty() || segments.sumOf { it.amount } == 0.0) {
        drawRect(
            color = placeholderColor,
            topLeft = Offset(barLeft, plotBottom - placeholderHeightPx),
            size = Size(barWidth, placeholderHeightPx)
        )
        return
    }
    var cursorY = plotBottom
    segments.forEachIndexed { index, seg ->
        val ratio = (seg.amount / maxValue).toFloat().coerceIn(0f, 1f)
        val h = ratio * plotHeight
        if (h <= 0f) return@forEachIndexed
        drawRect(
            color = seg.color,
            topLeft = Offset(barLeft, cursorY - h),
            size = Size(barWidth, h)
        )
        // Hairline divider between segments improves readability when adjacent segments
        // share a similar hue. Skip on the top segment so we don't paint above the stack.
        if (index < segments.lastIndex) {
            drawRect(
                color = dividerColor,
                topLeft = Offset(barLeft, cursorY - h - dividerHeightPx / 2f),
                size = Size(barWidth, dividerHeightPx)
            )
        }
        cursorY -= h
    }
}

/**
 * Scalar bar (single-category mode). Recolors to red when [bucketTotal] exceeds [limitAmount].
 */
private fun DrawScope.drawScalarBar(
    bucketTotal: Double,
    barLeft: Float,
    barWidth: Float,
    plotBottom: Float,
    plotHeight: Float,
    maxValue: Double,
    placeholderColor: Color,
    placeholderHeightPx: Float,
    normalColor: Color,
    limitAmount: Double?
) {
    if (bucketTotal == 0.0) {
        drawRect(
            color = placeholderColor,
            topLeft = Offset(barLeft, plotBottom - placeholderHeightPx),
            size = Size(barWidth, placeholderHeightPx)
        )
        return
    }
    val ratio = (bucketTotal / maxValue).toFloat().coerceIn(0f, 1f)
    val barHeight = ratio * plotHeight
    val color = if (limitAmount != null && bucketTotal > limitAmount) OVER_LIMIT_RED else normalColor
    drawRect(
        color = color,
        topLeft = Offset(barLeft, plotBottom - barHeight),
        size = Size(barWidth, barHeight)
    )
}

// ────────────────────────────────────────────────────────────────────────────────────────
// StackedBarChart — one stacked bar per bucket
// ────────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun StackedBarChart(
    buckets: List<StackedTrendBucket>,
    onBucketTapped: (start: Long, end: Long, total: Double) -> Unit,
    onBackgroundTapped: () -> Unit,
    modifier: Modifier,
    chartHeight: Dp
) {
    val density = LocalDensity.current
    val labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val dividerColor = MaterialTheme.colorScheme.surface

    val maxValue = remember(buckets) {
        val m = buckets.maxOfOrNull { it.total } ?: 0.0
        if (m <= 0.0) 1.0 else m
    }

    val labelTextSizePx = with(density) { 11.sp.toPx() }
    val yAxisLabelWidth = with(density) { 36.dp.toPx() }
    val xAxisLabelHeight = with(density) { 16.dp.toPx() }
    val chartTopPadding = with(density) { 8.dp.toPx() }
    val placeholderHeightPx = with(density) { 2.dp.toPx() }
    val dividerHeightPx = with(density) { 1.dp.toPx() }

    // (slotRect, start, end, total) — total used to suppress no-op taps on empty buckets.
    val barRects = remember(buckets) {
        mutableListOf<BarRect>()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
            .padding(8.dp)
            .pointerInput(buckets) {
                detectTapGestures { offset ->
                    val hit = barRects.firstOrNull { it.rect.contains(offset) }
                    if (hit != null && hit.total > 0.0) {
                        onBucketTapped(hit.start, hit.end, hit.total)
                    } else {
                        // Tap landed in empty space (gutter, axis margin) or on a slot with
                        // no data — treat as "clear filter" so the user has a way back to the
                        // unfiltered list without re-opening the period picker.
                        onBackgroundTapped()
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            barRects.clear()
            val layout = computeLayout(yAxisLabelWidth, chartTopPadding, xAxisLabelHeight)

            val paint = android.graphics.Paint().apply {
                color = labelTextColor.toArgb()
                textSize = labelTextSizePx
                isAntiAlias = true
            }

            drawAxes(layout, maxValue, paint, labelTextSizePx, gridColor)

            if (buckets.isEmpty()) return@Canvas

            val n = buckets.size
            val slotWidth = layout.plotWidth / n
            val groupWidth = slotWidth * 0.7f
            val groupInset = slotWidth * 0.15f

            buckets.forEachIndexed { i, bucket ->
                val slotLeft = layout.plotLeft + slotWidth * i
                val groupLeft = slotLeft + groupInset

                drawStackedBar(
                    segments = bucket.segments,
                    barLeft = groupLeft,
                    barWidth = groupWidth,
                    plotBottom = layout.plotBottom,
                    plotHeight = layout.plotHeight,
                    maxValue = maxValue,
                    placeholderColor = placeholderColor,
                    placeholderHeightPx = placeholderHeightPx,
                    dividerColor = dividerColor,
                    dividerHeightPx = dividerHeightPx
                )

                barRects.add(
                    BarRect(
                        rect = Rect(
                            offset = Offset(groupLeft, layout.plotTop),
                            size = Size(groupWidth, layout.plotHeight)
                        ),
                        start = bucket.start,
                        end = bucket.end,
                        total = bucket.total
                    )
                )

                drawXLabel(
                    bucket.label,
                    slotLeft,
                    slotWidth,
                    layout.plotBottom,
                    xAxisLabelHeight,
                    paint
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────────────
// PairedStackedBarChart — two stacked bars (expenses left, incomes right) per bucket
// ────────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun PairedStackedBarChart(
    buckets: List<PairedStackedTrendBucket>,
    onBucketTapped: (start: Long, end: Long, total: Double) -> Unit,
    onBackgroundTapped: () -> Unit,
    modifier: Modifier,
    chartHeight: Dp
) {
    val density = LocalDensity.current
    val labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val dividerColor = MaterialTheme.colorScheme.surface

    // Each bar is scaled against the larger of (expenseTotal, incomeTotal) across all
    // buckets — keeps both series comparable while preserving relative bar heights.
    val maxValue = remember(buckets) {
        val maxPaired = buckets.maxOfOrNull { maxOf(it.expenseTotal, it.incomeTotal) } ?: 0.0
        if (maxPaired <= 0.0) 1.0 else maxPaired
    }

    val labelTextSizePx = with(density) { 11.sp.toPx() }
    val yAxisLabelWidth = with(density) { 36.dp.toPx() }
    val xAxisLabelHeight = with(density) { 16.dp.toPx() }
    val chartTopPadding = with(density) { 8.dp.toPx() }
    val placeholderHeightPx = with(density) { 2.dp.toPx() }
    val dividerHeightPx = with(density) { 1.dp.toPx() }

    val barRects = remember(buckets) {
        mutableListOf<BarRect>()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
            .padding(8.dp)
            .pointerInput(buckets) {
                detectTapGestures { offset ->
                    val hit = barRects.firstOrNull { it.rect.contains(offset) }
                    if (hit != null && hit.total > 0.0) {
                        onBucketTapped(hit.start, hit.end, hit.total)
                    } else {
                        // Tap landed in empty space (gutter, axis margin) or on a slot with
                        // no data — treat as "clear filter" so the user has a way back to the
                        // unfiltered list without re-opening the period picker.
                        onBackgroundTapped()
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            barRects.clear()
            val layout = computeLayout(yAxisLabelWidth, chartTopPadding, xAxisLabelHeight)

            val paint = android.graphics.Paint().apply {
                color = labelTextColor.toArgb()
                textSize = labelTextSizePx
                isAntiAlias = true
            }

            drawAxes(layout, maxValue, paint, labelTextSizePx, gridColor)

            if (buckets.isEmpty()) return@Canvas

            val n = buckets.size
            val slotWidth = layout.plotWidth / n
            val groupWidth = slotWidth * 0.7f
            val groupInset = slotWidth * 0.15f
            val barGap = groupWidth * 0.1f
            val barWidth = (groupWidth - barGap) / 2f

            buckets.forEachIndexed { i, bucket ->
                val slotLeft = layout.plotLeft + slotWidth * i
                val groupLeft = slotLeft + groupInset
                val rightBarLeft = groupLeft + barWidth + barGap

                drawStackedBar(
                    segments = bucket.expenseSegments,
                    barLeft = groupLeft,
                    barWidth = barWidth,
                    plotBottom = layout.plotBottom,
                    plotHeight = layout.plotHeight,
                    maxValue = maxValue,
                    placeholderColor = placeholderColor,
                    placeholderHeightPx = placeholderHeightPx,
                    dividerColor = dividerColor,
                    dividerHeightPx = dividerHeightPx
                )
                drawStackedBar(
                    segments = bucket.incomeSegments,
                    barLeft = rightBarLeft,
                    barWidth = barWidth,
                    plotBottom = layout.plotBottom,
                    plotHeight = layout.plotHeight,
                    maxValue = maxValue,
                    placeholderColor = placeholderColor,
                    placeholderHeightPx = placeholderHeightPx,
                    dividerColor = dividerColor,
                    dividerHeightPx = dividerHeightPx
                )

                val total = bucket.expenseTotal + bucket.incomeTotal
                barRects.add(
                    BarRect(
                        rect = Rect(
                            offset = Offset(groupLeft, layout.plotTop),
                            size = Size(groupWidth, layout.plotHeight)
                        ),
                        start = bucket.start,
                        end = bucket.end,
                        total = total
                    )
                )

                drawXLabel(
                    bucket.label,
                    slotLeft,
                    slotWidth,
                    layout.plotBottom,
                    xAxisLabelHeight,
                    paint
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────────────
// SingleCategoryBarChart — scalar bars + optional limit line (preserves prior behaviour)
// ────────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun SingleCategoryBarChart(
    buckets: List<TrendBucket>,
    categoryColor: Color,
    limitAmount: Double?,
    onBucketTapped: (start: Long, end: Long, total: Double) -> Unit,
    onBackgroundTapped: () -> Unit,
    modifier: Modifier,
    chartHeight: Dp
) {
    val density = LocalDensity.current
    val labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant

    // Include limit so the dashed line is always inside the plot area, and use the raw max
    // (no headroom) so the top Y-label matches the limit/data exactly — a 40 limit reads as
    // 40 at the top of the axis, not 42.
    val maxValue = remember(buckets, limitAmount) {
        val datasetMax = buckets.maxOfOrNull { it.total } ?: 0.0
        val withLimit = if (limitAmount != null) maxOf(datasetMax, limitAmount) else datasetMax
        if (withLimit <= 0.0) 1.0 else withLimit
    }

    val labelTextSizePx = with(density) { 11.sp.toPx() }
    val yAxisLabelWidth = with(density) { 36.dp.toPx() }
    val xAxisLabelHeight = with(density) { 16.dp.toPx() }
    val chartTopPadding = with(density) { 8.dp.toPx() }
    val placeholderHeightPx = with(density) { 2.dp.toPx() }
    val limitStrokePx = with(density) { 1.5.dp.toPx() }

    val barRects = remember(buckets) {
        mutableListOf<BarRect>()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
            .padding(8.dp)
            .pointerInput(buckets) {
                detectTapGestures { offset ->
                    val hit = barRects.firstOrNull { it.rect.contains(offset) }
                    if (hit != null && hit.total > 0.0) {
                        onBucketTapped(hit.start, hit.end, hit.total)
                    } else {
                        // Tap landed in empty space (gutter, axis margin) or on a slot with
                        // no data — treat as "clear filter" so the user has a way back to the
                        // unfiltered list without re-opening the period picker.
                        onBackgroundTapped()
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            barRects.clear()
            val layout = computeLayout(yAxisLabelWidth, chartTopPadding, xAxisLabelHeight)

            val paint = android.graphics.Paint().apply {
                color = labelTextColor.toArgb()
                textSize = labelTextSizePx
                isAntiAlias = true
            }

            drawAxes(layout, maxValue, paint, labelTextSizePx, gridColor)

            if (buckets.isEmpty()) return@Canvas

            val n = buckets.size
            val slotWidth = layout.plotWidth / n
            val groupWidth = slotWidth * 0.7f
            val groupInset = slotWidth * 0.15f

            buckets.forEachIndexed { i, bucket ->
                val slotLeft = layout.plotLeft + slotWidth * i
                val groupLeft = slotLeft + groupInset

                drawScalarBar(
                    bucketTotal = bucket.total,
                    barLeft = groupLeft,
                    barWidth = groupWidth,
                    plotBottom = layout.plotBottom,
                    plotHeight = layout.plotHeight,
                    maxValue = maxValue,
                    placeholderColor = placeholderColor,
                    placeholderHeightPx = placeholderHeightPx,
                    normalColor = categoryColor,
                    limitAmount = limitAmount
                )

                barRects.add(
                    BarRect(
                        rect = Rect(
                            offset = Offset(groupLeft, layout.plotTop),
                            size = Size(groupWidth, layout.plotHeight)
                        ),
                        start = bucket.start,
                        end = bucket.end,
                        total = bucket.total
                    )
                )

                drawXLabel(
                    bucket.label,
                    slotLeft,
                    slotWidth,
                    layout.plotBottom,
                    xAxisLabelHeight,
                    paint
                )
            }

            if (limitAmount != null && limitAmount > 0.0 && limitAmount <= maxValue) {
                val ratio = (limitAmount / maxValue).toFloat().coerceIn(0f, 1f)
                val y = layout.plotBottom - ratio * layout.plotHeight
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                val path = Path().apply {
                    moveTo(layout.plotLeft, y)
                    lineTo(layout.plotRight, y)
                }
                drawPath(
                    path = path,
                    color = LIMIT_AMBER,
                    style = Stroke(width = limitStrokePx, pathEffect = dashEffect)
                )
            }
        }
    }
}

private data class BarRect(
    val rect: Rect,
    val start: Long,
    val end: Long,
    val total: Double
)
