package com.example.budgetcontrol.ui.components.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.budgetcontrol.core.domain.model.Bank
import com.example.budgetcontrol.core.domain.model.CashRateMode
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.CategoryLimit
import com.example.budgetcontrol.core.domain.model.CategoryLimitProgress
import com.example.budgetcontrol.core.domain.model.CurrencyExchange
import androidx.compose.ui.res.stringResource
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.CategoryType
import com.example.budgetcontrol.core.domain.model.NetworkStatus
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.core.domain.usecase.AccountWithBalance
import com.example.budgetcontrol.core.util.formatAmount
import com.example.budgetcontrol.core.util.getCurrencySymbol
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TransactionFormState(
    val amount: String,
    val description: String,
    val selectedCategory: Category?,
    val selectedBank: Bank?,
    val selectedCurrency: String,
    val date: Long,
    val transactionType: TransactionType,
    val paymentMethod: String,
    val cashRate: String,
    val cashRateMode: CashRateMode,
    val exactEurAmount: String,
    val isExactMode: Boolean
)

data class TransactionFormCallbacks(
    val onAmountChange: (String) -> Unit,
    val onDescriptionChange: (String) -> Unit,
    val onCategorySelect: (Category) -> Unit,
    val onBankSelect: (Bank) -> Unit,
    val onCurrencySelect: (String) -> Unit,
    val onDateChange: (Long) -> Unit,
    val onSave: (Long) -> Unit,
    val onPaymentMethodChange: (String) -> Unit,
    val onCashRateChange: (String) -> Unit,
    val onCashRateModeChange: (CashRateMode) -> Unit,
    val onCashExchangeSelect: (String) -> Unit,
    val onToggleSaveExchangeRecord: () -> Unit,
    val onExchangeRecordDescriptionChange: (String) -> Unit,
    val onExactEurAmountChange: (String) -> Unit,
    val onExactModeToggle: (Boolean) -> Unit
)

data class TransactionCategoryActions(
    val onCreateCategory: ((name: String, iconName: String, color: String, type: CategoryType, limitAmount: Double?) -> Unit)? = null,
    val onUpdateCategoryColor: (Category, String) -> Unit = { _, _ -> },
    val onUpdateCategory: (Category) -> Unit = {},
    val onDeleteCategory: (Category) -> Unit = {},
    val onSetCategoryLimit: (categoryId: String, amount: Double) -> Unit = { _, _ -> },
    val onClearCategoryLimit: (categoryId: String) -> Unit = { _ -> }
)

