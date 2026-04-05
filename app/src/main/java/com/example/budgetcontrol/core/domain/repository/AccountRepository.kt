package com.example.budgetcontrol.core.domain.repository

import com.example.budgetcontrol.core.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAllAccounts(): Flow<List<Account>>
    fun searchAccounts(query: String): Flow<List<Account>>
    suspend fun getAccountById(id: String): Account?
    suspend fun insertAccount(account: Account)
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(account: Account)
    suspend fun updateLastUsedAt(id: String, timestamp: Long = System.currentTimeMillis())
}
