package com.example.budgetcontrol.core.domain.repository

import com.example.budgetcontrol.core.domain.model.Income
import kotlinx.coroutines.flow.Flow

interface IncomeRepository {
    fun getAllIncomes(): Flow<List<Income>>
    fun getIncomesByDateRange(startDate: Long, endDate: Long): Flow<List<Income>>
    fun getIncomesByCategory(categoryId: String): Flow<List<Income>>
    suspend fun getIncomeById(id: String): Income?
    suspend fun insertIncome(income: Income)
    suspend fun updateIncome(income: Income)
    suspend fun deleteIncome(income: Income)
    suspend fun deleteIncomeById(id: String)
    suspend fun getTotalAmountByDateRange(startDate: Long, endDate: Long): Double
}