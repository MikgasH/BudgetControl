package com.example.budgetcontrol.core.data.repository

import com.example.budgetcontrol.core.data.local.database.dao.IncomeDao
import com.example.budgetcontrol.core.data.mapper.toDomain
import com.example.budgetcontrol.core.data.mapper.toEntity
import com.example.budgetcontrol.core.domain.model.Income
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomeRepositoryImpl @Inject constructor(
    private val incomeDao: IncomeDao
) : IncomeRepository {

    override fun getAllIncomes(): Flow<List<Income>> {
        return incomeDao.getAllIncomes().map { it.toDomain() }
    }

    override fun getIncomesByDateRange(startDate: Long, endDate: Long): Flow<List<Income>> {
        return incomeDao.getIncomesByDateRange(startDate, endDate).map { it.toDomain() }
    }

    override fun getIncomesByCategory(categoryId: String): Flow<List<Income>> {
        return incomeDao.getIncomesByCategory(categoryId).map { it.toDomain() }
    }

    override suspend fun getIncomeById(id: String): Income? {
        return incomeDao.getIncomeById(id)?.toDomain()
    }

    override suspend fun insertIncome(income: Income) {
        incomeDao.insertIncome(income.toEntity())
    }

    override suspend fun updateIncome(income: Income) {
        incomeDao.updateIncome(income.toEntity())
    }

    override suspend fun deleteIncome(income: Income) {
        incomeDao.deleteIncome(income.toEntity())
    }

    override suspend fun deleteIncomeById(id: String) {
        incomeDao.deleteIncomeById(id)
    }

    override suspend fun getTotalAmountByDateRange(startDate: Long, endDate: Long): Double {
        return incomeDao.getTotalAmountByDateRange(startDate, endDate) ?: 0.0
    }

    override suspend fun getMinDate(): Long? = incomeDao.getMinDate()

    override suspend fun getMaxDate(): Long? = incomeDao.getMaxDate()

    override fun getIncomesByAccount(accountId: String): Flow<List<Income>> {
        return incomeDao.getIncomesByAccount(accountId).map { it.toDomain() }
    }

    override fun getIncomesByAccountAndDateRange(accountId: String, startDate: Long, endDate: Long): Flow<List<Income>> {
        return incomeDao.getIncomesByAccountAndDateRange(accountId, startDate, endDate).map { it.toDomain() }
    }

    override fun getIncomesByAccounts(accountIds: List<String>): Flow<List<Income>> {
        return incomeDao.getIncomesByAccounts(accountIds).map { it.toDomain() }
    }

    override fun getIncomesByAccountsAndDateRange(accountIds: List<String>, startDate: Long, endDate: Long): Flow<List<Income>> {
        return incomeDao.getIncomesByAccountsAndDateRange(accountIds, startDate, endDate).map { it.toDomain() }
    }

    override suspend fun reassignIncomes(sourceAccountId: String, targetAccountId: String) {
        incomeDao.reassignIncomes(sourceAccountId, targetAccountId)
    }

    override suspend fun getIncomeCountByAccount(accountId: String): Int {
        return incomeDao.getIncomeCountByAccount(accountId)
    }

    override fun getTotalIncomesBeforeDate(date: Long, accountId: String?): Flow<Double> {
        return incomeDao.getTotalIncomesBeforeDate(date, accountId)
    }

    override fun getTotalIncomesBeforeDateInAccounts(date: Long, accountIds: List<String>): Flow<Double> {
        return incomeDao.getTotalIncomesBeforeDateInAccounts(date, accountIds)
    }
}