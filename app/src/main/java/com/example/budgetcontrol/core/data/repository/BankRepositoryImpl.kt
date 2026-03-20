package com.example.budgetcontrol.core.data.repository

import com.example.budgetcontrol.core.data.local.database.AppDatabase
import com.example.budgetcontrol.core.data.local.database.dao.BankDao
import com.example.budgetcontrol.core.data.mapper.toDomain
import com.example.budgetcontrol.core.data.mapper.toEntity
import com.example.budgetcontrol.core.domain.model.Bank
import com.example.budgetcontrol.core.domain.repository.BankRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankRepositoryImpl @Inject constructor(
    private val bankDao: BankDao
) : BankRepository {
    override fun getAllBanks(): Flow<List<Bank>> = bankDao.getAllBanks().map { it.toDomain() }

    override fun getFavoriteBanks(): Flow<List<Bank>> = bankDao.getFavoriteBanks().map { it.toDomain() }

    override suspend fun insertBank(bank: Bank) = bankDao.insertBank(bank.toEntity())

    override suspend fun updateBank(bank: Bank) = bankDao.updateBank(bank.toEntity())

    override suspend fun deleteBank(bank: Bank) = bankDao.deleteBank(bank.toEntity())

    override suspend fun deleteAllBanks() = bankDao.deleteAllBanks()

    override suspend fun insertDefaultBanks() {
        AppDatabase.DEFAULT_BANKS.forEach { bank ->
            bankDao.insertBank(bank)
        }
    }
}
