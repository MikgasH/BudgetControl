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

    fun getByAccounts(accountIds: List<String>): Flow<List<Income>> {
        return repository.getIncomesByAccounts(accountIds)
    }

    fun getByAccountsAndDateRange(accountIds: List<String>, startDate: Long, endDate: Long): Flow<List<Income>> {
        return repository.getIncomesByAccountsAndDateRange(accountIds, startDate, endDate)
    }

    suspend fun getMinDate(): Long? = repository.getMinDate()

    suspend fun getMaxDate(): Long? = repository.getMaxDate()

    suspend fun getIncomeCountByAccount(accountId: String): Int = repository.getIncomeCountByAccount(accountId)

    fun getTotalBeforeDate(date: Long, accountId: String?): Flow<Double> =
        repository.getTotalIncomesBeforeDate(date, accountId)

    fun getTotalBeforeDateInAccounts(date: Long, accountIds: List<String>): Flow<Double> =
        repository.getTotalIncomesBeforeDateInAccounts(date, accountIds)
}