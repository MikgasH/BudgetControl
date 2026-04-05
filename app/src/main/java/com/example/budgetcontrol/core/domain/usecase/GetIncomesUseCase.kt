package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.domain.model.Income
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetIncomesUseCase @Inject constructor(
    private val repository: IncomeRepository
) {
    operator fun invoke(): Flow<List<Income>> {
        return repository.getAllIncomes()
    }

    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<Income>> {
        return repository.getIncomesByDateRange(startDate, endDate)
    }

    fun getByCategory(categoryId: String): Flow<List<Income>> {
        return repository.getIncomesByCategory(categoryId)
    }

    fun getByAccount(accountId: String): Flow<List<Income>> {
        return repository.getIncomesByAccount(accountId)
    }

    fun getByAccountAndDateRange(accountId: String, startDate: Long, endDate: Long): Flow<List<Income>> {
        return repository.getIncomesByAccountAndDateRange(accountId, startDate, endDate)
    }

    suspend fun getMinDate(): Long? = repository.getMinDate()

    suspend fun getMaxDate(): Long? = repository.getMaxDate()

    suspend fun getIncomeCountByAccount(accountId: String): Int = repository.getIncomeCountByAccount(accountId)
}