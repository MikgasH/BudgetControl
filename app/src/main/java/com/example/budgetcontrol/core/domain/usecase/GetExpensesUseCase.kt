package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.domain.model.Expense
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExpensesUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    operator fun invoke(): Flow<List<Expense>> {
        return repository.getAllExpenses()
    }

    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>> {
        return repository.getExpensesByDateRange(startDate, endDate)
    }

    fun getByCategory(categoryId: String): Flow<List<Expense>> {
        return repository.getExpensesByCategory(categoryId)
    }

    fun getByAccount(accountId: String): Flow<List<Expense>> {
        return repository.getExpensesByAccount(accountId)
    }

    fun getByAccountAndDateRange(accountId: String, startDate: Long, endDate: Long): Flow<List<Expense>> {
        return repository.getExpensesByAccountAndDateRange(accountId, startDate, endDate)
    }

    suspend fun getMinDate(): Long? = repository.getMinDate()

    suspend fun getMaxDate(): Long? = repository.getMaxDate()

    suspend fun getExpenseCountByAccount(accountId: String): Int = repository.getExpenseCountByAccount(accountId)
}