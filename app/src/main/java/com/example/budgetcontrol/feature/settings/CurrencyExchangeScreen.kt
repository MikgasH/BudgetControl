package com.example.budgetcontrol.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.CurrencyExchange
import com.example.budgetcontrol.ui.components.common.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.Date
import com.example.budgetcontrol.core.util.RATE_FORMAT
import com.example.budgetcontrol.core.util.formatAmount
import com.example.budgetcontrol.core.util.toDoubleLocale
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyExchangeScreen(
    onBackClick: () -> Unit,
    viewModel: CurrencyExchangeViewModel = hiltViewModel()
) {
    val exchanges by viewModel.exchanges.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val availableCurrencies by viewModel.availableCurrencies.collectAsState()
    val baseCurrency by viewModel.baseCurrency.collectAsState()
    val favoriteCurrencies by viewModel.favoriteCurrencies.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = formState.date,
            onDateSelected = { date ->
                viewModel.updateDate(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.currency_exchanges),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Description text
            item {
                Text(
                    text = stringResource(R.string.currency_exchange_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Add exchange form
            item {
                AddExchangeForm(
                    formState = formState,
                    availableCurrencies = availableCurrencies,
                    baseCurrency = baseCurrency,
                    favoriteCurrencies = favoriteCurrencies,
                    onFromAmountChange = viewModel::updateFromAmount,
                    onFromCurrencyChange = viewModel::updateFromCurrency,
                    onToAmountChange = viewModel::updateToAmount,
                    onToCurrencyChange = viewModel::updateToCurrency,
                    onDescriptionChange = viewModel::updateDescription,
                    onDateClick = { showDatePicker = true },
                    onSave = viewModel::saveExchange
                )
            }

            // Exchange history header
            item {
                Text(
                    text = stringResource(R.string.exchange_history),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (exchanges.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.no_exchanges),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(exchanges, key = { it.id }) { exchange ->
                    ExchangeHistoryItem(
                        exchange = exchange,
                        onDelete = { viewModel.deleteExchange(exchange.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddExchangeForm(
    formState: ExchangeFormState,
    availableCurrencies: List<String>,
    baseCurrency: String,
    favoriteCurrencies: Set<String>,
    onFromAmountChange: (String) -> Unit,
    onFromCurrencyChange: (String) -> Unit,
    onToAmountChange: (String) -> Unit,
    onToCurrencyChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDateClick: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.add_exchange),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // From: amount + currency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = formState.fromAmount,
                    onValueChange = onFromAmountChange,
                    label = { Text(stringResource(R.string.exchange_from)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                CompactCurrencySelector(
                    selectedCurrency = formState.fromCurrency,
                    onCurrencySelect = onFromCurrencyChange,
                    currencies = availableCurrencies,
                    baseCurrency = baseCurrency,
                    favoriteCurrencies = favoriteCurrencies,
                    modifier = Modifier.width(110.dp)
                )
            }

            // To: amount + currency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = formState.toAmount,
                    onValueChange = onToAmountChange,
                    label = { Text(stringResource(R.string.exchange_to)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                CompactCurrencySelector(
                    selectedCurrency = formState.toCurrency,
                    onCurrencySelect = onToCurrencyChange,
                    currencies = availableCurrencies,
                    baseCurrency = baseCurrency,
                    favoriteCurrencies = favoriteCurrencies,
                    modifier = Modifier.width(110.dp)
                )
            }

            // Rate display
            val fromAmount = formState.fromAmount.toDoubleLocale()
            val toAmount = formState.toAmount.toDoubleLocale()
            if (fromAmount != null && fromAmount > 0 && toAmount != null && toAmount > 0) {
                val rate = toAmount / fromAmount
                val rateFormatted = String.format(Locale.US, RATE_FORMAT, rate)
                Text(
                    text = "${stringResource(R.string.exchange_rate)}: $rateFormatted ${formState.toCurrency} per ${formState.fromCurrency}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Description
            OutlinedTextField(
                value = formState.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.exchange_description)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            // Date
            val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
            OutlinedButton(
                onClick = onDateClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "${stringResource(R.string.date_label)}: ${dateFormat.format(Date(formState.date))}"
                )
            }

            // Error
            formState.error?.let {
                Text(
                    text = stringResource(R.string.exchange_amount_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Save button
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = stringResource(R.string.save_exchange),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ExchangeHistoryItem(
    exchange: CurrencyExchange,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${formatAmount(exchange.fromAmount)} ${exchange.fromCurrency} → ${formatAmount(exchange.toAmount)} ${exchange.toCurrency}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${stringResource(R.string.exchange_rate)}: ${String.format(Locale.US, RATE_FORMAT, exchange.exchangeRate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = dateFormat.format(Date(exchange.date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    exchange.location?.let { loc ->
                        Text(
                            text = "· $loc",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_action),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Compact "code + arrow" trigger sized to sit next to an OutlinedTextField on the same row.
 * Opens a search-enabled ModalBottomSheet — the full currency list lives there, not in this trigger.
 */
@Composable
private fun CompactCurrencySelector(
    selectedCurrency: String,
    onCurrencySelect: (String) -> Unit,
    currencies: List<String>,
    baseCurrency: String,
    favoriteCurrencies: Set<String>,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }

    // Transparent background lets the form Card's surfaceVariant show through —
    // matches OutlinedTextField's default container so the selector sits flush
    // with the amount field next to it. Border / shape / height already mirror
    // an unfocused OutlinedTextField (1.dp outline, RoundedCornerShape(12.dp), 56.dp).
    Surface(
        onClick = { showSheet = true },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.height(56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = selectedCurrency,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showSheet) {
        CurrencyPickerBottomSheet(
            currencies = currencies,
            baseCurrency = baseCurrency,
            favoriteCurrencies = favoriteCurrencies,
            onSelect = {
                onCurrencySelect(it)
                showSheet = false
            },
            onDismiss = { showSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPickerBottomSheet(
    currencies: List<String>,
    baseCurrency: String,
    favoriteCurrencies: Set<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val appLocale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    var query by remember { mutableStateOf("") }

    val filtered = remember(currencies, query, appLocale) {
        if (query.isBlank()) currencies
        else currencies.filter { code ->
            code.contains(query, ignoreCase = true) ||
                getCurrencyDisplayName(code, appLocale).contains(query, ignoreCase = true)
        }
    }
    val baseInFiltered = baseCurrency in filtered
    val favorites = filtered.filter { it != baseCurrency && it in favoriteCurrencies }
    val others = filtered.filter { it != baseCurrency && it !in favoriteCurrencies }

    fun pick(code: String) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onSelect(code)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_currencies)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                if (baseInFiltered) {
                    item(key = "base-$baseCurrency") {
                        CurrencyPickerRow(
                            code = baseCurrency,
                            displayName = getCurrencyDisplayName(baseCurrency, appLocale),
                            isBase = true,
                            onClick = { pick(baseCurrency) }
                        )
                    }
                }
                if (favorites.isNotEmpty()) {
                    item(key = "fav-header") {
                        CurrencyPickerSectionHeader(stringResource(R.string.favorite_currencies_header))
                    }
                    items(favorites, key = { "fav-$it" }) { code ->
                        CurrencyPickerRow(
                            code = code,
                            displayName = getCurrencyDisplayName(code, appLocale),
                            onClick = { pick(code) }
                        )
                    }
                }
                if (others.isNotEmpty()) {
                    item(key = "all-header") {
                        CurrencyPickerSectionHeader(stringResource(R.string.all_currencies_header))
                    }
                    items(others, key = { "all-$it" }) { code ->
                        CurrencyPickerRow(
                            code = code,
                            displayName = getCurrencyDisplayName(code, appLocale),
                            onClick = { pick(code) }
                        )
                    }
                }
            }
        }
    }

}

@Composable
private fun CurrencyPickerSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun CurrencyPickerRow(
    code: String,
    displayName: String,
    onClick: () -> Unit,
    isBase: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = code,
            fontWeight = if (isBase) FontWeight.Bold else FontWeight.Medium,
            fontSize = 16.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = displayName,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isBase) {
                Text(
                    text = stringResource(R.string.default_label),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getCurrencyDisplayName(code: String, locale: Locale): String =
    try { Currency.getInstance(code).getDisplayName(locale) }
    catch (_: IllegalArgumentException) { code }
