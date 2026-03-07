package com.example.budgetcontrol.feature.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.GridLines
import co.yml.charts.ui.linechart.model.IntersectionPoint
import co.yml.charts.ui.linechart.model.Line
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.ShadowUnderLine
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.data.remote.cerps.dto.TrendsResponse
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Locale

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
                        text = "$selectedFrom \u2192 $selectedTo",
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
                onToSelected = viewModel::selectTo
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
                        error = error!!,
                        onRetry = viewModel::retry
                    )
                }
                trendsData != null -> {
                    StatsRow(trendsData = trendsData!!)
                    RateChart(trendsData = trendsData!!)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPairSelector(
    availableCurrencies: List<String>,
    selectedFrom: String,
    selectedTo: String,
    onFromSelected: (String) -> Unit,
    onToSelected: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurrencyDropdown(
                label = stringResource(R.string.rate_history_from),
                selected = selectedFrom,
                currencies = availableCurrencies,
                onSelected = onFromSelected,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "\u2192",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
            style = MaterialTheme.typography.labelSmall,
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
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                currencies.forEach { currency ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currency,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = getCurrencyDisplayName(currency, appLocale),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun StatsRow(trendsData: TrendsResponse) {
    val changeColor = if (trendsData.changePercentage >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
    val changePrefix = if (trendsData.changePercentage >= 0) "+" else ""

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Change percentage
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = stringResource(R.string.rate_history_change),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${changePrefix}${String.format("%.2f", trendsData.changePercentage)}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = changeColor
                )
            }

            // Current rate
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.rate_history_current_rate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format("%.4f", trendsData.newRate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Data points
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.rate_history_period),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.rate_history_points, trendsData.dataPoints),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RateChart(trendsData: TrendsResponse) {
    val points = trendsData.points
    if (points.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
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

    val lineColor = if (trendsData.changePercentage >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    val rates = points.map { it.rate.toFloat() }
    val minRate = rates.min()
    val maxRate = rates.max()
    val padding = if (maxRate == minRate) 0.001f else (maxRate - minRate) * 0.1f

    val pointsData = points.mapIndexed { index, ratePoint ->
        Point(index.toFloat(), ratePoint.rate.toFloat())
    }

    // Format first and last date labels based on locale
    val appLocale = LocalConfiguration.current.locales[0]
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    val inputFormatAlt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val outputFormat = SimpleDateFormat("MMM d", appLocale)

    fun parseAndFormat(timestamp: String): String {
        return try {
            val date = try { inputFormat.parse(timestamp) } catch (_: Exception) { inputFormatAlt.parse(timestamp) }
            date?.let { outputFormat.format(it) } ?: timestamp.take(10)
        } catch (_: Exception) {
            timestamp.take(10)
        }
    }

    val firstLabel = parseAndFormat(points.first().timestamp)
    val lastLabel = parseAndFormat(points.last().timestamp)

    val steps = points.size - 1
    val xAxisData = AxisData.Builder()
        .axisStepSize(if (steps > 0) (260.dp / steps) else 260.dp)
        .steps(steps)
        .labelData { i ->
            when (i) {
                0 -> firstLabel
                steps -> lastLabel
                else -> ""
            }
        }
        .labelAndAxisLinePadding(16.dp)
        .build()

    val ySteps = 4
    val yAxisData = AxisData.Builder()
        .steps(ySteps)
        .labelData { i ->
            val yMin = minRate - padding
            val yMax = maxRate + padding
            val step = (yMax - yMin) / ySteps
            String.format("%.4f", yMin + step * i)
        }
        .labelAndAxisLinePadding(24.dp)
        .build()

    val lineChartData = LineChartData(
        linePlotData = LinePlotData(
            lines = listOf(
                Line(
                    dataPoints = pointsData,
                    lineStyle = LineStyle(color = lineColor, width = 3f),
                    intersectionPoint = IntersectionPoint(color = lineColor, radius = 3.dp),
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
        backgroundColor = Color.Transparent
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        LineChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .padding(8.dp),
            lineChartData = lineChartData
        )
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
