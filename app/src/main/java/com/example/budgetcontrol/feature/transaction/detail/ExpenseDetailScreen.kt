package com.example.budgetcontrol.feature.transaction.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.ui.components.common.TransactionDetailContent
import com.example.budgetcontrol.feature.transaction.common.TransactionDetailViewModel
import com.example.budgetcontrol.core.theme.AppBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    expenseId: String,
    onBackClick: () -> Unit,
    onEditClick: (String) -> Unit,
    onDeleteSuccess: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Загружаем транзакцию при создании экрана
    LaunchedEffect(expenseId) {
        viewModel.loadTransaction(expenseId, TransactionType.EXPENSE)
    }

    // Отслеживаем успешное удаление
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onDeleteSuccess()
        }
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
                            top = 60.dp, // ЕДИНЫЙ отступ сверху
                            bottom = 10.dp // ЕДИНЫЙ отступ снизу
                        )
                ) {
                    // Кнопка назад слева
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Заголовок по центру
                    Text(
                        text = "ДЕТАЛИ ОПЕРАЦИИ",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    // Кнопка редактирования справа
                    IconButton(
                        onClick = { onEditClick(expenseId) },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Редактировать",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
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
                    CircularProgressIndicator(color = AppBlue)
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
                            text = uiState.showError ?: "Неизвестная ошибка",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            else -> {
                uiState.transaction?.let { transaction ->
                    TransactionDetailContent(
                        transaction = transaction,
                        category = uiState.category,
                        onDeleteClick = viewModel::deleteTransaction,
                        modifier = Modifier.padding(paddingValues)
                    )
                } ?: run {
                    // Если транзакция не загрузилась, показываем ошибку
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
                                text = "Транзакция не найдена",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}