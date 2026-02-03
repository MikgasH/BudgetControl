package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.domain.model.Expense
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import javax.inject.Inject

class DeleteExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(expense: Expense) {
        repository.deleteExpense(expense)
    }

    suspend fun deleteById(id: String) {
        repository.deleteExpenseById(id)
    }
}