package com.example.budgetcontrol.feature.transaction.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.core.theme.AppBlue
import com.example.budgetcontrol.ui.components.common.AddTransactionContent
import com.example.budgetcontrol.ui.components.common.DatePickerDialog
import com.example.budgetcontrol.ui.components.common.CurrencySelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    transactionType: TransactionType,
    mode: TransactionFormMode,
    transactionId: String? = null,
    initialDate: Long = System.currentTimeMillis(),
    onBackClick: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: TransactionFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var hasChanges by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(transactionType, mode, transactionId) {
        viewModel.initialize(
            mode = mode,
            type = transactionType,
            transactionId = transactionId,
            initialDate = initialDate
        )
    }

    LaunchedEffect(
        uiState.amount,
        uiState.description,
        uiState.selectedCategory,
        uiState.selectedDate,
        uiState.transactionType,
        uiState.selectedCurrency
    ) {
        hasChanges = if (mode == TransactionFormMode.EDIT) {
            viewModel.hasChanges()
        } else {
            uiState.amount.isNotBlank() || uiState.description.isNotBlank()
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onSuccess()
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = uiState.selectedDate,
            onDateSelected = { date ->
                viewModel.updateDate(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = if (mode == TransactionFormMode.EDIT) {
                        "Операция была изменена"
                    } else {
                        "Форма не пуста"
                    }
                )
            },
            text = { Text("Выйти без сохранения?") },
            confirmButton = {
                TextButton(onClick = onBackClick) {
                    Text("ДА")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("НЕТ")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AppBlue
                ),
                shape = RoundedCornerShape(0.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = 60.dp,
                            bottom = 10.dp
                        )
                ) {
                    IconButton(
                        onClick = {
                            if (hasChanges && mode == TransactionFormMode.EDIT) {
                                showExitDialog = true
                            } else {
                                onBackClick()
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = getTitle(mode, uiState.transactionType, uiState.selectedCategory?.name),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppBlue)
            }
        } else {
            TransactionFormContent(
                uiState = uiState,
                onAmountChange = viewModel::updateAmount,
                onCategorySelect = viewModel::selectCategory,
                onDescriptionChange = viewModel::updateDescription,
                onDateChange = viewModel::updateDate,
                onSaveClick = viewModel::saveTransaction,
                onShowDatePicker = { showDatePicker = true },
                onTransactionTypeChange = viewModel::changeTransactionType,
                onCurrencySelect = viewModel::selectCurrency,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun TransactionFormContent(
    uiState: TransactionFormUiState,
    onAmountChange: (String) -> Unit,
    onCategorySelect: (com.example.budgetcontrol.core.domain.model.Category) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    onSaveClick: () -> Unit,
    onShowDatePicker: () -> Unit,
    onTransactionTypeChange: (TransactionType) -> Unit,
    onCurrencySelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.mode == TransactionFormMode.ADD) {
        AddTransactionContent(
            title = getContentTitle(uiState.mode, uiState.transactionType),
            transactionType = uiState.transactionType,
            amount = uiState.amount,
            onAmountChange = onAmountChange,
            categories = uiState.categories,
            selectedCategory = uiState.selectedCategory,
            onCategorySelect = onCategorySelect,
            description = uiState.description,
            onDescriptionChange = onDescriptionChange,
            selectedDate = uiState.selectedDate,
            onDateChange = onDateChange,
            isLoading = uiState.isLoading,
            errorMessage = uiState.showError,
            onAddClick = { _ -> onSaveClick() },
            modifier = modifier,
            availableCurrencies = uiState.availableCurrencies,
            selectedCurrency = uiState.selectedCurrency,
            onCurrencySelect = onCurrencySelect,
            isCurrenciesLoading = uiState.isCurrenciesLoading,
            currenciesError = uiState.currenciesError
        )
    } else {
        EditTransactionFormContent(
            uiState = uiState,
            onAmountChange = onAmountChange,
            onCategorySelect = onCategorySelect,
            onDescriptionChange = onDescriptionChange,
            onDateChange = onDateChange,
            onSaveClick = onSaveClick,
            onShowDatePicker = onShowDatePicker,
            onTransactionTypeChange = onTransactionTypeChange,
            onCurrencySelect = onCurrencySelect,
            modifier = modifier
        )
    }
}

@Composable
private fun EditTransactionFormContent(
    uiState: TransactionFormUiState,
    onAmountChange: (String) -> Unit,
    onCategorySelect: (com.example.budgetcontrol.core.domain.model.Category) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    onSaveClick: () -> Unit,
    onShowDatePicker: () -> Unit,
    onTransactionTypeChange: (TransactionType) -> Unit,
    onCurrencySelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (uiState.canChangeType) {
            TransactionTypeSelector(
                selectedType = uiState.transactionType,
                onTypeSelected = onTransactionTypeChange
            )
        } else {
            TransactionTypeDisplay(uiState.transactionType)
        }

        com.example.budgetcontrol.ui.components.common.AmountInputCard(
            amount = uiState.amount,
            onAmountChange = onAmountChange,
            transactionType = uiState.transactionType,
            currency = uiState.selectedCurrency
        )

        if (uiState.transactionType == TransactionType.EXPENSE) {
            CurrencySelector(
                currencies = uiState.availableCurrencies,
                selectedCurrency = uiState.selectedCurrency,
                onCurrencySelect = onCurrencySelect,
                isLoading = uiState.isCurrenciesLoading,
                error = uiState.currenciesError
            )
        }

        com.example.budgetcontrol.ui.components.common.CategorySelector(
            categories = uiState.categories,
            selectedCategory = uiState.selectedCategory,
            onCategorySelect = onCategorySelect,
            transactionType = uiState.transactionType
        )

        com.example.budgetcontrol.ui.components.common.DateSelector(
            selectedDate = uiState.selectedDate,
            onDateSelect = onDateChange,
            onShowDatePicker = onShowDatePicker
        )

        DescriptionField(
            description = uiState.description,
            onDescriptionChange = onDescriptionChange
        )

        Spacer(modifier = Modifier.weight(1f))

        uiState.showError?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
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
            onClick = onSaveClick,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppBlue
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White
                )
            } else {
                Text(
                    text = "Сохранить",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun TransactionTypeSelector(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppBlue
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTypeSelected(TransactionType.EXPENSE) },
                color = if (selectedType == TransactionType.EXPENSE) {
                    Color.White.copy(alpha = 0.2f)
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "РАСХОДЫ",
                    modifier = Modifier.padding(
                        horizontal = 4.dp,
                        vertical = 16.dp
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp
                    ),
                    fontWeight = if (selectedType == TransactionType.EXPENSE) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                    color = if (selectedType == TransactionType.EXPENSE) {
                        Color.White
                    } else {
                        Color.White.copy(alpha = 0.6f)
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTypeSelected(TransactionType.INCOME) },
                color = if (selectedType == TransactionType.INCOME) {
                    Color.White.copy(alpha = 0.2f)
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "ДОХОДЫ",
                    modifier = Modifier.padding(
                        horizontal = 4.dp,
                        vertical = 16.dp
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp
                    ),
                    fontWeight = if (selectedType == TransactionType.INCOME) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                    color = if (selectedType == TransactionType.INCOME) {
                        Color.White
                    } else {
                        Color.White.copy(alpha = 0.6f)
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun TransactionTypeDisplay(transactionType: TransactionType) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppBlue
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                color = if (transactionType == TransactionType.EXPENSE) {
                    Color.White.copy(alpha = 0.2f)
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "РАСХОДЫ",
                    modifier = Modifier.padding(
                        horizontal = 4.dp,
                        vertical = 16.dp
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp
                    ),
                    fontWeight = if (transactionType == TransactionType.EXPENSE) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                    color = if (transactionType == TransactionType.EXPENSE) {
                        Color.White
                    } else {
                        Color.White.copy(alpha = 0.6f)
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            Surface(
                modifier = Modifier.weight(1f),
                color = if (transactionType == TransactionType.INCOME) {
                    Color.White.copy(alpha = 0.2f)
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "ДОХОДЫ",
                    modifier = Modifier.padding(
                        horizontal = 4.dp,
                        vertical = 16.dp
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp
                    ),
                    fontWeight = if (transactionType == TransactionType.INCOME) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                    color = if (transactionType == TransactionType.INCOME) {
                        Color.White
                    } else {
                        Color.White.copy(alpha = 0.6f)
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DescriptionField(
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        label = { Text("Комментарий") },
        placeholder = { Text("Добавьте комментарий к операции") },
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

private fun getTitle(
    mode: TransactionFormMode,
    transactionType: TransactionType,
    categoryName: String?
): String {
    return when (mode) {
        TransactionFormMode.ADD -> {
            when (transactionType) {
                TransactionType.EXPENSE -> "Добавить трату"
                TransactionType.INCOME -> "Добавить доход"
            }
        }
        TransactionFormMode.EDIT -> {
            categoryName?.uppercase() ?: when (transactionType) {
                TransactionType.EXPENSE -> "РЕДАКТИРОВАНИЕ РАСХОДА"
                TransactionType.INCOME -> "РЕДАКТИРОВАНИЕ ДОХОДА"
            }
        }
    }
}

private fun getContentTitle(
    mode: TransactionFormMode,
    transactionType: TransactionType
): String {
    return when (mode) {
        TransactionFormMode.ADD -> {
            when (transactionType) {
                TransactionType.EXPENSE -> "Добавить трату"
                TransactionType.INCOME -> "Добавить доход"
            }
        }
        TransactionFormMode.EDIT -> {
            when (transactionType) {
                TransactionType.EXPENSE -> "Редактировать трату"
                TransactionType.INCOME -> "Редактировать доход"
            }
        }
    }
}