package com.example.budgetcontrol.core.data.repository

import com.example.budgetcontrol.core.data.local.database.AppDatabase
import com.example.budgetcontrol.core.data.local.database.dao.BankDao
import com.example.budgetcontrol.core.data.local.database.entities.BankEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankRepository @Inject constructor(
    private val bankDao: BankDao
) {
    fun getAllBanks(): Flow<List<BankEntity>> = bankDao.getAllBanks()

    fun getFavoriteBanks(): Flow<List<BankEntity>> = bankDao.getFavoriteBanks()

    suspend fun insertBank(bank: BankEntity) = bankDao.insertBank(bank)

    suspend fun updateBank(bank: BankEntity) = bankDao.updateBank(bank)

    suspend fun deleteBank(bank: BankEntity) = bankDao.deleteBank(bank)

    suspend fun deleteAllBanks() = bankDao.deleteAllBanks()

    suspend fun insertDefaultBanks() {
        AppDatabase.DEFAULT_BANKS.forEach { bank ->
            bankDao.insertBank(bank)
        }
    }
}

