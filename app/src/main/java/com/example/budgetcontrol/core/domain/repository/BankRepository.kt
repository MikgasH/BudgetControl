package com.example.budgetcontrol.core.domain.repository

import com.example.budgetcontrol.core.domain.model.Bank
import kotlinx.coroutines.flow.Flow

interface BankRepository {
    fun getAllBanks(): Flow<List<Bank>>
    fun getFavoriteBanks(): Flow<List<Bank>>
    suspend fun insertBank(bank: Bank)
    suspend fun updateBank(bank: Bank)
    suspend fun deleteBank(bank: Bank)
    suspend fun deleteAllBanks()
    suspend fun insertDefaultBanks()
}
