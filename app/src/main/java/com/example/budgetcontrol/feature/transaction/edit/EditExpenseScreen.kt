package com.example.budgetcontrol.feature.transaction.edit

import androidx.compose.runtime.Composable
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.feature.transaction.common.TransactionFormMode
import com.example.budgetcontrol.feature.transaction.common.TransactionFormScreen

/**
 * Экран редактирования расхода
 * Теперь простая обертка над универсальным TransactionFormScreen
 */
@Composable
fun EditExpenseScreen(
    expenseId: String,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    TransactionFormScreen(
        transactionType = TransactionType.EXPENSE,
        mode = TransactionFormMode.EDIT,
        transactionId = expenseId,
        onBackClick = onBackClick,
        onSuccess = onSaveSuccess
    )
}