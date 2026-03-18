package com.example.budgetcontrol.feature.transaction.common

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.data.local.database.entities.BankEntity
import com.example.budgetcontrol.core.domain.model.CategoryType
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.ui.components.common.AddTransactionContent
import com.example.budgetcontrol.ui.components.common.TransactionFormState
import com.example.budgetcontrol.ui.components.common.TransactionFormCallbacks
import com.example.budgetcontrol.ui.components.common.TransactionCategoryActions
import com.example.budgetcontrol.ui.util.displayName
import com.example.budgetcontrol.ui.components.common.DatePickerDialog

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
        if (uiState.isSuccess && !uiState.showSaveRateDialog) {
            onSuccess()
        }
    }

    if (uiState.showSaveRateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSaveRateDialog() },
            title = { Text(stringResource(R.string.save_cash_rate_title)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmSaveRate() }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSaveRateDialog() }) {
                    Text(stringResource(R.string.save_cash_rate_skip))
                }
            }
        )
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

    // Intercept system back press in both ADD and EDIT modes when form has data
    BackHandler(enabled = hasChanges) {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(stringResource(R.string.exit_without_saving))
            },
            confirmButton = {
                TextButton(onClick = onBackClick) {
                    Text(
                        text = stringResource(R.string.discard_button),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(R.string.keep_editing_button))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
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
                            if (hasChanges) {
                                showExitDialog = true
                            } else {
                                onBackClick()
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = getTitle(mode, uiState.transactionType, uiState.selectedCategory?.displayName()),
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
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                onBankSelect = viewModel::selectBank,
                onExactAmountToggle = viewModel::toggleExactAmount,
                onExactEurAmountChange = viewModel::updateExactEurAmount,
                onCreateCategory = viewModel::createCategory,
                onUpdateCategoryColor = viewModel::updateCategoryColor,
                onUpdateCategory = viewModel::updateCustomCategory,
                onDeleteCategory = viewModel::deleteCustomCategory,
                onPaymentMethodSelect = viewModel::selectPaymentMethod,
                onCashRateChange = viewModel::updateCashRate,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun TransactionFormContent(
    uiState: TransactionFormUiState,
    onAmountChange: (String) -> Unit,
    onCategorySelect: (Category) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    onSaveClick: () -> Unit,
    onShowDatePicker: () -> Unit,
    onTransactionTypeChange: (TransactionType) -> Unit,
    onCurrencySelect: (String) -> Unit,
    onBankSelect: (BankEntity) -> Unit,
    onExactAmountToggle: (Boolean) -> Unit,
    onExactEurAmountChange: (String) -> Unit,
    onCreateCategory: (name: String, iconName: String, color: String, type: CategoryType) -> Unit,
    onUpdateCategoryColor: (Category, String) -> Unit = { _, _ -> },
    onUpdateCategory: (Category) -> Unit = {},
    onDeleteCategory: (Category) -> Unit = {},
    onPaymentMethodSelect: (String) -> Unit = {},
    onCashRateChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (uiState.mode == TransactionFormMode.ADD) {
        AddTransactionContent(
            title = getContentTitle(uiState.mode, uiState.transactionType),
            formState = TransactionFormState(
                amount = uiState.amount,
                description = uiState.description,
                selectedCategory = uiState.selectedCategory,
                selectedBank = uiState.selectedBank,
                selectedCurrency = uiState.selectedCurrency,
                date = uiState.selectedDate,
                transactionType = uiState.transactionType,
                paymentMethod = uiState.paymentMethod,
                cashRate = uiState.cashRate,
                exactEurAmount = uiState.exactEurAmount,
                isExactMode = uiState.isExactAmountEnabled
            ),
            formCallbacks = TransactionFormCallbacks(
                onAmountChange = onAmountChange,
                onDescriptionChange = onDescriptionChange,
                onCategorySelect = onCategorySelect,
                onBankSelect = onBankSelect,
                onCurrencySelect = onCurrencySelect,
                onDateChange = onDateChange,
                onSave = { _ -> onSaveClick() },
                onPaymentMethodChange = onPaymentMethodSelect,
                onCashRateChange = onCashRateChange,
                onExactEurAmountChange = onExactEurAmountChange,
                onExactModeToggle = onExactAmountToggle
            ),
            categories = uiState.categories,
            isLoading = uiState.isLoading,
            errorMessage = uiState.showError,
            modifier = modifier,
            availableCurrencies = uiState.availableCurrencies,
            isCurrenciesLoading = uiState.isCurrenciesLoading,
            currenciesError = uiState.currenciesError,
            favoriteCurrencies = uiState.favoriteCurrencies,
            availableBanks = uiState.availableBanks,
            convertedAmountPreview = uiState.convertedAmountPreview,
            categoryActions = TransactionCategoryActions(
                onCreateCategory = onCreateCategory,
                onUpdateCategoryColor = onUpdateCategoryColor,
                onUpdateCategory = onUpdateCategory,
                onDeleteCategory = onDeleteCategory
            ),
            cashRatePlaceholder = uiState.cashRatePlaceholder,
            cashRateHint = uiState.cashRateHint,
            lastCashExchange = uiState.lastCashExchange,
            networkStatus = uiState.networkStatus
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
            onBankSelect = onBankSelect,
            onCreateCategory = onCreateCategory,
            onUpdateCategoryColor = onUpdateCategoryColor,
            onUpdateCategory = onUpdateCategory,
            onDeleteCategory = onDeleteCategory,
            modifier = modifier
        )
    }
}

@Composable
private fun EditTransactionFormContent(
    uiState: TransactionFormUiState,
    onAmountChange: (String) -> Unit,
    onCategorySelect: (Category) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    onSaveClick: () -> Unit,
    onShowDatePicker: () -> Unit,
    onTransactionTypeChange: (TransactionType) -> Unit,
    onCurrencySelect: (String) -> Unit,
    onBankSelect: (BankEntity) -> Unit,
    onCreateCategory: (name: String, iconName: String, color: String, type: CategoryType) -> Unit,
    onUpdateCategoryColor: (Category, String) -> Unit = { _, _ -> },
    onUpdateCategory: (Category) -> Unit = {},
    onDeleteCategory: (Category) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.canChangeType) {
            TransactionTypeSelector(
                selectedType = uiState.transactionType,
                onTypeSelected = onTransactionTypeChange
            )
        } else {
            TransactionTypeDisplay(uiState.transactionType)
        }

        val isAmountLocked = uiState.selectedCurrency != "EUR"

        com.example.budgetcontrol.ui.components.common.AmountInputCard(
            amount = if (isAmountLocked) uiState.originalAmount.toString() else uiState.amount,
            onAmountChange = onAmountChange,
            transactionType = uiState.transactionType,
            currency = uiState.selectedCurrency,
            readOnly = isAmountLocked,
            hint = if (isAmountLocked)
                stringResource(R.string.amount_locked_hint)
            else null
        )

        com.example.budgetcontrol.ui.components.common.CategorySelector(
            categories = uiState.categories,
            selectedCategory = uiState.selectedCategory,
            onCategorySelect = onCategorySelect,
            transactionType = uiState.transactionType,
            onCreateCategory = onCreateCategory,
            onUpdateCategoryColor = onUpdateCategoryColor,
            onUpdateCategory = onUpdateCategory,
            onDeleteCategory = onDeleteCategory
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

        Spacer(modifier = Modifier.height(8.dp))

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
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White
                )
            } else {
                Text(
                    text = stringResource(R.string.save),
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
            containerColor = MaterialTheme.colorScheme.primary
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
                    text = stringResource(R.string.expenses_upper),
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
                    text = stringResource(R.string.incomes_upper),
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
            containerColor = MaterialTheme.colorScheme.primary
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
                    text = stringResource(R.string.expenses_upper),
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
                    text = stringResource(R.string.incomes_upper),
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
        label = { Text(stringResource(R.string.comment_label)) },
        placeholder = { Text(stringResource(R.string.comment_placeholder)) },
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

@Composable
private fun getTitle(
    mode: TransactionFormMode,
    transactionType: TransactionType,
    categoryName: String?
): String {
    return when (mode) {
        TransactionFormMode.ADD -> {
            when (transactionType) {
                TransactionType.EXPENSE -> stringResource(R.string.add_expense)
                TransactionType.INCOME -> stringResource(R.string.add_income)
            }
        }
        TransactionFormMode.EDIT -> {
            categoryName?.uppercase() ?: when (transactionType) {
                TransactionType.EXPENSE -> stringResource(R.string.edit_expense_upper)
                TransactionType.INCOME -> stringResource(R.string.edit_income_upper)
            }
        }
    }
}

@Composable
private fun getContentTitle(
    mode: TransactionFormMode,
    transactionType: TransactionType
): String {
    return when (mode) {
        TransactionFormMode.ADD -> {
            when (transactionType) {
                TransactionType.EXPENSE -> stringResource(R.string.add_expense)
                TransactionType.INCOME -> stringResource(R.string.add_income)
            }
        }
        TransactionFormMode.EDIT -> {
            when (transactionType) {
                TransactionType.EXPENSE -> stringResource(R.string.edit_expense)
                TransactionType.INCOME -> stringResource(R.string.edit_income)
            }
        }
    }
}
