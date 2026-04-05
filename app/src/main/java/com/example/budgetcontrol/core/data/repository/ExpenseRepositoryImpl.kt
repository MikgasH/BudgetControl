package com.example.budgetcontrol.core.data.repository

import com.example.budgetcontrol.core.data.local.database.dao.ExpenseDao
import com.example.budgetcontrol.core.data.mapper.toDomain
import com.example.budgetcontrol.core.data.mapper.toEntity
import com.example.budgetcontrol.core.domain.model.Expense
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao
) : ExpenseRepository {

    override fun getAllExpenses(): Flow<List<Expense>> {
        return expenseDao.getAllExpenses().map { it.toDomain() }
    }

    override fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesByDateRange(startDate, endDate).map { it.toDomain() }
    }

    override fun getExpensesByCategory(categoryId: String): Flow<List<Expense>> {
        return expenseDao.getExpensesByCategory(categoryId).map { it.toDomain() }
    }

    override suspend fun getExpenseById(id: String): Expense? {
        return expenseDao.getExpenseById(id)?.toDomain()
    }

    override suspend fun insertExpense(expense: Expense) {
        expenseDao.insertExpense(expense.toEntity())
    }

    override suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense.toEntity())
    }

    override suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense.toEntity())
    }

    override suspend fun deleteExpenseById(id: String) {
        expenseDao.deleteExpenseById(id)
    }

    override suspend fun getTotalAmountByDateRange(startDate: Long, endDate: Long): Double {
        return expenseDao.getTotalAmountByDateRange(startDate, endDate) ?: 0.0
    }

    override suspend fun getMinDate(): Long? = expenseDao.getMinDate()

    override suspend fun getMaxDate(): Long? = expenseDao.getMaxDate()

    override fun getExpensesByAccount(accountId: String): Flow<List<Expense>> {
        return expenseDao.getExpensesByAccount(accountId).map { it.toDomain() }
    }
}