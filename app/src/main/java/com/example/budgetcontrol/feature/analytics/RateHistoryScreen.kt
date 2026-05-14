package com.example.budgetcontrol.feature.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.data.remote.cerps.dto.RatePoint
import com.example.budgetcontrol.core.data.remote.cerps.dto.TrendsResponse
import com.example.budgetcontrol.core.util.PERCENT_FORMAT
import com.example.budgetcontrol.core.util.PERIODS
import com.example.budgetcontrol.core.util.RATE_FORMAT
import com.example.budgetcontrol.core.util.formatAmount
import com.example.budgetcontrol.ui.components.common.CompactCurrencySelector
import java.util.Locale

private fun formatChangePercent(value: Double): String =
    String.format(Locale.US, PERCENT_FORMAT, value)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateHistoryScreen(
    onBackClick: () -> Unit,
    viewModel: RateHistoryViewModel = hiltViewModel()
) {
    val availableCurrencies by viewModel.availableCurrencies.collectAsState()
    val favoriteCurrencies by viewModel.favoriteCurrencies.collectAsState()
    val baseCurrency by viewModel.baseCurrency.collectAsState()
    val selectedFrom by viewModel.selectedFrom.collectAsState()
    val selectedTo by viewModel.selectedTo.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val trendsData by viewModel.trendsData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val sameCurrencyWarning by viewModel.sameCurrencyWarning.collectAsState()

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
                            Icons.AutoMirrored.Filled.ArrowBack,
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
                favoriteCurrencies = favoriteCurrencies,
                baseCurrency = baseCurrency,
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
                sameCurrencyWarning -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.rate_history_same_currency),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
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
                    val scrubState by viewModel.scrubState.collectAsState()
                    var selectedDate by remember { mutableStateOf<String?>(null) }
                    var amountText by remember { mutableStateOf("100") }

                    LaunchedEffect(trendsData) {
                        selectedDate = null
                    }

                    StatsRow(
                        trendsData = data,
                        scrubState = scrubState,
                        selectedDate = selectedDate,
                        selectedFrom = selectedFrom,
                        selectedTo = selectedTo,
                        amountText = amountText,
                        onAmountChange = { newText ->
                            amountText = newText
                            viewModel.onScrubUpdate(
                                rateEnd = scrubState?.rateEnd ?: data.newRate,
                                amount = newText.toDoubleOrNull() ?: 0.0
                            )
                        }
                    )

                    // For periods with < 2 data points (e.g. 1D), synthesize a 2-point
                    // line from oldRate→newRate so the chart can still render
                    val chartData = if (data.points.size < 2 && data.oldRate > 0 && data.newRate > 0) {
                        data.copy(
                            points = listOf(
                                RatePoint(timestamp = data.startDate, rate = data.oldRate),
                                RatePoint(timestamp = data.endDate, rate = data.newRate)
                            )
                        )
                    } else {
                        data
                    }

                    if (chartData.points.size >= 2) {
                        RateChart(
                            trendsData = chartData,
                            onPointSelected = { date, rate ->
                                selectedDate = date
                                viewModel.onScrubUpdate(
                                    rateEnd = rate,
                                    amount = amountText.toDoubleOrNull() ?: 0.0
                                )
                            }
                        )
                    } else {
                        InsufficientDataCard(
                            trendsData = data,
                            selectedFrom = selectedFrom,
                            selectedTo = selectedTo
                        )
                    }
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
    favoriteCurrencies: Set<String>,
    baseCurrency: String,
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            LabeledCurrencySelector(
                label = stringResource(R.string.rate_history_from),
                selected = selectedFrom,
                currencies = availableCurrencies,
                baseCurrency = baseCurrency,
                favoriteCurrencies = favoriteCurrencies,
                onSelected = onFromSelected,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onSwap,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = stringResource(R.string.rate_history_swap),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            LabeledCurrencySelector(
                label = stringResource(R.string.rate_history_to),
                selected = selectedTo,
                currencies = availableCurrencies,
                baseCurrency = baseCurrency,
                favoriteCurrencies = favoriteCurrencies,
                onSelected = onToSelected,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LabeledCurrencySelector(
    label: String,
    selected: String,
    currencies: List<String>,
    baseCurrency: String,
    favoriteCurrencies: Set<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        CompactCurrencySelector(
            selectedCurrency = selected,
            onCurrencySelect = onSelected,
            currencies = currencies,
            baseCurrency = baseCurrency,
            favoriteCurrencies = favoriteCurrencies,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit
) {
    val periods = PERIODS

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
    scrubState: ScrubState?,
    selectedDate: String?,
    selectedFrom: String,
    selectedTo: String,
    amountText: String,
    onAmountChange: (String) -> Unit
) {
    val amount = amountText.toDoubleOrNull() ?: 0.0
    val rateEnd = scrubState?.rateEnd ?: trendsData.newRate
    val rateStart = scrubState?.rateStart ?: trendsData.oldRate
    val nowAmount = amount * rateEnd

    val deltaPercent = if (rateStart > 0) {
        (rateEnd - rateStart) / rateStart * 100.0
    } else {
        trendsData.changePercentage
    }
    val changeColor = when {
        deltaPercent > 0 -> Color(0xFF4CAF50)
        deltaPercent < 0 -> Color(0xFFF44336)
        else -> Color.Gray
    }
    val changePrefix = if (deltaPercent > 0) "+" else ""

    val rateLabel = selectedDate ?: stringResource(R.string.rate_history_current_rate)

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
            // Change percentage (dynamic — from period start to current scrub point)
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
                    text = "$changePrefix${formatChangePercent(deltaPercent)}%",
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

            // Converted value: current amount in target currency
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
                            append(formatAmount(nowAmount))
                        }
                        append(" $selectedTo")
                    },
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun InsufficientDataCard(
    trendsData: TrendsResponse,
    selectedFrom: String,
    selectedTo: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "1 $selectedFrom = ${String.format(Locale.US, RATE_FORMAT, trendsData.newRate)} $selectedTo",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.rate_history_not_enough_data),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.rate_history_updates_interval),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
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
