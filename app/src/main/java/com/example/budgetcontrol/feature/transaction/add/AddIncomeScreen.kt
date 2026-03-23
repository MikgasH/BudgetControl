package com.example.budgetcontrol.feature.transaction.add

import androidx.compose.runtime.Composable
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.feature.transaction.common.TransactionFormMode
import com.example.budgetcontrol.feature.transaction.common.TransactionFormScreen

@Composable
fun AddIncomeScreen(
    selectedDate: Long = System.currentTimeMillis(),
    onBackClick: () -> Unit,
    onIncomeAdded: () -> Unit
) {
    TransactionFormScreen(
        transactionType = TransactionType.INCOME,
        mode = TransactionFormMode.ADD,
        initialDate = selectedDate,
        onBackClick = onBackClick,
        onSuccess = onIncomeAdded
    )
}