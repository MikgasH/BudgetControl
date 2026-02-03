package com.example.budgetcontrol.feature.transaction.edit

import androidx.compose.runtime.Composable
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.feature.transaction.common.TransactionFormMode
import com.example.budgetcontrol.feature.transaction.common.TransactionFormScreen

/**
 * Экран редактирования дохода
 * Теперь простая обертка над универсальным TransactionFormScreen
 */
@Composable
fun EditIncomeScreen(
    incomeId: String,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    TransactionFormScreen(
        transactionType = TransactionType.INCOME,
        mode = TransactionFormMode.EDIT,
        transactionId = incomeId,
        onBackClick = onBackClick,
        onSuccess = onSaveSuccess
    )
}