package com.example.budgetcontrol.ui.components.common

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.budgetcontrol.core.data.local.database.entities.BankEntity
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.CurrencyExchange
import androidx.compose.ui.res.stringResource
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.CategoryType
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.feature.transaction.common.NetworkStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TransactionFormState(
    val amount: String,
    val description: String,
    val selectedCategory: Category?,
    val selectedBank: BankEntity?,
    val selectedCurrency: String,
    val date: Long,
    val transactionType: TransactionType,
    val paymentMethod: String,
    val cashRate: String,
    val exactEurAmount: String,
    val isExactMode: Boolean
)

data class TransactionFormCallbacks(
    val onAmountChange: (String) -> Unit,
    val onDescriptionChange: (String) -> Unit,
    val onCategorySelect: (Category) -> Unit,
    val onBankSelect: (BankEntity) -> Unit,
    val onCurrencySelect: (String) -> Unit,
    val onDateChange: (Long) -> Unit,
    val onSave: (Long) -> Unit,
    val onPaymentMethodChange: (String) -> Unit,
    val onCashRateChange: (String) -> Unit,
    val onExactEurAmountChange: (String) -> Unit,
    val onExactModeToggle: (Boolean) -> Unit
)

data class TransactionCategoryActions(
    val onCreateCategory: ((name: String, iconName: String, color: String, type: CategoryType) -> Unit)? = null,
    val onUpdateCategoryColor: (Category, String) -> Unit = { _, _ -> },
    val onUpdateCategory: (Category) -> Unit = {},
    val onDeleteCategory: (Category) -> Unit = {}
)

@Composable
fun AddTransactionContent(
    title: String,
    formState: TransactionFormState,
    formCallbacks: TransactionFormCallbacks,
    categories: List<Category>,
    isLoading: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
    availableCurrencies: List<String> = listOf("EUR"),
    isCurrenciesLoading: Boolean = false,
    currenciesError: String? = null,
    favoriteCurrencies: Set<String> = emptySet(),
    availableBanks: List<BankEntity> = emptyList(),
    convertedAmountPreview: String = "",
    categoryActions: TransactionCategoryActions = TransactionCategoryActions(),
    cashRatePlaceholder: String = "",
    cashRateHint: String = "",
    lastCashExchange: CurrencyExchange? = null,
    networkStatus: NetworkStatus = NetworkStatus.ONLINE
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
            favoriteCurrencies = favoriteCurrencies,
            isLoading = isCurrenciesLoading,
            error = currenciesError
        )

        // Network status banner (only when currency != EUR)
        if (formState.selectedCurrency != "EUR" && networkStatus != NetworkStatus.ONLINE) {
            NetworkStatusBanner(networkStatus = networkStatus)
        }

        if (formState.selectedCurrency != "EUR") {
            // Payment method toggle: Card / Cash
            PaymentMethodSelector(
                selectedMethod = formState.paymentMethod,
                onMethodSelect = formCallbacks.onPaymentMethodChange
            )

            if (formState.paymentMethod == "CARD") {
                // Card mode: existing bank selector + conversion preview
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

                // Toggle: Уточнить сумму вручную
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
            } else {
                // Cash mode: exchange rate input
                CashRateSection(
                    cashRate = formState.cashRate,
                    onCashRateChange = formCallbacks.onCashRateChange,
                    cashRatePlaceholder = cashRatePlaceholder,
                    cashRateHint = cashRateHint,
                    lastCashExchange = lastCashExchange,
                    selectedCurrency = formState.selectedCurrency
                )

                // Toggle: Specify amount manually (same as card mode)
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
        }

        CategorySelector(
            categories = categories,
            selectedCategory = formState.selectedCategory,
            onCategorySelect = formCallbacks.onCategorySelect,
            transactionType = formState.transactionType,
            onCreateCategory = categoryActions.onCreateCategory,
            onUpdateCategoryColor = categoryActions.onUpdateCategoryColor,
            onUpdateCategory = categoryActions.onUpdateCategory,
            onDeleteCategory = categoryActions.onDeleteCategory
        )

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

@Composable
private fun CashRateSection(
    cashRate: String,
    onCashRateChange: (String) -> Unit,
    cashRatePlaceholder: String,
    cashRateHint: String = "",
    lastCashExchange: CurrencyExchange?,
    selectedCurrency: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Cash mode description
        Text(
            text = stringResource(R.string.cash_rate_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        OutlinedTextField(
            value = cashRate,
            onValueChange = onCashRateChange,
            label = { Text(stringResource(R.string.exchange_rate_label)) },
            placeholder = {
                if (cashRatePlaceholder.isNotBlank()) {
                    Text(stringResource(R.string.cash_rate_placeholder, cashRatePlaceholder))
                }
            },
            modifier = Modifier.fillMaxWidth(),
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

        // Hint about last exchange or interbank rate
        if (cashRateHint.isNotBlank()) {
            Text(
                text = cashRateHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        } else if (lastCashExchange != null) {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val dateStr = dateFormat.format(Date(lastCashExchange.date))
            val locationStr = lastCashExchange.location?.let { " · $it" } ?: ""
            Text(
                text = stringResource(R.string.last_exchange) + ": $dateStr$locationStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
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