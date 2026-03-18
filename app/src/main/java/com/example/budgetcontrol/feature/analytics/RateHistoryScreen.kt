package com.example.budgetcontrol.feature.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToInt

private fun getCurrencyDisplayName(code: String, locale: Locale): String =
    try { Currency.getInstance(code).getDisplayName(locale) }
    catch (_: IllegalArgumentException) { code }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateHistoryScreen(
    onBackClick: () -> Unit,
    viewModel: RateHistoryViewModel = hiltViewModel()
) {
    val availableCurrencies by viewModel.availableCurrencies.collectAsState()
    val selectedFrom by viewModel.selectedFrom.collectAsState()
    val selectedTo by viewModel.selectedTo.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val trendsData by viewModel.trendsData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.rate_history_screen_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CurrencyPairSelector(
                availableCurrencies = availableCurrencies,
                selectedFrom = selectedFrom,
                selectedTo = selectedTo,
                onFromSelected = viewModel::selectFrom,
                onToSelected = viewModel::selectTo,
                onSwap = viewModel::swapCurrencies
            )

            PeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = viewModel::selectPeriod
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                error != null -> {
                    ErrorContent(
                        error = error ?: "",
                        onRetry = viewModel::retry
                    )
                }
                trendsData != null -> {
                    val data = trendsData ?: return@Column
                    var selectedDate by remember { mutableStateOf<String?>(null) }
                    var selectedRate by remember { mutableStateOf<Double?>(null) }
                    var amountText by remember { mutableStateOf("100") }

                    LaunchedEffect(trendsData) {
                        selectedDate = null
                        selectedRate = null
                    }

                    StatsRow(
                        trendsData = data,
                        selectedDate = selectedDate,
                        selectedRate = selectedRate,
                        selectedFrom = selectedFrom,
                        selectedTo = selectedTo,
                        amountText = amountText,
                        onAmountChange = { amountText = it }
                    )
                    RateChart(
                        trendsData = data,
                        onPointSelected = { date, rate ->
                            selectedDate = date
                            selectedRate = rate
                        }
                    )
                }
            }

            Text(
                text = stringResource(R.string.rate_history_data_source),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CurrencyPairSelector(
    availableCurrencies: List<String>,
    selectedFrom: String,
    selectedTo: String,
    onFromSelected: (String) -> Unit,
    onToSelected: (String) -> Unit,
    onSwap: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            CurrencyDropdown(
                label = stringResource(R.string.rate_history_from),
                selected = selectedFrom,
                currencies = availableCurrencies,
                onSelected = onFromSelected,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier.align(Alignment.Bottom),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onSwap) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = stringResource(R.string.rate_history_swap),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            CurrencyDropdown(
                label = stringResource(R.string.rate_history_to),
                selected = selectedTo,
                currencies = availableCurrencies,
                onSelected = onToSelected,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(
    label: String,
    selected: String,
    currencies: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val appLocale = LocalConfiguration.current.locales[0]

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .widthIn(min = 280.dp)
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                currencies.forEach { currency ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currency,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.width(52.dp)
                                )
                                Text(
                                    text = getCurrencyDisplayName(currency, appLocale),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        onClick = {
                            onSelected(currency)
                            expanded = false
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit
) {
    val periods = RateHistoryViewModel.PERIODS

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        periods.forEach { period ->
            val isSelected = period == selectedPeriod

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPeriodSelected(period) },
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = period,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun StatsRow(
    trendsData: TrendsResponse,
    selectedDate: String?,
    selectedRate: Double?,
    selectedFrom: String,
    selectedTo: String,
    amountText: String,
    onAmountChange: (String) -> Unit
) {
    val changeColor = if (trendsData.changePercentage >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
    val changePrefix = if (trendsData.changePercentage >= 0) "+" else ""

    val rateLabel = selectedDate ?: stringResource(R.string.rate_history_current_rate)
    val rateValue = selectedRate ?: trendsData.newRate
    val amount = amountText.toDoubleOrNull() ?: 0.0
    val convertedAmount = amount * rateValue

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Change percentage
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.rate_history_change),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${changePrefix}${String.format(Locale.US, "%.2f", trendsData.changePercentage)}%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = changeColor,
                    maxLines = 1
                )
            }

            // Amount input
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.rate_history_amount),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    BasicTextField(
                        value = amountText,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                onAmountChange(newValue)
                            }
                        },
                        modifier = Modifier.width(56.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Column {
                                innerTextField()
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline,
                                    thickness = 1.dp
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = selectedFrom,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Converted rate
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = rateLabel,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(String.format(Locale.US, "%.2f", convertedAmount))
                        }
                        append(" $selectedTo")
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

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
private fun RateChart(
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

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            FilledTonalButton(onClick = onRetry) {
                Text(stringResource(R.string.rate_history_retry))
            }
        }
    }
}
