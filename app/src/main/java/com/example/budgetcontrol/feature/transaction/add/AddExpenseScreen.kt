package com.example.budgetcontrol.feature.transaction.add

import androidx.compose.runtime.Composable
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.feature.transaction.common.TransactionFormMode
import com.example.budgetcontrol.feature.transaction.common.TransactionFormScreen

@Composable
fun AddExpenseScreen(
    selectedDate: Long = System.currentTimeMillis(),
    preSelectedCategoryId: String? = null,
    onBackClick: () -> Unit,
    onExpenseAdded: () -> Unit
) {
    TransactionFormScreen(
        transactionType = TransactionType.EXPENSE,
        mode = TransactionFormMode.ADD,
        initialDate = selectedDate,
        preSelectedCategoryId = preSelectedCategoryId,
        onBackClick = onBackClick,
        onSuccess = onExpenseAdded
    )
}