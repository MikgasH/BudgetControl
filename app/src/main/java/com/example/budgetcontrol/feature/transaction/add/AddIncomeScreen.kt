package com.example.budgetcontrol.feature.transaction.add

import androidx.compose.runtime.Composable
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.feature.transaction.common.TransactionFormMode
import com.example.budgetcontrol.feature.transaction.common.TransactionFormScreen

@Composable
fun AddIncomeScreen(
    selectedDate: Long = System.currentTimeMillis(),
    preSelectedAccountId: String? = null,
    onBackClick: () -> Unit,
    onIncomeAdded: () -> Unit
) {
    TransactionFormScreen(
        transactionType = TransactionType.INCOME,
        mode = TransactionFormMode.ADD,
        initialDate = selectedDate,
        preSelectedAccountId = preSelectedAccountId,
        onBackClick = onBackClick,
        onSuccess = onIncomeAdded
    )
}