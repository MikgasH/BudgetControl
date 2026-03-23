package com.example.budgetcontrol.feature.analytics

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.core.os.ConfigurationCompat
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetcontrol.R
import android.util.Log
import com.example.budgetcontrol.core.data.remote.cerps.dto.RatePoint
import com.example.budgetcontrol.core.data.remote.cerps.dto.TrendsResponse
import com.example.budgetcontrol.core.util.formatAmount
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
                    var selectedDate by remember { mutableStateOf<String?>(null) }
                    var selectedRate by remember { mutableStateOf<Double?>(null) }
                    var amountText by remember { mutableStateOf("100") }

                    Log.d("RateHistory", "Render: period=${data.period}, " +
                            "points=${data.points.size}, dataPoints=${data.dataPoints}, " +
                            "oldRate=${data.oldRate}, newRate=${data.newRate}, " +
                            "change=${data.changePercentage}%, " +
                            "startDate=${data.startDate}, endDate=${data.endDate}")

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

                    // For periods with < 2 data points (e.g. 1D), synthesize a 2-point
                    // line from oldRate→newRate so the chart can still render
                    val chartData = if (data.points.size < 2 && data.oldRate > 0 && data.newRate > 0) {
                        Log.d("RateHistory", "Synthesizing chart: ${data.points.size} points → " +
                                "2 synthetic (${data.oldRate}→${data.newRate})")
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
                        Log.d("RateHistory", "RateChart: rendering ${chartData.points.size} points")
                        RateChart(
                            trendsData = chartData,
                            onPointSelected = { date, rate ->
                                selectedDate = date
                                selectedRate = rate
                            }
                        )
                    } else {
                        Log.d("RateHistory", "InsufficientData: no valid rates to synthesize")
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
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            CurrencyDropdown(
                label = stringResource(R.string.rate_history_from),
                selected = selectedFrom,
                currencies = availableCurrencies,
                favoriteCurrencies = favoriteCurrencies,
                baseCurrency = baseCurrency,
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
                favoriteCurrencies = favoriteCurrencies,
                baseCurrency = baseCurrency,
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
    favoriteCurrencies: Set<String>,
    baseCurrency: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val appLocale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()

    val filteredCurrencies = remember(currencies, searchQuery, appLocale) {
        if (searchQuery.isBlank()) currencies
        else currencies.filter { code ->
            code.contains(searchQuery, ignoreCase = true) ||
                    getCurrencyDisplayName(code, appLocale).contains(searchQuery, ignoreCase = true)
        }
    }

    // Base currency (if it matches the search filter)
    val baseItem = remember(filteredCurrencies, baseCurrency) {
        filteredCurrencies.filter { it == baseCurrency }
    }
    // Favorites excluding base
    val favorites = remember(filteredCurrencies, favoriteCurrencies, baseCurrency) {
        filteredCurrencies.filter { it != baseCurrency && favoriteCurrencies.contains(it) }
    }
    // Everything else
    val others = remember(filteredCurrencies, favoriteCurrencies, baseCurrency) {
        filteredCurrencies.filter { it != baseCurrency && !favoriteCurrencies.contains(it) }
    }

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
            onExpandedChange = {
                expanded = it
                if (!it) searchQuery = ""
            }
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
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
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                    searchQuery = ""
                },
                modifier = Modifier
                    .widthIn(min = 280.dp)
                    .heightIn(max = 400.dp),
                properties = PopupProperties(focusable = true)
            ) {
                BackHandler(enabled = searchQuery.isNotEmpty()) {
                    searchQuery = ""
                }

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_currencies)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                if (filteredCurrencies.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_currencies_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Base currency always first
                        baseItem.forEach { currency ->
                            CurrencyDropdownItem(
                                code = currency,
                                locale = appLocale,
                                suffix = stringResource(R.string.default_label)
                            ) {
                                onSelected(currency)
                                expanded = false
                                searchQuery = ""
                            }
                        }

                        // Favorites section
                        if (favorites.isNotEmpty()) {
                            if (baseItem.isNotEmpty()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    thickness = 0.5.dp
                                )
                            }
                            Text(
                                text = stringResource(R.string.favorite_currencies_header),
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            favorites.forEach { currency ->
                                CurrencyDropdownItem(currency, appLocale) {
                                    onSelected(currency)
                                    expanded = false
                                    searchQuery = ""
                                }
                            }
                        }

                        // All other currencies
                        if (others.isNotEmpty()) {
                            if (baseItem.isNotEmpty() || favorites.isNotEmpty()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    thickness = 0.5.dp
                                )
                            }
                            Text(
                                text = stringResource(R.string.all_currencies_header),
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            others.forEach { currency ->
                                CurrencyDropdownItem(currency, appLocale) {
                                    onSelected(currency)
                                    expanded = false
                                    searchQuery = ""
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyDropdownItem(
    code: String,
    locale: Locale,
    suffix: String? = null,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = code,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.width(52.dp)
                )
                Text(
                    text = getCurrencyDisplayName(code, locale),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (suffix != null) {
                    Text(
                        text = suffix,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    )
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
                            append(formatAmount(convertedAmount))
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
                text = "1 $selectedFrom = ${String.format(Locale.US, "%.4f", trendsData.newRate)} $selectedTo",
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
