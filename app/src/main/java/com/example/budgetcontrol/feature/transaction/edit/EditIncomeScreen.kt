package com.example.budgetcontrol.feature.transaction.edit

import androidx.compose.runtime.Composable
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.feature.transaction.common.TransactionFormMode
import com.example.budgetcontrol.feature.transaction.common.TransactionFormScreen

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