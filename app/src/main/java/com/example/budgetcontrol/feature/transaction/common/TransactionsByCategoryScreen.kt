package com.example.budgetcontrol.feature.transaction.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.Account
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.core.util.formatAmount
import com.example.budgetcontrol.core.util.getCurrencySymbol
import com.example.budgetcontrol.ui.components.common.TransactionItemDetailed
import com.example.budgetcontrol.ui.util.displayName
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsByCategoryScreen(
    categoryId: String,
    transactionType: TransactionType,
    startDate: Long? = null,
    endDate: Long? = null,
    accountId: String? = null,
    onBackClick: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onAddTransactionClick: (Long) -> Unit,
    viewModel: TransactionsByCategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val baseCurrency by viewModel.baseCurrency.collectAsState()

    LaunchedEffect(categoryId, transactionType, startDate, endDate, accountId) {
        viewModel.loadTransactions(categoryId, transactionType, startDate, endDate, accountId)
    }

    val isIncome = transactionType == TransactionType.INCOME
    val defaultTitleRes = if (isIncome) R.string.incomes_upper else R.string.expenses_upper
    val addContentDescRes = if (isIncome) R.string.add_income else R.string.add_expense
    val amountPrefix = if (isIncome) "+" else ""

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
                        onClick = onBackClick,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text(
                            text = uiState.category?.displayName()?.uppercase()
                                ?: stringResource(defaultTitleRes),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "$amountPrefix${formatAmount(uiState.totalAmount)} ${getCurrencySymbol(baseCurrency)}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddTransactionClick(System.currentTimeMillis()) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(addContentDescRes),
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            uiState.showError != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.showError ?: stringResource(R.string.unknown_error),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            else -> {
                val dateFormatPattern = stringResource(R.string.date_format_full)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val groupedTransactions = uiState.transactions.groupBy { transaction ->
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = transaction.date
                        SimpleDateFormat(dateFormatPattern, Locale.getDefault()).format(calendar.time)
                    }

                    groupedTransactions.forEach { (date, transactions) ->
                        item {
                            Text(
                                text = date,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(transactions) { transaction ->
                            val txAccountId = when (transaction) {
                                is com.example.budgetcontrol.core.domain.model.Transaction.ExpenseTransaction -> transaction.accountId
                                is com.example.budgetcontrol.core.domain.model.Transaction.IncomeTransaction -> transaction.accountId
                            } ?: Account.DEFAULT_ACCOUNT_ID
                            TransactionItemDetailed(
                                transaction = transaction,
                                category = uiState.category,
                                baseCurrency = baseCurrency,
                                onClick = { onTransactionClick(transaction.id) },
                                accountName = uiState.accountNames[txAccountId]
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}
