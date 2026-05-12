package com.example.budgetcontrol.core.domain.repository

import com.example.budgetcontrol.core.data.local.database.dao.CategorySpend
import com.example.budgetcontrol.core.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getAllExpenses(): Flow<List<Expense>>
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>>
    fun getExpensesByCategory(categoryId: String): Flow<List<Expense>>
    suspend fun getExpenseById(id: String): Expense?
    suspend fun insertExpense(expense: Expense)
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(expense: Expense)
    suspend fun deleteExpenseById(id: String)
    suspend fun getTotalAmountByDateRange(startDate: Long, endDate: Long): Double
    suspend fun getMinDate(): Long?
    suspend fun getMaxDate(): Long?
    fun getExpensesByAccount(accountId: String): Flow<List<Expense>>
    fun getExpensesByAccountAndDateRange(accountId: String, startDate: Long, endDate: Long): Flow<List<Expense>>
    fun getExpensesByAccounts(accountIds: List<String>): Flow<List<Expense>>
    fun getExpensesByAccountsAndDateRange(accountIds: List<String>, startDate: Long, endDate: Long): Flow<List<Expense>>
    suspend fun reassignExpenses(sourceAccountId: String, targetAccountId: String)
    suspend fun getExpenseCountByAccount(accountId: String): Int
    fun getTotalExpensesBeforeDate(date: Long, accountId: String?): Flow<Double>
    fun getTotalExpensesBeforeDateInAccounts(date: Long, accountIds: List<String>): Flow<Double>
    fun getSpentByCategoryInRange(periodStart: Long, periodEnd: Long): Flow<List<CategorySpend>>
}