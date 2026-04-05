package com.example.budgetcontrol.core.data.local.database.dao

import androidx.room.*
import com.example.budgetcontrol.core.data.local.database.entities.IncomeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {

    @Query("SELECT * FROM incomes ORDER BY date DESC")
    fun getAllIncomes(): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getIncomesByDateRange(startDate: Long, endDate: Long): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getIncomesByCategory(categoryId: String): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes WHERE id = :id")
    suspend fun getIncomeById(id: String): IncomeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeEntity)

    @Update
    suspend fun updateIncome(income: IncomeEntity)

    @Delete
    suspend fun deleteIncome(income: IncomeEntity)

    @Query("DELETE FROM incomes WHERE id = :id")
    suspend fun deleteIncomeById(id: String)

    @Query("SELECT SUM(amount) FROM incomes WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalAmountByDateRange(startDate: Long, endDate: Long): Double?

    @Query("SELECT MIN(date) FROM incomes")
    suspend fun getMinDate(): Long?

    @Query("SELECT MAX(date) FROM incomes")
    suspend fun getMaxDate(): Long?

    @Query("SELECT * FROM incomes WHERE accountId = :accountId ORDER BY date DESC")
    fun getIncomesByAccount(accountId: String): Flow<List<IncomeEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM incomes WHERE accountId = :accountId")
    fun getTotalIncomesByAccount(accountId: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM incomes WHERE accountId = :accountId AND date BETWEEN :startDate AND :endDate")
    fun getTotalIncomesByAccountAndDateRange(accountId: String, startDate: Long, endDate: Long): Flow<Double>

    @Query("SELECT * FROM incomes WHERE accountId = :accountId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getIncomesByAccountAndDateRange(accountId: String, startDate: Long, endDate: Long): Flow<List<IncomeEntity>>

    @Query("UPDATE incomes SET accountId = :targetAccountId WHERE accountId = :sourceAccountId")
    suspend fun reassignIncomes(sourceAccountId: String, targetAccountId: String)

    @Query("SELECT COUNT(*) FROM incomes WHERE accountId = :accountId")
    suspend fun getIncomeCountByAccount(accountId: String): Int
}