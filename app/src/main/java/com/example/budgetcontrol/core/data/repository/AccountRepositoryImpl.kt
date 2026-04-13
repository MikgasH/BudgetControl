package com.example.budgetcontrol.core.data.repository

import com.example.budgetcontrol.core.data.local.database.dao.AccountDao
import com.example.budgetcontrol.core.data.mapper.toDomain
import com.example.budgetcontrol.core.data.mapper.toEntity
import com.example.budgetcontrol.core.domain.model.Account
import com.example.budgetcontrol.core.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun getAllAccounts(): Flow<List<Account>> =
        accountDao.getAllAccounts().map { it.toDomain() }

    override suspend fun getAllAccountsList(): List<Account> =
        accountDao.getAllAccountsList().toDomain()

    override fun searchAccounts(query: String): Flow<List<Account>> =
        accountDao.searchAccounts(query).map { it.toDomain() }

    override suspend fun getAccountById(id: String): Account? =
        accountDao.getAccountById(id)?.toDomain()

    override suspend fun insertAccount(account: Account) =
        accountDao.insertAccount(account.toEntity())

    override suspend fun updateAccount(account: Account) =
        accountDao.updateAccount(account.toEntity())

    override suspend fun deleteAccount(account: Account) =
        accountDao.deleteAccount(account.toEntity())

    override suspend fun updateLastUsedAt(id: String, timestamp: Long) =
        accountDao.updateLastUsedAt(id, timestamp)
}
