package com.example.budgetcontrol.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.CurrencyExchange
import com.example.budgetcontrol.ui.components.common.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.Date
import com.example.budgetcontrol.core.util.RATE_FORMAT
import com.example.budgetcontrol.core.util.formatAmount
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyExchangeScreen(
    onBackClick: () -> Unit,
    viewModel: CurrencyExchangeViewModel = hiltViewModel()
) {
    val exchanges by viewModel.exchanges.collectAsState()
    val formState by viewModel.formState.collectAsState()
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
                    onFromAmountChange = viewModel::updateFromAmount,
                    onFromCurrencyChange = viewModel::updateFromCurrency,
                    onToAmountChange = viewModel::updateToAmount,
                    onToCurrencyChange = viewModel::updateToCurrency,
                    onLocationChange = viewModel::updateLocation,
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
    onFromAmountChange: (String) -> Unit,
    onFromCurrencyChange: (String) -> Unit,
    onToAmountChange: (String) -> Unit,
    onToCurrencyChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
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
                CurrencyDropdown(
                    selectedCurrency = formState.fromCurrency,
                    onCurrencySelect = onFromCurrencyChange,
                    modifier = Modifier.width(100.dp)
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
                CurrencyDropdown(
                    selectedCurrency = formState.toCurrency,
                    onCurrencySelect = onToCurrencyChange,
                    modifier = Modifier.width(100.dp)
                )
            }

            // Rate display
            val fromAmount = formState.fromAmount.toDoubleOrNull()
            val toAmount = formState.toAmount.toDoubleOrNull()
            if (fromAmount != null && fromAmount > 0 && toAmount != null && toAmount > 0) {
                val rate = toAmount / fromAmount
                Text(
                    text = "${stringResource(R.string.exchange_rate)}: ${String.format(Locale.US, RATE_FORMAT, rate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Location
            OutlinedTextField(
                value = formState.location,
                onValueChange = onLocationChange,
                label = { Text(stringResource(R.string.exchange_location)) },
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
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(
    selectedCurrency: String,
    onCurrencySelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val commonCurrencies = listOf("EUR", "USD", "GBP", "PLN", "CZK", "BYN", "RUB", "SEK", "NOK", "DKK", "CHF", "TRY", "GEL", "THB")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCurrency,
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            commonCurrencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency) },
                    onClick = {
                        onCurrencySelect(currency)
                        expanded = false
                    }
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
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

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
