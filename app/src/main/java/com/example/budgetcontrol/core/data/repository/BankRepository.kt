package com.example.budgetcontrol.core.data.repository

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
}

