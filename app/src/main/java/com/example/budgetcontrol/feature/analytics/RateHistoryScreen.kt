package com.example.budgetcontrol.feature.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                title = { Text(stringResource(R.string.rate_history)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
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
            // Currency pair selector
            CurrencyPairSelector(
                availableCurrencies = availableCurrencies,
                selectedFrom = selectedFrom,
                selectedTo = selectedTo,
                onFromSelected = viewModel::selectFrom,
                onToSelected = viewModel::selectTo
            )

            // Period selector
            PeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = viewModel::selectPeriod
            )

            // Content area
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
                    StatsCard(trendsData = trendsData!!)
                    RateChart(trendsData = trendsData!!)
                }
            }

            // Data source info
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
                onDismissRequest = { expanded = false }
            ) {
                currencies.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency) },
                        onClick = {
                            onSelected(currency)
                            expanded = false
                        }
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
    val selectedIndex = periods.indexOf(selectedPeriod).coerceAtLeast(0)

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 0.dp
    ) {
        periods.forEach { period ->
            Tab(
                selected = period == selectedPeriod,
                onClick = { onPeriodSelected(period) },
                text = {
                    Text(
                        text = period,
                        fontWeight = if (period == selectedPeriod) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

@Composable
private fun StatsCard(trendsData: TrendsResponse) {
    val changeColor = if (trendsData.changePercent >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
    val changePrefix = if (trendsData.changePercent >= 0) "+" else ""

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
            // Start rate
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = stringResource(R.string.rate_history_start_rate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format("%.4f", trendsData.startRate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Change percent
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.rate_history_change),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${changePrefix}${String.format("%.2f", trendsData.changePercent)}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = changeColor
                )
            }

            // End rate
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.rate_history_end_rate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format("%.4f", trendsData.endRate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RateChart(trendsData: TrendsResponse) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    val startRate = trendsData.startRate.toFloat()
    val endRate = trendsData.endRate.toFloat()
    val minRate = minOf(startRate, endRate)
    val maxRate = maxOf(startRate, endRate)
    val padding = if (maxRate == minRate) 0.001f else (maxRate - minRate) * 0.2f

    val pointsData = listOf(
        Point(0f, startRate),
        Point(1f, endRate)
    )

    val xAxisData = AxisData.Builder()
        .axisStepSize(260.dp)
        .steps(1)
        .labelData { i ->
            when (i) {
                0 -> trendsData.startTimestamp.take(10)
                else -> trendsData.endTimestamp.take(10)
            }
        }
        .labelAndAxisLinePadding(16.dp)
        .build()

    val yAxisData = AxisData.Builder()
        .steps(4)
        .labelData { i ->
            val yMin = minRate - padding
            val yMax = maxRate + padding
            val step = (yMax - yMin) / 4
            String.format("%.4f", yMin + step * i)
        }
        .labelAndAxisLinePadding(24.dp)
        .build()

    val lineChartData = LineChartData(
        linePlotData = LinePlotData(
            lines = listOf(
                Line(
                    dataPoints = pointsData,
                    lineStyle = LineStyle(color = primaryColor, width = 3f),
                    intersectionPoint = IntersectionPoint(color = primaryColor, radius = 5.dp),
                    shadowUnderLine = ShadowUnderLine(
                        alpha = 0.3f,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent)
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
