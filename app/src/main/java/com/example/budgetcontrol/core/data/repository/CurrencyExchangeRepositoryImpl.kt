package com.example.budgetcontrol.core.data.repository

import com.example.budgetcontrol.core.data.local.database.dao.CurrencyExchangeDao
import com.example.budgetcontrol.core.data.mapper.toDomain
import com.example.budgetcontrol.core.data.mapper.toEntity
import com.example.budgetcontrol.core.domain.model.CurrencyExchange
import com.example.budgetcontrol.core.domain.repository.CurrencyExchangeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyExchangeRepositoryImpl @Inject constructor(
    private val currencyExchangeDao: CurrencyExchangeDao
) : CurrencyExchangeRepository {

    override fun getAllExchanges(): Flow<List<CurrencyExchange>> {
        return currencyExchangeDao.getAllExchanges().map { it.toDomain() }
    }

    override suspend fun getLatestExchangeForCurrency(
        fromCurrency: String,
        toCurrency: String
    ): CurrencyExchange? {
        return currencyExchangeDao.getLatestExchangeForCurrency(fromCurrency, toCurrency)?.toDomain()
    }

    override suspend fun insertExchange(exchange: CurrencyExchange) {
        currencyExchangeDao.insertExchange(exchange.toEntity())
    }

    override suspend fun deleteExchange(id: String) {
        currencyExchangeDao.deleteExchange(id)
    }
}
