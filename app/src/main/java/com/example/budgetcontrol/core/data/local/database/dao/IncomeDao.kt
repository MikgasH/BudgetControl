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
}