@Composable
fun AddTransactionContent(
    formState: TransactionFormState,
    formCallbacks: TransactionFormCallbacks,
    categories: List<Category>,
    isLoading: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
    baseCurrency: String,
    accountCurrency: String = baseCurrency,
    availableCurrencies: List<String> = emptyList(),
    isCurrenciesLoading: Boolean = false,
    currenciesError: String? = null,
    favoriteCurrencies: Set<String> = emptySet(),
    availableBanks: List<Bank> = emptyList(),
    convertedAmountPreview: String = "",
    categoryActions: TransactionCategoryActions = TransactionCategoryActions(),
    cashRatePlaceholder: String = "",
    lastCashExchange: CurrencyExchange? = null,
    availableCashExchanges: List<CurrencyExchange> = emptyList(),
    selectedCashExchangeId: String? = null,
    isLastExchangeAvailable: Boolean = false,
    isCurrentRateAvailable: Boolean = true,
    isCurrentRateLoading: Boolean = false,
    saveExchangeRecord: Boolean = false,
    exchangeRecordDescription: String = "",
    networkStatus: NetworkStatus = NetworkStatus.ONLINE,
    staleRateWarning: String? = null,
    accounts: List<AccountWithBalance> = emptyList(),
    selectedAccountId: String = "",
    onAccountSelect: (String) -> Unit = {},
    foreignCurrencyDisabled: Boolean = false,
    selectedCategoryLimit: CategoryLimit? = null,
    selectedCategoryMonthSpend: Double = 0.0,
    limitProgressMap: Map<String, CategoryLimitProgress> = emptyMap(),
    categoryLimits: Map<String, CategoryLimit> = emptyMap()
) {
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = formState.date,
            onDateSelected = { date ->
                formCallbacks.onDateChange(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AmountInputCard(
            amount = formState.amount,
            onAmountChange = formCallbacks.onAmountChange,
            transactionType = formState.transactionType,
            currency = formState.selectedCurrency
        )

        CurrencySelector(
            currencies = availableCurrencies,
            selectedCurrency = formState.selectedCurrency,
            onCurrencySelect = formCallbacks.onCurrencySelect,
            baseCurrency = baseCurrency,
            favoriteCurrencies = favoriteCurrencies,
            isLoading = isCurrenciesLoading,
            error = currenciesError,
            enabled = !foreignCurrencyDisabled
        )

        if (foreignCurrencyDisabled) {
            Text(
                text = stringResource(R.string.foreign_currency_requires_internet),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Network status banner (only when tx currency differs from the account's currency)
        if (formState.selectedCurrency != accountCurrency && networkStatus != NetworkStatus.ONLINE) {
            NetworkStatusBanner(networkStatus = networkStatus)
        }

        // Stale rate warning
        if (formState.selectedCurrency != accountCurrency && staleRateWarning != null) {
            Text(
                text = staleRateWarning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        if (formState.selectedCurrency != accountCurrency) {
            // Payment method toggle: Card / Cash
            PaymentMethodSelector(
                selectedMethod = formState.paymentMethod,
                onMethodSelect = formCallbacks.onPaymentMethodChange
            )

            AnimatedContent(
                targetState = formState.paymentMethod,
                transitionSpec = { cashContentTransition() },
                label = "payment-method-content"
            ) { method ->
                if (method == "CARD") {
                    // Card mode: existing bank selector + conversion preview
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        BankSelector(
                            banks = availableBanks,
                            selectedBank = formState.selectedBank,
                            onBankSelect = formCallbacks.onBankSelect
                        )

                        // Conversion preview — hidden when user has entered exact EUR amount
                        if (formState.isExactMode && formState.exactEurAmount.isNotBlank()) {
                            Text(
                                text = "✓ " + stringResource(R.string.will_be_saved, formState.exactEurAmount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        } else {
                            ConversionPreview(preview = convertedAmountPreview)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = formState.isExactMode,
                                onCheckedChange = formCallbacks.onExactModeToggle,
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = stringResource(R.string.specify_amount_manually),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Exact EUR amount field — shown only when toggle is checked
                        if (formState.isExactMode) {
                            OutlinedTextField(
                                value = formState.exactEurAmount,
                                onValueChange = formCallbacks.onExactEurAmountChange,
                                label = { Text(stringResource(R.string.exact_eur_amount)) },
                                placeholder = { Text(stringResource(R.string.exact_eur_example)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                } else {
                    // Cash mode: exchange rate input
                    CashRateSection(
                        cashRate = formState.cashRate,
                        cashRateMode = formState.cashRateMode,
                        onCashRateChange = formCallbacks.onCashRateChange,
                        onCashRateModeSelect = formCallbacks.onCashRateModeChange,
                        onCashExchangeSelect = formCallbacks.onCashExchangeSelect,
                        onToggleSaveExchangeRecord = formCallbacks.onToggleSaveExchangeRecord,
                        onExchangeRecordDescriptionChange = formCallbacks.onExchangeRecordDescriptionChange,
                        cashRatePlaceholder = cashRatePlaceholder,
                        lastCashExchange = lastCashExchange,
                        availableCashExchanges = availableCashExchanges,
                        selectedCashExchangeId = selectedCashExchangeId,
                        isLastExchangeAvailable = isLastExchangeAvailable,
                        isCurrentRateAvailable = isCurrentRateAvailable,
                        isCurrentRateLoading = isCurrentRateLoading,
                        saveExchangeRecord = saveExchangeRecord,
                        exchangeRecordDescription = exchangeRecordDescription,
                        amount = formState.amount,
                        baseCurrency = baseCurrency,
                        selectedCurrency = formState.selectedCurrency
                    )
                }
            }
        }

        CategorySelector(
            categories = categories,
            selectedCategory = formState.selectedCategory,
            onCategorySelect = formCallbacks.onCategorySelect,
            transactionType = formState.transactionType,
            onCreateCategory = categoryActions.onCreateCategory,
            onUpdateCategoryColor = categoryActions.onUpdateCategoryColor,
            onUpdateCategory = categoryActions.onUpdateCategory,
            onDeleteCategory = categoryActions.onDeleteCategory,
            limitProgressMap = limitProgressMap,
            baseCurrency = baseCurrency,
            categoryLimits = categoryLimits,
            onSetCategoryLimit = categoryActions.onSetCategoryLimit,
            onClearCategoryLimit = categoryActions.onClearCategoryLimit
        )

        if (selectedCategoryLimit != null
            && formState.transactionType == TransactionType.EXPENSE
        ) {
            CategoryLimitSummary(
                limit = selectedCategoryLimit,
                monthSpend = selectedCategoryMonthSpend,
                amountInput = formState.amount,
                selectedCurrency = formState.selectedCurrency,
                accountCurrency = accountCurrency,
                baseCurrency = baseCurrency,
                convertedAmountPreview = convertedAmountPreview,
                exactBaseAmount = if (formState.isExactMode) formState.exactEurAmount else "",
                cashRate = formState.cashRate,
                paymentMethod = formState.paymentMethod
            )
        }

        if (accounts.size > 1) {
            AccountSelector(
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                onAccountSelect = onAccountSelect
            )
        }

        DescriptionSection(
            description = formState.description,
            onDescriptionChange = formCallbacks.onDescriptionChange,
            transactionType = formState.transactionType
        )

        DateSelector(
            selectedDate = formState.date,
            onDateSelect = formCallbacks.onDateChange,
            onShowDatePicker = { showDatePicker = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Button(
            onClick = { formCallbacks.onSave(formState.date) },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White
                )
            } else {
                val buttonText = when (formState.transactionType) {
                    TransactionType.EXPENSE -> stringResource(R.string.add_expense)
                    TransactionType.INCOME -> stringResource(R.string.add_income)
                }
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun DescriptionSection(
    description: String,
    onDescriptionChange: (String) -> Unit,
    transactionType: TransactionType,
    modifier: Modifier = Modifier
) {
    val placeholder = when (transactionType) {
        TransactionType.EXPENSE -> stringResource(R.string.expense_placeholder)
        TransactionType.INCOME -> stringResource(R.string.income_placeholder)
    }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.description_optional),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun PaymentMethodSelector(
    selectedMethod: String,
    onMethodSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.payment_method),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedMethod == "CARD",
                onClick = { onMethodSelect("CARD") },
                label = { Text(stringResource(R.string.card)) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            FilterChip(
                selected = selectedMethod == "CASH",
                onClick = { onMethodSelect("CASH") },
                label = { Text(stringResource(R.string.cash)) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

private val CashModeEnter = fadeIn(animationSpec = tween(200)) +
    slideInHorizontally(animationSpec = tween(200)) { it / 4 }
private val CashModeExit = fadeOut(animationSpec = tween(200))

private fun cashContentTransition() = CashModeEnter togetherWith CashModeExit

@Composable
private fun CashRateSection(
    cashRate: String,
    cashRateMode: CashRateMode,
    onCashRateChange: (String) -> Unit,
    onCashRateModeSelect: (CashRateMode) -> Unit,
    onCashExchangeSelect: (String) -> Unit,
    onToggleSaveExchangeRecord: () -> Unit,
    onExchangeRecordDescriptionChange: (String) -> Unit,
    cashRatePlaceholder: String,
    lastCashExchange: CurrencyExchange?,
    availableCashExchanges: List<CurrencyExchange>,
    selectedCashExchangeId: String?,
    isLastExchangeAvailable: Boolean,
    isCurrentRateAvailable: Boolean,
    isCurrentRateLoading: Boolean,
    saveExchangeRecord: Boolean,
    exchangeRecordDescription: String,
    amount: String,
    baseCurrency: String,
    selectedCurrency: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CashRateModeSelector(
            selectedMode = cashRateMode,
            isLastExchangeAvailable = isLastExchangeAvailable,
            isCurrentRateAvailable = isCurrentRateAvailable,
            isCurrentRateLoading = isCurrentRateLoading,
            onModeSelect = onCashRateModeSelect
        )

        // Contextual hint — also disappears in MANUAL once the user opts in to saving.
        val hintRes: Int? = when (cashRateMode) {
            CashRateMode.LAST_EXCHANGE -> R.string.cash_mode_hint_last_exchange
            CashRateMode.CURRENT_RATE -> R.string.cash_mode_hint_current
            CashRateMode.MANUAL ->
                if (saveExchangeRecord) null else R.string.cash_mode_hint_manual_unsaved
        }
        AnimatedContent(
            targetState = hintRes,
            transitionSpec = { cashContentTransition() },
            label = "cash-mode-hint"
        ) { res ->
            if (res != null) {
                Text(
                    text = stringResource(res),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            } else {
                Spacer(Modifier.height(0.dp))
            }
        }

        AnimatedContent(
            targetState = cashRateMode,
            transitionSpec = { cashContentTransition() },
            label = "cash-rate-mode-content"
        ) { mode ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (mode) {
                    CashRateMode.LAST_EXCHANGE -> {
                        if (availableCashExchanges.isNotEmpty()) {
                            CashExchangeHistoryDropdown(
                                exchanges = availableCashExchanges,
                                selectedId = selectedCashExchangeId,
                                onSelect = onCashExchangeSelect
                            )
                        }
                    }
                    CashRateMode.CURRENT_RATE -> {
                        CurrentRatePreview(
                            amount = amount,
                            cashRate = cashRate,
                            baseCurrency = baseCurrency
                        )
                    }
                    CashRateMode.MANUAL -> {
                        CashRateField(
                            cashRate = cashRate,
                            onCashRateChange = onCashRateChange,
                            cashRatePlaceholder = cashRatePlaceholder,
                            selectedCurrency = selectedCurrency,
                            readOnly = false
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = saveExchangeRecord,
                                onCheckedChange = { onToggleSaveExchangeRecord() },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = stringResource(R.string.cash_save_exchange_record),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (saveExchangeRecord) {
                            OutlinedTextField(
                                value = exchangeRecordDescription,
                                onValueChange = onExchangeRecordDescriptionChange,
                                label = { Text(stringResource(R.string.cash_exchange_description_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CashRateField(
    cashRate: String,
    onCashRateChange: (String) -> Unit,
    cashRatePlaceholder: String,
    selectedCurrency: String,
    readOnly: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = cashRate,
        onValueChange = onCashRateChange,
        readOnly = readOnly,
        label = { Text(stringResource(R.string.exchange_rate_label)) },
        placeholder = {
            if (cashRatePlaceholder.isNotBlank()) {
                Text(stringResource(R.string.cash_rate_placeholder, cashRatePlaceholder))
            }
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        supportingText = {
            if (cashRatePlaceholder.isNotBlank()) {
                Text(
                    text = stringResource(R.string.cash_rate_hint, cashRatePlaceholder, selectedCurrency),
                    fontSize = 12.sp
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CashExchangeHistoryDropdown(
    exchanges: List<CurrencyExchange>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val itemDateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val selected = exchanges.firstOrNull { it.id == selectedId } ?: exchanges.firstOrNull()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected?.let { exchangeSummary(it, itemDateFormat) }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.cash_exchange_picker_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            exchanges.forEach { exchange ->
                val location = exchange.location?.takeIf { it.isNotBlank() }
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = exchangeSummary(exchange, itemDateFormat),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (location != null) {
                                Text(
                                    text = location,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelect(exchange.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun exchangeSummary(exchange: CurrencyExchange, dateFormat: SimpleDateFormat): String {
    val date = dateFormat.format(Date(exchange.date))
    return "$date · ${formatAmount(exchange.fromAmount)} ${exchange.fromCurrency} → " +
        "${formatAmount(exchange.toAmount)} ${exchange.toCurrency}"
}

@Composable
private fun LastExchangeContextLine(exchange: CurrencyExchange) {
    val dateFormat = remember { SimpleDateFormat("dd.MM", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(exchange.date))
    val fromStr = formatAmount(exchange.fromAmount)
    val toStr = formatAmount(exchange.toAmount)
    val location = exchange.location?.takeIf { it.isNotBlank() }
    val text = if (location != null) {
        stringResource(
            R.string.cash_last_exchange_context_with_desc,
            dateStr, fromStr, exchange.fromCurrency, toStr, exchange.toCurrency, location
        )
    } else {
        stringResource(
            R.string.cash_last_exchange_context,
            dateStr, fromStr, exchange.fromCurrency, toStr, exchange.toCurrency
        )
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun CurrentRatePreview(
    amount: String,
    cashRate: String,
    baseCurrency: String,
    modifier: Modifier = Modifier
) {
    val amountValue = amount.replace(',', '.').toDoubleOrNull()
    val rateValue = cashRate.replace(',', '.').toDoubleOrNull()
    val converted = if (amountValue != null && amountValue > 0 && rateValue != null && rateValue > 0) {
        formatAmount(amountValue / rateValue)
    } else null

    if (converted != null) {
        Text(
            text = stringResource(R.string.cash_current_preview_format, converted, baseCurrency, cashRate),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}

@Composable
private fun CashRateModeSelector(
    selectedMode: CashRateMode,
    isLastExchangeAvailable: Boolean,
    isCurrentRateAvailable: Boolean,
    isCurrentRateLoading: Boolean,
    onModeSelect: (CashRateMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isLastExchangeAvailable) {
            CashRateModeChip(
                selected = selectedMode == CashRateMode.LAST_EXCHANGE,
                enabled = true,
                label = stringResource(R.string.cash_rate_mode_last_exchange),
                onClick = { onModeSelect(CashRateMode.LAST_EXCHANGE) },
                modifier = Modifier.weight(1f)
            )
        }
        CashRateModeChip(
            selected = selectedMode == CashRateMode.CURRENT_RATE,
            enabled = isCurrentRateAvailable && !isCurrentRateLoading,
            label = stringResource(R.string.cash_rate_mode_current),
            onClick = { onModeSelect(CashRateMode.CURRENT_RATE) },
            modifier = Modifier.weight(1f)
        )
        CashRateModeChip(
            selected = selectedMode == CashRateMode.MANUAL,
            enabled = true,
            label = stringResource(R.string.cash_rate_mode_manual),
            onClick = { onModeSelect(CashRateMode.MANUAL) },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Selectable chip whose container, border and label colors animate with
 * `animateColorAsState` when the selected state flips — keeps the
 * selection transition consistent with the AnimatedContent below.
 */
@Composable
private fun CashRateModeChip(
    selected: Boolean,
    enabled: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animSpec = tween<Color>(200)
    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
            selected -> MaterialTheme.colorScheme.primary
            else -> Color.Transparent
        },
        animationSpec = animSpec,
        label = "chip-container"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
            selected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline
        },
        animationSpec = animSpec,
        label = "chip-border"
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            selected -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = animSpec,
        label = "chip-content"
    )
    Surface(
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .height(36.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Renders the "X remaining of Y" / "limit reached" / "limit exceeded" line.
 *
 * The projected spend = month-to-date + the current entry converted to the base currency.
 * We try to honor the live `convertedAmountPreview` for foreign-currency card flows; for
 * other flows we approximate by parsing the amount as base currency.
 */
@Composable
private fun CategoryLimitSummary(
    limit: CategoryLimit,
    monthSpend: Double,
    amountInput: String,
    selectedCurrency: String,
    accountCurrency: String,
    baseCurrency: String,
    convertedAmountPreview: String,
    exactBaseAmount: String,
    cashRate: String,
    paymentMethod: String
) {
    val projectedEntry = computeProjectedBaseAmount(
        amountInput = amountInput,
        selectedCurrency = selectedCurrency,
        accountCurrency = accountCurrency,
        baseCurrency = baseCurrency,
        convertedAmountPreview = convertedAmountPreview,
        exactBaseAmount = exactBaseAmount,
        cashRate = cashRate,
        paymentMethod = paymentMethod
    )
    val projectedSpend = monthSpend + projectedEntry
    val remaining = limit.amount - projectedSpend
    val symbol = getCurrencySymbol(baseCurrency)

    val color = when {
        remaining > 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        remaining == 0.0 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    val text = when {
        remaining > 0 -> stringResource(
            R.string.limit_remaining_format,
            formatAmount(remaining),
            symbol,
            formatAmount(limit.amount),
            symbol
        )
        remaining == 0.0 -> stringResource(R.string.limit_reached)
        else -> stringResource(
            R.string.limit_exceeded_format,
            formatAmount(-remaining),
            symbol
        )
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

private fun computeProjectedBaseAmount(
    amountInput: String,
    selectedCurrency: String,
    accountCurrency: String,
    baseCurrency: String,
    convertedAmountPreview: String,
    exactBaseAmount: String,
    cashRate: String,
    paymentMethod: String
): Double {
    val amount = amountInput.replace(',', '.').toDoubleOrNull() ?: return 0.0
    if (amount <= 0.0) return 0.0
    val exactBase = exactBaseAmount.replace(',', '.').toDoubleOrNull()
    if (exactBase != null && exactBase > 0.0) return exactBase
    if (selectedCurrency == baseCurrency) return amount
    if (paymentMethod == "CASH") {
        val rate = cashRate.replace(',', '.').toDoubleOrNull()
        if (rate != null && rate > 0.0) return amount / rate
    }
    // Fallback: try to read the converted amount preview (format: "≈ X.XX BASE …")
    val previewNumber = convertedAmountPreview
        .replace(',', '.')
        .split(' ', ' ')
        .firstOrNull { it.toDoubleOrNull() != null }
        ?.toDoubleOrNull()
    if (previewNumber != null) return previewNumber
    return if (selectedCurrency == accountCurrency) amount else 0.0
}

@Composable
private fun NetworkStatusBanner(
    networkStatus: NetworkStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, text) = when (networkStatus) {
        NetworkStatus.NO_INTERNET -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            stringResource(R.string.offline_no_internet)
        )
        NetworkStatus.SERVICE_UNAVAILABLE -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            stringResource(R.string.offline_service_unavailable)
        )
        NetworkStatus.OFFLINE_NO_CACHE -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            stringResource(R.string.offline_no_rates)
        )
        NetworkStatus.ONLINE -> return
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            modifier = Modifier.padding(10.dp)
        )
    }
}