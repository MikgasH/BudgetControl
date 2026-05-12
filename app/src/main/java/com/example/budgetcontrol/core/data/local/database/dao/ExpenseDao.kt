package com.example.budgetcontrol.core.data.local.database.dao

import androidx.room.*
import com.example.budgetcontrol.core.data.local.database.entities.ExpenseEntity
import kotlinx.coroutines.flow.Flow

data class CategorySpend(val categoryId: String, val spent: Double)

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getExpensesByCategory(categoryId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: String): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: String)

    @Query("SELECT SUM(amount) FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalAmountByDateRange(startDate: Long, endDate: Long): Double?

    @Query("SELECT MIN(date) FROM expenses")
    suspend fun getMinDate(): Long?

    @Query("SELECT MAX(date) FROM expenses")
    suspend fun getMaxDate(): Long?

    @Query("SELECT * FROM expenses WHERE accountId = :accountId ORDER BY date DESC")
    fun getExpensesByAccount(accountId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE accountId = :accountId")
    fun getTotalExpensesByAccount(accountId: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE accountId = :accountId AND date BETWEEN :startDate AND :endDate")
    fun getTotalExpensesByAccountAndDateRange(accountId: String, startDate: Long, endDate: Long): Flow<Double>

    @Query("SELECT * FROM expenses WHERE accountId = :accountId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByAccountAndDateRange(accountId: String, startDate: Long, endDate: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE accountId IN (:accountIds) ORDER BY date DESC")
    fun getExpensesByAccounts(accountIds: List<String>): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE accountId IN (:accountIds) AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByAccountsAndDateRange(accountIds: List<String>, startDate: Long, endDate: Long): Flow<List<ExpenseEntity>>

    @Query("UPDATE expenses SET accountId = :targetAccountId WHERE accountId = :sourceAccountId")
    suspend fun reassignExpenses(sourceAccountId: String, targetAccountId: String)

    @Query("SELECT COUNT(*) FROM expenses WHERE accountId = :accountId")
    suspend fun getExpenseCountByAccount(accountId: String): Int

    @Query(
        "SELECT COALESCE(SUM(amount), 0.0) FROM expenses " +
                "WHERE date < :date AND (:accountId IS NULL OR accountId = :accountId)"
    )
    fun getTotalExpensesBeforeDate(date: Long, accountId: String?): Flow<Double>

    @Query(
        "SELECT COALESCE(SUM(amount), 0.0) FROM expenses " +
                "WHERE date < :date AND accountId IN (:accountIds)"
    )
    fun getTotalExpensesBeforeDateInAccounts(date: Long, accountIds: List<String>): Flow<Double>

    @Query(
        """
        SELECT categoryId, COALESCE(SUM(amount), 0.0) AS spent
        FROM expenses
        WHERE date BETWEEN :periodStart AND :periodEnd
        GROUP BY categoryId
        """
    )
    fun getSpentByCategoryInRange(periodStart: Long, periodEnd: Long): Flow<List<CategorySpend>>
}