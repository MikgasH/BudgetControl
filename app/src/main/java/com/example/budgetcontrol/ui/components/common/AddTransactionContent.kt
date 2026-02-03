package com.example.budgetcontrol.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.core.theme.AppBlue

@Composable
fun AddTransactionContent(
    title: String,
    transactionType: TransactionType,
    amount: String,
    onAmountChange: (String) -> Unit,
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelect: (Category) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    selectedDate: Long,
    onDateChange: (Long) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onAddClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    availableCurrencies: List<String> = listOf("EUR"),
    selectedCurrency: String = "EUR",
    onCurrencySelect: (String) -> Unit = {},
    isCurrenciesLoading: Boolean = false,
    currenciesError: String? = null
) {
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                onDateChange(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        AmountInputCard(
            amount = amount,
            onAmountChange = onAmountChange,
            transactionType = transactionType,
            currency = selectedCurrency
        )

        if (transactionType == TransactionType.EXPENSE) {
            CurrencySelector(
                currencies = availableCurrencies,
                selectedCurrency = selectedCurrency,
                onCurrencySelect = onCurrencySelect,
                isLoading = isCurrenciesLoading,
                error = currenciesError
            )
        }

        CategorySelector(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelect = onCategorySelect,
            transactionType = transactionType
        )

        DescriptionSection(
            description = description,
            onDescriptionChange = onDescriptionChange,
            transactionType = transactionType
        )

        DateSelector(
            selectedDate = selectedDate,
            onDateSelect = onDateChange,
            onShowDatePicker = { showDatePicker = true }
        )

        Spacer(modifier = Modifier.weight(1f))

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
            onClick = { onAddClick(selectedDate) },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppBlue
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White
                )
            } else {
                val buttonText = when (transactionType) {
                    TransactionType.EXPENSE -> "Добавить трату"
                    TransactionType.INCOME -> "Добавить доход"
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
        TransactionType.EXPENSE -> "Например: обед в ресторане"
        TransactionType.INCOME -> "Например: зарплата за декабрь"
    }

    Column(modifier = modifier) {
        Text(
            text = "Описание (необязательно)",
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
                focusedBorderColor = AppBlue,
                focusedLabelColor = AppBlue,
                cursorColor = AppBlue
            )
        )
    }
}